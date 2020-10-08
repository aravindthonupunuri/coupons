package com.tgt.backpackregistrycoupons.api.app

import com.tgt.backpackregistrycoupons.domain.model.RegistryCoupons
import com.tgt.backpackregistrycoupons.persistence.repository.registrycoupons.RegistryCouponsRepository
import com.tgt.backpackregistrycoupons.test.BasePersistenceFunctionalTest
import com.tgt.backpackregistrycoupons.util.CouponType
import com.tgt.backpackregistrycoupons.util.RegistryType
import io.micronaut.test.annotation.MicronautTest
import spock.lang.Stepwise
import spock.lang.Unroll

import javax.inject.Inject
import java.time.LocalDate

@MicronautTest
@Stepwise
class RegistryCouponsRepositoryFunctionalTest extends BasePersistenceFunctionalTest  {

    @Inject
    RegistryCouponsRepository registryCouponsRepository

    def "test save RegistryCoupons"() {
        given:
        def registryCoupons1 = new RegistryCoupons("1000000", CouponType.ONLINE, RegistryType.BABY, LocalDate.now(), "1")
        def registryCoupons2 = new RegistryCoupons("2000000", CouponType.ONLINE, RegistryType.BABY, LocalDate.now(), "1")
        def registryCoupons3 = new RegistryCoupons("3000000", CouponType.ONLINE, RegistryType.BABY, LocalDate.now(), "1")
        def registryCoupons4 = new RegistryCoupons("4000000", CouponType.ONLINE, RegistryType.BABY, LocalDate.now(), "1")
        def registryCoupons5 = new RegistryCoupons("5000000", CouponType.ONLINE, RegistryType.BABY, LocalDate.now(), "1")

        when:
        def result1 = registryCouponsRepository.save(registryCoupons1).block()
        def result2 = registryCouponsRepository.save(registryCoupons2).block()
        def result3 = registryCouponsRepository.save(registryCoupons3).block()
        def result4 = registryCouponsRepository.save(registryCoupons4).block()
        def result5 = registryCouponsRepository.save(registryCoupons5).block()

        then:
        result1 != null
        result2 != null
        result3 != null
        result4 != null
        result5 != null
    }

    def "test valid existsByCouponId"() {
        when:
        def result = registryCouponsRepository.existsByCouponId("1000000").block()

        then:
        result
    }

    def "test inValid existsByCouponId"() {
        when:
        def result = registryCouponsRepository.existsByCouponId("9000000").block()

        then:
        !result
    }
}
