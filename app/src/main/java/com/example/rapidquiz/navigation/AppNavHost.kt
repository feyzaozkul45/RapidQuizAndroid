package com.example.rapidquiz.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.rapidquiz.AppContainer
import com.example.rapidquiz.RapidQuizApp
import com.example.rapidquiz.data.model.SessionCreated
import com.example.rapidquiz.ui.home.HomeScreen
import com.example.rapidquiz.ui.home.HomeViewModel
import com.example.rapidquiz.ui.leaderboard.LeaderboardScreen
import com.example.rapidquiz.ui.leaderboard.LeaderboardViewModel
import com.example.rapidquiz.ui.quiz.QuizScreen
import com.example.rapidquiz.ui.quiz.QuizViewModel
import com.example.rapidquiz.ui.result.ResultScreen
import com.example.rapidquiz.ui.result.ResultViewModel
import kotlinx.serialization.Serializable

@Serializable
data object Home

@Serializable
data class Quiz(val sessionId: String, val category: String)

@Serializable
data class Result(val sessionId: String)

@Serializable
data class Leaderboard(val category: String? = null, val sessionId: String? = null)

private fun NavHostController.openQuiz(s: SessionCreated, popResult: Boolean = false) {
    navigate(Quiz(s.sessionId, s.category)) {
        if (popResult) popUpTo<Home>()
    }
}

private fun NavHostController.goHome() {
    navigate(Home) { popUpTo<Home> { inclusive = true } }
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    val container: AppContainer = (LocalContext.current.applicationContext as RapidQuizApp).container

    NavHost(navController = navController, startDestination = Home) {
        composable<Home> {
            val vm: HomeViewModel = viewModel(factory = HomeViewModel.factory(container))
            HomeScreen(
                viewModel = vm,
                onSessionStarted = { navController.openQuiz(it) },
                onOpenLeaderboard = { navController.navigate(Leaderboard()) },
            )
        }
        composable<Quiz> { entry ->
            val route = entry.toRoute<Quiz>()
            val vm: QuizViewModel = viewModel(factory = QuizViewModel.factory(container, route.sessionId, route.category))
            QuizScreen(
                viewModel = vm,
                // Quiz geri yığında kalmaz: geri tuşu sonuçtan quize değil ana sayfaya götürsün.
                onFinished = {
                    navController.navigate(Result(route.sessionId)) { popUpTo<Home>() }
                },
                onExit = { navController.goHome() },
            )
        }
        composable<Result> { entry ->
            val route = entry.toRoute<Result>()
            val vm: ResultViewModel = viewModel(factory = ResultViewModel.factory(container, route.sessionId))
            ResultScreen(
                viewModel = vm,
                onOpenLeaderboard = { category, sessionId ->
                    navController.navigate(Leaderboard(category, sessionId)) { popUpTo<Home>() }
                },
                onPlayAgain = { navController.openQuiz(it, popResult = true) },
                onHome = { navController.goHome() },
            )
        }
        composable<Leaderboard> { entry ->
            val route = entry.toRoute<Leaderboard>()
            val vm: LeaderboardViewModel =
                viewModel(factory = LeaderboardViewModel.factory(container, route.category, route.sessionId))
            LeaderboardScreen(
                viewModel = vm,
                onBack = { if (!navController.popBackStack()) navController.goHome() },
            )
        }
    }
}
