package com.tgt.backpackregistrycoupons.kafka.handler

import com.tgt.backpackregistrycoupons.kafka.model.CronEvent
import com.tgt.backpackregistrycoupons.service.async.CronEventService
import com.tgt.lists.msgbus.event.EventHeaderFactory
import com.tgt.lists.msgbus.event.EventHeaders
import com.tgt.lists.msgbus.event.EventProcessingResult
import mu.KotlinLogging
import reactor.core.publisher.Mono
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CronEventHandler(
    @Inject private val cronEventService: CronEventService,
    @Inject private val eventHeaderFactory: EventHeaderFactory

) {
    private val logger = KotlinLogging.logger { CronEventHandler::class.java.name }

    fun handleCronEvent(
        cronEvent: CronEvent,
        eventHeaders: EventHeaders,
        isPoisonEvent: Boolean
    ): Mono<EventProcessingResult> {
        val hourOfDay: Int = cronEvent.hourOfDay
        val minuteBlockOfHour: Long = cronEvent.minuteBlockOfHour

        return if (hourOfDay == 2 && minuteBlockOfHour == 1L) {
            cronEventService.processCronEvent().map {
                if (it) {
                    logger.debug("cronEvent processing is complete")
                    EventProcessingResult(true, eventHeaders, cronEvent)
                } else {
                    logger.debug("cronEvent didn't complete, adding it to DLQ for retry")
                    val message = "Error from handleCronEvent() for cronEvent with hourOfDay: $hourOfDay and minuteBlockOfHour: $minuteBlockOfHour"
                    val retryHeader = eventHeaderFactory.nextRetryHeaders(eventHeaders = eventHeaders, errorCode = 500, errorMsg = message)
                    EventProcessingResult(false, retryHeader, cronEvent)
                }
            }
        } else {
            Mono.just(EventProcessingResult(true, eventHeaders, cronEvent))
        }
    }
}
