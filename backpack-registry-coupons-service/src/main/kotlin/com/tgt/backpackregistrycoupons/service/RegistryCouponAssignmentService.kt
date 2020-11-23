package com.tgt.backpackregistrycoupons.service

import com.tgt.backpackregistrycoupons.domain.model.RegistryCoupons
import com.tgt.backpackregistrycoupons.persistence.repository.registrycoupons.RegistryCouponsRepository
import com.tgt.backpackregistrycoupons.util.RegistryType
import io.micronaut.context.annotation.Value
import mu.KotlinLogging
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RegistryCouponAssignmentService(
    @Inject private val registryCouponsRepository: RegistryCouponsRepository,
    @Value("\${registry.baby.sla}") val babyRegistrySLA: Long,
    @Value("\${registry.wedding.sla}") val weddingRegistrySLA: Long,
    @Value("\${registry.completion-coupon.sla}") val completionCouponSLA: Long
) {
    private val logger = KotlinLogging.logger { RegistryCouponAssignmentService::class.java.name }

    fun calculateCouponAssignmentDate(
        registryCoupon: RegistryCoupons
    ): LocalDateTime {
        val slaMap = hashMapOf<String, Long>()
        slaMap[RegistryType.BABY.name] = babyRegistrySLA
        slaMap[RegistryType.WEDDING.name] = weddingRegistrySLA

        val registryType = registryCoupon.registryType
        val registryCreatedDate = registryCoupon.createdTs?.toLocalDate()?.atStartOfDay()!!
        val registryEventDate = registryCoupon.eventDate.toLocalDate().atStartOfDay()!!
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
