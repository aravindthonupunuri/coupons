package com.tgt.backpackregistrycoupons.promo.api.app

import com.tgt.backpackregistrycoupons.domain.model.RegistryCoupons
import com.tgt.backpackregistrycoupons.domain.model.RegistryPk
import com.tgt.backpackregistrycoupons.kafka.handler.RegistryTransactionEventHandler
import com.tgt.backpackregistrycoupons.persistence.repository.registrycoupons.RegistryCouponsRepository
import com.tgt.backpackregistrycoupons.promo.api.app.app.TestGenericProducer
import com.tgt.backpackregistrycoupons.promo.kafka.consumer.PromoKafkaConsumer
import com.tgt.backpackregistrycoupons.test.BaseKafkaFunctionalTest
import com.tgt.backpackregistrycoupons.test.PreDispatchLambda
import com.tgt.backpackregistrycoupons.util.CouponRedemptionStatus
import com.tgt.backpackregistrycoupons.util.CouponType
import com.tgt.backpackregistrycoupons.util.RegistryStatus
import com.tgt.backpackregistrycoupons.util.RegistryType
import com.tgt.backpacktransactionsclient.transport.kafka.model.PromoCouponRedemptionTO
import com.tgt.lists.micronaut.persistence.instrumentation.DatabaseExecTestListener
import com.tgt.lists.micronaut.persistence.instrumentation.RepositoryInstrumenter
import com.tgt.lists.msgbus.GenericConsumer
import com.tgt.lists.msgbus.event.EventHeaders
import com.tgt.lists.msgbus.event.EventLifecycleNotificationProvider
import io.micronaut.context.annotation.Value
import io.micronaut.test.annotation.MicronautTest
import io.opentracing.Tracer
import org.jetbrains.annotations.NotNull
import spock.lang.Shared
import spock.util.concurrent.PollingConditions

import javax.inject.Inject
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

    def "test generic consumer valid"() {
        given:
        def listId = UUID.randomUUID()
        def couponCode = "1234"
        def promoCouponRedemptionTO = new PromoCouponRedemptionTO("3991", "1234", "BABY", "REDEEMED", "2020", "1234", "1234", "1234")

        RegistryPk registryPk = new RegistryPk(listId, CouponType.STORE);
        RegistryCoupons coupons = new RegistryCoupons(registryPk, RegistryType.BABY, RegistryStatus.ACTIVE.value, LocalDateTime.now(), LocalDateTime.now(), "1234", true, CouponRedemptionStatus.AVAILABLE, null, null, "abc", "abc", null, null)
        registryCouponsRepository.save(coupons).block()
        testEventListener.preDispatchLambda = new PreDispatchLambda() {
            @Override
            boolean onPreDispatchConsumerEvent(String topic, @NotNull EventHeaders eventHeaders, @NotNull byte[] data, boolean isPoisonEvent) {
                if (eventHeaders.source == "generic") {
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
        testGenericProducer.sendMessage(GenericConsumer.GENERIC_EVENT_TYPE, bytes, listId.toString()).block()

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