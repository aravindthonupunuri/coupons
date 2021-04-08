package com.tgt.backpackregistrycoupons.util

import com.tgt.backpackregistrycoupons.domain.model.RegistryCoupons
import com.tgt.backpackregistrycoupons.transport.CouponsTO

fun toCouponsTOList(registryCouponsList: Set<RegistryCoupons>): List<CouponsTO> {
    return registryCouponsList.map {
        CouponsTO(
            couponCode = it.couponCode,
            couponType = it.couponType,
            couponIssueDate = it.couponIssueDate,
            couponExpiryDate = it.couponExpiryDate,
            couponState = it.couponRedemptionStatus)
    }
}
