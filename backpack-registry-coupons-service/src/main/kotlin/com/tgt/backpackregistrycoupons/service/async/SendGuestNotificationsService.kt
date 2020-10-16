// package com.tgt.guestnotifications.service.async
//
// import com.tgt.guestnotifications.domain.model.GuestNotifications
// import com.tgt.guestnotifications.domain.model.GuestNotificationsType
// import com.tgt.guestnotifications.kafka.model.Data
// import com.tgt.guestnotifications.kafka.model.TracerEvent
// import com.tgt.guestnotifications.producer.GuestNotificationsProducer
// import org.apache.kafka.clients.producer.RecordMetadata
// import reactor.core.publisher.Mono
// import javax.inject.Inject
// import javax.inject.Singleton
//
// @Singleton
// class SendGuestNotificationsService(
//    @Inject val guestNotificationsProducer: GuestNotificationsProducer<String, Any>
// ) {
//
//    fun sendGuestNotifications(
//        guestNotification: GuestNotifications
//    ): Mono<RecordMetadata> {
//        return guestNotificationsProducer.sendMessage("tracer-event",
//            TracerEvent(
//                scenarioName = GuestNotificationsType.IN_STOCK.name,
//                id = guestNotification.guestId,
//                origin = guestNotification.channel.name,
//                transactionId = "0", // check this
//                data = Data(tcin = guestNotification.tcin, itemId = guestNotification.tcin)), guestNotification.guestId)
//    }
// }
