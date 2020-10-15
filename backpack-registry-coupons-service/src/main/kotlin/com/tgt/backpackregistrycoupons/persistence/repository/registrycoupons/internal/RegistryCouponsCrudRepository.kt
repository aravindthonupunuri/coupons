package com.tgt.backpackregistrycoupons.persistence.repository.registrycoupons.internal

import com.tgt.backpackregistrycoupons.domain.model.Registry
import com.tgt.backpackregistrycoupons.domain.model.RegistryCoupons
import com.tgt.backpackregistrycoupons.persistence.repository.registrycoupons.RegistryCouponsRepository
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.reactive.ReactiveStreamsCrudRepository

@JdbcRepository(dialect = Dialect.POSTGRES)
interface RegistryCouponsCrudRepository : ReactiveStreamsCrudRepository<RegistryCoupons, Registry>, RegistryCouponsRepository
