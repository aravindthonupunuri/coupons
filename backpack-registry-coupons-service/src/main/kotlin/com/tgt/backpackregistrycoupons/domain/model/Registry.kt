package com.tgt.backpackregistrycoupons.domain.model

import com.tgt.backpackregistrycoupons.util.CouponType
import io.micronaut.data.annotation.Embeddable
import java.util.*
import javax.persistence.Column

@Embeddable
data class Registry(

    @Column(name = "registry_id")
    val registryId: UUID,

    @Column(name = "coupon_type")
    val couponType: CouponType
)
