package com.tgt.backpackregistrycoupons.service

import com.tgt.backpackregistrycoupons.domain.model.RegistryCoupons
import com.tgt.backpackregistrycoupons.persistence.repository.registrycoupons.RegistryCouponsRepository
import com.tgt.backpackregistrycoupons.util.CouponRedemptionStatus
import com.tgt.backpackregistrycoupons.util.EventPublisher
import com.tgt.backpacktransactionsclient.transport.kafka.model.PromoCouponRedemptionTO
import com.tgt.backpacktransactionsclient.transport.kafka.model.RegistryItemPromoTransactionActionEvent
import mu.KotlinLogging
import reactor.core.publisher.Mono
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HandleRegistryTransactionService(
    @Inject val registryCouponsRepository: RegistryCouponsRepository,
    @Inject private val eventPublisher: EventPublisher
) {
    private val logger = KotlinLogging.logger {}

    fun processCouponCode(promoCouponRedemptionTO: PromoCouponRedemptionTO): Mono<Boolean> {
        return updateCouponStatus(promoCouponRedemptionTO.couponCode)
            .map {
                if (it?.couponCode != null) {
                    eventPublisher.publishEvent(
                        RegistryItemPromoTransactionActionEvent.getEventType(),
                        RegistryItemPromoTransactionActionEvent(promoCouponRedemptionTO),
                        it.id.registryId.toString())
                        .flatMap { Mono.just(true) }
                        .onErrorResume {
                            logger.error { "Error occurred while publishing events --  ${promoCouponRedemptionTO.couponCode}" }
                            Mono.just(false)
                        }
                    } else {
                        Mono.just(false)
                    }
            }
            .flatMap { it }
            .switchIfEmpty(Mono.just(false))
            .onErrorResume { Mono.just(false) }
    }

    fun updateCouponStatus(couponCode: String): Mono<RegistryCoupons> {
        return registryCouponsRepository.findByCouponCode(couponCode)
            .flatMap { registryCoupons ->
                registryCouponsRepository.updateStatusByCouponCode(couponCode, CouponRedemptionStatus.REDEEMED).map { registryCoupons }
            }.switchIfEmpty(Mono.empty())
            .onErrorResume {
                logger.error("Error occurred while updating coupon status", it)
                Mono.empty()
            }
    }
}
