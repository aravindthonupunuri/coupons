package com.tgt.backpackregistrycoupons.migration.model

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tgt.backpackregistrycoupons.util.CouponRedemptionStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
data class RegistryCouponMetaDataTO(

    @JsonProperty("online_coupon_code")
    val onlineCouponCode: String? = null,

    @JsonProperty("online_coupon_status")
    val onlineCouponStatus: CouponRedemptionStatus? = null,

    @JsonProperty("store_coupon_code")
    val storeCouponCode: String? = null,

    @JsonProperty("store_coupon_status")
    val storeCouponStatus: CouponRedemptionStatus? = null,

    @JsonSerialize(using = LocalDateSerializer::class)
    @JsonDeserialize(using = LocalDateDeserializer::class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @JsonProperty("coupon_issue_date")
    val couponIssueDate: LocalDate? = null,

    @JsonSerialize(using = LocalDateSerializer::class)
    @JsonDeserialize(using = LocalDateDeserializer::class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @JsonProperty("coupon_expiry_date")
    val couponExpiryDate: LocalDate? = null,

    @JsonProperty("added_date_time")
    @JsonSerialize(using = LocalDateTimeSerializer::class)
    @JsonDeserialize(using = LocalDateTimeDeserializer::class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val addedDate: LocalDateTime? = null,

    @JsonProperty("last_modified_date_time")
    @JsonSerialize(using = LocalDateTimeSerializer::class)
    @JsonDeserialize(using = LocalDateTimeDeserializer::class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val lastModifiedDate: LocalDateTime? = null
) {
    companion object {
        const val REGISTRY_COUPON_METADATA = "registry-coupon-metadata"
        // You need to use jackson-module-kotlin to deserialize to data classes otherwise we will end up with de-serialising error
        // com.fasterxml.jackson.databind.exc.MismatchedInputException: Cannot construct instance of `test.SerializationTests$TestDataClass` (although at least one Creator exists): cannot deserialize from Object value (no delegate- or property-based Creator)
        private val mapper = jacksonObjectMapper()

        @JvmStatic
        fun toEntityRegistryCouponMetadata(metadata: Map<String, Any>?): RegistryCouponMetaDataTO? {
            return metadata?.takeIf { metadata.containsKey(REGISTRY_COUPON_METADATA) }
                ?.let {
                    mapper.readValue<RegistryCouponMetaDataTO>(
                        mapper.writeValueAsString(metadata[REGISTRY_COUPON_METADATA]))
                }
        }

        @JvmStatic
        fun toStringRegistryCouponMetadata(registryCouponMetaData: RegistryCouponMetaDataTO?): String? {
            return mapper.writeValueAsString(mapOf(REGISTRY_COUPON_METADATA to
                RegistryCouponMetaDataTO(
                    registryCouponMetaData?.onlineCouponCode,
                    registryCouponMetaData?.onlineCouponStatus,
                    registryCouponMetaData?.storeCouponCode,
                    registryCouponMetaData?.storeCouponStatus,
                    registryCouponMetaData?.couponIssueDate,
                    registryCouponMetaData?.couponExpiryDate
                )))
        }
    }
}
