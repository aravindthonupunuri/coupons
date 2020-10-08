package com.tgt.backpackregistrycoupons.service

import com.tgt.backpackregistrycoupons.domain.model.RegistryCoupons
import com.tgt.backpackregistrycoupons.persistence.repository.registrycoupons.RegistryCouponsRepository
import com.tgt.backpackregistrycoupons.util.CouponType
import com.tgt.backpackregistrycoupons.util.RegistryType
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadRegistryCouponService(
    @Inject private val registryCouponsRepository: RegistryCouponsRepository
) {
    private val logger = KotlinLogging.logger {}

    fun uploadRegistryCoupons(
        registryType: RegistryType,
        couponType: CouponType,
        offerId: String,
        couponExpiryDate: LocalDate,
        file: File
    ): Mono<Void> {
        return Flux.fromStream(BufferedReader(FileReader(file)).lines()).flatMap { coupon ->
            if (coupon.isNullOrEmpty()) {
                logger.debug { "Empty coupon code" }
                Mono.just(true)
            } else {
                val couponCode = coupon.trim()
                registryCouponsRepository.existsByCouponId(couponCode).flatMap {
                    if (it) {
                        logger.debug { "Coupon code already exists $couponCode" }
                        Mono.just(true)
                    } else {
                        registryCouponsRepository.save(RegistryCoupons(couponCode, couponType, registryType, couponExpiryDate, offerId))
                    }
                }
            }
        }.collectList().then()
    }
}
