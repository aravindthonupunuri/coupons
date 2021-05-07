package com.tgt.backpackregistrycoupons.transport

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
data class PromoCouponRedemptionTO(
    val locationId: Long? = null,
    val couponCode: String,
    val couponType: String? = null,
    val status: String? = null,
    val transactionTime: String? = null,
    val registerId: String? = null,
    val transactionId: String? = null,
    var registryId: UUID? = null
) {
    companion object {
        private val jsonMapper = jacksonObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)

        @JvmStatic
        fun deserialize(byteArray: ByteArray): PromoCouponRedemptionTO {
            return jsonMapper.readValue(byteArray, PromoCouponRedemptionTO::class.java)
        }

        @JvmStatic
        fun serialize(promoCouponRedemptionTO: PromoCouponRedemptionTO): ByteArray {
            return jsonMapper.writeValueAsBytes(promoCouponRedemptionTO)
        }
    }
}
