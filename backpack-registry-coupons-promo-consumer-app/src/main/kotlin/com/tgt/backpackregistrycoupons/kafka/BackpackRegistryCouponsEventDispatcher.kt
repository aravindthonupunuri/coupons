package com.tgt.backpackregistrycoupons.kafka

import com.tgt.backpackregistrycoupons.kafka.handler.RegistryTransactionEventHandler
import com.tgt.backpacktransactionsclient.transport.kafka.model.PromoCouponRedemptionTO
import com.tgt.lists.msgbus.EventDispatcher
import com.tgt.lists.msgbus.event.DeadEventTransformedValue
import com.tgt.lists.msgbus.event.EventHeaders
import com.tgt.lists.msgbus.event.EventProcessingResult
import com.tgt.lists.msgbus.event.EventTransformedValue
import com.tgt.lists.msgbus.execution.ExecutionSerialization
import io.micronaut.context.annotation.Value
import mu.KotlinLogging
import reactor.core.publisher.Mono
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class BackpackRegistryCouponsEventDispatcher(
    @Inject val registryTransactionEventHandler: RegistryTransactionEventHandler,
    @Value("\${msgbus.source}") val source: String,
    @Value("\${msgbus.dlq-source}") val dlqSource: String,
    @Value("\${kafka-sources.allow}") val allowedSources: Set<String>
) : EventDispatcher {
    private val logger = KotlinLogging.logger { BackpackRegistryCouponsEventDispatcher::class.java.name }

    override fun dispatchEvent(eventHeaders: EventHeaders, data: Any, isPoisonEvent: Boolean): Mono<EventProcessingResult> {
        return when {
            eventHeaders.source == source || allowedSources.contains(eventHeaders.source) -> {
                val promoCouponRedemptionEvent = data as PromoCouponRedemptionTO
                logger.debug { "Source : ${eventHeaders.source} | Got promo redemption Event: $promoCouponRedemptionEvent" }
                registryTransactionEventHandler.handleCouponTransaction(promoCouponRedemptionEvent, eventHeaders, isPoisonEvent)
            }
            else -> {
                logger.debug { "Unhandled eventType: ${eventHeaders.eventType}" }
                Mono.just(EventProcessingResult(true, eventHeaders, data))
            }
        }
    }

    override fun transformValue(eventHeaders: EventHeaders, data: ByteArray): EventTransformedValue? {
        return if (eventHeaders.source == source || allowedSources.contains(eventHeaders.source)) {
            val promoCouponRedemptionTO = PromoCouponRedemptionTO.deserialize(data)
            val executionId = null
            val serializationValue = ExecutionSerialization.NO_SERIALIZATION
            EventTransformedValue(executionId, serializationValue, promoCouponRedemptionTO)
        } else {
            null
        }
    }

    override fun handleDlqDeadEvent(eventHeaders: EventHeaders, data: ByteArray): DeadEventTransformedValue? {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }
}
