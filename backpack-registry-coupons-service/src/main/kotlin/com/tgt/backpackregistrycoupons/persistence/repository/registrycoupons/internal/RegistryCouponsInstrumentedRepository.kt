package com.tgt.backpackregistrycoupons.persistence.repository.registrycoupons.internal

import com.tgt.backpackregistrycoupons.persistence.repository.registrycoupons.RegistryCouponsRepository
import com.tgt.lists.micronaut.persistence.instrumentation.InstrumentedRepository
import io.micronaut.context.annotation.Primary

@Primary // make it primary to instrument RegistryCouponsCrudRepository
@InstrumentedRepository("RegistryCouponsCrudRepository")
interface RegistryCouponsInstrumentedRepository : RegistryCouponsRepository
