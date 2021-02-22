package com.tgt.backpackregistrycoupons.api.app

import com.tgt.backpackregistryclient.transport.RegistryEventTO
import com.tgt.backpackregistryclient.util.RegistryType
import com.tgt.backpackregistrycoupons.domain.model.Registry
import com.tgt.backpackregistrycoupons.persistence.repository.registry.RegistryRepository
import com.tgt.backpackregistrycoupons.test.BaseKafkaFunctionalTest
import com.tgt.backpackregistrycoupons.test.PreDispatchLambda
import com.tgt.backpackregistrycoupons.test.util.RegistryDataProvider
import com.tgt.lists.atlas.api.type.LIST_STATE
import com.tgt.lists.atlas.kafka.model.UpdateListNotifyEvent
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
import java.util.stream.Collectors

@MicronautTest
@Stepwise
class UpdateListNotifyEventFunctionalTest extends BaseKafkaFunctionalTest {

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

    def "create new registry"() {
        given:
        def registry = new Registry(registryId1, RegistryType.WEDDING,  LIST_STATE.INACTIVE.value, LocalDate.now().minusDays(4), LocalDate.now().plusDays(20), false, null, null)
        when:
        def result = registryRepository.save(registry).block()

        then:
        result != null
        result.registryId == registryId1
    }

    def "Test UpdateListNotifyEvent, update event date and registry status"() {
        given:
        PollingConditions conditions = new PollingConditions(timeout: 30, delay: 1)
        def registryMetaData = registryDataProvider.getRegistryMetaDataMap(UUID.randomUUID(), "1234", false, false, null, null, new RegistryEventTO("city", "state", "country", LocalDate.of(2100,02,01)), null, null, null, null, "abcd", "xyz")
        def event = new UpdateListNotifyEvent("1234", registryId1, listType, RegistryType.WEDDING.name(), "title", "channel", "subChannel", LIST_STATE.ACTIVE, registryMetaData ,LocalDate.now().plusDays(50),null,
        null, null, null, null)

        testEventListener.preDispatchLambda = new PreDispatchLambda() {
            @Override
            boolean onPreDispatchConsumerEvent(String topic, @NotNull EventHeaders eventHeaders, @NotNull byte[] data, boolean isPoisonEvent) {
                if (eventHeaders.eventType == UpdateListNotifyEvent.getEventType()) {
                    def updateRegistry = UpdateListNotifyEvent.deserialize(data)
                    if (updateRegistry.listId.toString() == event.listId.toString() &&
                        updateRegistry.guestId == event.guestId) {
                        return true
                    }
                }
                return false
            }
        }

        when:
        msgBusClient.sendMessage(event.listId.toString(), UUID.randomUUID(), UpdateListNotifyEvent.getEventType(), "backpack-registry", event)

        then:
        testEventListener.verifyEvents { consumerEvents, producerEvents, consumerStatusEvents ->
            conditions.eventually {
                List<TestEventListener.Result> completedEvents = consumerEvents.stream().filter {
                    def result = (TestEventListener.Result) it
                    (!result.preDispatch)
                }.collect(Collectors.toList())
                assert completedEvents.any { it.eventHeaders.eventType == UpdateListNotifyEvent.getEventType() && it.success }
            }
        }
    }

    def "check if registry got updated"() {
        when:
        def result = registryRepository.findByRegistryId(registryId1).block()

        then:
        result != null
        result.registryId == registryId1
        result.registryStatus == LIST_STATE.ACTIVE.value
        result.eventDate == LocalDate.of(2100,02,01)


    }

    @KafkaClient(acks = KafkaClient.Acknowledge.ALL, id = "registry-internal-data-bus-stage")
    static interface ListMsgBusClient {
        @Topic("registry-internal-data-bus-stage")
        String sendMessage(@KafkaKey String id, @Header UUID uuid, @Header String event_type, @Header String source,
                           @Body UpdateListNotifyEvent updateListNotifyEvent)
    }
}
