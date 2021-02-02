package com.tgt.backpackregistrycoupons.api.app

import com.tgt.backpackregistryclient.util.RegistryType
import com.tgt.backpackregistrycoupons.domain.model.Coupons
import com.tgt.backpackregistrycoupons.persistence.repository.coupons.CouponsRepository
import com.tgt.backpackregistrycoupons.test.BasePersistenceFunctionalTest
import com.tgt.backpackregistrycoupons.util.CouponType
import io.micronaut.test.annotation.MicronautTest
import spock.lang.Stepwise

import javax.inject.Inject
import java.time.LocalDate
import java.time.LocalDateTime

@MicronautTest
@Stepwise
class CouponsRepositoryFunctionalTest extends BasePersistenceFunctionalTest  {

    @Inject
    CouponsRepository couponsRepository

    def "test save RegistryCoupons"() {
        given:
        def coupons1 = new Coupons("1000000", CouponType.ONLINE, RegistryType.BABY, LocalDate.now(), "1", LocalDateTime.now(),  LocalDateTime.now())
        def coupons2 = new Coupons("2000000", CouponType.ONLINE, RegistryType.BABY, LocalDate.now().plusDays(1), "1", LocalDateTime.now(),  LocalDateTime.now())
        def coupons3 = new Coupons("3000000", CouponType.ONLINE, RegistryType.BABY, LocalDate.now().plusDays(2), "1", LocalDateTime.now(),  LocalDateTime.now())
        def coupons4 = new Coupons("4000000", CouponType.ONLINE, RegistryType.BABY, LocalDate.now().plusDays(3), "1", LocalDateTime.now(),  LocalDateTime.now())
        def coupons5 = new Coupons("5000000", CouponType.ONLINE, RegistryType.BABY, LocalDate.now().plusDays(4), "1", LocalDateTime.now(),  LocalDateTime.now())
        def coupons6 = new Coupons("6000000", CouponType.STORE, RegistryType.BABY, LocalDate.now().plusDays(5), "1", LocalDateTime.now(),  LocalDateTime.now())

        when:
        def result1 = couponsRepository.save(coupons1).block()
        def result2 = couponsRepository.save(coupons2).block()
        def result3 = couponsRepository.save(coupons3).block()
        def result4 = couponsRepository.save(coupons4).block()
        def result5 = couponsRepository.save(coupons5).block()
        def result6 = couponsRepository.save(coupons6).block()

        then:
        result1 != null
        result2 != null
        result3 != null
        result4 != null
        result5 != null
        result6 != null
    }

    def "test valid existsByCouponCode"() {
        when:
        def result = couponsRepository.existsByCouponCode("1000000").block()

        then:
        result
    }

    def "test inValid existsByCouponCode"() {
        when:
        def result = couponsRepository.existsByCouponCode("9000000").block()

        then:
        !result
    }

    def "test findTop1ByCouponTypeAndRegistryTypeAndCouponExpiryDateGreaterThanEquals"() {
        when:
        def result = couponsRepository.findTop1ByCouponTypeAndRegistryTypeAndCouponExpiryDateGreaterThanEquals(CouponType.ONLINE, RegistryType.BABY, LocalDate.now().plusDays(3)).block()

        then:
        result.couponCode == "4000000"
        result != null
    }

    def "test deleteByCouponCode"() {
        when:
        def result = couponsRepository.deleteByCouponCodeInList(["1000000", "2000000"]).block()

        then:
        result == 2
    }

    def "test deleteByCouponCode if successful"() {
        when:
        def result = couponsRepository.findByRegistryType(RegistryType.BABY).collectList().block()

        then:
        result.size() ==  4
    }
}


