package com.tgt.backpackregistrycoupons.persistence.repository.coupons

import com.tgt.backpackregistryclient.util.RegistryType
import com.tgt.backpackregistrycoupons.domain.model.Coupons
import com.tgt.backpackregistrycoupons.util.CouponType
import io.micronaut.data.annotation.Query
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate

interface CouponsRepository {

    fun save(coupons: Coupons): Mono<Coupons>

    fun existsByCouponCode(couponCode: String): Mono<Boolean>

    fun findByRegistryType(registryType: RegistryType): Flux<Coupons>

    @Query("""SELECT * FROM COUPONS WHERE coupon_type IN (:couponType)""")
    fun findByCouponTypeInList(couponType: List<CouponType>): Flux<Coupons>

    fun findTop1ByCouponTypeAndRegistryTypeAndCouponExpiryDateGreaterThanEquals(couponType: CouponType, registryType: RegistryType, couponExpiryDate: LocalDate): Mono<Coupons>

    @Query("""SELECT * FROM COUPONS a INNER JOIN (SELECT coupon_type, MIN(created_ts) min_created_ts FROM COUPONS WHERE coupon_type IN (:couponType) AND registry_type=(:registryType) GROUP BY coupon_type) b ON a.coupon_type = b.coupon_type AND a.created_ts = b.min_create_ts""")
    fun findTop1ByCouponTypeInListAndRegistryType(couponType: List<CouponType>, registryType: RegistryType): Mono<Coupons>

    fun deleteByCouponCodeInList(couponCode: List<String>): Mono<Int>
}
