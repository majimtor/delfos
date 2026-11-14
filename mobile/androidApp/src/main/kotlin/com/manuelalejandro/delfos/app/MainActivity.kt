package com.manuelalejandro.delfos.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.manuelalejandro.delfos.mobile.App
import com.manuelalejandro.delfos.mobile.parseSurveyLink
import com.manuelalejandro.delfos.mobile.session.KeystoreTokenStore

/** The only activity: the whole application lives in the shared Compose module. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val deepLink = intent?.dataString?.let(::parseSurveyLink)
        val tokenStore = KeystoreTokenStore(applicationContext)
        setContent { App(deepLink = deepLink, tokenStore = tokenStore) }
    }
}
