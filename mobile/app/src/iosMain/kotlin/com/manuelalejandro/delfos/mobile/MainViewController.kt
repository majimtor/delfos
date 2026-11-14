package com.manuelalejandro.delfos.mobile

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/** The root view controller the iOS application embeds. */
@Suppress("FunctionName", "unused")
public fun MainViewController(): UIViewController = ComposeUIViewController { App() }
