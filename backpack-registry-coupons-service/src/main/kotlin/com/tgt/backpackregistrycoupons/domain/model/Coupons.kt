package com.tgt.backpackregistrycoupons.domain.model

import com.tgt.backpackregistryclient.util.RegistryType
import com.tgt.backpackregistrycoupons.util.CouponType
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.MappedEntity
import java.time.LocalDate
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
    val couponExpiryDate: LocalDate,

    @Column(name = "offer_id")
    val offerId: String,

    @DateCreated
    @Column(name = "created_ts")
    var createdTs: LocalDateTime? = null,

    @DateCreated
    @Column(name = "updated_ts")
    var updatedTs: LocalDateTime? = null
)
