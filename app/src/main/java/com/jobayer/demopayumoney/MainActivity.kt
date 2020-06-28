package com.jobayer.demopayumoney

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.jobayer.demopayumoney.Const.FAILURE_URL
import com.jobayer.demopayumoney.Const.MERCHANT_ID
import com.jobayer.demopayumoney.Const.MERCHANT_KEY
import com.jobayer.demopayumoney.Const.SUCCESS_URL
import com.payumoney.core.PayUmoneyConfig
import com.payumoney.core.PayUmoneySdkInitializer
import com.payumoney.core.entity.TransactionResponse
import com.payumoney.sdkui.ui.utils.PayUmoneyFlowManager
import com.payumoney.sdkui.ui.utils.ResultModel
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        pay.setOnClickListener {
            if (isNetworkAvailable()) {
                if (isInputOk()) {
                    payNow()
                } else showToast("Input Details Correctly")
            } else showToast("Network Unavailable")
        }
    }

    private fun Activity.payNow() {

        val name = this.name.text.toString()
        val phone = this.phone.text.toString()
        val email = this.email.text.toString()
        val amount = this.amount.text.toString()
        val txnID = System.currentTimeMillis().toString()

        val payUMoneyConfig = PayUmoneyConfig.getInstance()
        payUMoneyConfig.doneButtonText = "Finish"
        payUMoneyConfig.payUmoneyActivityTitle = "Pay Now Demo"

        val payUPaymentBuilder = PayUmoneySdkInitializer.PaymentParam.Builder()

        payUPaymentBuilder
            .setAmount(amount)
            .setTxnId(txnID)
            .setProductName("June 2020 Fees")
            .setFirstName(name)
            .setPhone(phone)
            .setEmail(email)
            .setsUrl(SUCCESS_URL)
            .setfUrl(FAILURE_URL)
            .setIsDebug(false)
            .setKey(MERCHANT_KEY)
            .setMerchantId(MERCHANT_ID)

        try {
            val payUMoneyPaymentParam = payUPaymentBuilder.build()
            val payUMoneyPPWithHash = Util.calcHash(payUMoneyPaymentParam)
            PayUmoneyFlowManager.startPayUMoneyFlow(payUMoneyPPWithHash, this, R.style.AppTheme_default, true)
        } catch (e: Exception) {
            showDialog("Builder Error", e.message.toString())
        }
    }

//private fun Activity.generateHash(text: String): String {
//    val textBytes = text.toByteArray()
//    val hexString: StringBuilder = StringBuilder()
//    try {
//        val algorithm = MessageDigest.getInstance("SHA-512")
//        algorithm.reset()
//        algorithm.update(textBytes)
//        val messageDigest = algorithm.digest()
//        for(md in messageDigest) {
//            val haxVal = java.lang.Inte
//        }
//    } catch (e: NoSuchAlgorithmException) {}
//}

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun Activity.isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nw = connectivityManager.activeNetwork ?: return false
            val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
            return when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
                else -> false
            }
        } else {
            val nwInfo = connectivityManager.activeNetworkInfo ?: return false
            return nwInfo.isConnected
        }
    }

    private fun Activity.showDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun Activity.showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun Activity.isInputOk(): Boolean {
        return (!name.text.isNullOrBlank()&&!phone.text.isNullOrBlank()&&!email.text.isNullOrBlank()&&!amount.text.isNullOrBlank())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PayUmoneyFlowManager.REQUEST_CODE_PAYMENT && resultCode == Activity.RESULT_OK && data != null) {
            val transactionResponse = data.getParcelableExtra<TransactionResponse>(PayUmoneyFlowManager.INTENT_EXTRA_TRANSACTION_RESPONSE)
            val resModel = data.getParcelableExtra<ResultModel>(PayUmoneyFlowManager.ARG_RESULT)
            if (transactionResponse?.getPayuResponse() != null) {
                if (transactionResponse.transactionStatus.equals(TransactionResponse.TransactionStatus.SUCCESSFUL)) {
                    showToast("Payment has been done successfully")

                    showDialog("PayU Response", transactionResponse.getPayuResponse().toString())
                    showDialog("Merchant Response", transactionResponse.transactionDetails.toString())

                } else showDialog("Error", "Payment could not be successful")
            } else if (resModel != null && resModel.error != null) {
                showDialog("Result Error", resModel.error.transactionResponse.toString())
            } else showDialog("Unknown Error", "Something unknown happened")
        }
    }


}