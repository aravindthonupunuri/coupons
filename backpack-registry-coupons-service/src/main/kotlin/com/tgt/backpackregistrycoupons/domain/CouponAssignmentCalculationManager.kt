package com.tgt.backpackregistrycoupons.domain

import com.tgt.backpackregistryclient.util.RegistryType
import com.tgt.backpackregistrycoupons.domain.model.Registry
import io.micronaut.context.annotation.Value
import mu.KotlinLogging
import java.time.LocalDateTime
import javax.inject.Singleton

@Singleton
class CouponAssignmentCalculationManager(
    @Value("\${registry.baby.sla}") val babyRegistrySLA: Long,
    @Value("\${registry.wedding.sla}") val weddingRegistrySLA: Long,
    @Value("\${registry.completion-coupon.sla}") val completionCouponSLA: Long
) {
private val logger = KotlinLogging.logger { CouponAssignmentCalculationManager::class.java.name }

fun calculateCouponAssignmentDate(
    registry: Registry
): LocalDateTime {
    val slaMap = hashMapOf<String, Long>()
    slaMap[RegistryType.BABY.name] = babyRegistrySLA
    slaMap[RegistryType.WEDDING.name] = weddingRegistrySLA

    val registryType = registry.registryType
    val registryCreatedDate = registry.registryCreatedTs.toLocalDate()?.atStartOfDay()!!
    val registryEventDate = registry.eventDate.atStartOfDay()!!
    val earliestCouponAssignmentDate = registryCreatedDate.plusDays(completionCouponSLA)

    return if (earliestCouponAssignmentDate.isAfter(registryEventDate)) {
        earliestCouponAssignmentDate
    } else {
        val registryQualifyingDate = registryEventDate.minusDays(slaMap[registryType.name]!!)
        if (registryQualifyingDate.isAfter(earliestCouponAssignmentDate)) {
            registryQualifyingDate
        } else {
            earliestCouponAssignmentDate
        }
    }
}
}
