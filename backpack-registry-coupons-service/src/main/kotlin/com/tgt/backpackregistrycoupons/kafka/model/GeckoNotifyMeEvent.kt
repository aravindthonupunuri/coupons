package com.tgt.backpackregistrycoupons.kafka.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.util.*

data class GeckoNotifyMeEvent(
    @JsonProperty("meta")
    val meta: Meta,

    @JsonProperty("payload")
    var payload: Payload
)

data class Meta(
    @JsonProperty("message_id")
    val messageId: String = UUID.randomUUID().toString(),

    @JsonProperty("message_type")
    val messageType: String,

    @JsonProperty("message_send_time")
    val messageSendTime: Instant = Instant.now(),

    @JsonProperty("create_timestamp")
    val createTimestamp: Instant = Instant.now()
)

data class Payload(
    @JsonProperty("guest_profile_id")
    val guestProfileId: String,

    @JsonProperty("tcin")
    val tcin: String
)
