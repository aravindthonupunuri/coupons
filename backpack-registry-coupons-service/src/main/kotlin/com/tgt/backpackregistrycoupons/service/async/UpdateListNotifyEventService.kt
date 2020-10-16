package com.tgt.backpackregistrycoupons.service.async

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tgt.backpackregistrycoupons.persistence.repository.registrycoupons.RegistryCouponsRepository
import mu.KotlinLogging
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateListNotifyEventService(
    @Inject val registryCouponsRepository: RegistryCouponsRepository
) {
    private val logger = KotlinLogging.logger { UpdateListNotifyEventService::class.java.name }

    fun processUpdateListNotifyEvent(
        listId: UUID,
        eventDate: LocalDateTime,
        retryState: RetryState
    ): Mono<RetryState> {
        return if (retryState.incompleteState()) {
            logger.debug("From processUpdateListNotifyEvent(), starting processing")
            return processUpdateEventDate(listId, eventDate)
                .map {
                    retryState.updateEventDate = it
                    retryState
                }
        } else {
            logger.debug("From processUpdateListNotifyEvent(), processing complete")
            Mono.just(retryState)
        }
    }

    fun processUpdateEventDate(
        listId: UUID,
        eventDate: LocalDateTime
    ): Mono<Boolean> {
        return registryCouponsRepository.findByIdRegistryId(listId).collectList()
            .flatMap {
                val existingEvenDate = it.firstOrNull()?.eventDate
                if (existingEvenDate == null || existingEvenDate.isEqual(eventDate)) {
                    Mono.just(true)
                } else {
                    registryCouponsRepository.updateByRegistryId(listId, eventDate).then().map { true }
                }
            }
            .onErrorResume {
                logger.error("Exception from processUpdateEventDate() for registryId: $listId and " +
                    "eventDate $eventDate sending it for retry", it)
                Mono.just(false)
            }
    }

    data class RetryState(
        var updateEventDate: Boolean = false
    ) {
        fun completeState(): Boolean {
            return updateEventDate
        }

        fun incompleteState(): Boolean {
            return !updateEventDate
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
