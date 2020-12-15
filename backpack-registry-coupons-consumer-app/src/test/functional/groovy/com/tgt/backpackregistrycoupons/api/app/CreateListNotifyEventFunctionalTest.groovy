package com.tgt.backpackregistrycoupons.api.app

import com.tgt.backpackregistryclient.transport.RegistryEventTO
import com.tgt.backpackregistryclient.util.RegistryType
import com.tgt.backpackregistrycoupons.persistence.repository.registry.RegistryRepository
import com.tgt.backpackregistrycoupons.test.BaseKafkaFunctionalTest
import com.tgt.backpackregistrycoupons.test.PreDispatchLambda
import com.tgt.backpackregistrycoupons.test.util.RegistryDataProvider
import com.tgt.lists.atlas.api.type.LIST_STATE
import com.tgt.lists.atlas.kafka.model.CreateListNotifyEvent
import com.tgt.lists.msgbus.event.EventHeaders
import com.tgt.lists.msgbus.event.EventLifecycleNotificationProvider
import io.micronaut.configuration.kafka.annotation.KafkaClient
import io.micronaut.configuration.kafka.annotation.KafkaKey
import io.micronaut.configuration.kafka.annotation.Topic
import io.micronaut.messaging.annotation.Body
import io.micronaut.messaging.annotation.Header
import io.micronaut.test.annotation.MicronautTest
import io.opentracing.Tracer
import org.jetbrains.annotations.NotNull
import spock.lang.Shared
import spock.lang.Stepwise
import spock.util.concurrent.PollingConditions

import javax.inject.Inject
import java.time.LocalDate
import java.util.stream.Collectors

@MicronautTest
@Stepwise
class CreateListNotifyEventFunctionalTest extends BaseKafkaFunctionalTest {

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

    RegistryDataProvider registryDataProvider = new RegistryDataProvider()

    def setupSpec() {
        testEventListener = new TestEventListener()
        testEventListener.tracer = tracer
        eventNotificationsProvider.registerListener(testEventListener)
    }

    def setup() {
        testEventListener.reset()
    }

    def "Test CreateListNotifyEvent"() {
        given:
        PollingConditions conditions = new PollingConditions(timeout: 30, delay: 1)
        def registryMetaData = registryDataProvider.getRegistryMetaDataMap(UUID.randomUUID(), false, false, null, null, new RegistryEventTO("city", "state", "country", LocalDate.now()), null, null, null, null)
        def event = new CreateListNotifyEvent("1234", registryId1, listType, RegistryType.WEDDING.name(), "title", "channel", "subChannel", LIST_STATE.INACTIVE, registryMetaData ,LocalDate.now().plusDays(50),null)

        testEventListener.preDispatchLambda = new PreDispatchLambda() {
            @Override
            boolean onPreDispatchConsumerEvent(String topic, @NotNull EventHeaders eventHeaders, @NotNull byte[] data, boolean isPoisonEvent) {
                if (eventHeaders.eventType == CreateListNotifyEvent.getEventType()) {
                    def createRegistry = CreateListNotifyEvent.deserialize(data)
                    if (createRegistry.listId.toString() == event.listId.toString() &&
                        createRegistry.guestId == event.guestId) {
                        return true
                    }
                }
                return false
            }
        }

        when:
        msgBusClient.sendMessage(event.listId.toString(), UUID.randomUUID(), CreateListNotifyEvent.getEventType(), "backpack-registry", event)

        then:
        testEventListener.verifyEvents { consumerEvents, producerEvents, consumerStatusEvents ->
            conditions.eventually {
                List<TestEventListener.Result> completedEvents = consumerEvents.stream().filter {
                    def result = (TestEventListener.Result) it
                    (!result.preDispatch)
                }.collect(Collectors.toList())
                assert completedEvents.any { it.eventHeaders.eventType == CreateListNotifyEvent.getEventType() && it.success }
            }
        }
    }

    def "check if registry got added"() {
        when:
        def result = registryRepository.findByRegistryId(registryId1).block()

        then:
        result != null
        result.registryId == registryId1
    }

    @KafkaClient(acks = KafkaClient.Acknowledge.ALL, id = "lists-msg-bus")
    static interface ListMsgBusClient {
        @Topic("lists-msg-bus")
        String sendMessage(@KafkaKey String id, @Header UUID uuid, @Header String event_type, @Header String source,
                           @Body CreateListNotifyEvent createListNotifyEvent)
    }
}
