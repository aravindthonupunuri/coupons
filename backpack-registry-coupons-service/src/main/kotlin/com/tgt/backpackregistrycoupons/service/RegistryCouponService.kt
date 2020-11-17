package com.tgt.backpackregistrycoupons.service

import com.tgt.backpackregistrycoupons.persistence.repository.registrycoupons.RegistryCouponsRepository
import com.tgt.backpackregistrycoupons.transport.CouponsTO.Companion.toCouponsTOList
import com.tgt.backpackregistrycoupons.transport.RegistryCouponsTO
import com.tgt.backpackregistrycoupons.util.RegistryStatus
import com.tgt.backpackregistrycoupons.util.RegistryType
import io.micronaut.context.annotation.Value
import mu.KotlinLogging
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.LocalDate
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RegistryCouponService(
    @Inject private val registryCouponsRepository: RegistryCouponsRepository,
    @Value("\${registry.baby.sla}") val babyRegistrySLA: Long,
    @Value("\${registry.wedding.sla}") val weddingRegistrySLA: Long,
    @Value("\${registry.completion-coupon.sla}") val completionCouponSLA: Long
) {
    private val logger = KotlinLogging.logger { RegistryCouponService::class.java.name }

    fun getRegistryCoupons(
        registryId: UUID
    ): Mono<RegistryCouponsTO> {
        val slaMap = hashMapOf<String, Long>()
        slaMap[RegistryType.BABY.name] = babyRegistrySLA
        slaMap[RegistryType.WEDDING.name] = weddingRegistrySLA

        return registryCouponsRepository.findByIdRegistryId(registryId).collectList()
            .map {
                val registryType = it.first().registryType
                val registryStatus = RegistryStatus.ACTIVE
                val eventDate = it.first().eventDate.toLocalDate()
                val registryCreatedDate = it.first().registryCreatedTs.toLocalDate()
                val couponCountDownDays: Long = it.first().couponCode?.let {
                    if (Duration.between(registryCreatedDate.atStartOfDay(), eventDate.atStartOfDay()).toDays() >= completionCouponSLA) {
                        val duration: Duration = Duration.between(LocalDate.now().atStartOfDay(),
                            eventDate.atStartOfDay().minusDays(slaMap[registryType.name]!!))
                        if (duration.isNegative) 0L else duration.toDays()
                    } else {
                        Duration.between(LocalDate.now().atStartOfDay(), registryCreatedDate.atStartOfDay().plusDays(14)).toDays()
                    }
                } ?: 0L
                RegistryCouponsTO(registryId, registryType, registryStatus, couponCountDownDays, toCouponsTOList(it))
            }
    }
}
