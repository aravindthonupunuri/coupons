package com.tgt.backpackregistrycoupons.api.controller

import com.tgt.backpackregistryclient.util.RegistryType
import com.tgt.backpackregistrycoupons.service.RegistryCouponService
import com.tgt.backpackregistrycoupons.service.UploadRegistryCouponService
import com.tgt.backpackregistrycoupons.transport.RegistryCouponsTO
import com.tgt.backpackregistrycoupons.util.CouponType
import com.tgt.backpackregistrycoupons.util.RegistryCouponsConstant
import com.tgt.backpacktransactionsclient.util.BackpackTransactionsConstants.PROFILE_ID
import io.micronaut.core.convert.format.Format
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.multipart.StreamingFileUpload
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.io.File
import java.time.LocalDate
import java.util.*

@Controller(RegistryCouponsConstant.BASEPATH)
class BackpackRegistryCouponsController(
    private val uploadRegistryCouponService: UploadRegistryCouponService,
    private val registryCouponService: RegistryCouponService
) {

    /**
     *
     * Upload coupons.
     *
     * @param registryType registry type
     * @param couponType coupon type
     * @param offerId offer id
     * @param couponExpiryDate coupon expiry date
     * @return Void
     *
     */
    @Post(value = "/upload", consumes = [MediaType.MULTIPART_FORM_DATA])
    @Status(HttpStatus.NO_CONTENT)
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
                    uploadRegistryCouponService.uploadRegistryCoupons(
                        registryType = registryType,
                        couponType = couponType,
                        offerId = offerId,
                        couponExpiryDate = LocalDate.parse(couponExpiryDate), // default, ISO_LOCAL_DATE("2016-08-16")
                        file = tempFile)
                } else {
                    throw RuntimeException("Exception uploading coupon codes")
                }
            }.then()
    }

    /**
     *
     * Get registry Coupons list.
     *
     * @param guestId guestId
     * @param locationId location id
     * @param registryId registry id
     * @return registry coupons transfer object
     *
     */
    @Get("/{registry_id}")
    @Status(HttpStatus.OK)
    @ApiResponse(content = [Content(mediaType = "application/json", schema = Schema(implementation = RegistryCouponsTO::class))])
    fun getRegistryCoupons(
        @Header(PROFILE_ID) guestId: String,
        @QueryValue("location_id") locationId: Long?,
        @PathVariable("registry_id") registryId: UUID
    ): Mono<RegistryCouponsTO> {
        return registryCouponService.getRegistryCoupons(registryId)
    }
}
