package com.tgt.backpackregistrycoupons.service

import com.tgt.backpackregistrycoupons.domain.model.RegistryCoupons
import com.tgt.backpackregistrycoupons.persistence.repository.registrycoupons.RegistryCouponsRepository
import com.tgt.backpackregistrycoupons.util.EventPublisher
import com.tgt.backpackregistrycoupons.transport.PromoCouponRedemptionTO
import com.tgt.backpackregistrycoupons.transport.RegistryItemPromoTransactionActionEvent
import mu.KotlinLogging
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RegistryTransactionService(
    @Inject val registryCouponsRepository: RegistryCouponsRepository,
    @Inject private val eventPublisher: EventPublisher
) {
    private val logger = KotlinLogging.logger {}

    fun processCouponCode(promoCouponRedemptionTO: PromoCouponRedemptionTO): Mono<Boolean> {
        return updateCouponStatus(promoCouponRedemptionTO.couponCode, promoCouponRedemptionTO.status!!)
            .flatMap {
                promoCouponRedemptionTO.registryId = it.registry?.registryId
                eventPublisher.publishEvent(
                        RegistryItemPromoTransactionActionEvent.getEventType(),
                        RegistryItemPromoTransactionActionEvent(promoCouponRedemptionTO),
                        it.registry?.registryId.toString())
                        .map { true }
                        .onErrorResume {
                            logger.error { "[RegistryTransactionService] Error occurred while publishing events --  ${promoCouponRedemptionTO.couponCode}" }
                            Mono.just(false)
                        }
            }
            .switchIfEmpty {
                logger.error("[RegistryTransactionService] Coupon ${promoCouponRedemptionTO.couponCode} found to update CouponRedemptionStatus status")
                Mono.just(true)
            }
            .onErrorResume {
                logger.error("[RegistryTransactionService] Exception while updating CouponRedemptionStatus for coupon  ${promoCouponRedemptionTO.couponCode}", it)
                Mono.just(false)
            }
    }

    fun updateCouponStatus(couponCode: String, couponStatus: String): Mono<RegistryCoupons> {
        return registryCouponsRepository.findByCouponCode(couponCode)
            .flatMap { registryCoupons ->
                registryCouponsRepository.updateCouponRedemptionStatus(couponCode, couponStatus)
                    .map { registryCoupons }
            }
    }
}
