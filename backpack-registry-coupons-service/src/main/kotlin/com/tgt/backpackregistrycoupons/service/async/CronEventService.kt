package com.tgt.backpackregistrycoupons.service.async

import com.tgt.backpackregistrycoupons.domain.model.RegistryCoupons
import com.tgt.backpackregistrycoupons.kafka.model.TracerEvent
import com.tgt.backpackregistrycoupons.persistence.repository.coupons.CouponsRepository
import com.tgt.backpackregistrycoupons.persistence.repository.registrycoupons.RegistryCouponsRepository
import com.tgt.backpackregistrycoupons.service.RegistryCouponAssignmentService
import com.tgt.backpackregistrycoupons.util.CouponType
import com.tgt.backpackregistrycoupons.util.RegistryStatus
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CronEventService(
    @Inject val registryCouponAssignmentService: RegistryCouponAssignmentService,
    @Inject val sendGuestNotificationsService: SendGuestNotificationsService,
    @Inject val registryCouponsRepository: RegistryCouponsRepository,
    @Inject val couponsRepository: CouponsRepository

) {
    private val logger = KotlinLogging.logger { CronEventService::class.java.name }

    fun processCronEvent(): Mono<Boolean> {
        return registryCouponsRepository.findByRegistryStatusAndCouponCodeIsNull(RegistryStatus.ACTIVE.value).collectList()
            .flatMap { list ->
                // RegistryPk is a composite key of  registryId and CouponType. So we create a Map of registryId and
                // RegistryCoupons list to uniquely identify a Registry.
                val registryMap = hashMapOf<UUID, ArrayList<RegistryCoupons>>()
                list.map {
                    if (registryMap.containsKey(it.id.registryId)) {
                        val existingCoupons = registryMap[it.id.registryId]!!
                        existingCoupons.add(it)
                        registryMap.put(it.id.registryId, existingCoupons)
                    } else {
                        registryMap.put(it.id.registryId, arrayListOf(it))
                    }
                }

                Flux.fromIterable(registryMap.values.asIterable())
                .flatMap { registryCoupons ->
                    assignCouponCode(registryCoupons).flatMap { sendGuestNotification(registryCoupons.first().id.registryId) } }
                .collectList().map { true } }
            .onErrorResume {
                logger.error("Exception from processCronEvent() sending it for retry", it)
                Mono.just(false)
            }
    }

    fun assignCouponCode(registryCouponsList: List<RegistryCoupons>): Mono<Boolean> {
        val couponAssignmentDate = registryCouponAssignmentService.calculateCouponAssignmentDate(registryCouponsList.first())
        return if (LocalDateTime.now().isAfter(couponAssignmentDate)) {
            Flux.fromIterable(registryCouponsList).flatMap {
                val registryCoupons = it
                couponsRepository.findTop1ByCouponTypeAndRegistryType(registryCoupons.id.couponType, registryCoupons.registryType)
                    .flatMap { coupon ->
                        registryCouponsRepository.updateRegistry(registryCoupons.id.registryId, coupon.couponType,
                            coupon.couponCode).flatMap { couponsRepository.deleteByCouponCode(coupon.couponCode) }
                    }
            }.collectList().map { true }
        } else {
            Mono.just(true)
        }
    }

    fun sendGuestNotification(registryId: UUID): Mono<Boolean> {
        return registryCouponsRepository.findByIdRegistryId(registryId).collectList().flatMap {
            var registryStoreCouponCode: String
            var registryOnlineCouponCode: String

            it.map {
                if (it.id.couponType == CouponType.STORE) {
                    registryStoreCouponCode = it.couponCode!!
                } else {
                    registryOnlineCouponCode = it.couponCode!!
                }
            }

            val tracerEvent = TracerEvent(
                scenarioName = "Completion_Coupon",
                id = it.first().id.registryId.toString(),
                idType = "PROFILE",
                origin = "GR",
                transactionId = it.first().id.registryId.toString(),
                earliestSendTime = null,
                latestSendTime = null,
                data = null
            )
            sendGuestNotificationsService.sendGuestNotifications(tracerEvent, it.first().id.registryId).map { true }
        }
    }
}
