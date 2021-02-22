package com.tgt.backpackregistrycoupons.api

import com.target.platform.connector.micronaut.config.PlatformPropertySource
import com.tgt.lists.common.components.tap.TAPEnvironmentLoader
import io.micronaut.runtime.Micronaut
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info

import java.util.*

@OpenAPIDefinition(info = Info(title = "backpack-registry-coupons", version = "v1"))
object Application {

    @JvmStatic
    fun main(args: Array<String>) {

        // TAP deployment specific
        TAPEnvironmentLoader().setupTAPSpecificEnvironment()
        System.setProperty("APP_UUID", UUID.randomUUID().toString())
        Micronaut.build()
            .propertySources(PlatformPropertySource.connect())
            .packages("com.tgt.backpackregistrycoupons.api.controller")
            .mainClass(Application.javaClass)
            .start()
    }
}
