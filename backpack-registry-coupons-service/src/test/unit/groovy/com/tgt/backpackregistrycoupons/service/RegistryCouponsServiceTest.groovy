package com.tgt.backpackregistrycoupons.service

import com.tgt.backpackregistryclient.util.RegistryType
import com.tgt.backpackregistrycoupons.domain.CouponAssignmentCalculationManager
import com.tgt.backpackregistrycoupons.domain.model.Registry
import com.tgt.backpackregistrycoupons.domain.model.RegistryCoupons
import com.tgt.backpackregistrycoupons.persistence.repository.registry.RegistryRepository
import com.tgt.backpackregistrycoupons.util.CouponRedemptionStatus
import com.tgt.backpackregistrycoupons.util.CouponType
import com.tgt.lists.atlas.api.type.LIST_STATE
import reactor.core.publisher.Mono
import spock.lang.Specification

import java.time.LocalDate

class RegistryCouponsServiceTest extends Specification {

    RegistryRepository registryRepository
    RegistryCouponService registryCouponService
    CouponAssignmentCalculationManager couponAssignmentCalculationManager

    def setup() {
        registryRepository = Mock(RegistryRepository)
        couponAssignmentCalculationManager = new CouponAssignmentCalculationManager(7L , 56L, 14L)
        registryCouponService = new RegistryCouponService(registryRepository, couponAssignmentCalculationManager)
    }

    def "Test getRegistryCoupons"() {
        given:
        def registryId = UUID.randomUUID()
        def registryCreatedDate = LocalDate.now().minusDays(3)
        def eventDate = LocalDate.now()
        def alternateRegistryId = "12345"

        def registry = new Registry(registryId, alternateRegistryId, RegistryType.BABY,  LIST_STATE.ACTIVE.value, registryCreatedDate,  eventDate, false, null, null)

        when:
        def actual = registryCouponService.getRegistryCoupons(registryId).block()

        then:
        1 * registryRepository.getByRegistryId(registryId) >> Mono.just(registry)

        actual != null
        actual.couponCountDownDays == 11
    }

    def "Test getRegistryCoupons where completion coupon sla date is before event date"() {
        given:
        def registryId = UUID.randomUUID()
        def registryCreatedDate = LocalDate.now()
        def eventDate = LocalDate.now().plusDays(60)
        def alternateRegistryId = "12345"

        def registry = new Registry(registryId, alternateRegistryId, RegistryType.BABY,  LIST_STATE.ACTIVE.value, registryCreatedDate,  eventDate, false, null, null)

        when:
        def actual = registryCouponService.getRegistryCoupons(registryId).block()

        then:
        1 * registryRepository.getByRegistryId(registryId) >> Mono.just(registry)

        actual != null
        actual.couponCountDownDays == 53
    }

    def "Test getRegistryCoupons where completion coupon sla date is before event date but after the registry SLA date"() {
        given:
        def registryId = UUID.randomUUID()
        def registryCreatedDate = LocalDate.now()
        def eventDate = LocalDate.now().plusDays(60)
        def alternateRegistryId = "12345"

        def registry = new Registry(registryId, alternateRegistryId, RegistryType.WEDDING,  LIST_STATE.ACTIVE.value, registryCreatedDate,  eventDate, false, null, null)

        when:
        def actual = registryCouponService.getRegistryCoupons(registryId).block()

        then:
        1 * registryRepository.getByRegistryId(registryId) >> Mono.just(registry)

        actual != null
        actual.couponCountDownDays == 14
    }

    def "Test getRegistryCoupons with INACTIVE registry"() {
        given:
        def registryId = UUID.randomUUID()
        def alternateRegistryId = "12345"

        def registry = new Registry(registryId, alternateRegistryId, RegistryType.BABY,  LIST_STATE.INACTIVE.value, LocalDate.now().minusDays(3),  LocalDate.now(), false, null, null)
        def registryCoupons = new RegistryCoupons("1234", registry, CouponType.STORE, CouponRedemptionStatus.AVAILABLE, LocalDate.now(), LocalDate.now().plusDays(2), null , null)
        registry.registryCoupons = [registryCoupons] as Set

        when:
        def actual = registryCouponService.getRegistryCoupons(registryId).block()

        then:
        1 * registryRepository.getByRegistryId(registryId) >> Mono.just(registry)

        actual != null
        actual.couponCountDownDays == null
    }

    def "Test getRegistryCoupons with coupon code already assigned"() {
        given:
        def registryId = UUID.randomUUID()
        def alternateRegistryId = "12345"

        def registry = new Registry(registryId, alternateRegistryId, RegistryType.BABY,  LIST_STATE.ACTIVE.value, LocalDate.now().minusDays(3),  LocalDate.now(), false, null, null)
        def registryCoupons = new RegistryCoupons("1234", registry, CouponType.STORE, CouponRedemptionStatus.AVAILABLE, LocalDate.now(), LocalDate.now().plusDays(2), null , null)
        registry.registryCoupons = [registryCoupons] as Set

        when:
        def actual = registryCouponService.getRegistryCoupons(registryId).block()

        then:
        1 * registryRepository.getByRegistryId(registryId) >> Mono.just(registry)

        actual != null
        actual.couponCountDownDays == 0L
    }
}

