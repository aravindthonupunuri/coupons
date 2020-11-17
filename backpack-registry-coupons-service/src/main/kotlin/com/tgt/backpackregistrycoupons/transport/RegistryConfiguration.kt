package com.tgt.backpackregistrycoupons.transport

import io.micronaut.context.annotation.EachProperty
import io.micronaut.core.annotation.Introspected

@Introspected
@EachProperty("registry-configuration")
data class RegistryConfiguration(
    val name: String,
    val sla: Int
)
