package com.tgt.backpackregistrycoupons.domain

import com.tgt.backpackregistryclient.util.RegistryType
import com.tgt.backpackregistrycoupons.domain.model.Registry
import com.tgt.backpackregistrycoupons.persistence.repository.registry.RegistryRepository
import com.tgt.lists.atlas.api.type.LIST_STATE
import spock.lang.Specification

import java.time.LocalDate

class CouponAssignmentCalculationManagerTest extends Specification {

    RegistryRepository registryRepository
    CouponAssignmentCalculationManager CouponAssignmentCalculationManager

    def setup() {
        registryRepository = Mock(RegistryRepository)
        couponAssignmentCalculationManager = new CouponAssignmentCalculationManager(7L , 56L, 14L)
    }

    def "Test registry where completion coupon sla date is after event date"() {
        given:
        def registryId = UUID.randomUUID()
        def registryCreatedDate = LocalDate.now().minusDays(3)
        def eventDate = LocalDate.now()
        def alternateRegistryId = "12345"

        def registry = new Registry(registryId, alternateRegistryId, RegistryType.BABY,  LIST_STATE.ACTIVE.value, registryCreatedDate,  eventDate, false, null, null)

        when:
        def actual = couponAssignmentCalculationManager.calculateCouponAssignmentDate(registry)

        then:
        actual.toLocalDate() == registryCreatedDate.plusDays(couponAssignmentCalculationManager.completionCouponSLA)
    }

    def "Test registry where completion coupon sla date is before event date"() {
        given:
        def registryId = UUID.randomUUID()
        def registryCreatedDate = LocalDate.now()
        def eventDate = LocalDate.now().plusDays(60)
        def alternateRegistryId = "12345"

        def registry = new Registry(registryId, alternateRegistryId, RegistryType.BABY,  LIST_STATE.ACTIVE.value, registryCreatedDate,  eventDate, false, null, null)

        when:
        def actual = couponAssignmentCalculationManager.calculateCouponAssignmentDate(registry)

        then:
        actual.toLocalDate() == eventDate.minusDays(couponAssignmentCalculationManager.babyRegistrySLA)
    }

    def "Test registry where completion coupon sla date is before event date but after the registry SLA date"() {
        given:
        def registryId = UUID.randomUUID()
        def registryCreatedDate = LocalDate.now()
        def eventDate = LocalDate.now().plusDays(60) // wedding registry
        def alternateRegistryId = "12345"

        def registry = new Registry(registryId, alternateRegistryId, RegistryType.WEDDING,  LIST_STATE.ACTIVE.value, registryCreatedDate,  eventDate, false, null, null)

        when:
        def actual = couponAssignmentCalculationManager.calculateCouponAssignmentDate(registry)

        then:
        actual.toLocalDate() == registryCreatedDate.plusDays(couponAssignmentCalculationManager.completionCouponSLA)
    }
}

