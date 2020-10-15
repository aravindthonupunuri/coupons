package com.tgt.backpackregistrycoupons.api.controller

import com.tgt.backpackregistrycoupons.service.UploadRegistryCouponService
import com.tgt.backpackregistrycoupons.util.CouponType
import com.tgt.backpackregistrycoupons.util.RegistryCouponsConstant
import com.tgt.backpackregistrycoupons.util.RegistryType
import io.micronaut.core.convert.format.Format
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.multipart.StreamingFileUpload
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.io.File
import java.time.LocalDate

@Controller(RegistryCouponsConstant.BASEPATH)
class BackpackRegistryCouponsController(
    private val uploadRegistryCouponService: UploadRegistryCouponService

) {

    /**
     *
     * Upload coupons.
     *
     */
    @Post(value = "/upload", consumes = [MediaType.MULTIPART_FORM_DATA])
    @Status(HttpStatus.CREATED)
    fun uploadRegistryCoupons(
        @QueryValue("registry_type") registryType: RegistryType,
        @QueryValue("coupon_type") couponType: CouponType,
        @QueryValue("offer_id") offerId: String,
        @QueryValue("coupon_expiry_date") @Format("dd-MM-yyyy") couponExpiryDate: String,
        @Body coupons: StreamingFileUpload
    ): Mono<Void> {
        val tempFile = File.createTempFile(coupons.filename, "temp")
        val uploadPublisher = coupons.transferTo(tempFile)
        return uploadPublisher.toMono()
            .flatMap {
                if (it) {
                    uploadRegistryCouponService.uploadRegistryCoupons(registryType, couponType,
                        offerId, LocalDate.parse(couponExpiryDate).atStartOfDay(), tempFile)
                } else {
                    throw RuntimeException("Exception uploading coupon codes")
                }
            }.then()
    }
}
