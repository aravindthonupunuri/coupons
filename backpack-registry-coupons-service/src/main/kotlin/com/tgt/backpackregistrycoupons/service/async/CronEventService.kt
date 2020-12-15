package com.tgt.backpackregistrycoupons.service.async

import com.tgt.backpackregistryclient.util.RegistryType
import com.tgt.backpackregistrycoupons.domain.CouponAssignmentCalculationManager
import com.tgt.backpackregistrycoupons.domain.model.RegistryCoupons
import com.tgt.backpackregistrycoupons.persistence.repository.coupons.CouponsRepository
import com.tgt.backpackregistrycoupons.persistence.repository.registry.RegistryRepository
import com.tgt.backpackregistrycoupons.persistence.repository.registrycoupons.RegistryCouponsRepository
import com.tgt.backpackregistrycoupons.util.CouponRedemptionStatus
import com.tgt.backpackregistrycoupons.util.CouponType
import com.tgt.lists.atlas.api.type.LIST_STATE
import com.tgt.notification.tracer.client.model.NotificationTracerEvent
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
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

    fun processCronEvent(): Mono<Boolean> {
        return registryRepository.findByRegistryStatusAndCouponAssignmentComplete(LIST_STATE.ACTIVE.value, false).collectList()
            .flatMap { registryList ->
                Flux.fromIterable(registryList).flatMap {
                    val couponAssignmentDate = couponAssignmentCalculationManager.calculateCouponAssignmentDate(it)
                    if (LocalDateTime.now().isAfter(couponAssignmentDate)) {
                        assignCouponCode(it.registryId).flatMap { sendGuestNotification(it) }
                    } else {
                        Mono.just(true)
                    }
                }.collectList().map { true }
            }
    }

    fun assignCouponCode(registryId: UUID): Mono<List<RegistryCoupons>> {
        return registryRepository.getByRegistryId(registryId).flatMap {
            val validCouponTypes: List<CouponType> = registryCouponTypeMap[it.registryType] ?: emptyList()
            val assignedCouponTypes: List<CouponType> = it.registryCoupons?.map { it.couponType } ?: emptyList()
            val registryCoupons = arrayListOf<RegistryCoupons>()

            Flux.fromIterable(validCouponTypes).flatMap { validCouponType ->
                if (assignedCouponTypes.contains(validCouponType)) {
                    Mono.just(true)
                } else {
                    couponsRepository.findTop1ByCouponTypeAndRegistryType(validCouponType, it.registryType)
                        .flatMap { coupon ->
                            val registryCoupon = RegistryCoupons(
                                coupon.couponCode,
                                it,
                                coupon.couponType,
                                CouponRedemptionStatus.AVAILABLE,
                                LocalDateTime.now(),
                                coupon.couponExpiryDate,
                                null,
                                null
                            )
                            registryCouponsRepository.save(registryCoupon)
                                .flatMap { couponsRepository.deleteByCouponCode(coupon.couponCode) }
                                .map {
                                    registryCoupons.add(registryCoupon)
                                    true
                                }
                        }
                        .switchIfEmpty {
                            Mono.just(false)
                        }
                        .onErrorResume {
                            Mono.just(false)
                        }
                }
            }.collectList().flatMap {
                val couponAssignmentCompletion = !it.contains(false)
                if (couponAssignmentCompletion) {
                    registryRepository.updateCouponAssignmentComplete(registryId, true).map { true }
                } else {
                    Mono.just(true)
                }
            }.map { registryCoupons.toList() }
        }
    }

    fun sendGuestNotification(registryCoupons: List<RegistryCoupons>): Mono<Boolean> {
        return if (registryCoupons.isNullOrEmpty()) {
            Mono.just(true)
        } else {
            val registryId = registryCoupons.first().registry?.registryId.toString()
            var registryStoreCouponCode: String
            var registryOnlineCouponCode: String

            registryCoupons.map {
                if (it.couponType == CouponType.STORE) {
                    registryStoreCouponCode = it.couponCode!!
                } else {
                    registryOnlineCouponCode = it.couponCode!!
                }
            }
            val notificationTracerEvent = NotificationTracerEvent(
                scenarioName = "Completion_Coupon",
                id = registryId,
                idType = "PROFILE",
                origin = "GR",
                transactionId = registryId,
                earliestSendTime = null,
                latestSendTime = null,
                data = null
            )
            sendGuestNotificationsService.sendGuestNotifications(notificationTracerEvent, registryId)
        }
    }
}
