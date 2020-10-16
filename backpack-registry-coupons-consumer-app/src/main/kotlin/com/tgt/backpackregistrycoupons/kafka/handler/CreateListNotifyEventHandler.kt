package com.tgt.backpackregistrycoupons.kafka.handler

import com.tgt.backpackregistrycoupons.service.async.CreateListNotifyEventService
import com.tgt.backpackregistrycoupons.transport.RegistryMetaDataTO
import com.tgt.lists.lib.kafka.model.CreateListNotifyEvent
import com.tgt.lists.msgbus.event.EventHeaderFactory
import com.tgt.lists.msgbus.event.EventHeaders
import com.tgt.lists.msgbus.event.EventProcessingResult
import mu.KotlinLogging
import reactor.core.publisher.Mono
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreateListNotifyEventHandler(
    @Inject private val createListNotifyEventService: CreateListNotifyEventService,
    @Inject private val eventHeaderFactory: EventHeaderFactory
) {
    private val logger = KotlinLogging.logger { CreateListNotifyEventHandler::class.java.name }

    fun handleCreateListNotifyEvent(
        createListNotifyEvent: CreateListNotifyEvent,
        eventHeaders: EventHeaders,
        isPoisonEvent: Boolean
    ): Mono<EventProcessingResult> {
        val processingState: CreateListNotifyEventService.RetryState = if (createListNotifyEvent.retryState != null) {
            CreateListNotifyEventService.RetryState.deserialize(createListNotifyEvent.retryState.toString())
        } else {
            CreateListNotifyEventService.RetryState()
        }

        return createListNotifyEventService.processCreateListNotifyEvent(createListNotifyEvent.guestId,
            createListNotifyEvent.listId,
            RegistryMetaDataTO.getRegistryMetadata(createListNotifyEvent.userMetaData)?.registryType!!,
            RegistryMetaDataTO.getRegistryMetadata(createListNotifyEvent.userMetaData)?.registryCreatedTs!!,
            RegistryMetaDataTO.getRegistryMetadata(createListNotifyEvent.userMetaData)?.event?.eventDateTs!!,
            processingState)
            .map {
                if (it.completeState()) {
                    logger.debug("createListNotifyEvent processing is complete")
                    EventProcessingResult(true, eventHeaders, createListNotifyEvent)
                } else {
                    logger.debug("createListNotifyEvent didn't complete, adding it to DLQ for retry")
                    val message = "Error from handleCreateListNotifyEvent() for guest: " +
                        "${createListNotifyEvent.guestId} with listId: ${createListNotifyEvent.listId}"
                    val retryHeader = eventHeaderFactory.nextRetryHeaders(eventHeaders = eventHeaders,
                        errorCode = 500, errorMsg = message)
                    createListNotifyEvent.retryState = CreateListNotifyEventService.RetryState.serialize(it)
                    EventProcessingResult(false, retryHeader, createListNotifyEvent)
                }
            }
    }
}
