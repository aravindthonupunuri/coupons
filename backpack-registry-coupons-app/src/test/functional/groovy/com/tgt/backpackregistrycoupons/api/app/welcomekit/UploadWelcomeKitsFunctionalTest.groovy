package com.tgt.backpackregistrycoupons.api.app.welcomekit

import com.tgt.backpackregistrycoupons.test.BasePersistenceFunctionalTest
import com.tgt.backpackregistrycoupons.util.RegistryCouponsConstant
import com.tgt.backpackregistrycoupons.welcomekit.persistence.repository.welcomekit.WelcomeKitsRepository
import com.tgt.backpackregistrycoupons.welcomekit.transport.UploadWelcomeKitResponseTO
import groovy.json.JsonOutput
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.test.extensions.spock.annotation.MicronautTest

import javax.inject.Inject

import static com.tgt.backpackregistrycoupons.test.DataProvider.getHeaders

@MicronautTest
class UploadWelcomeKitsFunctionalTest extends BasePersistenceFunctionalTest {

    @Inject
    WelcomeKitsRepository welcomeKitsRepository

    String uri = RegistryCouponsConstant.BASEPATH + "/welcome_kits"

    def "test upload welcome kits integrity"() {
        when:
        HttpResponse<UploadWelcomeKitResponseTO> response = client.toBlocking().exchange(HttpRequest.POST(uri, JsonOutput.toJson(["tcin1", "tcin2", "tcin3"])).headers(getHeaders("1234", false)), UploadWelcomeKitResponseTO)
        def actualStatus = response.status()
        def responseBody = response.body()

        then:
        actualStatus == HttpStatus.CREATED
        responseBody.uploadedTcins.size() == 3
    }

    def "test valid existsByCouponCode"() {
        when:
        def result1 = welcomeKitsRepository.findByTcinInList(["tcin1", "tcin2", "tcin3"]).collectList().block()

        then:
        result1 != null
        result1.size() == 3
    }

    def "test upload welcome kits with pre existing tcins and new tcins"() {
        when:
        HttpResponse<UploadWelcomeKitResponseTO> response = client.toBlocking().exchange(HttpRequest.POST(uri, JsonOutput.toJson(["tcin3", "tcin4", "tcin5"])).headers(getHeaders("1234", false)), UploadWelcomeKitResponseTO)
        def actualStatus = response.status()
        def responseBody = response.body()

        then:
        actualStatus == HttpStatus.CREATED
        responseBody.uploadedTcins.size() == 2
        responseBody.existingTcins.size() == 1
    }

    def "test upload welcome kits with only pre existing tcins"() {
        when:
        HttpResponse<UploadWelcomeKitResponseTO> response = client.toBlocking().exchange(HttpRequest.POST(uri, JsonOutput.toJson(["tcin3"])).headers(getHeaders("1234", false)), UploadWelcomeKitResponseTO)
        def actualStatus = response.status()
        def responseBody = response.body()

        then:
        actualStatus == HttpStatus.CREATED
        responseBody.uploadedTcins.size() == 0
        responseBody.existingTcins.size() == 1
    }
}
