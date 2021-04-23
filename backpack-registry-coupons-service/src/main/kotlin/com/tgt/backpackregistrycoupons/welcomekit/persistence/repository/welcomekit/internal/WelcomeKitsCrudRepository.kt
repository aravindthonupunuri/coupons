package com.tgt.backpackregistrycoupons.welcomekit.persistence.repository.welcomekit.internal

import com.tgt.backpackregistrycoupons.welcomekit.domain.model.WelcomeKits
import com.tgt.backpackregistrycoupons.welcomekit.persistence.repository.welcomekit.WelcomeKitsRepository
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.reactive.ReactiveStreamsCrudRepository

@JdbcRepository(dialect = Dialect.POSTGRES)
interface WelcomeKitsCrudRepository : ReactiveStreamsCrudRepository<WelcomeKits, String>, WelcomeKitsRepository
