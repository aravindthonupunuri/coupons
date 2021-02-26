package com.tgt.backpackregistrycoupons.transport

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
data class PromoCouponRedemptionTO(
    @JsonProperty("location_id")
    val locationId: String? = null,
    @JsonProperty("coupon_code")
    val couponCode: String,
    @JsonProperty("coupon_type")
    val couponType: String? = null,
    @JsonProperty("status")
    val status: String? = null,
    @JsonProperty("transaction_time")
    val transactionTime: LocalDateTime? = null,
    @JsonProperty("register_id")
    val registerId: String? = null,
    @JsonProperty("transaction_id")
    val tranId: String? = null,
    @JsonProperty("registry_id")
    val registryId: UUID
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
