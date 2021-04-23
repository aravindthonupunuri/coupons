package com.tgt.backpackregistrycoupons.api.controller

import com.tgt.backpackregistryclient.util.BackpackRegistryConstants.AUTHORIZATION
import com.tgt.backpackregistrycoupons.util.RegistryCouponsConstant
import com.tgt.backpackregistrycoupons.welcomekit.service.WelcomeKitUploadService
import com.tgt.backpackregistrycoupons.welcomekit.transport.UploadWelcomeKitResponseTO
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import reactor.core.publisher.Mono

@Controller(RegistryCouponsConstant.BASEPATH)
class BackpackWelcomeKitController(
    private val welcomeKitUploadService: WelcomeKitUploadService
) {

    /**
     *
     * Upload welcome kit.
     * @return Void
     *
     */
    @Post(value = "/welcome_kits")
    @Status(HttpStatus.CREATED)
    fun uploadWelcomeKits(
        @Header(AUTHORIZATION) authorizationHeader: String,
        @Body tcins: List<String>
    ): Mono<UploadWelcomeKitResponseTO> {
        return welcomeKitUploadService.uploadWelcomeKits(tcins = tcins)
    }
}
