package com.tgt.backpackregistrycoupons.persistence.repository.coupons

import com.tgt.backpackregistrycoupons.domain.model.Coupons
import reactor.core.publisher.Mono

interface CouponsRepository {
    /*
    Insert RegistryCoupons
    */
    fun save(coupons: Coupons): Mono<Coupons>

    fun existsByCouponCode(couponCode: String): Mono<Boolean>
}
