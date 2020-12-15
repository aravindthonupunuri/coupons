package com.tgt.backpackregistrycoupons.service

import com.tgt.backpackregistryclient.util.RegistryStatus
import com.tgt.backpackregistryclient.util.RegistryType
import com.tgt.backpackregistrycoupons.domain.CouponAssignmentCalculationManager
import com.tgt.backpackregistrycoupons.persistence.repository.registry.RegistryRepository
import com.tgt.backpackregistrycoupons.transport.CouponsTO.Companion.toCouponsTOList
import com.tgt.backpackregistrycoupons.transport.RegistryCouponsTO
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
    @Inject private val registryRepository: RegistryRepository,
    @Inject private val couponAssignmentCalculationManager: CouponAssignmentCalculationManager,
    @Value("\${registry.baby.sla}") val babyRegistrySLA: Long,
    @Value("\${registry.wedding.sla}") val weddingRegistrySLA: Long
) {
private val logger = KotlinLogging.logger { RegistryCouponService::class.java.name }

fun getRegistryCoupons(
    registryId: UUID
): Mono<RegistryCouponsTO> {
    val slaMap = hashMapOf<String, Long>()
    slaMap[RegistryType.BABY.name] = babyRegistrySLA
    slaMap[RegistryType.WEDDING.name] = weddingRegistrySLA

    return registryRepository.getByRegistryId(registryId)
        .map {
            val registryType = it.registryType
            val registryStatus = RegistryStatus.ACTIVE
            val couponCountDownDays: Long = if (it.registryCoupons.isNullOrEmpty()) {
                val couponAssignmentDate = couponAssignmentCalculationManager.calculateCouponAssignmentDate(it)
                val duration: Duration = Duration.between(LocalDate.now().atStartOfDay(), couponAssignmentDate.minusDays(slaMap[registryType.name]!!))
                if (duration.isNegative) 0L else duration.toDays()
            } else {
                0L
            }
            RegistryCouponsTO(registryId, registryType, registryStatus, couponCountDownDays, toCouponsTOList(it.registryCoupons ?: emptySet()))
        }
}
}
