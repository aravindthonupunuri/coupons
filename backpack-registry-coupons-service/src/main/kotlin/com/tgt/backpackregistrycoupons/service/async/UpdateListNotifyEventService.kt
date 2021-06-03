package com.tgt.backpackregistrycoupons.service.async

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tgt.backpackregistryclient.util.RegistryType
import com.tgt.backpackregistrycoupons.domain.model.Registry
import com.tgt.backpackregistrycoupons.persistence.repository.registry.RegistryRepository
import com.tgt.backpackregistrycoupons.util.isNotBabyOrWeddingRegistryType
import com.tgt.lists.atlas.api.type.LIST_STATE
import mu.KotlinLogging
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateListNotifyEventService(
    @Inject val registryRepository: RegistryRepository
) {
private val logger = KotlinLogging.logger { UpdateListNotifyEventService::class.java.name }

fun processUpdateListNotifyEvent(
    registryId: UUID,
    registryType: RegistryType,
    registryStatus: LIST_STATE,
    eventDate: LocalDate,
    retryState: RetryState
): Mono<RetryState> {
    return if (isNotBabyOrWeddingRegistryType(registryType))
        Mono.just(RetryState(true, true))
    else registryRepository.findByRegistryId(registryId).flatMap {
        when {
            retryState.incompleteState() -> {
                logger.debug("From processUpdateListNotifyEvent(), starting processing for listId: $registryId")
                updateEventDate(it, registryId, eventDate, retryState)
                    .flatMap { retryState -> updateRegistryStatus(it, registryId, registryStatus, retryState) }
            }
            retryState.incompleteUpdateEventDateState() -> {
                logger.debug("From processUpdateListNotifyEvent(), starting processing of updateEventDate for listId: $registryId")
                updateEventDate(it, registryId, eventDate, retryState)
            }
            retryState.incompleteUpdateRegistryStatusState() -> {
                logger.debug("From processUpdateListNotifyEvent(), starting processing of updateRegistryStatus for listId: $registryId")
                updateRegistryStatus(it, registryId, registryStatus, retryState)
            }
            retryState.completeState() -> {
                logger.debug("From processUpdateListNotifyEvent(), processing complete for listId: $registryId")
                Mono.just(retryState)
            }
            else -> {
                logger.error("Unknown step for listId $registryId from processUpdateListNotifyEvent()")
                retryState.updateEventDate = true
                retryState.updateRegistryStatus = true
                Mono.just(retryState)
            }
        }
    }
}

private fun updateEventDate(
    registry: Registry,
    registryId: UUID,
    eventDate: LocalDate,
    retryState: RetryState
): Mono<RetryState> {
    val existingEvenDate = registry.eventDate
    return if (existingEvenDate.isEqual(eventDate)) {
        Mono.just(true)
    } else {
        // Note: If the registry is already assigned a coupon, updating the event date should not impact the
        // coupon validity. The coupon cannot be removed due to change in event date once its assigned to a registry.
        registryRepository.updateRegistryEventDate(registryId, eventDate).map { true }
            .onErrorResume {
                logger.error("Exception from updateEventDate() for registryId: $registryId and " +
                    "eventDate $eventDate sending it for retry", it)
                Mono.just(false)
            }
    }.map {
        retryState.updateEventDate = it
        retryState
    }
}

private fun updateRegistryStatus(
    registry: Registry,
    registryId: UUID,
    registryStatus: LIST_STATE,
    retryState: RetryState
): Mono<RetryState> {
    return if (registry.registryStatus != registryStatus.value) {
        registryRepository.updateRegistryStatus(registryId, registryStatus.value).map { true }
            .onErrorResume {
                logger.error("Exception from updateRegistryStatus() for registryId: $registryId and " +
                    "registryStatus $registryStatus sending it for retry", it)
                Mono.just(false)
            }
    } else {
        logger.debug("From updateRegistryStatus(), skipping the update since registry is already ACTIVE")
        Mono.just(true)
    }.map {
        retryState.updateRegistryStatus = it
        retryState
    }
}

data class RetryState(
    var updateEventDate: Boolean = false,
    var updateRegistryStatus: Boolean = false
) {
    fun completeState(): Boolean {
        return updateEventDate && updateRegistryStatus
    }

    fun incompleteUpdateEventDateState(): Boolean {
        return !updateEventDate && updateRegistryStatus
    }

    fun incompleteUpdateRegistryStatusState(): Boolean {
        return updateEventDate && !updateRegistryStatus
    }

    fun incompleteState(): Boolean {
        return !updateEventDate && !updateRegistryStatus
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
