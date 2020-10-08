package com.tgt.backpackregistrycoupons.api.app

import com.tgt.backpackregistrycoupons.domain.model.RegistryCoupons
import com.tgt.backpackregistrycoupons.persistence.repository.registrycoupons.RegistryCouponsRepository
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
class UploadRegistryCouponsFunctionalTest extends BasePersistenceFunctionalTest {

    @Inject
    RegistryCouponsRepository registryCouponsRepository

    String uri = RegistryCouponsConstant.BASEPATH + "/upload?registry_type=BABY&coupon_type=ONLINE&offer_id=1&coupon_expiry_date=2991-12-03"

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
        HttpResponse<RegistryCoupons> response = client.toBlocking()
            .exchange(HttpRequest.POST(uri, multipartBody).contentType(MediaType.MULTIPART_FORM_DATA_TYPE), RegistryCoupons)

        def actualStatus = response.status()

        then:
        actualStatus == HttpStatus.CREATED
    }

    def "test valid existsByCouponId"() {
        when:
        def result1 = registryCouponsRepository.existsByCouponId("1000000").block()

        then:
        result1
    }
}
