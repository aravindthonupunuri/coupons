package com.tgt.backpackregistrycoupons.api.app

import com.tgt.backpackregistryclient.util.RegistryType
import com.tgt.backpackregistrycoupons.domain.model.Coupons
import com.tgt.backpackregistrycoupons.domain.model.Registry
import com.tgt.backpackregistrycoupons.domain.model.RegistryCoupons
import com.tgt.backpackregistrycoupons.persistence.repository.compositetransaction.CompositeTransactionalRepository
import com.tgt.backpackregistrycoupons.persistence.repository.coupons.CouponsRepository
import com.tgt.backpackregistrycoupons.persistence.repository.registry.RegistryRepository
import com.tgt.backpackregistrycoupons.test.BasePersistenceFunctionalTest
import com.tgt.backpackregistrycoupons.util.CouponRedemptionStatus
import com.tgt.backpackregistrycoupons.util.CouponType
import com.tgt.lists.atlas.api.type.LIST_STATE
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Shared
import spock.lang.Stepwise

import javax.inject.Inject
import java.time.LocalDate
import java.time.LocalDateTime

@MicronautTest
@Stepwise
class CompositeTransactionalRepositoryFunctionalTest extends BasePersistenceFunctionalTest  {

    @Inject
    CouponsRepository couponsRepository

    @Inject
    RegistryRepository registryRepository

    @Inject
    CompositeTransactionalRepository compositeTransactionalRepository

    @Shared
    UUID registryId1 = UUID.randomUUID()
    @Shared
    UUID registryId2 = UUID.randomUUID()
    @Shared
    String couponCode1 =  "1000000"
    @Shared
    String couponCode2 =  "2000000"
    @Shared
    String couponCode3 =  "3000000"
    @Shared
    String couponCode4 =  "4000000"

    def "test save Registry and RegistryCoupons"() {
        given:
        def registry1 = new Registry(registryId1, RegistryType.BABY, LIST_STATE.ACTIVE.value, LocalDate.now().minusDays(3), LocalDate.now(), true, null, null)
        def registry2 = new Registry(registryId2, RegistryType.WEDDING, LIST_STATE.ACTIVE.value, LocalDate.now().minusDays(4), LocalDate.now(), true, null, null)

        def coupons1 = new Coupons(couponCode1, CouponType.STORE, RegistryType.BABY, LocalDate.now(), "1", LocalDateTime.now(), LocalDateTime.now())
        def coupons2 = new Coupons(couponCode2, CouponType.ONLINE, RegistryType.BABY, LocalDate.now().plusDays(1), "1", LocalDateTime.now(), LocalDateTime.now())
        def coupons3 = new Coupons(couponCode3, CouponType.STORE, RegistryType.WEDDING, LocalDate.now().plusDays(2), "1", LocalDateTime.now(), LocalDateTime.now())
        def coupons4 = new Coupons(couponCode4, CouponType.ONLINE, RegistryType.WEDDING, LocalDate.now().plusDays(3), "1", LocalDateTime.now(), LocalDateTime.now())

        def registryCoupons11 = new RegistryCoupons(coupons1.couponCode, registry1, CouponType.STORE, CouponRedemptionStatus.AVAILABLE, LocalDate.now(), LocalDate.now().plusDays(2), null, null)
        def registryCoupons12 = new RegistryCoupons(coupons2.couponCode, registry1, CouponType.ONLINE, CouponRedemptionStatus.AVAILABLE, LocalDate.now(), LocalDate.now().plusDays(2), null, null)

        def registryCoupons21 = new RegistryCoupons(coupons3.couponCode, registry2, CouponType.STORE, CouponRedemptionStatus.AVAILABLE, LocalDate.now(), LocalDate.now().plusDays(2), null, null)
        def registryCoupons22 = new RegistryCoupons(coupons4.couponCode, registry2, CouponType.ONLINE, CouponRedemptionStatus.AVAILABLE, LocalDate.now(), LocalDate.now().plusDays(2), null, null)

        when:
        def result = registryRepository.saveAll([registry1, registry2] as Set).collectList().block()

        then:
        result.size() == 2

        and:

        when:
        def result1 = couponsRepository.save(coupons1).block()
        def result2 = couponsRepository.save(coupons2).block()
        def result3 = couponsRepository.save(coupons3).block()
        def result4 = couponsRepository.save(coupons4).block()

        then:
        result1 != null
        result2 != null
        result3 != null
        result4 != null

        and:

        when:
        def finalResult = compositeTransactionalRepository.assignCoupons([registryCoupons11, registryCoupons12, registryCoupons21, registryCoupons22]).block()

        then:
        finalResult
    }

    def "test if coupons got assigned to registry and coupons got deleted from coupons table"() {
        when:
        def result1 = registryRepository.getByRegistryId(registryId1).block()

        then:
        result1.registryCoupons.size() == 2

        and:

        when:
        def result2 = registryRepository.getByRegistryId(registryId2).block()

        then:
        result2.registryCoupons.size() == 2

        and:

        when:
        def result3 = couponsRepository.existsByCouponCode(couponCode1).block()
        def result4 = couponsRepository.existsByCouponCode(couponCode2).block()
        def result5 = couponsRepository.existsByCouponCode(couponCode3).block()
        def result6 = couponsRepository.existsByCouponCode(couponCode4).block()

        then:
        !result3
        !result4
        !result5
        !result6
    }
}


