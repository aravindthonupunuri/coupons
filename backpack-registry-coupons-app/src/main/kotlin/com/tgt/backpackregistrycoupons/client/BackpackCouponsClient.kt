package com.tgt.backpackregistrycoupons.client

import com.tgt.backpackregistryclient.util.BackpackRegistryConstants
import com.tgt.backpackregistryclient.util.RegistryType
import com.tgt.backpackregistrycoupons.transport.RegistryCouponsTO
import com.tgt.backpackregistrycoupons.util.CouponType
import com.tgt.backpackregistrycoupons.util.RegistryCouponsConstant
import com.tgt.backpacktransactionsclient.util.BackpackTransactionsConstants
import io.micronaut.core.convert.format.Format
import io.micronaut.http.annotation.*
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.multipart.StreamingFileUpload
import io.micronaut.validation.Validated
import reactor.core.publisher.Mono
import java.util.*

@Client(id = "backpack-coupons-api", path = RegistryCouponsConstant.BASEPATH)
@Validated
interface BackpackCouponsClient {
    @Get("/{registry_id}")
    fun getRegistryCoupons(
        @Header(BackpackTransactionsConstants.PROFILE_ID) guestId: String,
        @QueryValue("location_id") locationId: Long?,
        @PathVariable("registry_id") registryId: UUID
    ): Mono<RegistryCouponsTO>

    @Post("/uploads")
    fun uploadRegistryCoupons(
        @Header(BackpackRegistryConstants.AUTHORIZATION) authorizationHeader: String,
        @QueryValue("registry_type") registryType: RegistryType,
        @QueryValue("coupon_type") couponType: CouponType,
        @QueryValue("offer_id") offerId: String,
        @QueryValue("coupon_expiry_date") @Format("dd-MM-yyyy") couponExpiryDate: String,
        @Body coupons: StreamingFileUpload
    ): Mono<Void>
}
