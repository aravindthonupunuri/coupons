package com.tgt.backpackregistrycoupons.kafka.handler

import com.tgt.backpackregistryclient.util.RegistryType
import com.tgt.backpackregistrycoupons.kafka.migration.model.CouponAssignmentNotifyEvent
import com.tgt.backpackregistrycoupons.kafka.migration.model.RegistryCouponMetaDataTO
import com.tgt.backpackregistrycoupons.kafka.migration.service.MigrationAssignCouponNotifyEventService
import com.tgt.lists.msgbus.event.EventHeaderFactory
import com.tgt.lists.msgbus.event.EventHeaders
import com.tgt.lists.msgbus.event.EventProcessingResult
import mu.KotlinLogging
import reactor.core.publisher.Mono
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MigrationCouponAssignmentNotifyEventHandler(
    @Inject private val migrationAssignCouponNotifyEventService: MigrationAssignCouponNotifyEventService,
    @Inject private val eventHeaderFactory: EventHeaderFactory
) {
    private val logger = KotlinLogging.logger { MigrationCouponAssignmentNotifyEventHandler::class.java.name }

    fun handleMigrationAssignCouponNotifyEventHandler(
        couponAssignmentNotifyEvent: CouponAssignmentNotifyEvent,
        eventHeaders: EventHeaders,
        isPoisonEvent: Boolean
    ): Mono<EventProcessingResult> {
        val processingState: MigrationAssignCouponNotifyEventService.RetryState = if (couponAssignmentNotifyEvent.retryState != null) {
            MigrationAssignCouponNotifyEventService.RetryState.deserialize(couponAssignmentNotifyEvent.retryState.toString())
        } else {
            MigrationAssignCouponNotifyEventService.RetryState()
        }

        return migrationAssignCouponNotifyEventService.processMigrationAssignCouponNotifyEvent(
            guestId = couponAssignmentNotifyEvent.guestId,
            registryId = couponAssignmentNotifyEvent.listId,
            registryType = RegistryType.toRegistryType(couponAssignmentNotifyEvent.listSubType!!),
            registryCouponMetaDataTO = RegistryCouponMetaDataTO.toEntityRegistryCouponMetadata(couponAssignmentNotifyEvent.userMetaData)!!,
            retryState = processingState
        ).map {
                if (it.completeState()) {
                    logger.debug("assignCouponNotifyEvent processing is complete")
                    EventProcessingResult(true, eventHeaders, couponAssignmentNotifyEvent)
                } else {
                    logger.debug("assignCouponNotifyEvent didn't complete, adding it to DLQ for retry")
                    val message = "Error from handleMigrationAssignCouponNotifyEventHandler() for registry id: " +
                        "${couponAssignmentNotifyEvent.listId} and registry type: ${RegistryType.toRegistryType(couponAssignmentNotifyEvent.listSubType!!)}"
                    val retryHeader = eventHeaderFactory.nextRetryHeaders(eventHeaders = eventHeaders,
                        errorCode = 500, errorMsg = message)
                    couponAssignmentNotifyEvent.retryState = MigrationAssignCouponNotifyEventService.RetryState.serialize(it)
                    EventProcessingResult(false, retryHeader, couponAssignmentNotifyEvent)
                }
            }
    }
}
