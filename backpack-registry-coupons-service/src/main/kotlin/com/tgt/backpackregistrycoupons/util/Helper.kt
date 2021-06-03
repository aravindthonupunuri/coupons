package com.tgt.backpackregistrycoupons.util

import com.tgt.backpackregistryclient.util.RegistryType
import com.tgt.backpackregistrycoupons.domain.model.RegistryCoupons
import com.tgt.backpackregistrycoupons.transport.CouponsTO

fun toCouponsListResponse(registryCouponsList: Set<RegistryCoupons>): List<CouponsTO> {
    return registryCouponsList.map {
        CouponsTO(
            couponCode = it.couponCode,
            couponType = it.couponType,
            couponIssueDate = it.couponIssueDate,
            couponExpiryDate = it.couponExpiryDate,
            couponState = it.couponRedemptionStatus)
    }
}

fun toCouponRedemptionStatus(status: String): CouponRedemptionStatus {
    return CouponRedemptionStatus.values().find { it.name.equals(status, ignoreCase = true) } ?: throw RuntimeException("Unsupported CouponRedemptionStatus value: $status")
}

fun isNotBabyOrWeddingRegistryType(registryType: RegistryType): Boolean {
    return registryType !in listOf(RegistryType.BABY, RegistryType.WEDDING)
}
