package com.tgt.backpackregistrycoupons.kafka

import com.tgt.backpackregistrycoupons.kafka.handler.*
import com.tgt.backpackregistrycoupons.migration.model.CouponAssignmentNotifyEvent
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
open class BackpackRegistryCouponsMigrationEventDispatcher(
    @Inject val migrationCouponAssignmentNotifyEventHandler: MigrationCouponAssignmentNotifyEventHandler,
    @Value("\${msgbus.source}") val source: String,
    @Value("\${msgbus.dlq-source}") val dlqSource: String,
    @Value("\${kafka-sources.allow}") val allowedSources: Set<String>
) : EventDispatcher {

    private val logger = KotlinLogging.logger {}

    override fun dispatchEvent(eventHeaders: EventHeaders, data: Any, isPoisonEvent: Boolean): Mono<EventProcessingResult> {
        if (eventHeaders.source == source || eventHeaders.source == dlqSource || allowedSources.contains(eventHeaders.source)) {
            when (eventHeaders.eventType) {
                CouponAssignmentNotifyEvent.getEventType() -> {
                    // always use transformValue to convert raw data to concrete type
                    val assignCouponNotifyEvent = data as CouponAssignmentNotifyEvent
                    logger.debug { "Got cron Event: $assignCouponNotifyEvent" }
                    return migrationCouponAssignmentNotifyEventHandler.handleMigrationAssignCouponNotifyEventHandler(assignCouponNotifyEvent, eventHeaders, isPoisonEvent)
                }
            }
        }

        logger.debug { "Unhandled eventType: ${eventHeaders.eventType}" }
        return Mono.just(EventProcessingResult(true, eventHeaders, data))
    }

    /**
     * Transform ByteArray data to a concrete type based on event type header
     * It is also used by msgbus framework during dql publish exception handling
     */
    override fun transformValue(eventHeaders: EventHeaders, data: ByteArray): EventTransformedValue? {
        if (eventHeaders.source == source || eventHeaders.source == dlqSource || allowedSources.contains(eventHeaders.source)) {
            return when (eventHeaders.eventType) {
                CouponAssignmentNotifyEvent.getEventType() -> {
                    val couponAssignmentNotifyEvent = CouponAssignmentNotifyEvent.deserialize(data)
                    EventTransformedValue("lists_${couponAssignmentNotifyEvent.listId}", ExecutionSerialization.ID_SERIALIZATION, couponAssignmentNotifyEvent)
                }
                else -> null
            }
        }

        return null
    }

    /**
     * Handle DLQ dead events here
     * @return Triple<ExecutionId?, ExecutionSerialization, Mono<Void>>
     *                          Possible values:
     *                          null - to discard this event as we don't want to handle this dead event
     *                          OR
     *                          Triple:
     *                          =======s
     *                          ExecutionId - used only for ID_SERIALIZATION to denote a unique string identifying the processing of this event (usually some kind of business id)
     *                          ExecutionSerialization - type of serialization processing required for this event
     *                          Mono<Void> - dead event processing lambda to be run
     */
    override fun handleDlqDeadEvent(eventHeaders: EventHeaders, data: ByteArray): DeadEventTransformedValue? {
        return null
    }
}
