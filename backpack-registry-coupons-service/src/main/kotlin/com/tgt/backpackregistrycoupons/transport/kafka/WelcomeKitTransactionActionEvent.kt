package com.tgt.backpackregistrycoupons.transport.kafka

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tgt.lists.msgbus.EventType
import java.util.*

data class WelcomeKitTransactionActionEvent(
    @JsonProperty("registry_id")
    val registryId: UUID
) {
    companion object {
        private val jsonMapper = jacksonObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)

        @JvmStatic
        fun getEventType(): EventType {
            return "WELCOME-KIT-TRANSACT-ACTION-EVENT"
        }

        @JvmStatic
        fun deserialize(byteArray: ByteArray): WelcomeKitTransactionActionEvent {
            return jsonMapper.readValue(byteArray, WelcomeKitTransactionActionEvent::class.java)
        }
    }
}
