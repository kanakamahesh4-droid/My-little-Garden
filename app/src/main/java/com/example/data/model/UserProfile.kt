package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val subscriptionStatus: String, // "NONE", "ACTIVE", "EXPIRED"
    val expiryDate: Long, // timestamp
    val email: String = "guest@example.com",
    val billingProvider: String = "NONE", // "PLAY" or "UPI"
    val isLoggedIn: Boolean = false,
    val displayName: String = "",
    val photoUrl: String = ""
)
