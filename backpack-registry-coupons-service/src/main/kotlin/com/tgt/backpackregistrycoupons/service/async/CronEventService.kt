package com.tgt.backpackregistrycoupons.service.async

import com.tgt.backpackregistryclient.util.RegistryType
import com.tgt.backpackregistrycoupons.domain.CouponAssignmentCalculationManager
import com.tgt.backpackregistrycoupons.domain.model.RegistryCoupons
import com.tgt.backpackregistrycoupons.persistence.repository.coupons.CouponsRepository
import com.tgt.backpackregistrycoupons.persistence.repository.registry.RegistryRepository
import com.tgt.backpackregistrycoupons.persistence.repository.registrycoupons.RegistryCouponsRepository
import com.tgt.backpackregistrycoupons.transport.CompletionCouponNotificationTO
import com.tgt.backpackregistrycoupons.util.CouponRedemptionStatus
import com.tgt.backpackregistrycoupons.util.CouponType
import com.tgt.backpackregistrycoupons.util.RegistryCouponsConstant.COMPLETION_COUPON
import com.tgt.backpackregistrycoupons.util.RegistryCouponsConstant.GIFT_REGISTRY
import com.tgt.backpackregistrycoupons.util.RegistryCouponsConstant.PROFILE
import com.tgt.lists.atlas.api.type.LIST_STATE
import com.tgt.notification.tracer.client.model.NotificationTracerEvent
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CronEventService(
    @Inject val couponAssignmentCalculationManager: CouponAssignmentCalculationManager,
    @Inject val sendGuestNotificationsService: SendGuestNotificationsService,
    @Inject val registryCouponsRepository: RegistryCouponsRepository,
    @Inject val registryRepository: RegistryRepository,
    @Inject val couponsRepository: CouponsRepository
) {
    private val logger = KotlinLogging.logger { CronEventService::class.java.name }
    private final val registryCouponTypeMap = hashMapOf<RegistryType, List<CouponType>>()

    init {
        registryCouponTypeMap[RegistryType.BABY] = listOf(CouponType.ONLINE, CouponType.STORE)
        registryCouponTypeMap[RegistryType.WEDDING] = listOf(CouponType.ONLINE, CouponType.STORE)
    }

    fun processCronEvent(cronEventDate: LocalDateTime): Mono<Boolean> {
        return registryRepository.findByRegistryStatusAndCouponAssignmentComplete(LIST_STATE.ACTIVE.value, false).collectList()
            .flatMap { registryList ->
                logger.debug("[processCronEvent], Registry's being assigned coupon code: ${registryList.size} ")
                Flux.fromIterable(registryList).flatMap { registry ->
                    val couponAssignmentDate = couponAssignmentCalculationManager.calculateCouponAssignmentDate(registry)
                    if (cronEventDate.isAfter(couponAssignmentDate)) {
                        processAssignCouponCode(registry.registryId).flatMap { sendGuestNotification(registry.registryId, it) }
                    } else {
                        logger.debug("[processCronEvent], Skipping coupon assignment for Registry's ${registry.registryId} with couponAssignmentDate $couponAssignmentDate")
                        Mono.just(true)
                    }
                }.collectList().map {
                    logger.debug("[processCronEvent], Registry coupon assignment cron event complete")
                    true
                }
            }
            .onErrorResume {
                logger.error("[processCronEvent], Assigning coupon codes, sending for retry")
                Mono.just(false)
            }
    }

    private fun processAssignCouponCode(registryId: UUID): Mono<List<RegistryCoupons>> {
        return registryRepository.getByRegistryId(registryId).flatMap {
            val validCouponTypes: List<CouponType> = registryCouponTypeMap[it.registryType] ?: emptyList()
            val assignedCouponTypes: List<CouponType> = it.registryCoupons?.map { it.couponType } ?: emptyList()
            val unAssignedCouponTypes: List<CouponType> = validCouponTypes.filter { !assignedCouponTypes.contains(it) }
            val registryCoupons = arrayListOf<RegistryCoupons>()
            logger.debug("[processAssignCouponCode ], UnAssignedCouponTypes: $unAssignedCouponTypes for registry: $registryId")

            Flux.fromIterable(unAssignedCouponTypes).flatMap { couponType ->
                val registryType = it.registryType
                couponsRepository.findTop1ByCouponTypeAndRegistryType(couponType, registryType)
                    .map { coupon ->
                        registryCoupons.add(
                            RegistryCoupons(
                                couponCode = coupon.couponCode,
                                registry = it,
                                couponType = coupon.couponType,
                                couponRedemptionStatus = CouponRedemptionStatus.AVAILABLE,
                                couponIssueDate = LocalDate.now(),
                                couponExpiryDate = coupon.couponExpiryDate
                            )
                        )
                        true
                    }
                    .switchIfEmpty {
                        logger.info("[processAssignCouponCode], Empty response getting coupon code of type: $couponType for registry $registryId of type: $registryType")
                        Mono.just(false)
                    }
                    .onErrorResume {
                        logger.error("[processAssignCouponCode], Exception getting coupon code of type: $couponType for registry $registryId of type: $registryType")
                        Mono.just(false)
                    }
            }.collectList().flatMap {
                assignCouponCode(registryId, registryCoupons, it).map { registryCoupons }
            }
        }
    }

    private fun assignCouponCode(
        registryId: UUID,
        registryCoupons: List<RegistryCoupons>,
        couponsAllocation: List<Boolean>
    ): Mono<Boolean> {
        return if (registryCoupons.isNullOrEmpty()) {
            logger.error("[assignCouponCode], Registry $registryId already has been assigned with valid coupon types so skipping coupon assignment process and updating coupon assignment flag to complete")
            registryRepository.updateCouponAssignmentComplete(registryId, true).map { true }
        } else {
            registryCouponsRepository.saveAll(registryCoupons).collectList()
                .flatMap { couponsRepository.deleteByCouponCodeInList(registryCoupons.map { it.couponCode!! }) }
                .flatMap {
                    if (couponsAllocation.contains(false)) {
                        logger.debug("[assignCouponCode], Partial coupon assignment for Registry $registryId so not updating coupon assignment flag to complete")
                        Mono.just(true)
                    } else {
                        registryRepository.updateCouponAssignmentComplete(registryId, true).map { true }
                    }
                }
        }
    }

    private fun sendGuestNotification(registryId: UUID, registryCoupons: List<RegistryCoupons>): Mono<Boolean> {
        return if (registryCoupons.isNullOrEmpty()) {
            logger.debug("[sendGuestNotification], Coupons not assigned for registry $registryId skipping notification process")
            Mono.just(true)
        } else {
            val expirationDate = registryCoupons.first().couponExpiryDate // TODO: which coupon expiration should be sent out for notification
            var registryStoreCouponCode: String? = null
            var registryOnlineCouponCode: String? = null

            registryCoupons.map {
                if (it.couponType == CouponType.STORE) {
                    registryStoreCouponCode = it.couponCode!!
                } else {
                    registryOnlineCouponCode = it.couponCode!!
                }
            }
            val notificationTracerEvent = NotificationTracerEvent(
                scenarioName = COMPLETION_COUPON,
                id = registryId.toString(),
                idType = PROFILE,
                origin = GIFT_REGISTRY,
                transactionId = registryId.toString(),
                earliestSendTime = null,
                latestSendTime = null,
                data = CompletionCouponNotificationTO(
                    firstName = null, // TODO: find guest name
                    lastName = null,
                    emailAddress = null,
                    registryId = registryId.toString(),
                    onlinePromoCode = registryOnlineCouponCode,
                    storeCouponCode = registryStoreCouponCode,
                    expirationDate = expirationDate.toString()
                )
            )
            sendGuestNotificationsService.sendGuestNotifications(notificationTracerEvent, registryId.toString())
        }
    }
}
