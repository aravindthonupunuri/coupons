package com.tgt.backpackregistrycoupons.kafka.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class TracerEvent(
    @JsonProperty("scenario_name")
    val scenarioName: String?,

    @JsonProperty("id")
    val id: String?,

    @JsonProperty("id_type")
    val idType: String = "PROFILE",

    @JsonProperty("origin")
    val origin: String?,

    @JsonProperty("transaction_id")
    val transactionId: String?,

    @JsonProperty("earliest_send_time")
    val earliestSendTime: LocalDateTime?,

    @JsonProperty("latest_send_time")
    val latestSendTime: LocalDateTime?,

    @JsonProperty("data")
    var data: Data? = null
)

data class Data(
    @JsonProperty("first_name")
    val firstName: String? = null,

    @JsonProperty("last_name")
    val lastName: String? = null,

    @JsonProperty("online_promo_code")
    val onlinePromoCode: String? = null,

    @JsonProperty("store_coupon_code")
    val storeCouponCode: String? = null,

    @JsonProperty("registry_type")
    val RegistryType: String? = null,

    @JsonProperty("expiration_date")
    val ExpirationDate: LocalDateTime? = null
)
