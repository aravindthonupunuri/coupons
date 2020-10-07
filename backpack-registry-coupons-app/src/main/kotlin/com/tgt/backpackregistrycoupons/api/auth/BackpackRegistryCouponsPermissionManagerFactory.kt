package com.tgt.backpackregistrycoupons.api.auth

import com.tgt.lists.cart.CartClient
import com.tgt.lists.common.components.filters.auth.permissions.CartPermissionManager
import com.tgt.lists.common.components.filters.auth.permissions.DefaultListPermissionManager
import com.tgt.lists.common.components.filters.auth.permissions.ListPermissionManager
import com.tgt.listspermissions.api.client.ListPermissionsClient
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory

@Factory
class BackpackRegistryCouponsPermissionManagerFactory(private val cartClient: CartClient, private val permissionsClient: ListPermissionsClient) {

    val listPermissionClientManager = ListPermissionClientManager(permissionsClient)
    val cartPermissionManager = CartPermissionManager(cartClient)

    @Bean
    fun newListPermissionManager(): ListPermissionManager {
        return DefaultListPermissionManager(listPermissionClientManager, cartPermissionManager)
    }
}
