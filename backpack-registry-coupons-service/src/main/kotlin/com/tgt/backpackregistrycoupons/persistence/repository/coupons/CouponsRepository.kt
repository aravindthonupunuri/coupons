package com.tgt.backpackregistrycoupons.persistence.repository.coupons

import com.tgt.backpackregistrycoupons.domain.model.Coupons
import com.tgt.backpackregistrycoupons.util.CouponType
import com.tgt.backpackregistrycoupons.util.RegistryType
import reactor.core.publisher.Mono
import java.util.*

interface CouponsRepository {

    fun save(coupons: Coupons): Mono<Coupons>

    fun existsByCouponCode(couponCode: String): Mono<Boolean>

    fun findTop1ByCouponTypeAndRegistryType(couponType: CouponType, registryType: RegistryType): Mono<Coupons>

    fun deleteByCouponCode(couponCode: String): Mono<Int>
}
