package com.manuelalejandro.delfos.mobile

import androidx.compose.ui.window.ComposeUIViewController
import com.manuelalejandro.delfos.mobile.session.KeychainTokenStore
import platform.UIKit.UIViewController

/** The root view controller the iOS application embeds. */
@Suppress("FunctionName", "unused")
public fun MainViewController(): UIViewController = ComposeUIViewController { App(tokenStore = KeychainTokenStore()) }
