package com.tgt.backpackregistrycoupons.kafka.handler

import com.tgt.backpackregistrycoupons.service.async.UpdateListNotifyEventService
import com.tgt.backpackregistrycoupons.transport.RegistryMetaDataTO
import com.tgt.lists.atlas.kafka.model.UpdateListNotifyEvent
import com.tgt.lists.msgbus.event.EventHeaderFactory
import com.tgt.lists.msgbus.event.EventHeaders
import com.tgt.lists.msgbus.event.EventProcessingResult
import mu.KotlinLogging
import reactor.core.publisher.Mono
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateListNotifyEventHandler(
    @Inject private val updateListNotifyEventService: UpdateListNotifyEventService,
    @Inject private val eventHeaderFactory: EventHeaderFactory
) {
    private val logger = KotlinLogging.logger { UpdateListNotifyEventHandler::class.java.name }

    fun handleUpdateListNotifyEvent(
        updateListNotifyEvent: UpdateListNotifyEvent,
        eventHeaders: EventHeaders,
        isPoisonEvent: Boolean
    ): Mono<EventProcessingResult> {
        val processingState: UpdateListNotifyEventService.RetryState = if (updateListNotifyEvent.retryState != null) {
            UpdateListNotifyEventService.RetryState.deserialize(updateListNotifyEvent.retryState.toString())
        } else {
            UpdateListNotifyEventService.RetryState()
        }

        return updateListNotifyEventService.processUpdateListNotifyEvent(updateListNotifyEvent.listId, updateListNotifyEvent.listState!!,
            RegistryMetaDataTO.getRegistryMetadata(updateListNotifyEvent.userMetaData)?.event?.eventDateTs!!,

            processingState).map {
            if (it.completeState()) {
                logger.debug("updateListNotifyEvent processing is complete")
                EventProcessingResult(true, eventHeaders, updateListNotifyEvent)
            } else {
                logger.debug("updateListNotifyEvent didn't complete, adding it to DLQ for retry")
                val message = "Error from handleUpdateListNotifyEvent() for guest: " +
                        "${updateListNotifyEvent.guestId} with listId: ${updateListNotifyEvent.listId}"
                val retryHeader = eventHeaderFactory.nextRetryHeaders(eventHeaders = eventHeaders,
                    errorCode = 500, errorMsg = message)
                updateListNotifyEvent.retryState = UpdateListNotifyEventService.RetryState.serialize(it)
                EventProcessingResult(false, retryHeader, updateListNotifyEvent)
            }
        }
    }
}
