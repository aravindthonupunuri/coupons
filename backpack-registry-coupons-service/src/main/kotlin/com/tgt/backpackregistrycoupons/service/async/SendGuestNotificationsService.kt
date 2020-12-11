package com.tgt.backpackregistrycoupons.service.async

import com.tgt.backpackregistrycoupons.kafka.model.TracerEvent
import com.tgt.backpackregistrycoupons.producer.GuestNotificationsProducer
import org.apache.kafka.clients.producer.RecordMetadata
import reactor.core.publisher.Mono
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SendGuestNotificationsService(@Inject val guestNotificationsProducer: GuestNotificationsProducer<String, Any>) {
    fun sendGuestNotifications(
        tracerEvent: TracerEvent,
        registryId: UUID
    ): Mono<RecordMetadata> {
        return guestNotificationsProducer.sendMessage("tracer-event", tracerEvent, registryId.toString())
    }
}
