package com.tgt.backpackregistrycoupons.persistence.repository.coupons.internal

import com.tgt.backpackregistrycoupons.domain.model.Coupons
import com.tgt.backpackregistrycoupons.persistence.repository.coupons.CouponsRepository
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.reactive.ReactiveStreamsCrudRepository

@JdbcRepository(dialect = Dialect.POSTGRES)
interface CouponsCrudRepository : ReactiveStreamsCrudRepository<Coupons, String>, CouponsRepository
