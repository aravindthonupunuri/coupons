package com.tgt.backpackregistrycoupons.service.async

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tgt.backpackregistryclient.util.RegistryType
import com.tgt.backpackregistrycoupons.domain.model.Registry
import com.tgt.backpackregistrycoupons.persistence.repository.registry.RegistryRepository
import com.tgt.lists.atlas.api.type.LIST_STATE
import mu.KotlinLogging
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject

class CreateCollegeListNotifyEventService(
    @Inject val registryCouponsRepository: RegistryRepository
) {
    private val logger = KotlinLogging.logger { CreateCollegeListNotifyEventService::class.java.name }

    fun processCreateCollegeListNotifyEvent(
        guestId: String,
        registryId: UUID,
        alternateRegistryId: String?,
        registryStatus: LIST_STATE,
        registryType: RegistryType,
        registryCreatedDate: LocalDate,
        eventDate: LocalDate,
        addedDate: LocalDateTime?,
        lastModifiedDate: LocalDateTime?,
        retryState: RetryState
    ): Mono<RetryState> {
        return if (retryState.incompleteState()) {
            logger.debug("From processCreateCollegeListNotifyEvent(), starting processing")
            return addGuestRegistry(guestId, registryId, alternateRegistryId, registryStatus, registryType, registryCreatedDate, eventDate, addedDate, lastModifiedDate)
                .map {
                    retryState.addGuestRegistry = it
                    retryState
                }
        } else {
            logger.debug("From processCreateCollegeListNotifyEvent(), processing complete")
            Mono.just(retryState)
        }
    }

    private fun addGuestRegistry(
        guestId: String,
        registryId: UUID,
        alternateRegistryId: String?,
        registryStatus: LIST_STATE,
        registryType: RegistryType,
        registryCreatedDate: LocalDate,
        eventDate: LocalDate,
        addedDate: LocalDateTime?,
        lastModifiedDate: LocalDateTime?
    ): Mono<Boolean> {
        // The RegistryStatus is INACTIVE when the registry is created. RegistryStatus is updated to ACTIVE once an item is added to the registry.
        // Coupon is assigned only for BABY and WEDDING registries, ignore all the other types
        return registryCouponsRepository.save(Registry(registryId, alternateRegistryId, registryType, registryStatus.value, registryCreatedDate, eventDate, false, addedDate, lastModifiedDate))
            .map { true }
            .onErrorResume {
                logger.error("Exception from addGuestRegistry() for guestID: $guestId and registryId: $registryId," +
                    " sending it for retry", it)
                Mono.just(false)
            }
    }

    data class RetryState(
        var addGuestRegistry: Boolean = false
    ) {
        fun completeState(): Boolean {
            return addGuestRegistry
        }

        fun incompleteState(): Boolean {
            return !addGuestRegistry
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
