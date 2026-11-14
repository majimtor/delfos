package com.manuelalejandro.delfos.mobile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.manuelalejandro.delfos.mobile.answer.AnswerScreen
import com.manuelalejandro.delfos.mobile.api.DelfosApi
import com.manuelalejandro.delfos.mobile.home.HomeScreen
import com.manuelalejandro.delfos.mobile.platform.httpClientEngine
import com.manuelalejandro.delfos.mobile.ui.DelfosTheme

/**
 * The whole application. [deepLink] is the survey a link opened the application with, if any;
 * the home screen stays underneath so that the respondent can go on to another survey.
 */
@Composable
public fun App(deepLink: AnswerRoute? = null) {
    val api = remember { DelfosApi(httpClientEngine()) }
    DelfosTheme {
        val navController = rememberNavController()
        LaunchedEffect(deepLink) {
            if (deepLink != null) navController.navigate(deepLink)
        }
        NavHost(navController = navController, startDestination = HomeRoute) {
            composable<HomeRoute> {
                HomeScreen(onOpen = { navController.navigate(it) })
            }
            composable<AnswerRoute> { entry ->
                val route = entry.toRoute<AnswerRoute>()
                AnswerScreen(
                    route = route,
                    api = api,
                    onBack = { navController.popBackStack() },
                    onAnswerAnother = { navController.popBackStack(HomeRoute, inclusive = false) },
                )
            }
        }
    }
}
