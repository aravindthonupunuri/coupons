package com.tgt.backpackregistrycoupons.welcomekit.service

import com.tgt.backpackregistrycoupons.transport.kafka.WelcomeKitTransactionActionEvent
import com.tgt.backpackregistrycoupons.util.EventPublisher
import com.tgt.backpackregistrycoupons.welcomekit.persistence.repository.welcomekit.WelcomeKitsRepository
import com.tgt.backpacktransactionsclient.transport.kafka.model.RegistryTransactionTO
import mu.KotlinLogging
import reactor.core.publisher.Mono
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WelcomeKitTransactionService(
    @Inject val welcomeKitsRepository: WelcomeKitsRepository,
    @Inject private val eventPublisher: EventPublisher
) {
    private val logger = KotlinLogging.logger {}

    fun processWelcomeKitTransaction(registryTransactionTO: RegistryTransactionTO): Mono<Boolean> {
        return if (registryTransactionTO.tcin.isNullOrEmpty()) Mono.just(true) // Ignore non-tcin items
            else welcomeKitsRepository.existsByTcin(registryTransactionTO.tcin!!)
            .flatMap {
                if (it) {
                    eventPublisher.publishEvent(
                        eventType = WelcomeKitTransactionActionEvent.getEventType(),
                        message = WelcomeKitTransactionActionEvent(registryTransactionTO.registryId),
                        partitionKey = registryTransactionTO.registryId.toString())
                        .map { true }
                        .onErrorResume {
                            logger.error("[WelcomeKitTransactionService] Error occurred while publishing " +
                                "WelcomeKitTransactionActionEvent event for registry: ${registryTransactionTO.registryId}")
                            Mono.just(false)
                        }
                } else {
                    logger.debug("[WelcomeKitTransactionService] Not a welcome kit transaction with tcin: ${registryTransactionTO.tcin} " +
                        "and registry ${registryTransactionTO.registryId}, skipping processing the event")
                    Mono.just(true)
                }
            }
            .onErrorResume {
                logger.error("[WelcomeKitTransactionService] Exception while processing WelcomeKitTransaction with tcin: ${registryTransactionTO.tcin} and registry ${registryTransactionTO.registryId}", it)
                Mono.just(false)
            }
    }
}
