package com.tgt.backpackregistrycoupons.service.async

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tgt.backpackregistrycoupons.domain.model.RegistryCoupons
import com.tgt.backpackregistrycoupons.domain.model.RegistryPk
import com.tgt.backpackregistrycoupons.persistence.repository.registrycoupons.RegistryCouponsRepository
import com.tgt.backpackregistrycoupons.util.CouponType
import com.tgt.backpackregistrycoupons.util.RegistryType
import com.tgt.lists.atlas.api.type.LIST_STATE
import mu.KotlinLogging
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreateListNotifyEventService(
    @Inject val registryCouponsRepository: RegistryCouponsRepository
) {
    private val logger = KotlinLogging.logger { CreateListNotifyEventService::class.java.name }

    fun processCreateListNotifyEvent(
        guestId: String,
        listId: UUID,
        registryStatus: LIST_STATE,
        registryType: RegistryType,
        registryCreatedTs: LocalDateTime,
        eventDateTs: LocalDateTime,
        retryState: RetryState
    ): Mono<RetryState> {
        return if (retryState.incompleteState()) {
            logger.debug("From processCreateListNotifyEvent(), starting processing")
            return addGuestRegistry(guestId, listId, registryStatus, registryType, registryCreatedTs, eventDateTs)
                .map {
                    retryState.addGuestRegistry = it
                    retryState
                }
        } else {
            logger.debug("From processCreateListNotifyEvent(), processing complete")
            Mono.just(retryState)
        }
    }

    fun addGuestRegistry(
        guestId: String,
        listId: UUID,
        registryStatus: LIST_STATE,
        registryType: RegistryType,
        registryCreatedTs: LocalDateTime,
        eventDateTs: LocalDateTime
    ): Mono<Boolean> {
        val guestRegistry = arrayListOf<RegistryCoupons>()
        // Adding 2 records for every registry created, for ONLINE and STORE coupons assignment. The RegistryStatus
        // is INACTIVE when the registry is created. RegistryStatus is updated to ACTIVE once an item is added to the
        // registry.
        guestRegistry.add(RegistryCoupons(RegistryPk(listId, CouponType.ONLINE), registryType, registryStatus.value,
            registryCreatedTs, eventDateTs, null, false, null, null,
            null, guestId, guestId))
        guestRegistry.add(RegistryCoupons(RegistryPk(listId, CouponType.STORE), registryType, registryStatus.value,
            registryCreatedTs, eventDateTs, null, false, null, null,
            null, guestId, guestId))

        return registryCouponsRepository.saveAll(guestRegistry).then().map { true }
            .onErrorResume {
                logger.error("Exception from addGuestRegistry() for guestID: $guestId and registryId: $listId," +
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
