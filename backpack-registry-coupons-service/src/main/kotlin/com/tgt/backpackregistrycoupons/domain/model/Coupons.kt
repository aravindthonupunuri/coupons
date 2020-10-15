package com.tgt.backpackregistrycoupons.domain.model

import com.tgt.backpackregistrycoupons.util.CouponType
import com.tgt.backpackregistrycoupons.util.RegistryType
import io.micronaut.data.annotation.MappedEntity
import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Id
import javax.persistence.Table

@MappedEntity
@Table(name = "coupons")
data class Coupons(
    @Id
    @Column(name = "coupon_code")
    val couponCode: String,

    @Column(name = "coupon_type")
    val couponType: CouponType,

    @Column(name = "registry_type")
    val registryType: RegistryType,

    @Column(name = "coupon_expiry_date")
    val couponExpiryDate: LocalDateTime,

    @Column(name = "offer_id")
    val offerId: String
)
