package com.tgt.backpackregistrycoupons.api.app

import com.tgt.backpackregistryclient.util.RegistryType
import com.tgt.backpackregistrycoupons.domain.model.Registry
import com.tgt.backpackregistrycoupons.domain.model.RegistryCoupons
import com.tgt.backpackregistrycoupons.persistence.repository.registry.RegistryRepository
import com.tgt.backpackregistrycoupons.persistence.repository.registrycoupons.RegistryCouponsRepository
import com.tgt.backpackregistrycoupons.service.RegistryCouponService
import com.tgt.backpackregistrycoupons.test.BasePersistenceFunctionalTest
import com.tgt.backpackregistrycoupons.transport.RegistryCouponsTO
import com.tgt.backpackregistrycoupons.util.CouponRedemptionStatus
import com.tgt.backpackregistrycoupons.util.CouponType
import com.tgt.backpackregistrycoupons.util.RegistryCouponsConstant
import com.tgt.lists.atlas.api.type.LIST_STATE
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Shared
import static com.tgt.backpackregistrycoupons.test.DataProvider.*

import javax.inject.Inject
import java.time.LocalDate

@MicronautTest
class GetRegistryCouponsFunctionalTest extends BasePersistenceFunctionalTest {

    @Inject
    RegistryRepository registryRepository

    @Inject
    RegistryCouponsRepository registryCouponsRepository

    @Inject
    RegistryCouponService couponService;

    @Shared
    def registryId = UUID.randomUUID()

    @Shared
    def alternateRegistryId = "12345"

    def "test save Registry and RegistryCoupons"() {
        given:
        def registry = new Registry(registryId, alternateRegistryId, RegistryType.BABY,  LIST_STATE.ACTIVE.value, LocalDate.now().minusDays(3), LocalDate.now(), true, null, null)

        def registryCoupons11 = new RegistryCoupons("1234", registry, CouponType.STORE, CouponRedemptionStatus.AVAILABLE, LocalDate.now(), LocalDate.now().plusDays(2), null , null)
        def registryCoupons12 = new RegistryCoupons("3456", registry, CouponType.ONLINE, CouponRedemptionStatus.AVAILABLE, LocalDate.now(), LocalDate.now().plusDays(2), null , null)


        when:
        def result1 = registryRepository.saveAll([registry] as Set).collectList().block()

        then:
        result1.size() == 1

        and:

        when:
        def result3 = registryCouponsRepository.saveAll([registryCoupons11, registryCoupons12]).collectList().block()

        then:
        result3.size() == 2
    }

    def "test save Registry and multiple RegistryCoupons"() {
        given:
        def registry = new Registry(registryId, alternateRegistryId, RegistryType.BABY,  LIST_STATE.ACTIVE.value, LocalDate.now().minusDays(3), LocalDate.now(), true, null, null)

        def registryCoupons11 = new RegistryCoupons("1234", registry, CouponType.STORE, CouponRedemptionStatus.AVAILABLE, LocalDate.parse("2018-11-10"), LocalDate.now().plusDays(2), null , null)
        def registryCoupons12 = new RegistryCoupons("3456", registry, CouponType.ONLINE, CouponRedemptionStatus.AVAILABLE, LocalDate.parse("2018-11-10"), LocalDate.now().plusDays(2), null , null)
        def registryCoupons13 = new RegistryCoupons("2345", registry, CouponType.STORE, CouponRedemptionStatus.AVAILABLE, LocalDate.now(), LocalDate.now().plusDays(2), null , null)
        def registryCoupons14 = new RegistryCoupons("4321", registry, CouponType.ONLINE, CouponRedemptionStatus.AVAILABLE, LocalDate.now(), LocalDate.now().plusDays(2), null , null)


        when:
        def result1 = registryRepository.saveAll([registry] as Set).collectList().block()

        then:
        result1.size() == 1

        and:

        when:
        registryCouponsRepository.saveAll([registryCoupons11, registryCoupons12, registryCoupons13, registryCoupons14]).collectList().block()
        def result3 = couponService.getRegistryCoupons(registryId).block()

        then:
        result3.getCoupons().size() == 2
    }

    def "test get registry coupons integrity"() {
        given:
        def guestId = "1111111111111"
        String uri = RegistryCouponsConstant.BASEPATH + "/" + registryId

        when:
        HttpResponse<RegistryCouponsTO> response = client.toBlocking().exchange(HttpRequest.GET(uri).headers (getHeaders(guestId)), RegistryCouponsTO)

        def actualStatus = response.status()
        def actual = response.body()

        then:
        actualStatus == HttpStatus.OK
        actual.coupons.size() == 2
    }
}
