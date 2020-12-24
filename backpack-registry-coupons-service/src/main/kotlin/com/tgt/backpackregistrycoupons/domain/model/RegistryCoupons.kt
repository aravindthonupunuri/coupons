package com.tgt.backpackregistrycoupons.domain.model

import com.tgt.backpackregistrycoupons.util.CouponRedemptionStatus
import com.tgt.backpackregistrycoupons.util.CouponType
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import java.time.LocalDate
import javax.annotation.Nullable
import javax.persistence.Column
import javax.persistence.Id
import javax.persistence.Table

@MappedEntity
@Table(name = "registry_coupons")
data class RegistryCoupons(
    @Id
    @Column(name = "coupon_code")
    val couponCode: String?,

    /*
    Column registry_id is used as foreign key to Registry,
     */
    @Relation(value = Relation.Kind.MANY_TO_ONE)
    @Column(name = "registry_id")
    @Nullable
    var registry: Registry?,

    @Column(name = "coupon_type")
    val couponType: CouponType,

    @Column(name = "coupon_redemption_status")
    val couponRedemptionStatus: CouponRedemptionStatus?,

    @Column(name = "coupon_issue_date")
    val couponIssueDate: LocalDate?,

    @Column(name = "coupon_expiry_date")
    val couponExpiryDate: LocalDate?,

    @DateCreated
    @Column(name = "created_ts")
    var createdTs: LocalDate? = null,

    @DateCreated
    @Column(name = "updated_ts")
    var updatedTs: LocalDate? = null

)
