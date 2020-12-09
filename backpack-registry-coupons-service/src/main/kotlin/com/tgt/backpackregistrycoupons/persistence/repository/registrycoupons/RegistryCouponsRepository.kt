package com.tgt.backpackregistrycoupons.persistence.repository.registrycoupons

import com.tgt.backpackregistrycoupons.domain.model.RegistryCoupons
import com.tgt.backpackregistrycoupons.util.CouponType
import com.tgt.backpackregistrycoupons.util.CouponRedemptionStatus
import io.micronaut.data.annotation.Query
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.*

interface RegistryCouponsRepository {

    fun saveAll(registryCoupons: List<RegistryCoupons>): Flux<RegistryCoupons>

    fun save(registryCoupons: RegistryCoupons): Mono<RegistryCoupons>

    fun findByIdRegistryId(registryId: UUID): Flux<RegistryCoupons>

    fun findByRegistryStatusAndCouponCodeIsNull(registryStatus: String): Flux<RegistryCoupons>

    fun findByCouponCode(couponCode: String): Mono<RegistryCoupons>

    fun deleteByRegistryId(registryId: UUID): Mono<Int>

    @Query("UPDATE registry_coupons SET event_date = (:eventDate) WHERE registry_id=:registryId")
    fun updateRegistryEventDate(registryId: UUID, eventDate: LocalDateTime): Mono<Int>

    @Query("""UPDATE registry_coupons SET registry_status = (:registryStatus) WHERE registry_id=:registryId""")
    fun updateRegistryStatus(registryId: UUID, registryStatus: String): Mono<Int>

    @Query("""UPDATE registry_coupons SET coupon_code = (:couponCode) WHERE registry_id=:registryId AND coupon_type = (:couponType)""")
    fun updateRegistry(registryId: UUID, couponType: CouponType, couponCode: String): Mono<Int>

    @Query("""UPDATE registry_coupons SET coupon_redemption_status = (:couponRedemptionStatus) WHERE coupon_code=:couponCode""")
    fun updateStatusByCouponCode(couponCode: String, couponRedemptionStatus: CouponRedemptionStatus): Mono<Int>
}
