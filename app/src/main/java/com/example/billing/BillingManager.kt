package com.example.billing

import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class BillingManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val onPurchaseSuccess: (String, String) -> Unit, // purchaseToken, planType ("DAILY", "WEEKLY", "YEARLY")
    private val onPurchaseFailed: (String) -> Unit // error message
) : PurchasesUpdatedListener, BillingClientStateListener {

    private var billingClient: BillingClient? = null
    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    init {
        try {
            billingClient = BillingClient.newBuilder(context)
                .setListener(this)
                .enablePendingPurchases()
                .build()
            startConnection()
        } catch (e: Exception) {
            Log.e("BillingManager", "Failed to initialize BillingClient (likely in testing/robolectric environment): ${e.message}")
        }
    }

    fun startConnection() {
        billingClient?.startConnection(this)
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            _isConnected.value = true
            Log.d("BillingManager", "Billing setup successful.")
        } else {
            Log.e("BillingManager", "Billing setup failed: ${billingResult.debugMessage}")
        }
    }

    override fun onBillingServiceDisconnected() {
        _isConnected.value = false
        Log.w("BillingManager", "Billing service disconnected.")
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            onPurchaseFailed("Payment cancelled by user.")
        } else {
            onPurchaseFailed("Payment unsuccessful. Please retry.")
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            val planType = when {
                purchase.products.contains("sub_daily_5") -> "DAILY"
                purchase.products.contains("sub_weekly_50") -> "WEEKLY"
                purchase.products.contains("sub_yearly_365") -> "YEARLY"
                else -> "DAILY"
            }
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        onPurchaseSuccess(purchase.purchaseToken, planType)
                    } else {
                        onPurchaseFailed("Failed to acknowledge purchase: ${billingResult.debugMessage}")
                    }
                }
            } else {
                onPurchaseSuccess(purchase.purchaseToken, planType)
            }
        }
    }

    /**
     * Simulate a Billing Purchase Flow for testing and fallback environments (e.g., when Play Services are not available)
     */
    fun simulatePurchase(planType: String, shouldSucceed: Boolean = true) {
        if (shouldSucceed) {
            val mockToken = "mock_token_gp_${System.currentTimeMillis()}"
            onPurchaseSuccess(mockToken, planType)
        } else {
            onPurchaseFailed("Payment unsuccessful. Please retry.")
        }
    }
}
