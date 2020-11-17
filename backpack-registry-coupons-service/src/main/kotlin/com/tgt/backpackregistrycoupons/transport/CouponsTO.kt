package com.tgt.backpackregistrycoupons.transport

import com.fasterxml.jackson.annotation.JsonInclude
import com.tgt.backpackregistrycoupons.domain.model.RegistryCoupons
import com.tgt.backpackregistrycoupons.util.CouponRedemptionStatus
import com.tgt.backpackregistrycoupons.util.CouponType
import java.time.LocalDateTime
import javax.validation.constraints.NotNull

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CouponsTO(
    @field:NotNull(message = "Coupon code must not be empty") val couponCode: String?,
    @field:NotNull(message = "Coupon type must not be empty") val couponType: CouponType?,
    val couponIssueDate: LocalDateTime?,
    val couponExpiryDate: LocalDateTime?,
    val couponState: CouponRedemptionStatus?
) {
    constructor(registryCoupons: RegistryCoupons) : this(
        couponCode = registryCoupons.couponCode,
        couponType = registryCoupons.id.couponType,
        couponIssueDate = registryCoupons.couponIssueDate,
        couponExpiryDate = registryCoupons.couponExpiryDate,
        couponState = registryCoupons.couponRedemptionStatus
    )

    companion object {
        @JvmStatic
        fun toCouponsTOList(registryCouponsList: List<RegistryCoupons>): List<CouponsTO> {
            return registryCouponsList.map { CouponsTO(it) }
        }
    }
}
