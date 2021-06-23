package com.tgt.backpackregistrycoupons.service

import com.tgt.backpackregistryclient.util.RegistryStatus
import com.tgt.backpackregistrycoupons.domain.CouponAssignmentCalculationManager
import com.tgt.backpackregistrycoupons.domain.model.RegistryCoupons
import com.tgt.backpackregistrycoupons.persistence.repository.registry.RegistryRepository
import com.tgt.backpackregistrycoupons.transport.RegistryCouponsTO
import com.tgt.backpackregistrycoupons.util.CouponType
import com.tgt.backpackregistrycoupons.util.toCouponsListResponse
import com.tgt.lists.atlas.api.type.LIST_STATE
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.LocalDate
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RegistryCouponService(
    @Inject private val registryRepository: RegistryRepository,
    @Inject private val couponAssignmentCalculationManager: CouponAssignmentCalculationManager
) {

    fun getRegistryCoupons(registryId: UUID): Mono<RegistryCouponsTO> {
        return registryRepository.getByRegistryId(registryId)
            .map {
                if (!it.registryCoupons.isNullOrEmpty()) {
                    val storeCoupons = it.registryCoupons?.filter { it.couponType == CouponType.STORE }?.sortedByDescending { it.couponIssueDate }
                    val onlineCoupons = it.registryCoupons?.filter { it.couponType == CouponType.ONLINE }?.sortedByDescending { it.couponIssueDate }
                    if (!storeCoupons.isNullOrEmpty() && !onlineCoupons.isNullOrEmpty()) {
                        val registryCoupons: Set<RegistryCoupons>? = setOf(storeCoupons[0], onlineCoupons[0])
                        it.registryCoupons = registryCoupons
                    }
                }
                val registryType = it.registryType
                val registryStatus = RegistryStatus.toRegistryStatus(LIST_STATE.values().first { listState -> listState.value == it.registryStatus }.name)
                val couponCountDownDays: Long? =
                    if (registryStatus == RegistryStatus.ACTIVE) {
                        if (it.registryCoupons.isNullOrEmpty()) {
                            val couponAssignmentDate = couponAssignmentCalculationManager.calculateCouponAssignmentDate(it)
                            val duration: Duration = Duration.between(LocalDate.now().atStartOfDay(), couponAssignmentDate)
                            if (duration.isNegative) 0L else duration.toDays()
                        } else {
                            0L
                        }
                } else {
                    null
                }
                RegistryCouponsTO(registryId, it.alternateRegistryId, registryType, registryStatus, couponCountDownDays, toCouponsListResponse(it.registryCoupons ?: emptySet()))
            }
    }
}
