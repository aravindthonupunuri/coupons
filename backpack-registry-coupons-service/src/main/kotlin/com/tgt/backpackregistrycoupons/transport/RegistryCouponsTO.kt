package com.tgt.backpackregistrycoupons.transport

import com.fasterxml.jackson.annotation.JsonInclude
import com.tgt.backpackregistryclient.util.RegistryStatus
import com.tgt.backpackregistryclient.util.RegistryType
import java.util.*
import javax.validation.constraints.NotNull

@JsonInclude(JsonInclude.Include.NON_NULL)
data class RegistryCouponsTO(
    @field:NotNull(message = "Registry id must not be empty") val registryId: UUID?,
    @field:NotNull(message = "Registry type must not be empty") val registryType: RegistryType?,
    @field:NotNull(message = "Registry status must not be empty") val registryStatus: RegistryStatus?,
    val couponCountDownDays: Long?,
    val coupons: List<CouponsTO>? = null
)
