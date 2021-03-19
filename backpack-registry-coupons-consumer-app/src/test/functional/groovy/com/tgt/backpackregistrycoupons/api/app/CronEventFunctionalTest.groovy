package com.tgt.backpackregistrycoupons.api.app

import com.tgt.backpackregistryclient.transport.RegistryDetailsResponseTO
import com.tgt.backpackregistryclient.util.RegistryType
import com.tgt.backpackregistrycoupons.domain.model.Coupons
import com.tgt.backpackregistrycoupons.domain.model.Registry
import com.tgt.backpackregistrycoupons.persistence.repository.coupons.CouponsRepository
import com.tgt.backpackregistrycoupons.persistence.repository.registry.RegistryRepository
import com.tgt.backpackregistrycoupons.test.BaseKafkaFunctionalTest
import com.tgt.backpackregistrycoupons.test.PreDispatchLambda
import com.tgt.backpackregistrycoupons.test.util.RegistryDataProvider
import com.tgt.backpackregistrycoupons.util.CouponType
import com.tgt.cronbeacon.kafka.model.CronEvent
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
import java.time.ZoneId
import java.util.stream.Collectors

@MicronautTest
@Stepwise
class CronEventFunctionalTest extends BaseKafkaFunctionalTest {

    @Shared
    @Inject
    Tracer tracer
    @Shared
    @Inject
    EventLifecycleNotificationProvider eventNotificationsProvider
    @Shared
    TestEventListener testEventListener
    @Inject
    BeaconClient beaconClient

    @Inject
    RegistryRepository registryRepository

    @Inject
    CouponsRepository couponsRepository

    @Shared def registryId1 = UUID.randomUUID()

    RegistryDataProvider registryDataProvider = new RegistryDataProvider()

    def getDetailsURI = "/registries/v2/"+registryId1+"/summary_details"

    def setupSpec() {
        testEventListener = new TestEventListener()
        testEventListener.tracer = tracer
        eventNotificationsProvider.registerListener(testEventListener)
    }

    def setup() {
        testEventListener.reset()
    }

    def "create registry"() {
        given:
        def alternateRegistryId = "12345"
        def registry = new Registry(registryId1, alternateRegistryId, RegistryType.WEDDING,  LIST_STATE.ACTIVE.value, LocalDate.now().minusDays(50), LocalDate.now().plusDays(1), false, null, null)
        when:
        def result = registryRepository.save(registry).block()

        then:
        result != null
        result.registryId == registryId1
    }

    def "create unassigned coupons"() {
        given:
        def coupons1 = new Coupons("1000000", CouponType.ONLINE, RegistryType.BABY, LocalDate.now().plusDays(365), "1", null, null)
        def coupons2 = new Coupons("2000000", CouponType.STORE, RegistryType.WEDDING, LocalDate.now().plusDays(365), "1", null, null)
        def coupons3 = new Coupons("3000000", CouponType.ONLINE, RegistryType.BABY, LocalDate.now().plusDays(365), "1", null, null)
        def coupons4 = new Coupons("4000000", CouponType.STORE, RegistryType.BABY, LocalDate.now().plusDays(365), "1", null, null)
        def coupons5 = new Coupons("5000000", CouponType.ONLINE, RegistryType.WEDDING, LocalDate.now().plusDays(365), "1", null, null)

        when:
        def result1 = couponsRepository.save(coupons1).block()
        def result2 = couponsRepository.save(coupons2).block()
        def result3 = couponsRepository.save(coupons3).block()
        def result4 = couponsRepository.save(coupons4).block()
        def result5 = couponsRepository.save(coupons5).block()

        then:
        result1 != null
        result2 != null
        result3 != null
        result4 != null
        result5 != null
    }

    def "Test CronEvent"() {
        given:
        PollingConditions conditions = new PollingConditions(timeout: 30, delay: 1)
        CronEvent event = registryDataProvider.createCronEvent(LocalDateTime.of(2100, 03, 01, 02, 01), 1L, 5, ZoneId.of("America/Chicago"))

        testEventListener.preDispatchLambda = new PreDispatchLambda() {
            @Override
            boolean onPreDispatchConsumerEvent(String topic, @NotNull EventHeaders eventHeaders, @NotNull byte[] data, boolean isPoisonEvent) {
                if (eventHeaders.eventType == CronEvent.getEventType()) {
                    def cronEvent = CronEvent.deserialize(data)
                    if (cronEvent.minuteBlockOfHour == event.minuteBlockOfHour) {
                        return true
                    }
                }
                return false
            }
        }
        RegistryDetailsResponseTO registryDetailsResponseTO = registryDataProvider.getRegistryDetails(registryId1.toString())

        1 * mockServer.get({ path -> path.contains(getDetailsURI)},_) >> [status: 200, body: registryDetailsResponseTO]

        when:
        beaconClient.sendMessage(UUID.randomUUID().toString(), UUID.randomUUID(), CronEvent.getEventType(), "cronbeacon", event)

        then:
        testEventListener.verifyEvents { consumerEvents, producerEvents, consumerStatusEvents ->
            conditions.eventually {
                TestEventListener.Result[] completedEvents = consumerEvents.stream().filter {
                    def result = (TestEventListener.Result) it
                    (!result.preDispatch)
                }.collect(Collectors.toList())
                assert completedEvents.any { it.eventHeaders.eventType == CronEvent.getEventType() && it.success }
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

    @KafkaClient(acks = KafkaClient.Acknowledge.ALL, id = "beacon-topic-client")
    static interface BeaconClient {
        @Topic("beacon-topic")
        String sendMessage(@KafkaKey String id, @Header UUID uuid, @Header String event_type, @Header String source,
                           @Body CronEvent cronEvent)
    }
}
