package com.tgt.backpackregistrycoupons.util

enum class RegistryType {
    BABY, WEDDING, COLLEGE
}

enum class RegistryStatus(val value: String) {
    ACTIVE("A"), // active
    INACTIVE("I") // inactive
}

enum class CouponType {
    ONLINE, STORE
}

enum class CouponRedemptionStatus {
    AVAILABLE, REDEEMED, EXPIRED
}
