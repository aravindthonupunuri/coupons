package com.tgt.backpackregistrycoupons.persistence.repository.registry.internal

import com.tgt.backpackregistrycoupons.domain.model.Registry
import com.tgt.backpackregistrycoupons.persistence.repository.registry.RegistryRepository
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.reactive.ReactiveStreamsCrudRepository
import java.util.*

@JdbcRepository(dialect = Dialect.POSTGRES)
interface RegistryCrudRepository : ReactiveStreamsCrudRepository<Registry, UUID>, RegistryRepository
