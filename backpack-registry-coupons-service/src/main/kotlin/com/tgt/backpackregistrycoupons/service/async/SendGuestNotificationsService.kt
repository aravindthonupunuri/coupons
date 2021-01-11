package com.tgt.backpackregistrycoupons.service.async

import com.tgt.notification.tracer.client.model.NotificationTracerEvent
import com.tgt.notification.tracer.client.producer.NotificationTracerProducer
import mu.KotlinLogging
import reactor.core.publisher.Mono
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SendGuestNotificationsService(@Inject val notificationTracerProducer: NotificationTracerProducer<String, Any>) {
    private val logger = KotlinLogging.logger { CronEventService::class.java.name }

    fun sendGuestNotifications(
        notificationTracerEvent: NotificationTracerEvent,
        registryId: String
    ): Mono<Boolean> {
        return notificationTracerProducer.sendMessage(NotificationTracerEvent.getEventType(), notificationTracerEvent, registryId).map { true }
            .onErrorResume {
                logger.error("[sendGuestNotifications], Exception sending guest notification for registry $registryId")
                Mono.just(true)
            }
    }
}
