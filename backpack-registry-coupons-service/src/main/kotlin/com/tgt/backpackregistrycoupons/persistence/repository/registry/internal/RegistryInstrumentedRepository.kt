package com.tgt.backpackregistrycoupons.persistence.repository.registry.internal

import com.tgt.backpackregistrycoupons.persistence.repository.registry.RegistryRepository
import com.tgt.lists.micronaut.persistence.instrumentation.InstrumentedRepository
import io.micronaut.context.annotation.Primary

@Primary // make it primary to instrument RegistryCrudRepository
@InstrumentedRepository("RegistryCrudRepository")
interface RegistryInstrumentedRepository : RegistryRepository
