package com.tgt.backpackregistrycoupons.service.async

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tgt.backpackregistryclient.util.RegistryType
import com.tgt.backpackregistrycoupons.persistence.repository.compositetransaction.CompositeTransactionalRepository
import com.tgt.backpackregistrycoupons.util.isNotBabyOrWeddingRegistryType
import mu.KotlinLogging
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeleteListNotifyEventService(
    @Inject val compositeTransactionalRepository: CompositeTransactionalRepository
) {
private val logger = KotlinLogging.logger { DeleteListNotifyEventService::class.java.name }

fun processDeleteListNotifyEvent(
    registryId: UUID,
    registryType: RegistryType,
    retryState: RetryState
): Mono<RetryState> {
    return if (retryState.incompleteState()) {
        logger.debug("From processDeleteListNotifyEvent(), starting processing")
        deleteGuestRegistry(registryId, registryType)
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
    registryId: UUID,
    registryType: RegistryType
): Mono<Boolean> {
    return if (isNotBabyOrWeddingRegistryType(registryType))
        Mono.just(true)
    else {
        compositeTransactionalRepository.deleteRegistryCascaded(registryId)
            .map { true }
            .onErrorResume {
                logger.error("Exception from deleteGuestRegistry() for registryId: $registryId " +
                    "sending it for retry", it)
                Mono.just(false)
            }
            .switchIfEmpty {
                logger.error("Exception from deleteGuestRegistry(), registryId: $registryId not found to delete")
                Mono.just(true)
            }
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
