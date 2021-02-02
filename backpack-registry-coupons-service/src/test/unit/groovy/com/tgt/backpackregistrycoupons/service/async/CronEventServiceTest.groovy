package com.tgt.backpackregistrycoupons.service.async

import com.tgt.backpackregistryclient.client.BackpackRegistryClient
import com.tgt.backpackregistryclient.transport.RegistryDetailsResponseTO
import com.tgt.backpackregistryclient.util.RegistryType
import com.tgt.backpackregistrycoupons.domain.CouponAssignmentCalculationManager
import com.tgt.backpackregistrycoupons.domain.model.Coupons
import com.tgt.backpackregistrycoupons.domain.model.Registry
import com.tgt.backpackregistrycoupons.domain.model.RegistryCoupons
import com.tgt.backpackregistrycoupons.persistence.repository.compositetransaction.CompositeTransactionalRepository
import com.tgt.backpackregistrycoupons.persistence.repository.coupons.CouponsRepository
import com.tgt.backpackregistrycoupons.persistence.repository.registry.RegistryRepository
import com.tgt.backpackregistrycoupons.persistence.repository.registrycoupons.RegistryCouponsRepository
import com.tgt.backpackregistrycoupons.util.CouponRedemptionStatus
import com.tgt.backpackregistrycoupons.util.CouponType
import com.tgt.lists.atlas.api.type.LIST_STATE
import com.tgt.notification.tracer.client.model.NotificationTracerEvent
import com.tgt.notification.tracer.client.producer.NotificationTracerProducer
import org.apache.kafka.clients.producer.RecordMetadata
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import spock.lang.Specification

import java.time.LocalDate
import java.time.LocalDateTime

class CronEventServiceTest extends Specification {

    CouponAssignmentCalculationManager couponAssignmentCalculationManager
    CompositeTransactionalRepository compositeTransactionalRepository
    SendGuestNotificationsService sendGuestNotificationsService
    RegistryCouponsRepository registryCouponsRepository
    RegistryRepository registryRepository
    CouponsRepository couponsRepository
    NotificationTracerProducer notificationTracerProducer
    CronEventService cronEventService
    BackpackRegistryClient backpackRegistryClient

    def setup() {
        couponsRepository = Mock(CouponsRepository)
        registryRepository = Mock(RegistryRepository)
        registryCouponsRepository = Mock(RegistryCouponsRepository)
        compositeTransactionalRepository = Mock(CompositeTransactionalRepository)
        notificationTracerProducer = Mock(NotificationTracerProducer)
        backpackRegistryClient = Mock(BackpackRegistryClient)
        sendGuestNotificationsService = new SendGuestNotificationsService(notificationTracerProducer)
        couponAssignmentCalculationManager = new CouponAssignmentCalculationManager(7L , 56L, 14L)
        cronEventService = new CronEventService(couponAssignmentCalculationManager, compositeTransactionalRepository, sendGuestNotificationsService, registryRepository, couponsRepository, backpackRegistryClient, 180)
    }

