package com.tgt.backpackregistrycoupons.service.async

import com.tgt.lists.lib.api.async.PendingToCompletedItemStateService
import mu.KotlinLogging
import reactor.core.publisher.Mono
import javax.inject.Singleton

@Singleton
class CronEventService() {
    private val logger = KotlinLogging.logger { PendingToCompletedItemStateService::class.java.name }

    fun processCronEvent(): Mono<Boolean> {
        return Mono.just(true)
    }
}
