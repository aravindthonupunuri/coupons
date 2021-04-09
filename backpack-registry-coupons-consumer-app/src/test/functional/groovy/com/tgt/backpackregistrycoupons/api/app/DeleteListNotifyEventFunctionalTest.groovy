package com.tgt.backpackregistrycoupons.api.app


import com.tgt.backpackregistryclient.util.RegistryType
import com.tgt.backpackregistrycoupons.domain.model.Registry
import com.tgt.backpackregistrycoupons.domain.model.RegistryCoupons
import com.tgt.backpackregistrycoupons.persistence.repository.registry.RegistryRepository
import com.tgt.backpackregistrycoupons.persistence.repository.registrycoupons.RegistryCouponsRepository
import com.tgt.backpackregistrycoupons.test.BaseKafkaFunctionalTest
import com.tgt.backpackregistrycoupons.test.PreDispatchLambda
import com.tgt.backpackregistrycoupons.test.util.RegistryDataProvider
import com.tgt.backpackregistrycoupons.util.CouponRedemptionStatus
import com.tgt.backpackregistrycoupons.util.CouponType
import com.tgt.lists.atlas.api.type.LIST_STATE
import com.tgt.lists.atlas.kafka.model.DeleteListNotifyEvent
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
class DeleteListNotifyEventFunctionalTest extends BaseKafkaFunctionalTest {

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

    @Inject
    RegistryCouponsRepository registryCouponsRepository

    @Shared def registryId1 = UUID.randomUUID()

    @Shared
    List<String> couponCodes = ["1234", "4567"]

    RegistryDataProvider registryDataProvider = new RegistryDataProvider()


    def setupSpec() {
        testEventListener = new TestEventListener()
        testEventListener.tracer = tracer
        eventNotificationsProvider.registerListener(testEventListener)
    }

    def setup() {
        testEventListener.reset()
    }

    def "create registry and registry coupons"() {
        given:
        def alternateRegistryId = "12345"
        def registry = new Registry(registryId1, alternateRegistryId, RegistryType.WEDDING,  LIST_STATE.INACTIVE.value, LocalDate.now().minusDays(4), LocalDate.now().plusDays(20), false, null, null)

        def registryCoupons1 = new RegistryCoupons(couponCodes[0], registry, CouponType.STORE, CouponRedemptionStatus.AVAILABLE, LocalDate.now(), LocalDate.now().plusDays(2), null , null)
        def registryCoupons2 = new RegistryCoupons(couponCodes[1], registry, CouponType.ONLINE, CouponRedemptionStatus.AVAILABLE, LocalDate.now(), LocalDate.now().plusDays(2), null , null)

        when:
        def result = registryRepository.save(registry).block()

        then:
        result != null
        result.registryId == registryId1

        and:

        when:
        def result3 = registryCouponsRepository.saveAll([registryCoupons1, registryCoupons2]).collectList().block()

        then:
        result3.size() == 2
    }

    def "Test DeleteListNotifyEvent, delete registry and its coupons"() {
        given:
        PollingConditions conditions = new PollingConditions(timeout: 30, delay: 1)
        def event = new DeleteListNotifyEvent("1234", registryId1, listType, RegistryType.WEDDING.name(), "title", "channel", "sub_channel", "3991", "", null, LIST_STATE.INACTIVE, LocalDate.now().plusDays(50), null,
            null, null, null, null, null, null)

        testEventListener.preDispatchLambda = new PreDispatchLambda() {
            @Override
            boolean onPreDispatchConsumerEvent(String topic, @NotNull EventHeaders eventHeaders, @NotNull byte[] data, boolean isPoisonEvent) {
                if (eventHeaders.eventType == DeleteListNotifyEvent.getEventType()) {
                    def deleteRegistry = DeleteListNotifyEvent.deserialize(data)
                    if (deleteRegistry.listId.toString() == event.listId.toString() &&
                        deleteRegistry.guestId == event.guestId) {
                        return true
                    }
                }
                return false
            }
        }

        when:
        msgBusClient.sendMessage(event.listId.toString(), UUID.randomUUID(), DeleteListNotifyEvent.getEventType(), "backpack-registry", event)

        then:
        testEventListener.verifyEvents { consumerEvents, producerEvents, consumerStatusEvents ->
            conditions.eventually {
                List<TestEventListener.Result> completedEvents = consumerEvents.stream().filter {
                    def result = (TestEventListener.Result) it
                    (!result.preDispatch)
                }.collect(Collectors.toList())
                assert completedEvents.any { it.eventHeaders.eventType == DeleteListNotifyEvent.getEventType() && it.success }
            }
        }
    }

    def "check if registry got deleted"() {
        when:
        def result = registryRepository.findByRegistryId(registryId1).block()

        then:
        result == null
    }

    @KafkaClient(acks = KafkaClient.Acknowledge.ALL, id = "registry-internal-data-bus-stage")
    static interface ListMsgBusClient {
        @Topic("registry-internal-data-bus-stage")
        String sendMessage(@KafkaKey String id, @Header UUID uuid, @Header String event_type, @Header String source,
                           @Body DeleteListNotifyEvent deleteListNotifyEvent)
    }
}
