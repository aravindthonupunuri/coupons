package com.tgt.backpackregistrycoupons.welcomekit.persistence.repository.welcomekit.internal

import com.tgt.backpackregistrycoupons.welcomekit.persistence.repository.welcomekit.WelcomeKitsRepository
import com.tgt.lists.micronaut.persistence.instrumentation.InstrumentedRepository
import io.micronaut.context.annotation.Primary

@Primary // make it primary to instrument WelcomeKitsCrudRepository
@InstrumentedRepository("WelcomeKitsCrudRepository")
interface WelcomeKitsInstrumentedRepository : WelcomeKitsRepository
