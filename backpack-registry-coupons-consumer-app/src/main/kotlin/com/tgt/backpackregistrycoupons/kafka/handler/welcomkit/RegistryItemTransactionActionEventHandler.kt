package com.tgt.backpackregistrycoupons.kafka.handler.welcomkit

import com.tgt.backpackregistrycoupons.welcomekit.service.WelcomeKitTransactionService
import com.tgt.backpacktransactionsclient.transport.kafka.model.RegistryItemTransactionActionEvent
import com.tgt.lists.msgbus.event.EventHeaderFactory
import com.tgt.lists.msgbus.event.EventHeaders
import com.tgt.lists.msgbus.event.EventProcessingResult
import mu.KotlinLogging
import reactor.core.publisher.Mono
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RegistryItemTransactionActionEventHandler(
    @Inject private val welcomeKitTransactionService: WelcomeKitTransactionService,
    @Inject private val eventHeaderFactory: EventHeaderFactory
) {
    private val logger = KotlinLogging.logger { RegistryItemTransactionActionEventHandler::class.java.name }

    fun handleItemTransactionEvent(
        registryItemTransactionActionEvent: RegistryItemTransactionActionEvent,
        eventHeaders: EventHeaders,
        isPoisonEvent: Boolean
    ): Mono<EventProcessingResult> {
        return welcomeKitTransactionService.processWelcomeKitTransaction(registryItemTransactionActionEvent.registryTransactionTO)
            .map {
                if (it) {
                    logger.debug("[RegistryItemTransactionActionEventHandler] Event processing complete")
                    EventProcessingResult(it, eventHeaders, registryItemTransactionActionEvent)
                } else {
                    handleFailedEvent(registryItemTransactionActionEvent, eventHeaders)
                }
            }
            .onErrorResume {
                logger.error("[RegistryItemTransactionActionEventHandler] Error occurred")
                Mono.just(handleFailedEvent(registryItemTransactionActionEvent, eventHeaders))
            }
    }

    private fun handleFailedEvent(registryItemTransactionActionEvent: RegistryItemTransactionActionEvent, eventHeaders: EventHeaders): EventProcessingResult {
        val message = "Error from registryItemTransactActionEventHandler() for transaction: " +
            registryItemTransactionActionEvent.registryTransactionTO.registryId
        val retryHeader = eventHeaderFactory.nextRetryHeaders(eventHeaders = eventHeaders, errorCode = 500, errorMsg = message)
        return EventProcessingResult(false, retryHeader, registryItemTransactionActionEvent)
    }
}
