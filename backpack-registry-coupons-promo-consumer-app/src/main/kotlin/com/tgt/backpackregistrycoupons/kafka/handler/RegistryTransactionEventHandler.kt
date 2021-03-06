package com.tgt.backpackregistrycoupons.kafka.handler

import com.tgt.backpackregistrycoupons.service.RegistryTransactionService
import com.tgt.backpackregistrycoupons.transport.PromoCouponRedemptionTO
import com.tgt.lists.msgbus.event.EventHeaderFactory
import com.tgt.lists.msgbus.event.EventHeaders
import com.tgt.lists.msgbus.event.EventProcessingResult
import mu.KotlinLogging
import reactor.core.publisher.Mono
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RegistryTransactionEventHandler(
    @Inject private val registryTransactionService: RegistryTransactionService,
    @Inject private val eventHeaderFactory: EventHeaderFactory
) {

    private val logger = KotlinLogging.logger { RegistryTransactionEventHandler::class.java.name }

    fun handleCouponTransaction(
        promoCouponRedemptionTO: PromoCouponRedemptionTO,
        eventHeaders: EventHeaders,
        isPoisonEvent: Boolean
    ): Mono<EventProcessingResult> {
        return registryTransactionService.processCouponCode(promoCouponRedemptionTO).map {
            if (it) {
                logger.debug("promoCouponRedemptionEvent processing is complete")
                EventProcessingResult(it, eventHeaders, promoCouponRedemptionTO)
            } else {
                handleFailedEvents(promoCouponRedemptionTO, eventHeaders)
            }
        }.onErrorResume {
            Mono.just(handleFailedEvents(promoCouponRedemptionTO, eventHeaders))
        }
}

    private fun handleFailedEvents(promoCouponRedemptionTO: PromoCouponRedemptionTO, eventHeaders: EventHeaders): EventProcessingResult {
        logger.debug("promoCouponRedemptionEvent didn't complete, adding it to DLQ for retry")
        val message = "Error from RegistryCouponTransactionEventHandler() for transaction:" + promoCouponRedemptionTO.couponCode
        val retryHeader = eventHeaderFactory.nextRetryHeaders(eventHeaders = eventHeaders,
            errorCode = 500, errorMsg = message)
        return EventProcessingResult(false, retryHeader, promoCouponRedemptionTO)
    }
}
