package com.tgt.backpackregistrycoupons.promo.api.app.app

import com.tgt.lists.msgbus.GenericProducer
import com.tgt.lists.msgbus.event.EventHeaderFactory
import com.tgt.lists.msgbus.event.EventLifecycleNotificationProvider
import com.tgt.lists.msgbus.metrics.MetricsPublisher
import com.tgt.lists.msgbus.tracing.EventTracer
import com.tgt.lists.msgbus.tracing.MdcContext
import io.micronaut.context.env.Environment

import javax.inject.Inject

class TestGenericProducer<K,V> extends GenericProducer<K, V>  {

    @Inject
    TestGenericProducer(EventHeaderFactory eventHeaderFactory, TestGenericProducerClient testGenericProducerClient,
                        EventLifecycleNotificationProvider eventLifecycleNotificationProvider,
                        EventTracer eventTracer, MetricsPublisher metricsPublisher,
                        MdcContext mdcContext, Environment environment) {
        super(
                "test-producer",
                "generic",
                "backpack-registry-coupons",
                "promo-coupon-redemption-notifications-v2",
                eventHeaderFactory,
                testGenericProducerClient,
                eventLifecycleNotificationProvider,
                eventTracer,
                metricsPublisher,
                mdcContext,
                "test-producer",
                environment
        )
    }
}
