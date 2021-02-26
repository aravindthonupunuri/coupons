package com.tgt.backpackregistrycoupons.transport

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tgt.lists.msgbus.EventType

data class RegistryItemPromoTransactionActionEvent(

    @JsonProperty("registry_transaction")
    val promoCouponRedemptionTO: PromoCouponRedemptionTO

) {
    companion object {
        private val jsonMapper = jacksonObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)

        @JvmStatic
        fun getEventType(): EventType {
            return "REGISTRY-COUPON-TRANSACT_ACTION_EVENT"
        }

        @JvmStatic
        fun deserialize(byteArray: ByteArray): RegistryItemPromoTransactionActionEvent {
            return jsonMapper.readValue(byteArray, RegistryItemPromoTransactionActionEvent::class.java)
        }
    }
}
