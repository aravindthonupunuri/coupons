package com.tgt.backpackregistrycoupons.api.auth

import com.tgt.lists.common.components.filters.auth.permissions.DefaultListPermissionManager
import com.tgt.lists.common.components.filters.auth.permissions.ListPermissionManager
import com.tgt.listspermissions.api.client.ListPermissionsClient
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory

@Factory
class BackpackRegistryCouponsPermissionManagerFactory(private val permissionsClient: ListPermissionsClient) {
    val listFallbackPermissionManager = com.tgt.listspermissions.api.client.ListPermissionClientManager(permissionsClient)

    // TODO Also validate using Atlas repo?
    @Bean
    fun newListPermissionManager(): ListPermissionManager {
        // There is no fall back for permissions in coupons MS
        return DefaultListPermissionManager(listFallbackPermissionManager, null, false)
    }
}
