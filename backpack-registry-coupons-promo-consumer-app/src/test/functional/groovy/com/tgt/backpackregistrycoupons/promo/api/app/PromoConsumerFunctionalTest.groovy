package com.tgt.backpackregistrycoupons.promo.api.app

import com.tgt.backpackregistryclient.util.RegistryType
import com.tgt.backpackregistrycoupons.domain.model.Registry
import com.tgt.backpackregistrycoupons.domain.model.RegistryCoupons
import com.tgt.backpackregistrycoupons.kafka.consumer.PromoKafkaConsumer
import com.tgt.backpackregistrycoupons.kafka.handler.RegistryTransactionEventHandler
import com.tgt.backpackregistrycoupons.persistence.repository.registry.RegistryRepository
import com.tgt.backpackregistrycoupons.persistence.repository.registrycoupons.RegistryCouponsRepository
import com.tgt.backpackregistrycoupons.promo.api.app.app.TestGenericProducer
import com.tgt.backpackregistrycoupons.test.BaseKafkaFunctionalTest
import com.tgt.backpackregistrycoupons.test.PreDispatchLambda
import com.tgt.backpackregistrycoupons.transport.PromoCouponRedemptionTO
import com.tgt.backpackregistrycoupons.util.CouponRedemptionStatus
import com.tgt.backpackregistrycoupons.util.CouponType
import com.tgt.lists.atlas.api.type.LIST_STATE
import com.tgt.lists.micronaut.persistence.instrumentation.DatabaseExecTestListener
import com.tgt.lists.micronaut.persistence.instrumentation.RepositoryInstrumenter
import com.tgt.lists.msgbus.GenericConsumer
import com.tgt.lists.msgbus.event.EventHeaders
import com.tgt.lists.msgbus.event.EventLifecycleNotificationProvider
import io.micronaut.context.annotation.Value
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.opentracing.Tracer
import org.jetbrains.annotations.NotNull
import spock.lang.Shared
import spock.util.concurrent.PollingConditions

import javax.inject.Inject
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.stream.Collectors

@MicronautTest
class PromoConsumerFunctionalTest extends  BaseKafkaFunctionalTest {
    @Value("\${promo.source}")
    String promoSource

    @Value("\${promo.kafka.consumer.default-event-type}")
    String promoEventType

    @Value("\${msgbus.dlq-source}")
    String dlqSource

    @Shared
    @Inject
    EventLifecycleNotificationProvider eventNotificationsProvider

    @Shared
    @Inject
    Tracer tracer

    @Shared
    TestEventListener testEventListener

    @Inject
    TestGenericProducer testGenericProducer

    @Inject
    PromoKafkaConsumer<String, String> backpackExternalConsumer

    @Inject
    RegistryTransactionEventHandler registryTransactionHandler

    @Inject
    RegistryCouponsRepository registryCouponsRepository

    @Inject
    RegistryRepository registryRepository

    @Inject
    RepositoryInstrumenter repositoryInstrumenter

    @Shared
    boolean executeTimeout = false

    @Shared
    LocalDateTime date = LocalDateTime.now()

    PollingConditions conditions = new PollingConditions(timeout: 30, delay: 1)

    def setupSpec() {
        testEventListener = new TestEventListener()
        testEventListener.tracer = tracer
        eventNotificationsProvider.registerListener(testEventListener)
    }

    def setup() {
        testEventListener.reset()
        repositoryInstrumenter.attachTestListener(new DatabaseExecTestListener() {
            @Override
            boolean shouldOverrideWithTimeoutQuery(@NotNull String repoName, @NotNull String methodName) {
                return executeTimeout
            }
        })
    }

    @Override
    Map<String, String> getAdditionalProperties() {
        return ["jdbc-stmt-timeout.serverStatementTimeoutMillis": "500"]
    }

