package com.tgt.backpackregistrycoupons.api.app.welcomekit

import com.tgt.backpackregistrycoupons.test.BaseKafkaFunctionalTest
import com.tgt.backpackregistrycoupons.test.PreDispatchLambda
import com.tgt.backpackregistrycoupons.welcomekit.domain.model.WelcomeKits
import com.tgt.backpackregistrycoupons.welcomekit.persistence.repository.welcomekit.WelcomeKitsRepository
import com.tgt.backpacktransactionsclient.transport.kafka.model.RegistryItemTransactionActionEvent
import com.tgt.backpacktransactionsclient.transport.kafka.model.RegistryTransactionTO
import com.tgt.backpackregistrycoupons.transport.kafka.WelcomeKitTransactionActionEvent
import com.tgt.lists.micronaut.persistence.instrumentation.DatabaseExecTestListener
import com.tgt.lists.micronaut.persistence.instrumentation.RepositoryInstrumenter
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
import java.util.stream.Collectors

@MicronautTest
@Stepwise
class RegistryItemTransactionActionEventFunctionalTest extends BaseKafkaFunctionalTest {

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
    WelcomeKitsRepository welcomeKitsRepository

    @Shared
    def tcin = "1234"

    @Shared
    boolean executeTimeout = false

    @Inject
    RepositoryInstrumenter repositoryInstrumenter

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

    def "Add welcome kit"() {
        given:
        def welcomeKit = new WelcomeKits(tcin, null, null)

        when:
        def result = welcomeKitsRepository.save([welcomeKit]).collectList().block()

        then:
        !result.empty
    }

    def "Test welcome kit transaction event"() {
        given:
        PollingConditions conditions = new PollingConditions(timeout: 30, delay: 1)
        def event = new RegistryItemTransactionActionEvent(new RegistryTransactionTO(123L, null, null, null, null,UUID.randomUUID(), null, "transaction type", null, null, null, null, null, tcin, null, null, 1, null, 1375L, null, null, null, null, null, null, null, null, null, null, null, null, null))

        testEventListener.preDispatchLambda = new PreDispatchLambda() {
            @Override
            boolean onPreDispatchConsumerEvent(String topic, @NotNull EventHeaders eventHeaders, @NotNull byte[] data, boolean isPoisonEvent) {
                if (eventHeaders.eventType == RegistryItemTransactionActionEvent.getEventType()) {
                    def registryItemTransaction = RegistryItemTransactionActionEvent.deserialize(data)
                    if (registryItemTransaction.registryTransactionTO.registryId == event.registryTransactionTO.registryId) {
                        return true
                    }
                }
                return false
            }
        }

        when:
        msgBusClient.sendMessage(event.registryTransactionTO.registryId.toString(), UUID.randomUUID(), RegistryItemTransactionActionEvent.getEventType(), "backpack-registry", event)

        then:
        testEventListener.verifyEvents { consumerEvents, producerEvents, consumerStatusEvents ->
            conditions.eventually {
                List<TestEventListener.Result> completedEvents = consumerEvents.stream().filter {
                    def result = (TestEventListener.Result) it
                    (!result.preDispatch)
                }.collect(Collectors.toList())
                List<TestEventListener.Result> producedEvents = producerEvents.stream().filter {
                    def result = (TestEventListener.Result) it
                    (!result.preDispatch)
                }.collect(Collectors.toList())

                assert completedEvents.any {it.eventHeaders.eventType == RegistryItemTransactionActionEvent.getEventType() && it.success }
                assert producedEvents.any {it.eventHeaders.eventType == WelcomeKitTransactionActionEvent.getEventType()}
            }
        }
    }

    def "Test welcome kit transaction event failure scenario"() {
        given:
        PollingConditions conditions = new PollingConditions(timeout: 30, delay: 1)
        def event = new RegistryItemTransactionActionEvent(new RegistryTransactionTO(123L, null, null, null, null,UUID.randomUUID(), null, "transaction type", null, null, null, null, null, tcin, null, null, 1, null, 1375L, null, null, null, null, null, null, null, null, null, null, null, null, null))

        testEventListener.preDispatchLambda = new PreDispatchLambda() {
            @Override
            boolean onPreDispatchConsumerEvent(String topic, @NotNull EventHeaders eventHeaders, @NotNull byte[] data, boolean isPoisonEvent) {
                if (eventHeaders.eventType == RegistryItemTransactionActionEvent.getEventType()) {
                    def registryItemTransaction = RegistryItemTransactionActionEvent.deserialize(data)
                    if (registryItemTransaction.registryTransactionTO.registryId == event.registryTransactionTO.registryId) {
                        if (eventHeaders.source == "backpack-registry-coupons-dlq") {
                            executeTimeout = false
                            return true
                        }
                        if (eventHeaders.source == "backpack-registry") {
                            executeTimeout = true
                            return true
                        }
                    }
                }
                return false
            }
        }

        when:
        msgBusClient.sendMessage(event.registryTransactionTO.registryId.toString(), UUID.randomUUID(), RegistryItemTransactionActionEvent.getEventType(), "backpack-registry", event)

        then:
        testEventListener.verifyEvents { consumerEvents, producerEvents, consumerStatusEvents ->
            conditions.eventually {
                def completedEvents = consumerEvents.stream().filter {
                    def result = (TestEventListener.Result) it
                    (!result.preDispatch)
                }.collect(Collectors.toList())
                assert completedEvents.any{
                    it.success
                } // after the first time failure , message from dlq will be processed and completed
                assert ((TestEventListener.Result) producerEvents[0]).topic == "registry-internal-data-bus-stage-dlq"
                assert ((TestEventListener.Result) producerEvents[1]).topic == "registry-internal-data-bus-stage"
            }
        }
    }

    @KafkaClient(acks = KafkaClient.Acknowledge.ALL, id = "registry-internal-data-bus-stage")
    static interface ListMsgBusClient {
        @Topic("registry-internal-data-bus-stage")
        String sendMessage(@KafkaKey String id, @Header UUID uuid, @Header String event_type, @Header String source,
                           @Body RegistryItemTransactionActionEvent registryItemTransactionActionEvent)
    }
}
