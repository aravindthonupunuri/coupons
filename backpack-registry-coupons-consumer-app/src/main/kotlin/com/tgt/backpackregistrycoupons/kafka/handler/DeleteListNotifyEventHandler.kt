package com.tgt.backpackregistrycoupons.kafka.handler

import com.tgt.backpackregistrycoupons.service.async.DeleteListNotifyEventService
import com.tgt.lists.lib.kafka.model.DeleteListNotifyEvent
import com.tgt.lists.msgbus.event.EventHeaderFactory
import com.tgt.lists.msgbus.event.EventHeaders
import com.tgt.lists.msgbus.event.EventProcessingResult
import mu.KotlinLogging
import reactor.core.publisher.Mono
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeleteListNotifyEventHandler(
    @Inject private val deleteListNotifyEventService: DeleteListNotifyEventService,
    @Inject private val eventHeaderFactory: EventHeaderFactory
) {
    private val logger = KotlinLogging.logger { DeleteListNotifyEventHandler::class.java.name }

    fun handleDeleteListNotifyEvent(
        deleteListNotifyEvent: DeleteListNotifyEvent,
        eventHeaders: EventHeaders,
        isPoisonEvent: Boolean
    ): Mono<EventProcessingResult> {
        val processingState: DeleteListNotifyEventService.RetryState = if (deleteListNotifyEvent.retryState != null) {
            DeleteListNotifyEventService.RetryState.deserialize(deleteListNotifyEvent.retryState.toString())
        } else {
            DeleteListNotifyEventService.RetryState()
        }

        return deleteListNotifyEventService.processDeleteListNotifyEvent(deleteListNotifyEvent.listId, processingState)
            .map {
                if (it.completeState()) {
                    logger.debug("deleteListNotifyEvent processing is complete")
                    EventProcessingResult(true, eventHeaders, deleteListNotifyEvent)
                } else {
                    logger.debug("deleteListNotifyEvent didn't complete, adding it to DLQ for retry")
                    val message = "Error from handleDeleteListNotifyEvent() for guest: " +
                        "${deleteListNotifyEvent.guestId} with listId: ${deleteListNotifyEvent.listId}"
                    val retryHeader = eventHeaderFactory.nextRetryHeaders(eventHeaders = eventHeaders,
                        errorCode = 500, errorMsg = message)
                    deleteListNotifyEvent.retryState = DeleteListNotifyEventService.RetryState.serialize(it)
                    EventProcessingResult(false, retryHeader, deleteListNotifyEvent)
                }
            }
    }
}
