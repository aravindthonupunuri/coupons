package com.tgt.backpackregistrycoupons.api.app

import com.tgt.backpackregistrycoupons.domain.model.Registry
import com.tgt.backpackregistrycoupons.domain.model.RegistryCoupons
import com.tgt.backpackregistrycoupons.persistence.repository.registrycoupons.RegistryCouponsRepository
import com.tgt.backpackregistrycoupons.test.BasePersistenceFunctionalTest
import com.tgt.backpackregistrycoupons.util.CouponType
import com.tgt.backpackregistrycoupons.util.RegistryType
import io.micronaut.test.annotation.MicronautTest
import spock.lang.Shared
import spock.lang.Stepwise

import javax.inject.Inject
import java.time.LocalDateTime

@MicronautTest
@Stepwise
class RegistryCouponsRepositoryFunctionalTest extends BasePersistenceFunctionalTest  {

    @Inject
    RegistryCouponsRepository registryCouponsRepository

    @Shared
    UUID registryId1 = UUID.randomUUID()

    def "test saveAll RegistryCoupons"() {
        given:
        def registryCoupons1 = new RegistryCoupons(new Registry(registryId1, CouponType.ONLINE), RegistryType.BABY,
            LocalDateTime.now().minusDays(3), LocalDateTime.now(), null , false,
            null, null, null, "1234", "1234", LocalDateTime.now(), LocalDateTime.now()  )
        def registryCoupons2 = new RegistryCoupons(new Registry(registryId1, CouponType.STORE), RegistryType.BABY,
            LocalDateTime.now().minusDays(3), LocalDateTime.now(), null , false,
            null, null, null, "1234", "1234", LocalDateTime.now(), LocalDateTime.now()  )

        when:
        def result = registryCouponsRepository.saveAll([registryCoupons1, registryCoupons2]).collectList().block()

        then:
        result.size() == 2
    }

    def "test findByIdRegistryId"() {
        when:
        def result = registryCouponsRepository.findByIdRegistryId(registryId1).collectList().block()

        then:
        result.size() == 2
    }

    def "test updateByRegistryId"() {
        when:
        def result = registryCouponsRepository.updateByRegistryId(registryId1, LocalDateTime.now()).block()

        then:
        result == 2
    }

    def "test deleteByIdRegistryId"() {
        when:
        def result = registryCouponsRepository.deleteByIdRegistryId(registryId1).block()

        then:
        result == 2
    }
}


