package com.tgt.backpackregistrycoupons.transport

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tgt.backpackregistrycoupons.util.RegistryType
import java.time.LocalDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class RegistryMetaDataTO(
    @JsonProperty("event")
    val event: Event? = null,
    @JsonProperty("registry_type")
    val registryType: RegistryType? = null,
    @JsonProperty("registry_created_ts")
    val registryCreatedTs: LocalDateTime? = null
) {
    companion object {
        const val REGISTRY_METADATA = "registry-metadata"
        val mapper = ObjectMapper()

        @JvmStatic
        fun getRegistryMetadata(metadata: Map<String, Any>?): RegistryMetaDataTO? {
            return metadata?.takeIf { metadata.containsKey(REGISTRY_METADATA) }
                ?.let {
                    mapper.readValue<RegistryMetaDataTO>(
                        mapper.writeValueAsString(metadata[REGISTRY_METADATA]))
                }
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Event(
    @JsonProperty("event_date_ts")
    val eventDateTs: LocalDateTime? = null
)
