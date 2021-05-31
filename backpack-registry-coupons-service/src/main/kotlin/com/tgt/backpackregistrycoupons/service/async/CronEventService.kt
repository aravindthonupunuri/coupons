package com.tgt.backpackregistrycoupons.service.async

import com.tgt.backpackregistryclient.client.BackpackRegistryClient
import com.tgt.backpackregistryclient.transport.RegistryDetailsResponseTO
import com.tgt.backpackregistryclient.util.RecipientType
import com.tgt.backpackregistryclient.util.RegistryChannel
import com.tgt.backpackregistryclient.util.RegistrySubChannel
import com.tgt.backpackregistryclient.util.RegistryType
import com.tgt.backpackregistrycoupons.domain.CouponAssignmentCalculationManager
import com.tgt.backpackregistrycoupons.domain.model.RegistryCoupons
import com.tgt.backpackregistrycoupons.persistence.repository.compositetransaction.CompositeTransactionalRepository
import com.tgt.backpackregistrycoupons.persistence.repository.coupons.CouponsRepository
import com.tgt.backpackregistrycoupons.persistence.repository.registry.RegistryRepository
import com.tgt.backpackregistrycoupons.transport.CompletionCouponNotificationTO
import com.tgt.backpackregistrycoupons.util.CouponRedemptionStatus
import com.tgt.backpackregistrycoupons.util.CouponType
import com.tgt.backpackregistrycoupons.util.RegistryCouponsConstant.COMPLETION_COUPON
import com.tgt.backpackregistrycoupons.util.RegistryCouponsConstant.GIFT_REGISTRY
import com.tgt.backpackregistrycoupons.util.RegistryCouponsConstant.PROFILE
import com.tgt.lists.atlas.api.type.LIST_STATE
import com.tgt.lists.atlas.api.util.ClientConstants
import com.tgt.notification.tracer.client.model.NotificationTracerEvent
import io.micronaut.context.annotation.Value
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CronEventService(
    @Inject val couponAssignmentCalculationManager: CouponAssignmentCalculationManager,
    @Inject val compositeTransactionalRepository: CompositeTransactionalRepository,
    @Inject val sendGuestNotificationsService: SendGuestNotificationsService,
    @Inject val registryRepository: RegistryRepository,
    @Inject val couponsRepository: CouponsRepository,
    @Inject val backpackClient: BackpackRegistryClient,
    @Value("\${registry.completion-coupon.expiration-days}") val couponExpirationDays: Long,
    @Value("\${registry.completion-coupon.db-fetch-size}") val couponDbReadLimit: Int
) {
    private val logger = KotlinLogging.logger { CronEventService::class.java.name }
    private final val registryCouponTypeMap = hashMapOf<RegistryType, List<CouponType>>()

    init {
        registryCouponTypeMap[RegistryType.BABY] = listOf(CouponType.ONLINE, CouponType.STORE)
        registryCouponTypeMap[RegistryType.WEDDING] = listOf(CouponType.ONLINE, CouponType.STORE)
    }

    fun processCronEvent(cronEventDate: LocalDateTime): Mono<Boolean> {
        val readMoreDBRecords = AtomicBoolean(false)
        return registryRepository.findByRegistryStatusAndCouponAssignmentComplete(LIST_STATE.ACTIVE.value, false, couponDbReadLimit).collectList()
            .flatMap { registryList ->
                logger.debug("[processCronEvent], Registry's being assigned coupon code: ${registryList.size} ")
                readMoreDBRecords.set(registryList.size == couponDbReadLimit)
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
            .switchIfEmpty {
                readMoreDBRecords.set(false)
                Mono.just(true)
            }
            .repeat {
                readMoreDBRecords.get()
            }
            .collectList()
            .map { resultList ->
                val failure = resultList.firstOrNull() {
                    !it
                }
                failure == null
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
                val eventDate = it.eventDate
                val couponExpirationDate = eventDate.plusDays(couponExpirationDays)
                couponsRepository.findTop1ByCouponTypeAndRegistryTypeAndCouponExpiryDateGreaterThanEquals(couponType, registryType, couponExpirationDate)
                    .map { coupon ->
                        registryCoupons.add(
                            RegistryCoupons(
                                couponCode = coupon.couponCode,
                                registry = it,
                                couponType = coupon.couponType,
                                couponRedemptionStatus = CouponRedemptionStatus.AVAILABLE,
                                couponIssueDate = LocalDate.now(),
                                couponExpiryDate = eventDate.plusDays(couponExpirationDays) // Coupon expiration will always be event date plus 180 days
                            ) // irrespective of the actual coupon expiration date
                        )
                        true
                    }
                    .switchIfEmpty {
                        logger.error("[processAssignCouponCode], Empty response getting coupon code of type: $couponType for registry $registryId with expiration $couponExpirationDate of type: $registryType")
                        Mono.just(false)
                    }
                    .onErrorResume {
                        logger.error("[processAssignCouponCode], Exception getting coupon code of type: $couponType for registry $registryId expiration $couponExpirationDate of type: $registryType")
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
            compositeTransactionalRepository.assignCoupons(registryCoupons)
                .onErrorResume {
                    logger.error("[assignCouponCode], Exception assigning coupon for registry $registryId")
                    Mono.just(false)
                }
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
            backpackClient.getRegistryDetails(ClientConstants.SYSTEM_PROFILE_ID, registryId, 3991L, RegistryChannel.WEB, RegistrySubChannel.TGTWEB, false)
                .flatMap { sendNotification(it, registryId, registryCoupons) }
                .onErrorResume {
                    logger.error("[sendGuestNotification] Exception finding registry $registryId while sending guest notification for completion coupons", it)
                    Mono.just(true)
                }
                .switchIfEmpty {
                    logger.debug("[sendGuestNotification] No registry found with registryId $registryId while sending guest notification for completion coupons")
                    Mono.just(true)
                }
        }
    }

    private fun sendNotification(
        registryDetailsResponseTO: RegistryDetailsResponseTO,
        registryId: UUID,
        registryCoupons: List<RegistryCoupons>
    ): Mono<Boolean> {
        val expirationDate = registryCoupons.first().couponExpiryDate
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
                firstName = registryDetailsResponseTO.recipients?.first {
                    it.recipientType == RecipientType.REGISTRANT }?.firstName,
                lastName = registryDetailsResponseTO.recipients?.first {
                    it.recipientType == RecipientType.REGISTRANT }?.firstName,
                emailAddress = registryDetailsResponseTO.emailAddress,
                registryId = registryId.toString(),
                onlinePromoCode = registryOnlineCouponCode,
                storeCouponCode = registryStoreCouponCode,
                expirationDate = expirationDate.toString()
            )
        )
        return sendGuestNotificationsService.sendGuestNotifications(notificationTracerEvent, registryId.toString())
            .onErrorResume {
                logger.error("[sendNotification], Exception sending guest notification for registry $registryId")
                Mono.just(true)
            }
    }
}
