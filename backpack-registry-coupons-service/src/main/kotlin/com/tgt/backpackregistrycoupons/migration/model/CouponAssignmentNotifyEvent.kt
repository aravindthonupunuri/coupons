package com.tgt.backpackregistrycoupons.migration.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tgt.lists.msgbus.EventType
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
data class CouponAssignmentNotifyEvent(

    @JsonProperty("guest_id")
    val guestId: String,

    @JsonProperty("list_id")
    val listId: UUID,

    @JsonProperty("list_type")
    val listType: String,

    @JsonProperty("list_sub_type")
    val listSubType: String? = null,

    @JsonProperty("user_meta_data")
    val userMetaData: Map<String, Any>? = null,

    @JsonProperty("retry_state")
    var retryState: String? = null
) {
    companion object {
        // jacksonObjectMapper() returns a normal ObjectMapper with the KotlinModule registered
        private val jsonMapper = jacksonObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)

        @JvmStatic
        fun getEventType(): EventType {
            return "COUPON-ASSIGNMENT-NOTIFY-EVENT"
        }

        @JvmStatic
        fun deserialize(byteArray: ByteArray): CouponAssignmentNotifyEvent {
            return jsonMapper.readValue(byteArray, CouponAssignmentNotifyEvent::class.java)
        }
    }
}
