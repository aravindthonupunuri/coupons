package com.tgt.backpackregistrycoupons.api.app

import com.tgt.backpackregistryclient.util.RegistryType
import com.tgt.backpackregistrycoupons.kafka.model.CreateCollegeListNotifyEvent
import com.tgt.backpackregistrycoupons.persistence.repository.registry.RegistryRepository
import com.tgt.backpackregistrycoupons.test.BaseKafkaFunctionalTest
import com.tgt.backpackregistrycoupons.test.PreDispatchLambda
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
class CreateCollegeListNotifyEventFunctionalTest extends BaseKafkaFunctionalTest {

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

    PollingConditions conditions = new PollingConditions(timeout: 30, delay: 1)

    def setupSpec() {
        testEventListener = new TestEventListener()
        testEventListener.tracer = tracer
        eventNotificationsProvider.registerListener(testEventListener)
    }

    def setup() {
        testEventListener.reset()
    }

    def "Test CreateListNotifyEvent - CUSTOM registry type, gets added into postgres"() {
        given:

        def registryId = UUID.randomUUID()
        def event = new CreateCollegeListNotifyEvent("1234", registryId, listType, RegistryType.CUSTOM.name(),
            LIST_STATE.INACTIVE, "alternateRegistryId", LocalDate.now().plusDays(50), LocalDateTime.now(), null, null, null)

        testEventListener.preDispatchLambda = new PreDispatchLambda() {
            @Override
            boolean onPreDispatchConsumerEvent(String topic, @NotNull EventHeaders eventHeaders, @NotNull byte[] data, boolean isPoisonEvent) {
                if (eventHeaders.eventType == CreateCollegeListNotifyEvent.getEventType()) {
                    def createCollegeRegistry = CreateCollegeListNotifyEvent.deserialize(data)
                    if (createCollegeRegistry.listId.toString() == event.listId.toString() &&
                        createCollegeRegistry.guestId == event.guestId) {
                        return true
                    }
                }
                return false
            }
        }

        when:
        msgBusClient.sendMessage(event.listId.toString(), UUID.randomUUID(), CreateCollegeListNotifyEvent.getEventType(), "backpack-registry", event)

        then:
        testEventListener.verifyEvents { consumerEvents, producerEvents, consumerStatusEvents ->
            conditions.eventually {
                List<TestEventListener.Result> completedEvents = consumerEvents.stream().filter {
                    def result = (TestEventListener.Result) it
                    (!result.preDispatch)
                }.collect(Collectors.toList())
                assert completedEvents.any { it.eventHeaders.eventType == CreateCollegeListNotifyEvent.getEventType() && it.success }
            }
        }

        and:
        def result = registryRepository.findByRegistryId(registryId).block()
        result.registryId == registryId
        result.alternateRegistryId == "alternateRegistryId"
    }

    @KafkaClient(acks = KafkaClient.Acknowledge.ALL, id = "registry-internal-data-bus-stage")
    static interface ListMsgBusClient {
        @Topic("registry-internal-data-bus-stage")
        String sendMessage(@KafkaKey String id, @Header UUID uuid, @Header String event_type, @Header String source,
                           @Body CreateCollegeListNotifyEvent createCollegeListNotifyEvent)
    }
}
