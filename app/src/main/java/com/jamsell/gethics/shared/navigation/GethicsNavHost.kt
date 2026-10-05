package com.jamsell.gethics.shared.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.jamsell.gethics.analytics.presentation.reports.ReportsScreen
import com.jamsell.gethics.analytics.presentation.reports.ReportsViewModel
import com.jamsell.gethics.finance.presentation.finance_summary.FinanceSummaryScreen
import com.jamsell.gethics.finance.presentation.finance_summary.FinanceSummaryViewModel
import com.jamsell.gethics.finance.presentation.register_transaction.RegisterTransactionScreen
import com.jamsell.gethics.finance.presentation.register_transaction.RegisterTransactionViewModel
import com.jamsell.gethics.iam.domain.model.Role
import com.jamsell.gethics.iam.presentation.forgot_password.ForgotPasswordScreen
import com.jamsell.gethics.iam.presentation.forgot_password.ForgotPasswordViewModel
import com.jamsell.gethics.iam.presentation.profile.ProfileScreen
import com.jamsell.gethics.iam.presentation.profile.ProfileViewModel
import com.jamsell.gethics.iam.presentation.reset_password.ResetPasswordScreen
import com.jamsell.gethics.iam.presentation.reset_password.ResetPasswordViewModel
import com.jamsell.gethics.iam.presentation.sign_in.SignInScreen
import com.jamsell.gethics.iam.presentation.sign_in.SignInViewModel
import com.jamsell.gethics.iam.presentation.sign_up.SignUpScreen
import com.jamsell.gethics.iam.presentation.sign_up.SignUpViewModel
import com.jamsell.gethics.livestock.presentation.animal_detail.AnimalDetailScreen
import com.jamsell.gethics.livestock.presentation.animal_detail.AnimalDetailViewModel
import com.jamsell.gethics.livestock.presentation.animal_list.AnimalListScreen
import com.jamsell.gethics.livestock.presentation.animal_list.AnimalListViewModel
import com.jamsell.gethics.livestock.presentation.animal_register.AnimalRegisterScreen
import com.jamsell.gethics.livestock.presentation.animal_register.AnimalRegisterViewModel
import com.jamsell.gethics.sanitary.presentation.clinical_history.ClinicalHistoryScreen
import com.jamsell.gethics.sanitary.presentation.clinical_history.ClinicalHistoryViewModel
import com.jamsell.gethics.sanitary.presentation.register_event.RegisterEventScreen
import com.jamsell.gethics.sanitary.presentation.register_event.RegisterEventViewModel
import com.jamsell.gethics.sanitary.presentation.sanitary_calendar.SanitaryCalendarScreen
import com.jamsell.gethics.sanitary.presentation.sanitary_calendar.SanitaryCalendarViewModel
import com.jamsell.gethics.shared.di.AppContainer
import com.jamsell.gethics.shared.di.gethicsViewModel
import com.jamsell.gethics.subscription.presentation.plans.PlansScreen
import com.jamsell.gethics.subscription.presentation.plans.PlansViewModel
import com.jamsell.gethics.veterinary.presentation.assigned_clients.AssignedClientsScreen
import com.jamsell.gethics.veterinary.presentation.assigned_clients.AssignedClientsViewModel
import com.jamsell.gethics.veterinary.presentation.client_patients.ClientPatientsScreen
import com.jamsell.gethics.veterinary.presentation.client_patients.ClientPatientsViewModel

/**
 * Grafo de navegacion. Cada Screen recibe su ViewModel + callbacks; solo este archivo conoce el NavController.
 * Para agregar una pantalla: (1) ruta en Routes, (2) composable(...) aqui.
 */
