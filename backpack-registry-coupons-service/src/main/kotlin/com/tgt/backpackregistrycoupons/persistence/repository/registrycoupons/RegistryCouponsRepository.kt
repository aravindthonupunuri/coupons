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

    @Query("""SELECT * FROM registry_coupons WHERE registry_status='A' AND coupon_code IS NULL""")
    fun findUnAssignedActiveRegistries(): Flux<RegistryCoupons>

    @Query("""SELECT * FROM registry_coupons WHERE coupon_code=:couponCode""")
    fun findByCouponCode(couponCode: String): Mono<RegistryCoupons>

    @Query("""DELETE FROM registry_coupons WHERE registry_id=uuid(:registryId)""")
    fun deleteByIdRegistryId(registryId: UUID): Mono<Int>

    @Query("""UPDATE registry_coupons SET event_date = (:eventDate) WHERE registry_id=uuid(:registryId)""")
    fun updateByRegistryId(registryId: UUID, eventDate: LocalDateTime): Mono<Int>

    @Query("""UPDATE registry_coupons SET registry_status = (:registryStatus) WHERE registry_id=uuid(:registryId)""")
    fun updateByRegistryId(registryId: UUID, registryStatus: String): Mono<Int>

    @Query("""UPDATE registry_coupons SET coupon_code = (:couponCode) WHERE registry_id=uuid(:registryId) AND coupon_type = (:couponType)""")
    fun updateByRegistryId(registryId: UUID, couponType: CouponType, couponCode: String): Mono<Int>

    @Query("""UPDATE registry_coupons SET coupon_redemption_status = (:couponRedemptionStatus) WHERE coupon_code=:couponCode""")
    fun updateStatusByCouponCode(couponCode: String, couponRedemptionStatus: CouponRedemptionStatus): Mono<Int>
}
