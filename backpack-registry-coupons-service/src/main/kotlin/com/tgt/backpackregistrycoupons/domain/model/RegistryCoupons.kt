package com.tgt.backpackregistrycoupons.domain.model

import com.tgt.backpackregistrycoupons.util.CouponRedemptionStatus
import com.tgt.backpackregistrycoupons.util.RegistryType
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.EmbeddedId
import io.micronaut.data.annotation.MappedEntity
import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Table

@MappedEntity
@Table(name = "registry_coupons")
data class RegistryCoupons(
    @EmbeddedId
    val id: RegistryPk,

    @Column(name = "registry_type")
    val registryType: RegistryType,

    @Column(name = "registry_created_ts")
    val registryCreatedTs: LocalDateTime,

    @Column(name = "event_date")
    val eventDate: LocalDateTime,

    @Column(name = "coupon_code")
    val couponCode: String?,

    @Column(name = "coupon_notified")
    val couponNotified: Boolean,

    @Column(name = "coupon_redemption_status")
    val couponRedemptionStatus: CouponRedemptionStatus?,

    @Column(name = "coupon_issue_date")
    val couponIssueDate: LocalDateTime?,

    @Column(name = "coupon_expiry_date")
    val couponExpiryDate: LocalDateTime?,

    @Column(name = "created_user")
    val createdUser: String,

    @Column(name = "updated_user")
    val updatedUser: String,

    @DateCreated
    @Column(name = "created_ts")
    var createdTs: LocalDateTime? = null,

    @DateCreated
    @Column(name = "updated_ts")
    var updatedTs: LocalDateTime? = null

)
