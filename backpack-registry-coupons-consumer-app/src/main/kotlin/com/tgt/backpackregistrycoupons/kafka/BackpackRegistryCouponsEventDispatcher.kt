package com.tgt.backpackregistrycoupons.kafka

import com.tgt.backpackregistrycoupons.kafka.handler.CreateListNotifyEventHandler
import com.tgt.backpackregistrycoupons.kafka.handler.CronEventHandler
import com.tgt.backpackregistrycoupons.kafka.handler.DeleteListNotifyEventHandler
import com.tgt.backpackregistrycoupons.kafka.handler.UpdateListNotifyEventHandler
import com.tgt.guestnotifications.kafka.model.CronEvent
import com.tgt.lists.lib.kafka.model.CreateListNotifyEvent
import com.tgt.lists.lib.kafka.model.DeleteListNotifyEvent
import com.tgt.lists.lib.kafka.model.UpdateListNotifyEvent
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
    @Inject val createListNotifyEventHandler: CreateListNotifyEventHandler,
    @Inject val updateListNotifyEventHandler: UpdateListNotifyEventHandler,
    @Inject val deleteListNotifyEventHandler: DeleteListNotifyEventHandler,
    @Inject val cronEventHandler: CronEventHandler,
    @Value("\${msgbus.source}") val source: String,
    @Value("\${msgbus.dlq-source}") val dlqSource: String,
    @Value("\${kafka-sources.allow}") val allowedSources: Set<String>
) : EventDispatcher {

    private val logger = KotlinLogging.logger {}

    override fun dispatchEvent(eventHeaders: EventHeaders, data: Any, isPoisonEvent: Boolean): Mono<EventProcessingResult> {
        if (eventHeaders.source == source || eventHeaders.source == dlqSource || allowedSources.contains(eventHeaders.source)) {
            // handle following events only from configured source
            when (eventHeaders.eventType) {
                CreateListNotifyEvent.getEventType() -> {
                    // always use transformValue to convert raw data to concrete type
                    val createListNotifyEvent = data as CreateListNotifyEvent
                    logger.debug { "Got CreateList Event: $createListNotifyEvent" }
                    return createListNotifyEventHandler.handleCreateListNotifyEvent(createListNotifyEvent, eventHeaders, isPoisonEvent)
                }
                UpdateListNotifyEvent.getEventType() -> {
                    // always use transformValue to convert raw data to concrete type
                    val updateListNotifyEvent = data as UpdateListNotifyEvent
                    logger.debug { "Got UpdateList Event: $updateListNotifyEvent" }
                    return updateListNotifyEventHandler.handleUpdateListNotifyEvent(updateListNotifyEvent, eventHeaders, isPoisonEvent)
                }
                DeleteListNotifyEvent.getEventType() -> {
                    // always use transformValue to convert raw data to concrete type
                    val deleteListNotifyEvent = data as DeleteListNotifyEvent
                    logger.debug { "Got DeleteList Event: $deleteListNotifyEvent" }
                    return deleteListNotifyEventHandler.handleDeleteListNotifyEvent(deleteListNotifyEvent, eventHeaders, isPoisonEvent)
                }
                CronEvent.getEventType() -> {
                    // always use transformValue to convert raw data to concrete type
                    val cronEvent = data as CronEvent
                    logger.debug { "Got cron Event: $cronEvent" }
                    return cronEventHandler.handleCronEvent(cronEvent, eventHeaders, isPoisonEvent)
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
                CreateListNotifyEvent.getEventType() -> {
                    val createListNotifyEvent = CreateListNotifyEvent.deserialize(data)
                    EventTransformedValue("lists_${createListNotifyEvent.listId}", ExecutionSerialization.ID_SERIALIZATION, createListNotifyEvent)
                }
                UpdateListNotifyEvent.getEventType() -> {
                    val updateListNotifyEvent = UpdateListNotifyEvent.deserialize(data)
                    EventTransformedValue("lists_${updateListNotifyEvent.listId}", ExecutionSerialization.ID_SERIALIZATION, updateListNotifyEvent)
                }
                DeleteListNotifyEvent.getEventType() -> {
                    val deleteListNotifyEvent = DeleteListNotifyEvent.deserialize(data)
                    EventTransformedValue("lists_${deleteListNotifyEvent.listId}", ExecutionSerialization.ID_SERIALIZATION, deleteListNotifyEvent)
                }
                CronEvent.getEventType() -> {
                    val cronEvent = CronEvent.deserialize(data)
                    EventTransformedValue("cron_${cronEvent.eventDateTime}", ExecutionSerialization.ID_SERIALIZATION, cronEvent)
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
