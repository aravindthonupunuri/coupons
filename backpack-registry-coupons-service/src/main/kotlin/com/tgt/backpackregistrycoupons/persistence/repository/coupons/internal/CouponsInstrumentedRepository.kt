package com.tgt.backpackregistrycoupons.persistence.repository.coupons.internal

import com.tgt.backpackregistrycoupons.persistence.repository.coupons.CouponsRepository
import com.tgt.lists.micronaut.persistence.instrumentation.InstrumentedRepository
import io.micronaut.context.annotation.Primary

@Primary // make it primary to instrument CouponsCrudRepository
@InstrumentedRepository("CouponsCrudRepository")
interface CouponsInstrumentedRepository : CouponsRepository
