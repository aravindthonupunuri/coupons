package com.tgt.backpackregistrycoupons.transport

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
data class PromoCouponRedemptionTO(
    val locationId: Long? = null,
    val couponCode: String,
    val couponType: String? = null,
    val status: String? = null,
    @JsonDeserialize(using = LocalDateTimeDeserializer::class)
    @JsonSerialize(using = LocalDateTimeSerializer::class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val transactionTime: LocalDateTime? = null,
    val registerId: String? = null,
    val transactionId: String? = null,
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
