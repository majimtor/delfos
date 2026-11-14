package com.manuelalejandro.delfos.mobile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.manuelalejandro.delfos.mobile.answer.AnswerScreen
import com.manuelalejandro.delfos.mobile.api.DelfosApi
import com.manuelalejandro.delfos.mobile.home.HomeScreen
import com.manuelalejandro.delfos.mobile.manager.LoginScreen
import com.manuelalejandro.delfos.mobile.manager.LoginViewModel
import com.manuelalejandro.delfos.mobile.manager.ResultsScreen
import com.manuelalejandro.delfos.mobile.manager.ResultsViewModel
import com.manuelalejandro.delfos.mobile.manager.SurveysScreen
import com.manuelalejandro.delfos.mobile.manager.SurveysViewModel
import com.manuelalejandro.delfos.mobile.platform.httpClientEngine
import com.manuelalejandro.delfos.mobile.session.SessionManager
import com.manuelalejandro.delfos.mobile.session.TokenStore
import com.manuelalejandro.delfos.mobile.ui.DelfosTheme

/**
 * The whole application. [deepLink] is the survey a link opened the application with, if any;
 * the home screen stays underneath so that the respondent can go on to another survey.
 * [tokenStore] is where the platform keeps the manager's session between launches.
 */
@Composable
public fun App(deepLink: AnswerRoute? = null, tokenStore: TokenStore) {
    val session = remember { SessionManager(tokenStore) }
    val api = remember { DelfosApi(httpClientEngine(), token = { session.token }) }
    DelfosTheme {
        val navController = rememberNavController()
        LaunchedEffect(deepLink) {
            if (deepLink != null) navController.navigate(deepLink)
        }
        val toLogin = {
            navController.navigate(LoginRoute) { popUpTo(HomeRoute) }
        }
        NavHost(navController = navController, startDestination = HomeRoute) {
            composable<HomeRoute> {
                HomeScreen(
                    onOpen = { navController.navigate(it) },
                    onManage = { navController.navigate(if (session.signedIn) SurveysRoute else LoginRoute) },
                )
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
            composable<LoginRoute> {
                LoginScreen(
                    viewModel = viewModel { LoginViewModel(api, session) },
                    onBack = { navController.popBackStack() },
                    onSignedIn = { navController.navigate(SurveysRoute) { popUpTo(LoginRoute) { inclusive = true } } },
                )
            }
            composable<SurveysRoute> {
                SurveysScreen(
                    viewModel = viewModel { SurveysViewModel(api, session) },
                    session = session,
                    onBack = { navController.popBackStack() },
                    onOpenResults = { navController.navigate(it) },
                    onSignedOut = toLogin,
                )
            }
            composable<ResultsRoute> { entry ->
                val route = entry.toRoute<ResultsRoute>()
                ResultsScreen(
                    viewModel = viewModel(key = "results/${route.surveyId}/${route.versionNumber}") {
                        ResultsViewModel(api, session, route.surveyId, route.versionNumber)
                    },
                    session = session,
                    title = route.title,
                    onBack = { navController.popBackStack() },
                    onSignedOut = toLogin,
                )
            }
        }
    }
}
