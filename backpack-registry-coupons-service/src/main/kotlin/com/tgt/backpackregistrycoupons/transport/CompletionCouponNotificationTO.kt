package com.tgt.backpackregistrycoupons.transport

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CompletionCouponNotificationTO(
    @JsonProperty("FirstName")
    val firstName: String?,

    @JsonProperty("LastName")
    val lastName: String?,

    @JsonProperty("CoRegFirstName")
    val coRegFirstName: String? = null,

    @JsonProperty("CoRegLastName")
    val coRegLastName: String? = null,

    @JsonProperty("EmailAddress")
    val emailAddress: String?,

    @JsonProperty("RegistryId")
    val registryId: String?,

    @JsonProperty("OnlinePromoCode")
    val onlinePromoCode: String?,

    @JsonProperty("StoreCouponCode")
    val storeCouponCode: String?,

    @JsonProperty("BarCode")
    var barCode: String? = null,

    @JsonProperty("RegistryType")
    val registryType: String? = null, // TODO: check format of registry type

    @JsonProperty("ExpirationDate")
    val expirationDate: String?,

    @JsonProperty("EventCity")
    val eventCity: String? = null,

    @JsonProperty("EventState")
    val eventState: String? = null,

    @JsonProperty("RegistryLink")
    val registryLink: String? = null
)
