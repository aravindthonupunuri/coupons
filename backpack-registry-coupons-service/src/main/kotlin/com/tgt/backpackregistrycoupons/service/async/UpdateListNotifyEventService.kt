package com.tgt.backpackregistrycoupons.service.async

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tgt.backpackregistrycoupons.domain.model.RegistryCoupons
import com.tgt.backpackregistrycoupons.persistence.repository.registrycoupons.RegistryCouponsRepository
import com.tgt.lists.atlas.api.util.LIST_STATE
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
        registryStatus: LIST_STATE,
        eventDate: LocalDateTime,
        retryState: RetryState
    ): Mono<RetryState> {
        return registryCouponsRepository.findByIdRegistryId(listId).collectList().flatMap {
            when {
                retryState.incompleteState() -> {
                    logger.debug("From processUpdateListNotifyEvent(), starting processing for listId: $listId")
                    updateEventDate(it, listId, eventDate, retryState)
                        .flatMap { retryState -> updateRegistryStatus(it, listId, registryStatus, retryState) }
                }
                retryState.incompleteUpdateEventDateState() -> {
                    logger.debug("From processUpdateListNotifyEvent(), starting processing of updateEventDate for listId: $listId")
                    updateEventDate(it, listId, eventDate, retryState)
                }
                retryState.incompleteUpdateRegistryStatusState() -> {
                    logger.debug("From processUpdateListNotifyEvent(), starting processing of updateRegistryStatus for listId: $listId")
                    updateRegistryStatus(it, listId, registryStatus, retryState)
                }
                retryState.completeState() -> {
                    logger.debug("From processUpdateListNotifyEvent(), processing complete for listId: $listId")
                    Mono.just(retryState)
                }
                else -> {
                    logger.error("Unknown step for listId $listId from processUpdateListNotifyEvent()")
                    retryState.updateEventDate = true
                    retryState.updateRegistryStatus = true
                    Mono.just(retryState)
                }
            }
        }
    }

    private fun updateEventDate(
        registryCoupons: List<RegistryCoupons>,
        listId: UUID,
        eventDate: LocalDateTime,
        retryState: RetryState
    ): Mono<RetryState> {
        val existingEvenDate = registryCoupons.firstOrNull()?.eventDate
        return if (existingEvenDate == null || existingEvenDate.isEqual(eventDate)) {
            Mono.just(true)
        } else {
            // Note: If the registry is already assigned a coupon, updating the event date should not impact the
            // coupon validity. The coupon cannot be removed due to change in event date once its assigned to a registry.
            registryCouponsRepository.updateByRegistryId(listId, eventDate).then().map { true }
        }.map {
            retryState.updateEventDate = true
            retryState
        }.onErrorResume {
            logger.error("Exception from updateEventDate() for registryId: $listId and " +
                "eventDate $eventDate sending it for retry", it)
            retryState.updateEventDate = false
            Mono.just(retryState)
        }
    }

    private fun updateRegistryStatus(
        registryCoupons: List<RegistryCoupons>,
        listId: UUID,
        registryStatus: LIST_STATE,
        retryState: RetryState
    ): Mono<RetryState> {
        return if (registryCoupons.any { it.registryStatus != registryStatus.value }) {
            registryCouponsRepository.updateByRegistryId(listId, registryStatus.value).then()
        } else {
            logger.debug("From updateRegistryStatus(), skipping the update since registry is already ACTIVE")
            Mono.just(true)
        }.map {
            retryState.updateRegistryStatus = true
            retryState
        }.onErrorResume {
            logger.error("Exception from updateRegistryStatus() for registryId: $listId and " +
                "registryStatus $registryStatus sending it for retry", it)
            retryState.updateRegistryStatus = false
            Mono.just(retryState)
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
