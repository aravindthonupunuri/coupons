 package com.tgt.backpackregistrycoupons.api.app

import com.tgt.backpackregistryclient.util.RegistryType
import com.tgt.backpackregistrycoupons.domain.model.Registry
import com.tgt.backpackregistrycoupons.domain.model.RegistryCoupons
 import com.tgt.backpackregistrycoupons.persistence.repository.compositetransaction.CompositeTransactionalRepository
 import com.tgt.backpackregistrycoupons.persistence.repository.registry.RegistryRepository
import com.tgt.backpackregistrycoupons.persistence.repository.registrycoupons.RegistryCouponsRepository
import com.tgt.backpackregistrycoupons.test.BasePersistenceFunctionalTest
import com.tgt.backpackregistrycoupons.util.CouponRedemptionStatus
import com.tgt.backpackregistrycoupons.util.CouponType
 import com.tgt.lists.atlas.api.type.LIST_STATE
 import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Shared
import spock.lang.Stepwise

import javax.inject.Inject
 import java.time.LocalDate
 import java.time.LocalDate

@MicronautTest
@Stepwise
class RegistryRepositoryFunctionalTest extends BasePersistenceFunctionalTest  {

    @Inject
    RegistryRepository registryRepository

    @Inject
    RegistryCouponsRepository registryCouponsRepository

    @Inject
    CompositeTransactionalRepository compositeTransactionalRepository

    @Shared
    UUID registryId1 = UUID.randomUUID()
    @Shared
    UUID registryId2 = UUID.randomUUID()
    @Shared
    UUID registryId3 = UUID.randomUUID()
    @Shared
    UUID registryId4 = UUID.randomUUID()
    @Shared
    String alternateRegistryId = "12345"

    @Shared
    List<String> couponCodes = ["1234", "4567", "7890", "0123"]

    def "test save Registry and RegistryCoupons"() {
        given:
        def registry1 = new Registry(registryId1, alternateRegistryId, RegistryType.BABY,  LIST_STATE.ACTIVE.value, LocalDate.now().minusDays(3), LocalDate.now(), true, null, null)
        def registry2 = new Registry(registryId2, alternateRegistryId, RegistryType.WEDDING,  LIST_STATE.ACTIVE.value, LocalDate.now().minusDays(4), LocalDate.now(), true, null, null)
        def registry3 = new Registry(registryId3, alternateRegistryId, RegistryType.WEDDING,  LIST_STATE.ACTIVE.value, LocalDate.now().minusDays(4), LocalDate.now().plusDays(20), false, null, null)
        def registry4 = new Registry(registryId4, alternateRegistryId, RegistryType.WEDDING,  LIST_STATE.INACTIVE.value, LocalDate.now().minusDays(4), LocalDate.now().plusDays(20), false, null, null)


        def registryCoupons11 = new RegistryCoupons(couponCodes[0], registry1, CouponType.STORE, CouponRedemptionStatus.AVAILABLE, LocalDate.now(), LocalDate.now().plusDays(2), null , null)
        def registryCoupons12 = new RegistryCoupons(couponCodes[1], registry1, CouponType.ONLINE, CouponRedemptionStatus.AVAILABLE, LocalDate.now(), LocalDate.now().plusDays(2), null , null)

        def registryCoupons21 = new RegistryCoupons(couponCodes[2], registry2, CouponType.STORE, CouponRedemptionStatus.AVAILABLE, LocalDate.now(), LocalDate.now().plusDays(2), null , null)
        def registryCoupons22 = new RegistryCoupons(couponCodes[3], registry2, CouponType.ONLINE, CouponRedemptionStatus.AVAILABLE, LocalDate.now(), LocalDate.now().plusDays(2), null , null)

        when:
        def result1 = registryRepository.saveAll([registry1, registry2, registry3] as Set).collectList().block()

        then:
        result1.size() == 3

        and:

        when:
        def result2 = registryRepository.save(registry4).block()

        then:
        result2 != null

        and:

        when:
        def result3 = registryCouponsRepository.saveAll([registryCoupons11, registryCoupons12, registryCoupons21]).collectList().block()

        then:
        result3.size() == 3

        and:

        when:
        def result4 = registryCouponsRepository.save(registryCoupons22).block()

        then:
        result4 != null
    }

    def "test getByRegistryId"() {
        when:
        def result = registryRepository.getByRegistryId(registryId1).block()

        then:
        result != null
        result.registryCoupons.size() == 2
    }

    def "test findByRegistryId"() {
        when:
        def result = registryRepository.findByRegistryId(registryId1).block()

        then:
        result != null
        result.registryCoupons == null
    }

    def "test findByCouponAssignmentComplete"() {
        when:
        def result = registryRepository.findByRegistryStatusAndCouponAssignmentComplete(LIST_STATE.ACTIVE.value, false, 2).collectList().block()

        then:
        result != null
        result.size() == 1
    }

    def "test updateRegistryEventDate"() {
        when:
        def result = registryRepository.updateRegistryEventDate(registryId1, LocalDate.now().plusDays(3)).block()

        then:
        result != null
    }

    def "test updateRegistryStatus"() {
        when:
        def result1 = registryRepository.updateRegistryStatus(registryId1, LIST_STATE.ACTIVE.value).block()

        then:
        result1 != null

        and:

        when:
        def result2 = registryRepository.getByRegistryId(registryId1).block()

        then:
        result2 != null
        result2.registryStatus == LIST_STATE.ACTIVE.value
    }

    def "test updateCouponAssignmentComplete"() {
        when:
        def result1 = registryRepository.updateCouponAssignmentComplete(registryId3, true).block()

        then:
        result1 != null

        and:

        when:
        def result2 = registryRepository.getByRegistryId(registryId3).block()

        then:
        result2 != null
        result2.couponAssignmentComplete
    }

    def "test deleteByRegistryId"() {
        when:
        def result1 = compositeTransactionalRepository.deleteRegistryCascaded(registryId1).block()

        then:
        result1 != null

        and:

        when:
        def result2 = registryRepository.getByRegistryId(registryId1).block()

        then:
        result2 == null

        and:

        when:
        def result3 = registryCouponsRepository.findByCouponCode(couponCodes[0]).block()

        then:
        result3 == null
    }
}


