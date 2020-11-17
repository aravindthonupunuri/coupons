package com.tgt.guestnotifications.producer

import com.tgt.lists.msgbus.event.EventHeaders
import com.tgt.lists.msgbus.producer.EventProducerClient
import com.tgt.lists.msgbus.tracing.MdcContext
import com.tgt.lists.msgbus.tracing.TraceHeaders
import io.micronaut.configuration.kafka.annotation.KafkaClient
import io.micronaut.configuration.kafka.annotation.KafkaKey
import io.micronaut.configuration.kafka.annotation.Topic
import io.micronaut.context.annotation.Requires
import io.micronaut.messaging.annotation.Header
import org.apache.kafka.clients.producer.RecordMetadata
import reactor.core.publisher.Mono

@Requires(property = "tracer.kafka.producer.enabled", value = "true")
@KafkaClient("guest-notifications-producer")
interface GuestNotificationsProducerClient<K, V> : EventProducerClient<K, V> {

    @Topic("\${tracer.kafka.topic}")
    override fun sendEvent(
        @KafkaKey partitionKey: K,
        data: Mono<V>,
        @Header(EventHeaders.EventHeaderNames.UUID_HDR_NAME) eventId: ByteArray,
        @Header(EventHeaders.EventHeaderNames.EVENT_TYPE_HDR_NAME) eventType: ByteArray,
        @Header(EventHeaders.EventHeaderNames.CORRELATION_ID_HDR_NAME) correlationId: ByteArray?,
        @Header(EventHeaders.EventHeaderNames.TIMESTAMP_HDR_NAME) timestamp: ByteArray?,
        @Header(EventHeaders.EventHeaderNames.ERROR_CODE) errorCode: ByteArray?,
        @Header(EventHeaders.EventHeaderNames.ERROR_MESSAGE) errorMessage: ByteArray?,
        @Header(EventHeaders.EventHeaderNames.RETRY_COUNT) retryCount: ByteArray?,
        @Header(EventHeaders.EventHeaderNames.RETRY_TIMESTAMP) retryTimestamp: ByteArray?,
        @Header(EventHeaders.EventHeaderNames.MAX_RETRY_COUNT) maxRetryCount: ByteArray?,
        @Header(EventHeaders.EventHeaderNames.SOURCE) source: ByteArray?,
        @Header(TraceHeaders.TRACE_HEADER) traceHeader: ByteArray?,
        @Header(MdcContext.MDC_HEADER) mdcHeader: ByteArray?
    ): Mono<RecordMetadata>
}