@Composable
fun GethicsNavHost(
    navController: NavHostController,
    container: AppContainer,
    modifier: Modifier = Modifier
) {
    val startDestination = if (container.sessionStorage.isLoggedIn()) {
        Routes.homeFor(Role.from(container.sessionStorage.getRole()))
    } else {
        Routes.SIGN_IN
    }

    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {

        // ---------------- iam ----------------
        composable(Routes.SIGN_IN) {
            SignInScreen(
                viewModel = gethicsViewModel { SignInViewModel(container.authRepository) },
                onSignedIn = { role ->
                    navController.navigate(Routes.homeFor(role)) {
                        popUpTo(Routes.SIGN_IN) { inclusive = true }
                    }
                },
                onNavigateToSignUp = { navController.navigate(Routes.SIGN_UP) },
                onNavigateToForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) }
            )
        }
        composable(Routes.SIGN_UP) {
            SignUpScreen(
                viewModel = gethicsViewModel { SignUpViewModel(container.authRepository) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                viewModel = gethicsViewModel { ForgotPasswordViewModel(container.authRepository) },
                onBack = { navController.popBackStack() },
                onHaveToken = { navController.navigate(Routes.RESET_PASSWORD) }
            )
        }
        composable(Routes.RESET_PASSWORD) {
            ResetPasswordScreen(
                viewModel = gethicsViewModel { ResetPasswordViewModel(container.authRepository) },
                onDone = {
                    navController.navigate(Routes.SIGN_IN) { popUpTo(Routes.SIGN_IN) { inclusive = true } }
                }
            )
        }
        composable(Routes.PROFILE) {
            ProfileScreen(
                viewModel = gethicsViewModel { ProfileViewModel(container.userRepository) },
                onViewPlans = { navController.navigate(Routes.PLANS) },
                onSignOut = {
                    container.authRepository.signOut()
                    navController.navigate(Routes.SIGN_IN) { popUpTo(0) { inclusive = true } }
                }
            )
        }

        // ---------------- livestock ----------------
        composable(Routes.ANIMAL_LIST) {
            AnimalListScreen(
                viewModel = gethicsViewModel { AnimalListViewModel(container.livestockRepository) },
                onAnimalClick = { id -> navController.navigate(Routes.animalDetail(id)) },
                onAddAnimal = { navController.navigate(Routes.ANIMAL_REGISTER) }
            )
        }
        composable(Routes.ANIMAL_REGISTER) {
            AnimalRegisterScreen(
                viewModel = gethicsViewModel { AnimalRegisterViewModel(container.livestockRepository) },
                onSaved = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.ANIMAL_DETAIL,
            arguments = listOf(navArgument("animalId") { type = NavType.StringType })
        ) { entry ->
            val animalId = entry.arguments?.getString("animalId").orEmpty()
            AnimalDetailScreen(
                viewModel = gethicsViewModel { AnimalDetailViewModel(container.livestockRepository) },
                animalId = animalId,
                onOpenClinicalHistory = { id -> navController.navigate(Routes.clinicalHistory(id)) },
                onRegisterEvent = { id -> navController.navigate(Routes.registerEvent(id)) }
            )
        }

        // ---------------- sanitary ----------------
        composable(Routes.SANITARY_CALENDAR) {
            SanitaryCalendarScreen(
                viewModel = gethicsViewModel { SanitaryCalendarViewModel(container.sanitaryRepository) }
            )
        }
        composable(
            route = Routes.REGISTER_EVENT,
            arguments = listOf(navArgument("animalId") { type = NavType.StringType })
        ) { entry ->
            RegisterEventScreen(
                viewModel = gethicsViewModel { RegisterEventViewModel(container.sanitaryRepository) },
                animalId = entry.arguments?.getString("animalId").orEmpty(),
                onSaved = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.CLINICAL_HISTORY,
            arguments = listOf(navArgument("animalId") { type = NavType.StringType })
        ) { entry ->
            val animalId = entry.arguments?.getString("animalId").orEmpty()
            ClinicalHistoryScreen(
                viewModel = gethicsViewModel { ClinicalHistoryViewModel(container.sanitaryRepository, animalId) }
            )
        }

        // ---------------- veterinary ----------------
        composable(Routes.ASSIGNED_CLIENTS) {
            AssignedClientsScreen(
                viewModel = gethicsViewModel { AssignedClientsViewModel(container.veterinaryRepository) },
                onClientClick = { id -> navController.navigate(Routes.clientPatients(id)) }
            )
        }
        composable(
            route = Routes.CLIENT_PATIENTS,
            arguments = listOf(navArgument("clientId") { type = NavType.LongType })
        ) { entry ->
            ClientPatientsScreen(
                viewModel = gethicsViewModel { ClientPatientsViewModel(container.veterinaryRepository) },
                clientId = entry.arguments?.getLong("clientId") ?: 0L,
                onAnimalClick = { id -> navController.navigate(Routes.animalDetail(id)) }
            )
        }

        // ---------------- finance ----------------
        composable(Routes.FINANCE_SUMMARY) {
            FinanceSummaryScreen(
                viewModel = gethicsViewModel { FinanceSummaryViewModel(container.financeRepository) },
                onRegisterTransaction = { navController.navigate(Routes.REGISTER_TRANSACTION) }
            )
        }
        composable(Routes.REGISTER_TRANSACTION) {
            RegisterTransactionScreen(
                viewModel = gethicsViewModel { RegisterTransactionViewModel(container.financeRepository) },
                onSaved = { navController.popBackStack() }
            )
        }

        // ---------------- analytics ----------------
        composable(Routes.REPORTS) {
            ReportsScreen(
                viewModel = gethicsViewModel { ReportsViewModel(container.analyticsRepository) }
            )
        }

        // ---------------- subscription ----------------
        composable(Routes.PLANS) {
            PlansScreen(
                viewModel = gethicsViewModel { PlansViewModel(container.subscriptionRepository) },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
