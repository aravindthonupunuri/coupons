package com.tgt.backpackregistrycoupons.producer

import com.tgt.lists.msgbus.GenericProducer
import com.tgt.lists.msgbus.MetricsName
import com.tgt.lists.msgbus.event.EventHeaderFactory
import com.tgt.lists.msgbus.event.EventLifecycleNotificationProvider
import com.tgt.lists.msgbus.metrics.MetricsPublisher
import com.tgt.lists.msgbus.tracing.EventTracer
import com.tgt.lists.msgbus.tracing.MdcContext
import io.micronaut.context.annotation.Requires
import io.micronaut.context.annotation.Value
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sends a message to tracer-incoming-events topic
 */
@Requires(property = "tracer.kafka.producer.enabled", value = "true")
@Singleton
open class GuestNotificationsProducer<K, V>(
    @Value("\${tracer.source}") val source: String,
    @Value("\${tracer.kafka.topic}") val topic: String,
    @Inject private val eventHeaderFactory: EventHeaderFactory,
    @Inject private val tracerKafkaProducerClient: GuestNotificationsProducerClient<K, V>,
    @Inject private val eventLifecycleNotificationProvider: EventLifecycleNotificationProvider,
    @Inject private val eventTracer: EventTracer,
    @Inject private val metricsPublisher: MetricsPublisher,
    @Inject private val mdcContext: MdcContext? = null
) : GenericProducer<K, V>(
    producerName = "GuestNotificationsProducer",
    source = source,
    topic = topic,
    eventHeaderFactory = eventHeaderFactory,
    eventProducerClient = tracerKafkaProducerClient,
    eventLifecycleNotificationProvider = eventLifecycleNotificationProvider,
    eventTracer = eventTracer,
    metricsPublisher = metricsPublisher,
    mdcContext = mdcContext,
    metricsName = MetricsName.PRODUCER_METRICS.value
)
