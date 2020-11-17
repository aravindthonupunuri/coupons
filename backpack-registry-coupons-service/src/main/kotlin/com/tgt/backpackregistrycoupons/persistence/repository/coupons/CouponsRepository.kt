package com.tgt.backpackregistrycoupons.persistence.repository.coupons

import com.tgt.backpackregistrycoupons.domain.model.Coupons
import com.tgt.backpackregistrycoupons.util.CouponType
import com.tgt.backpackregistrycoupons.util.RegistryType
import io.micronaut.data.annotation.Query
import reactor.core.publisher.Mono
import java.util.*

interface CouponsRepository {
    /*
    Insert RegistryCoupons
    */
    fun save(coupons: Coupons): Mono<Coupons>

    fun existsByCouponCode(couponCode: String): Mono<Boolean>

    @Query(""" SELECT * FROM coupons  WHERE coupon_type = (:couponType) AND registry_type = (:registryType) LIMIT 1 """)
    fun findCouponCode(couponType: CouponType, registryType: RegistryType): Mono<Coupons>

    @Query(""" DELETE FROM coupons WHERE coupon_code = (:couponCode) """)
    fun deleteByCouponCode(couponCode: String): Mono<Int>
}