    def "Test processCronEvent"() {
        given:
        // First registry: No coupons assigned
        def registryCreatedDate1 = LocalDate.now().minusDays(15)
        def eventDate1 = LocalDate.now().minusDays(5)
        def registry1 = new Registry(UUID.randomUUID(), RegistryType.BABY,  LIST_STATE.ACTIVE.value, registryCreatedDate1,  eventDate1, false, null, null)

        // Second registry: Partial coupon assigned
        def registryCreatedDate2 = LocalDate.now().minusDays(60)
        def eventDate2 = LocalDate.now()
        def registry2 = new Registry(UUID.randomUUID(), RegistryType.WEDDING,  LIST_STATE.ACTIVE.value, registryCreatedDate2,  eventDate2, false, null, null)
        def registryCoupons21 = new RegistryCoupons("1234", registry2, CouponType.STORE, CouponRedemptionStatus.AVAILABLE, LocalDate.now(), LocalDate.now().plusDays(2), null , null)
        registry2.registryCoupons = [registryCoupons21] as Set

        // Third registry: Registry not ready for coupon assignment yet
        def registryCreatedDate3 = LocalDate.now()
        def eventDate3 = LocalDate.now()
        def registry3 = new Registry(UUID.randomUUID(), RegistryType.BABY,  LIST_STATE.ACTIVE.value, registryCreatedDate3,  eventDate3, false, null, null)

        // Fourth registry: Assign only one coupons type (partial assignment)
        def registryCreatedDate4 = LocalDate.now().minusDays(15)
        def eventDate4 = LocalDate.now().minusDays(5)
        def registry4 = new Registry(UUID.randomUUID(), RegistryType.BABY,  LIST_STATE.ACTIVE.value, registryCreatedDate4,  eventDate4, false, null, null)

        def cronEventDate = LocalDateTime.now()
        def recordMetadata = GroovyMock(RecordMetadata)

        when:
        def actual = cronEventService.processCronEvent(cronEventDate).block()

        then:
        1 * registryRepository.findByRegistryStatusAndCouponAssignmentComplete(LIST_STATE.ACTIVE.value, false) >> Flux.just(registry1, registry2, registry3, registry4)

        // First Registry
        1 * registryRepository.getByRegistryId(registry1.registryId) >> Mono.just(registry1)
        1 * couponsRepository.findTop1ByCouponTypeAndRegistryTypeAndCouponExpiryDateGreaterThanEquals(CouponType.STORE, RegistryType.BABY, eventDate1.plusDays(180)) >> Mono.just(new Coupons("1000000", CouponType.STORE, RegistryType.BABY, LocalDate.now(), "1", null, null))
        1 * couponsRepository.findTop1ByCouponTypeAndRegistryTypeAndCouponExpiryDateGreaterThanEquals(CouponType.ONLINE, RegistryType.BABY, eventDate1.plusDays(180)) >> Mono.just(new Coupons("2000000", CouponType.ONLINE, RegistryType.BABY, LocalDate.now(), "1", null, null))
        1 * compositeTransactionalRepository.assignCoupons(_ as List<RegistryCoupons>) >> { arguments ->
            final List<RegistryCoupons> list = arguments[0]
            assert list.size() == 2
            Mono.just(true)
        }
        1 * registryRepository.updateCouponAssignmentComplete(registry1.registryId, true) >> Mono.just(1)
        1 * backpackRegistryClient.getRegistryDetails(_,_,_,_,_,_) >> Mono.just(new RegistryDetailsResponseTO(registry1.registryId, "", "", null, null, "regfname", "reglname", "coregfname", "coreglname", LocalDate.now()))
        1 * notificationTracerProducer.sendMessage(NotificationTracerEvent.getEventType(), _, registry1.registryId.toString()) >> Mono.just(recordMetadata)

        // Second Registry with partial coupon assigned
        1 * registryRepository.getByRegistryId(registry2.registryId) >> Mono.just(registry2)
        1 * couponsRepository.findTop1ByCouponTypeAndRegistryTypeAndCouponExpiryDateGreaterThanEquals(CouponType.ONLINE, RegistryType.WEDDING, eventDate2.plusDays(180)) >> Mono.just(new Coupons("3000000", CouponType.ONLINE, RegistryType.WEDDING, LocalDate.now(), "1", null, null))
        1 * compositeTransactionalRepository.assignCoupons(_ as List<RegistryCoupons>) >> { arguments ->
            final List<RegistryCoupons> list = arguments[0]
            assert list.size() == 1
            Mono.just(true)
        }
        1 * registryRepository.updateCouponAssignmentComplete(registry2.registryId, true) >> Mono.just(1)
        1 * backpackRegistryClient.getRegistryDetails(_,_,_,_,_,_) >> Mono.just(new RegistryDetailsResponseTO(registry2.registryId, "", "", null, null, "regfname", "reglname", "coregfname", "coreglname", LocalDate.now()))
        1 * notificationTracerProducer.sendMessage(NotificationTracerEvent.getEventType(), _, registry2.registryId.toString()) >> Mono.just(recordMetadata)

        // Forth Registry assigning only one coupon
        1 * registryRepository.getByRegistryId(registry4.registryId) >> Mono.just(registry4)
        1 * couponsRepository.findTop1ByCouponTypeAndRegistryTypeAndCouponExpiryDateGreaterThanEquals(CouponType.ONLINE, RegistryType.BABY, eventDate4.plusDays(180)) >> Mono.just(new Coupons("4000000", CouponType.ONLINE, RegistryType.BABY, LocalDate.now(), "1", null, null))
        1 * couponsRepository.findTop1ByCouponTypeAndRegistryTypeAndCouponExpiryDateGreaterThanEquals(CouponType.STORE, RegistryType.BABY, eventDate4.plusDays(180)) >> Mono.empty()
        1 * compositeTransactionalRepository.assignCoupons(_ as List<RegistryCoupons>) >> { arguments ->
            final List<RegistryCoupons> list = arguments[0]
            assert list.size() == 1
            Mono.just(true)
        }
        1 * backpackRegistryClient.getRegistryDetails(_,_,_,_,_,_) >> Mono.just(new RegistryDetailsResponseTO(registry4.registryId, "", "", null, null, "regfname", "reglname", "coregfname", "coreglname", LocalDate.now()))
        1 * notificationTracerProducer.sendMessage(NotificationTracerEvent.getEventType(), _, registry4.registryId.toString()) >> Mono.just(recordMetadata)

        actual
    }

    def "Test findByRegistryStatusAndCouponAssignmentComplete with no result"() {
        when:
        def actual = cronEventService.processCronEvent(LocalDateTime.now()).block()

        then:
        1 * registryRepository.findByRegistryStatusAndCouponAssignmentComplete(LIST_STATE.ACTIVE.value, false) >> Flux.empty()

        actual
    }

    def "Test findByRegistryStatusAndCouponAssignmentComplete exception"() {
        when:
        def actual = cronEventService.processCronEvent(LocalDateTime.now()).block()

        then:
        1 * registryRepository.findByRegistryStatusAndCouponAssignmentComplete(LIST_STATE.ACTIVE.value, false) >> Flux.error(new RuntimeException("some exception"))

        !actual
    }
}

