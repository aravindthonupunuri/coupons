package com.tgt.backpackregistrycoupons.kafka.handler

import com.tgt.backpackregistryclient.transport.RegistryMetaDataTO
import com.tgt.backpackregistryclient.util.RegistryStatus
import com.tgt.backpackregistryclient.util.RegistryType.Companion.toRegistryType
import com.tgt.backpackregistrycoupons.service.async.CreateListNotifyEventService
import com.tgt.lists.atlas.kafka.model.CreateListNotifyEvent
import com.tgt.lists.msgbus.event.EventHeaderFactory
import com.tgt.lists.msgbus.event.EventHeaders
import com.tgt.lists.msgbus.event.EventProcessingResult
import mu.KotlinLogging
import reactor.core.publisher.Mono
import java.time.LocalDate
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

        return createListNotifyEventService.processCreateListNotifyEvent(
            guestId = createListNotifyEvent.guestId,
            registryId = createListNotifyEvent.listId,
            registryStatus = RegistryStatus.toRegistryStatus(createListNotifyEvent.listState!!.name),
            registryType = toRegistryType(createListNotifyEvent.listSubType!!), // defaulted to baby
            registryCreatedDate = LocalDate.now(), // TODO: createListNotifyEvent not having registry create ts
            eventDate = RegistryMetaDataTO.toEntityRegistryMetadata(createListNotifyEvent.userMetaData)?.event?.eventDate!!,
            retryState = processingState
        ).map {
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
