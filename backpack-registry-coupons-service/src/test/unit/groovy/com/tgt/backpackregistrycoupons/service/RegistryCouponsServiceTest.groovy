package com.tgt.backpackregistrycoupons.service

import com.tgt.backpackregistrycoupons.domain.CouponAssignmentCalculationManager
import com.tgt.backpackregistrycoupons.persistence.repository.registry.RegistryRepository
import spock.lang.Specification

class RegistryCouponsServiceTest extends Specification {

    RegistryCouponService registryCouponService
    RegistryRepository registryRepository
    CouponAssignmentCalculationManager couponAssignmentCalculationManager


    def setup() {
        registryRepository = Mock(RegistryRepository)
        couponAssignmentCalculationManager = new CouponAssignmentCalculationManager(7L , 56L, 14L)
        registryCouponService = new RegistryCouponService(registryRepository, couponAssignmentCalculationManager, 7L , 56L)
        }



}

