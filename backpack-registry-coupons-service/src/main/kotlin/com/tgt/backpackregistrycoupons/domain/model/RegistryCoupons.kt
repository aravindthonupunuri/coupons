package com.tgt.backpackregistrycoupons.domain.model

import com.tgt.backpackregistrycoupons.util.CouponType
import com.tgt.backpackregistrycoupons.util.RegistryType
import io.micronaut.data.annotation.MappedEntity
import java.time.LocalDate
import java.util.*
import javax.persistence.Column
import javax.persistence.Id
import javax.persistence.Table

@MappedEntity
@Table(name = "registry_coupons")
data class RegistryCoupons(
    @Id
    @Column(name = "coupon_id")
    val couponId: String,

    @Column(name = "coupon_type")
    val couponType: CouponType,

    @Column(name = "registry_type")
    val registryType: RegistryType,

    @Column(name = "coupon_expiry_date")
    val couponExpiryDate: LocalDate,

    @Column(name = "offer_id")
    val offerId: String
)
