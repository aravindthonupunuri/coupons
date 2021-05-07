package com.tgt.backpackregistrycoupons.persistence.repository.registrycoupons

import com.tgt.backpackregistrycoupons.domain.model.RegistryCoupons
import com.tgt.backpackregistrycoupons.util.CouponType
import com.tgt.backpackregistrycoupons.util.CouponRedemptionStatus
import io.micronaut.data.annotation.Join
import io.micronaut.data.annotation.Query
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

interface RegistryCouponsRepository {

    fun save(registryCoupons: RegistryCoupons): Mono<RegistryCoupons>

    fun saveAll(registryCoupons: List<RegistryCoupons>): Flux<RegistryCoupons>

    @Join(value = "registry", type = Join.Type.FETCH)
    fun findByCouponCode(couponCode: String): Mono<RegistryCoupons>

    @Query("UPDATE registry_coupons SET Coupon_redemption_status = (:CouponRedemptionStatus) WHERE coupon_code=(:couponCode)")
    fun updateCouponRedemptionStatus(couponCode: String, CouponRedemptionStatus: String): Mono<Int>
}
