package com.tgt.backpackregistrycoupons.kafka.handler

import com.tgt.backpackregistryclient.transport.RegistryMetaDataTO
import com.tgt.backpackregistryclient.util.RegistryType.Companion.toRegistryType
import com.tgt.backpackregistrycoupons.kafka.model.CreateCollegeListNotifyEvent
import com.tgt.backpackregistrycoupons.service.async.CreateCollegeListNotifyEventService
import com.tgt.lists.msgbus.event.EventHeaderFactory
import com.tgt.lists.msgbus.event.EventHeaders
import com.tgt.lists.msgbus.event.EventProcessingResult
import mu.KotlinLogging
import reactor.core.publisher.Mono
import java.time.LocalDate
import javax.inject.Inject

class CreateCollegeListNotifyEventHandler(
    @Inject private val createCollegeListNotifyEventService: CreateCollegeListNotifyEventService,
    @Inject private val eventHeaderFactory: EventHeaderFactory
) {
    private val logger = KotlinLogging.logger { CreateCollegeListNotifyEventHandler::class.java.name }

    fun handleCreateCollegeListNotifyEvent(
        createCollegeListNotifyEvent: CreateCollegeListNotifyEvent,
        eventHeaders: EventHeaders,
        isPoisonEvent: Boolean
    ): Mono<EventProcessingResult> {
        val processingState: CreateCollegeListNotifyEventService.RetryState = if (createCollegeListNotifyEvent.retryState != null) {
            CreateCollegeListNotifyEventService.RetryState.deserialize(createCollegeListNotifyEvent.retryState.toString())
        } else {
            CreateCollegeListNotifyEventService.RetryState()
        }

        return createCollegeListNotifyEventService.processCreateCollegeListNotifyEvent(
            guestId = createCollegeListNotifyEvent.guestId,
            registryId = createCollegeListNotifyEvent.listId,
            alternateRegistryId = createCollegeListNotifyEvent.alternateRegistryId,
            registryStatus = createCollegeListNotifyEvent.listState!!,
            registryType = toRegistryType(createCollegeListNotifyEvent.listSubType!!), // defaulted to baby
            registryCreatedDate = LocalDate.from(createCollegeListNotifyEvent.addedDate),
            eventDate = createCollegeListNotifyEvent.eventDate!!,
            addedDate = createCollegeListNotifyEvent.addedDate,
            lastModifiedDate = createCollegeListNotifyEvent.lastModifiedDate,
            retryState = processingState
        ).map {
            if (it.completeState()) {
                logger.debug("createCollegeListNotifyEvent processing is complete")
                EventProcessingResult(true, eventHeaders, createCollegeListNotifyEvent)
            } else {
                logger.debug("createCollegeListNotifyEvent didn't complete, adding it to DLQ for retry")
                val message = "Error from handleCreateCollegeListNotifyEvent() for guest: " +
                    "${createCollegeListNotifyEvent.guestId} with listId: ${createCollegeListNotifyEvent.listId}"
                val retryHeader = eventHeaderFactory.nextRetryHeaders(eventHeaders = eventHeaders,
                    errorCode = 500, errorMsg = message)
                createCollegeListNotifyEvent.retryState = CreateCollegeListNotifyEventService.RetryState.serialize(it)
                EventProcessingResult(false, retryHeader, createCollegeListNotifyEvent)
            }
        }
    }
}