    @Override
    Map<String, String> getProperties() {
        def map = super.getProperties()
        map.put("kafka.producers.generic-producer.key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
        map.put("kafka.producers.generic-producer.value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer")
        return map
    }

    def "test updating coupon redemption status happy path"() {
        given:
        def registryId = UUID.randomUUID()
        def couponCode = UUID.randomUUID().toString()
        def promoCouponRedemptionTO = new PromoCouponRedemptionTO(3991, couponCode, "BABY", "redeemed", "2020", "1234", "1234", registryId)
        def registry = new Registry(registryId, "1234", RegistryType.BABY,  LIST_STATE.ACTIVE.value, LocalDate.now().minusDays(3), LocalDate.now(), true, null, null)
        def registryCoupons = new RegistryCoupons(couponCode, registry, CouponType.STORE, CouponRedemptionStatus.AVAILABLE, LocalDate.now(), LocalDate.now().plusDays(2), null , null)
        registryRepository.save(registry).block()
        registryCouponsRepository.save(registryCoupons).block()
        testEventListener.preDispatchLambda = new PreDispatchLambda() {
            @Override
            boolean onPreDispatchConsumerEvent(String topic, @NotNull EventHeaders eventHeaders, @NotNull byte[] data, boolean isPoisonEvent) {
                if (eventHeaders.source == "backpack-registry-coupons") {
                    if (eventHeaders.eventType == GenericConsumer.GENERIC_EVENT_TYPE) {
                        def eventItem = PromoCouponRedemptionTO.deserialize(data)
                        if (eventItem.couponCode == couponCode) {
                            return true // returning false to force this event to be discarded
                        }
                    }
                }
                return false
            }
        }

        when:
        def bytes = promoCouponRedemptionTO.serialize(promoCouponRedemptionTO)
        testGenericProducer.sendMessage(GenericConsumer.GENERIC_EVENT_TYPE, bytes, registryId.toString(), null).block()

        then:
        testEventListener.verifyEvents { consumerEvents, producerEvents, consumerStatusEvents ->
            conditions.eventually {
                List<TestEventListener.Result> completedEvents = consumerEvents.stream().filter {
                    def result = (TestEventListener.Result)it
                    (!result.preDispatch)
                }.collect(Collectors.toList())
                assert completedEvents.any{
                    it.success
                }
            }
        }

        when:
        def registryCouponsResult = registryCouponsRepository.findByCouponCode(couponCode).block()

        then:
        registryCouponsResult.couponRedemptionStatus == CouponRedemptionStatus.REDEEMED
    }

    def "test updating coupon redemption status when coupon code not found"() {
        given:
        def registryId = UUID.randomUUID()
        def couponCode = UUID.randomUUID().toString()
        def promoCouponRedemptionTO = new PromoCouponRedemptionTO(3991, couponCode, "BABY", "redeemed", "2020", "1234", "1234", registryId)

        testEventListener.preDispatchLambda = new PreDispatchLambda() {
            @Override
            boolean onPreDispatchConsumerEvent(String topic, @NotNull EventHeaders eventHeaders, @NotNull byte[] data, boolean isPoisonEvent) {
                if (eventHeaders.source == "backpack-registry-coupons") {
                    if (eventHeaders.eventType == GenericConsumer.GENERIC_EVENT_TYPE) {
                        def eventItem = PromoCouponRedemptionTO.deserialize(data)
                        if (eventItem.couponCode == couponCode) {
                            return true // returning false to force this event to be discarded
                        }
                    }
                }
                return false
            }
        }

        when:
        def bytes = promoCouponRedemptionTO.serialize(promoCouponRedemptionTO)
        testGenericProducer.sendMessage(GenericConsumer.GENERIC_EVENT_TYPE, bytes, registryId.toString(), null).block()

        then:
        testEventListener.verifyEvents { consumerEvents, producerEvents, consumerStatusEvents ->
            conditions.eventually {
                List<TestEventListener.Result> completedEvents = consumerEvents.stream().filter {
                    def result = (TestEventListener.Result)it
                    (!result.preDispatch)
                }.collect(Collectors.toList())
                assert completedEvents.any{
                    it.success
                }
            }
        }
    }
}
