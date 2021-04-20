package com.tgt.backpackregistrycoupons.kafka.migration.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tgt.backpackregistryclient.util.RegistryType
import com.tgt.backpackregistrycoupons.domain.model.RegistryCoupons
import com.tgt.backpackregistrycoupons.kafka.migration.model.RegistryCouponMetaDataTO
import com.tgt.backpackregistrycoupons.persistence.repository.registry.RegistryRepository
import com.tgt.backpackregistrycoupons.persistence.repository.registrycoupons.RegistryCouponsRepository
import com.tgt.backpackregistrycoupons.util.CouponType
import mu.KotlinLogging
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MigrationAssignCouponNotifyEventService(
    @Inject val registryCouponsRepository: RegistryCouponsRepository,
    @Inject val registryRepository: RegistryRepository
) {
    private val logger = KotlinLogging.logger { MigrationAssignCouponNotifyEventService::class.java.name }

    fun processMigrationAssignCouponNotifyEvent(
        guestId: String,
        registryId: UUID,
        registryType: RegistryType,
        registryCouponMetaDataTO: RegistryCouponMetaDataTO,
        retryState: RetryState
    ): Mono<RetryState> {
        return if (retryState.incompleteState()) {
            logger.debug("[MigrationAssignCouponNotifyEventService] From processMigrationAssignCouponNotifyEvent(), starting processing")
            return assignRegistryCoupon(registryId, registryCouponMetaDataTO)
                .map {
                    retryState.assignCoupon = it
                    retryState
                }
        } else {
            logger.debug("[MigrationCreateListNotifyEventService] From processMigrationCreateListNotifyEvent(), processing complete")
            Mono.just(retryState)
        }
    }

    fun assignRegistryCoupon(
        registryId: UUID,
        registryCouponMetaDataTO: RegistryCouponMetaDataTO
    ): Mono<Boolean> {
        return registryRepository.findByRegistryId(registryId).flatMap {
            val couponsList = arrayListOf<RegistryCoupons>()
            if (registryCouponMetaDataTO.onlineCouponCode != null) {
                couponsList.add(RegistryCoupons(registryCouponMetaDataTO.onlineCouponCode, it, CouponType.ONLINE, registryCouponMetaDataTO.onlineCouponStatus, registryCouponMetaDataTO.couponIssueDate, registryCouponMetaDataTO.couponExpiryDate, null, null))
            }
            if (registryCouponMetaDataTO.storeCouponCode != null) {
                couponsList.add(RegistryCoupons(registryCouponMetaDataTO.storeCouponCode, it, CouponType.STORE, registryCouponMetaDataTO.storeCouponStatus, registryCouponMetaDataTO.couponIssueDate, registryCouponMetaDataTO.couponExpiryDate, null, null))
            }

            if (couponsList.isNotEmpty()) {
                registryCouponsRepository.saveAll(couponsList).collectList().map { true }
            } else {
                Mono.just(true)
            }
        }.switchIfEmpty {
            logger.error("[MigrationAssignCouponNotifyEventService] Registry $registryId not found sending it for retry")
            Mono.just(false)
        }.onErrorResume {
            logger.error("[MigrationAssignCouponNotifyEventService] Exception from assignRegistryCoupon() for registryId: $registryId with online coupon code: ${registryCouponMetaDataTO.onlineCouponCode} and store coupon code ${registryCouponMetaDataTO.storeCouponCode} sending it for retry", it)
            Mono.just(false)
        }
    }

    data class RetryState(var assignCoupon: Boolean = false) {
        fun completeState(): Boolean {
            return assignCoupon
        }

        fun incompleteState(): Boolean {
            return !assignCoupon
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
