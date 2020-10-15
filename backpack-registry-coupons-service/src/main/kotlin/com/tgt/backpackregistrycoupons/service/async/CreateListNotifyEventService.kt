package com.tgt.backpackregistrycoupons.service.async

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tgt.backpackregistrycoupons.domain.model.Registry
import com.tgt.backpackregistrycoupons.domain.model.RegistryCoupons
import com.tgt.backpackregistrycoupons.persistence.repository.registrycoupons.RegistryCouponsRepository
import com.tgt.backpackregistrycoupons.util.CouponType
import com.tgt.backpackregistrycoupons.util.RegistryType
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
        registryType: RegistryType,
        registryCreatedTs: LocalDateTime,
        eventDateTs: LocalDateTime,
        retryState: RetryState
    ): Mono<RetryState> {
        return if (retryState.incompleteState()) {
            logger.debug("From processCreateListNotifyEvent(), starting processing")
            return addGuestRegistry(guestId, listId, registryType, registryCreatedTs, eventDateTs)
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
        registryType: RegistryType,
        registryCreatedTs: LocalDateTime,
        eventDateTs: LocalDateTime
    ): Mono<Boolean> {
        val guestRegistry = arrayListOf<RegistryCoupons>()
        guestRegistry.add(RegistryCoupons(Registry(listId, CouponType.ONLINE), registryType, registryCreatedTs,
            eventDateTs, null, false, null, null,
            null, guestId, guestId, LocalDateTime.now(), LocalDateTime.now()))
        guestRegistry.add(RegistryCoupons(Registry(listId, CouponType.STORE), registryType, registryCreatedTs,
            eventDateTs, null, false, null, null,
            null, guestId, guestId, LocalDateTime.now(), LocalDateTime.now()))

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
