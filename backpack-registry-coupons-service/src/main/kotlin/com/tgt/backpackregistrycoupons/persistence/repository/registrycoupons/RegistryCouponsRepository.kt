package com.tgt.backpackregistrycoupons.persistence.repository.registrycoupons

import com.tgt.backpackregistrycoupons.domain.model.RegistryCoupons
import reactor.core.publisher.Mono

interface RegistryCouponsRepository {
    /*
    Insert RegistryCoupons
    */
    fun save(registryCoupons: RegistryCoupons): Mono<RegistryCoupons>

    fun existsByCouponId(couponId: String): Mono<Boolean>
}
