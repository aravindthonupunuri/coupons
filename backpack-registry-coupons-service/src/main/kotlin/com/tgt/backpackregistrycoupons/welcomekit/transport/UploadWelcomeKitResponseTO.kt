package com.tgt.backpackregistrycoupons.welcomekit.transport

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UploadWelcomeKitResponseTO(
    val uploadedTcins: List<String> = emptyList(),
    val existingTcins: List<String> = emptyList()
)
