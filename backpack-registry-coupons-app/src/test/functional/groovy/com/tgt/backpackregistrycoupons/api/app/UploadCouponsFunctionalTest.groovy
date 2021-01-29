package com.tgt.backpackregistrycoupons.api.app

import com.tgt.backpackregistrycoupons.domain.model.Coupons
import com.tgt.backpackregistrycoupons.persistence.repository.coupons.CouponsRepository
import com.tgt.backpackregistrycoupons.test.BasePersistenceFunctionalTest
import com.tgt.backpackregistrycoupons.util.RegistryCouponsConstant
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.client.multipart.MultipartBody
import io.micronaut.test.annotation.MicronautTest

import javax.inject.Inject

@MicronautTest
class UploadCouponsFunctionalTest extends BasePersistenceFunctionalTest {

    @Inject
    CouponsRepository couponsRepository

    String uri = RegistryCouponsConstant.BASEPATH + "/uploads?registry_type=BABY&coupon_type=ONLINE&offer_id=1&coupon_expiry_date=2991-12-03"

    def "test upload registry coupons integrity"() {
        given:
        def file = new File("file.txt")
        file.write("1000000\n")
        file.append("2000000\n")
        file.append("3000000\n")
        file.append("4000000\n")

        MultipartBody multipartBody = MultipartBody
            .builder()
            .addPart("coupons", "file.txt", MediaType.TEXT_PLAIN_TYPE, file)
            .build()

        when:
        HttpResponse<Coupons> response = client.toBlocking()
            .exchange(HttpRequest.POST(uri, multipartBody).contentType(MediaType.MULTIPART_FORM_DATA_TYPE), Coupons)

        def actualStatus = response.status()

        then:
        actualStatus == HttpStatus.NO_CONTENT
    }

    def "test valid existsByCouponCode"() {
        when:
        def result1 = couponsRepository.existsByCouponCode("1000000").block()

        then:
        result1
    }
}
