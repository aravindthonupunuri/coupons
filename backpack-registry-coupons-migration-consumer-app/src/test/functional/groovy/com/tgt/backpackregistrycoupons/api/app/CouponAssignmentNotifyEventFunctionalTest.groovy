package com.tgt.backpackregistrycoupons.api.app

import com.tgt.backpackregistryclient.util.RegistryType
import com.tgt.backpackregistrycoupons.domain.model.Registry
import com.tgt.backpackregistrycoupons.migration.model.CouponAssignmentNotifyEvent
import com.tgt.backpackregistrycoupons.persistence.repository.registry.RegistryRepository
import com.tgt.backpackregistrycoupons.test.BaseKafkaFunctionalTest
import com.tgt.backpackregistrycoupons.test.PreDispatchLambda
import com.tgt.backpackregistrycoupons.test.util.RegistryDataProvider
import com.tgt.backpackregistrycoupons.util.CouponRedemptionStatus
import com.tgt.lists.atlas.api.type.LIST_STATE
import com.tgt.lists.msgbus.event.EventHeaders
import com.tgt.lists.msgbus.event.EventLifecycleNotificationProvider
import io.micronaut.configuration.kafka.annotation.KafkaClient
import io.micronaut.configuration.kafka.annotation.KafkaKey
import io.micronaut.configuration.kafka.annotation.Topic
import io.micronaut.messaging.annotation.Body
import io.micronaut.messaging.annotation.Header
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.opentracing.Tracer
import org.jetbrains.annotations.NotNull
import spock.lang.Shared
import spock.lang.Stepwise
import spock.util.concurrent.PollingConditions

import javax.inject.Inject
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.stream.Collectors

@MicronautTest
@Stepwise
class CouponAssignmentNotifyEventFunctionalTest extends BaseKafkaFunctionalTest {

    @Shared
    @Inject
    Tracer tracer
    @Shared
    @Inject
    EventLifecycleNotificationProvider eventNotificationsProvider
    @Shared
    TestEventListener testEventListener
    @Inject
    ListMsgBusClient msgBusClient

    @Inject
    RegistryRepository registryRepository

    @Shared def registryId1 = UUID.randomUUID()
    @Shared def onlineCouponCode = "1234"
    @Shared def storeCouponCode = "5678"

    RegistryDataProvider registryDataProvider = new RegistryDataProvider()

    def setupSpec() {
        testEventListener = new TestEventListener()
        testEventListener.tracer = tracer
        eventNotificationsProvider.registerListener(testEventListener)
    }

    def setup() {
        testEventListener.reset()
    }

    def "create new registry"() {
        given:
        def registry = new Registry(registryId1, RegistryType.WEDDING,  LIST_STATE.INACTIVE.value, LocalDate.now().minusDays(4), LocalDate.now().plusDays(20), false, null, null)
        when:
        def result = registryRepository.save(registry).block()

        then:
        result != null
        result.registryId == registryId1
    }

    def "Test CouponAssignmentNotifyEvent"() {
        given:
        PollingConditions conditions = new PollingConditions(timeout: 30, delay: 1)
        def registryCouponMetaData = registryDataProvider.getRegistryCouponMetaDataMap(onlineCouponCode, CouponRedemptionStatus.AVAILABLE, storeCouponCode, CouponRedemptionStatus.AVAILABLE, LocalDate.now(), LocalDate.now(), LocalDateTime.now(), LocalDateTime.now())
        def event = new CouponAssignmentNotifyEvent("1234", registryId1, listType, RegistryType.WEDDING.name(), registryCouponMetaData,null)

        testEventListener.preDispatchLambda = new PreDispatchLambda() {
            @Override
            boolean onPreDispatchConsumerEvent(String topic, @NotNull EventHeaders eventHeaders, @NotNull byte[] data, boolean isPoisonEvent) {
                if (eventHeaders.eventType == CouponAssignmentNotifyEvent.getEventType()) {
                    def couponAssignmentNotifyEvent = CouponAssignmentNotifyEvent.deserialize(data)
                    if (couponAssignmentNotifyEvent.listId == event.listId) {
                        return true
                    }
                }
                return false
            }
        }

        when:
        msgBusClient.sendMessage(event.listId.toString(), UUID.randomUUID(), CouponAssignmentNotifyEvent.getEventType(), "backpack-registry", event)

        then:
        testEventListener.verifyEvents { consumerEvents, producerEvents, consumerStatusEvents ->
            conditions.eventually {
                List<TestEventListener.Result> completedEvents = consumerEvents.stream().filter {
                    def result = (TestEventListener.Result) it
                    (!result.preDispatch)
                }.collect(Collectors.toList())
                assert completedEvents.any { it.eventHeaders.eventType == CouponAssignmentNotifyEvent.getEventType() && it.success }
            }
        }
    }

    def "check if coupons got assigned to regitry"() {
        when:
        def result = registryRepository.getByRegistryId(registryId1).block()

        then:
        result != null
        result.registryId == registryId1
        result.getRegistryCoupons().size() == 2
    }

    @KafkaClient(acks = KafkaClient.Acknowledge.ALL, id = "registry-internal-data-bus-stage")
    static interface ListMsgBusClient {
        @Topic("registry-internal-data-bus-stage")
        String sendMessage(@KafkaKey String id, @Header UUID uuid, @Header String event_type, @Header String source,
                           @Body CouponAssignmentNotifyEvent couponAssignmentNotifyEvent)
    }
}
