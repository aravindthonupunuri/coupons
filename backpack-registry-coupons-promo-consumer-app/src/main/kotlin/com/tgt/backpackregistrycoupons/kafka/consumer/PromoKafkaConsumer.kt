package com.tgt.backpackregistrycoupons.kafka.consumer

import com.tgt.lists.msgbus.EventDispatcher
import com.tgt.lists.msgbus.GenericConsumer
import com.tgt.lists.msgbus.ListsDlqProducer
import com.tgt.lists.msgbus.event.EventHeaderFactory
import com.tgt.lists.msgbus.event.EventHeaders
import com.tgt.lists.msgbus.event.EventLifecycleNotificationProvider
import com.tgt.lists.msgbus.execution.EventDispatcherExecutorFactory
import com.tgt.lists.msgbus.metrics.MetricsPublisher
import com.tgt.lists.msgbus.tracing.EventTracer
import com.tgt.lists.msgbus.tracing.MdcContext
import io.micronaut.configuration.kafka.ConsumerRegistry
import io.micronaut.configuration.kafka.annotation.KafkaListener
import io.micronaut.configuration.kafka.annotation.OffsetReset
import io.micronaut.configuration.kafka.annotation.OffsetStrategy
import io.micronaut.configuration.kafka.annotation.Topic
import io.micronaut.context.annotation.Value
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.header.Header
import org.apache.kafka.common.header.internals.RecordHeader
import javax.inject.Inject
import javax.inject.Provider

@KafkaListener(
    clientId = ("ex\${APP_UUID}"),
    batch = true,
    offsetReset = OffsetReset.EARLIEST,
    groupId = ("\${promo.kafka.consumer.consumer-group}"),
    offsetStrategy = OffsetStrategy.DISABLED
)
class PromoKafkaConsumer<K, V>(
    @Value("ex\${APP_UUID}") val clientId: String,
    @Value("\${promo.kafka.consumer.consumer-group}") val consumerName: String,
    @Value("\${promo.source}") val defaultEventSource: String,
    @Value("\${promo.kafka.consumer.consumer-batch-size}") val consumerBatchSize: Int,
    @Value("\${promo.kafka.consumer.max-count-down-latch-wait-time}") val maxCountDownLatchWaitTime: Long,
    @Value("\${promo.kafka.consumer.metrics-name}") val metricsName: String,
    @Value("\${promo.kafka.consumer.default-event-type}") val defaultEventType: String,
    @Inject val consumerRegistry: Provider<ConsumerRegistry>,
    @Inject val eventDispatcher: EventDispatcher,
    @Inject val eventLifecycleNotificationProvider: EventLifecycleNotificationProvider,
    @Inject val listsDlqProducer: ListsDlqProducer<K, V>? = null,
    @Inject val eventHeaderFactory: EventHeaderFactory,
    @Inject val eventDispatcherExecutorFactory: EventDispatcherExecutorFactory<K>,
    @Inject val eventTracer: EventTracer,
    @Inject val metricsPublisher: MetricsPublisher,
    @Inject val mdcContext: MdcContext? = null
) : GenericConsumer<K, V>(
    consumerName,
    clientId,
    defaultEventSource,
    consumerRegistry,
    eventDispatcher,
    eventLifecycleNotificationProvider,
    listsDlqProducer,
    consumerBatchSize,
    maxCountDownLatchWaitTime,
    eventHeaderFactory,
    eventDispatcherExecutorFactory,
    eventTracer,
    metricsPublisher,
    mdcContext,
    metricsName
) {
    @Topic(("\${promo.kafka.consumer.topic}"))
    fun receive(
        consumerRecords: List<ConsumerRecord<K, ByteArray>>,
        offsets: List<Long>,
        partitions: List<Int>,
        topics: List<String>,
        kafkaConsumer: Consumer<K, V>
    ) {
        this.receiveInternal(consumerRecords, offsets, partitions, topics, kafkaConsumer)
    }

    override fun applyEventHeadersIfNeeded(consumerRecord: ConsumerRecord<K, ByteArray>) {
        consumerRecord.headers().add(RecordHeader(EventHeaders.EventHeaderNames.EVENT_TYPE_HDR_NAME, defaultEventType.toByteArray()) as Header?)
        super.populateConsumerRecordWithDefaultEventHeaders(consumerRecord)
    }
}
