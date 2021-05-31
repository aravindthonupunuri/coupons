package com.tgt.backpackregistrycoupons.promo.api.app.app


import com.tgt.lists.msgbus.producer.EventProducerClient
import io.micronaut.configuration.kafka.annotation.KafkaClient
import io.micronaut.configuration.kafka.annotation.KafkaKey
import io.micronaut.configuration.kafka.annotation.Topic
import io.micronaut.messaging.annotation.Header
import org.apache.kafka.clients.producer.RecordMetadata
import reactor.core.publisher.Mono

@KafkaClient("generic-producer")
interface TestGenericProducerClient<K,V> extends EventProducerClient<K, V>  {

    @Topic("promo-coupon-redemption-notifications-v2")
    Mono<RecordMetadata> sendEvent(
            @KafkaKey K partitionKey,
            Mono<V> data,
            // groovy doesn't like EventHeaders.EventHeaderNames.* as inline constant values.
            // Hence copying those values as string here which is not perfect but works for now, just make
            // sure this is always in sync with EventHeaders.EventHeaderNames.
            @Header(/*EventHeaders.EventHeaderNames.UUID_HDR_NAME*/ "uuid") byte[] eventId,
            @Header(/*EventHeaders.EventHeaderNames.EVENT_TYPE_HDR_NAME*/ "event_type") byte[] eventType,
            @Header(/*EventHeaders.EventHeaderNames.CORRELATION_ID_HDR_NAME*/ "correlation_id") byte[] correlationId,
            @Header(/*EventHeaders.EventHeaderNames.TIMESTAMP_HDR_NAME*/ "timestamp") byte[] timestamp,
            @Header(/*EventHeaders.EventHeaderNames.ERROR_CODE*/ "error_code") byte[] errorCode,
            @Header(/*EventHeaders.EventHeaderNames.ERROR_MESSAGE*/ "error_message") byte[] errorMessage,
            @Header(/*EventHeaders.EventHeaderNames.RETRY_COUNT*/ "retry_count") byte[] retryCount,
            @Header(/*EventHeaders.EventHeaderNames.RETRY_TIMESTAMP*/ "retry_timestamp") byte[] retryTimestamp,
            @Header(/*EventHeaders.EventHeaderNames.MAX_RETRY_COUNT*/ "max_retry_count") byte[] maxRetryCount,
            @Header(/*EventHeaders.EventHeaderNames.SOURCE*/ "source") byte[] source,
            @Header(/*TraceHeaders.TRACE_HEADER*/ "b3") byte[] traceHeader,
            @Header(/*MdcContext.MDC_HEADER*/ "mdc") byte[] mdcHeader,
            @Header(/*EventHeaders.EventHeaderNames.TEST_MODE*/ "test_mode") byte[] testMode
    )
}
