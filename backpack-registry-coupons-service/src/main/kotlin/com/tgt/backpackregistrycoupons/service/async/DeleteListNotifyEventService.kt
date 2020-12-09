package com.tgt.backpackregistrycoupons.service.async

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tgt.backpackregistrycoupons.persistence.repository.registrycoupons.RegistryCouponsRepository
import mu.KotlinLogging
import reactor.core.publisher.Mono
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeleteListNotifyEventService(
    @Inject val registryCouponsRepository: RegistryCouponsRepository
) {
    private val logger = KotlinLogging.logger { DeleteListNotifyEventService::class.java.name }

    fun processDeleteListNotifyEvent(
        listId: UUID,
        retryState: RetryState
    ): Mono<RetryState> {
        return if (retryState.incompleteState()) {
            logger.debug("From processDeleteListNotifyEvent(), starting processing")
            return deleteGuestRegistry(listId)
                .map {
                    retryState.deleteGuestRegistry = it
                    retryState
                }
        } else {
            logger.debug("From processDeleteListNotifyEvent(), processing complete")
            Mono.just(retryState)
        }
    }

    fun deleteGuestRegistry(
        listId: UUID
    ): Mono<Boolean> {
        return registryCouponsRepository.deleteByRegistryId(listId).then().map { true }
            .onErrorResume {
                logger.error("Exception from deleteGuestRegistry() for registryId: $listId " +
                    "sending it for retry", it)
                Mono.just(false)
            }
    }

    data class RetryState(
        var deleteGuestRegistry: Boolean = false
    ) {
        fun completeState(): Boolean {
            return deleteGuestRegistry
        }

        fun incompleteState(): Boolean {
            return !deleteGuestRegistry
        }

        companion object {
            // jacksonObjectMapper() returns a normal ObjectMapper with the KotlinModule registered
            val jsonMapper: ObjectMapper = jacksonObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)

            @JvmStatic
            fun deserialize(retryState: String): RetryState {
                return jsonMapper.readValue<RetryState>(retryState, RetryState::class.java)
            }

            @JvmStatic
            fun serialize(retryState: RetryState): String {
                return jsonMapper.writeValueAsString(retryState)
            }
        }
    }
}
