package ch.no1hardy.orbit7.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ch.no1hardy.orbit7.MainActivity
import ch.no1hardy.orbit7.R
import ch.no1hardy.orbit7.core.designsystem.TestTags
import ch.no1hardy.orbit7.core.designsystem.component.ScanlineOverlay
import ch.no1hardy.orbit7.feature.budget.BudgetsRoute
import ch.no1hardy.orbit7.feature.contracts.ContractEditorRoute
import ch.no1hardy.orbit7.feature.contracts.ContractEditorViewModel
import ch.no1hardy.orbit7.feature.contracts.DrainsRoute
import ch.no1hardy.orbit7.feature.expenses.LogRoute
import ch.no1hardy.orbit7.feature.expenses.QuickAddRoute
import ch.no1hardy.orbit7.feature.expenses.QuickAddViewModel
import ch.no1hardy.orbit7.feature.reports.ReportsRoute
import ch.no1hardy.orbit7.feature.settings.OnboardingRoute
import ch.no1hardy.orbit7.feature.settings.SettingsRoute
import ch.no1hardy.orbit7.feature.station.BridgeRoute
import ch.no1hardy.orbit7.feature.station.SettlementSummaryRoute
import ch.no1hardy.orbit7.feature.station.SettlementSummaryViewModel
import ch.no1hardy.orbit7.feature.station.StationRoute
import ch.no1hardy.orbit7.core.designsystem.R as CoreR

/** Every destination in the app, in one place. */
object Routes {
    const val ONBOARDING = "onboarding"
    const val BRIDGE = "bridge"
    const val LOG = "log"
    const val STATION = "station"
    const val DRAINS = "drains"
    const val BUDGETS = "budgets"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"
    const val QUICK_ADD = "quick_add?${QuickAddViewModel.ARG_EXPENSE_ID}={${QuickAddViewModel.ARG_EXPENSE_ID}}"
    const val CONTRACT_EDITOR =
        "contract_editor?${ContractEditorViewModel.ARG_CONTRACT_ID}={${ContractEditorViewModel.ARG_CONTRACT_ID}}"
    const val SETTLEMENT_SUMMARY =
        "settlement_summary/{${SettlementSummaryViewModel.ARG_WEEK_ID}}"

    fun quickAdd(expenseId: Long = 0): String = "quick_add?${QuickAddViewModel.ARG_EXPENSE_ID}=$expenseId"

    fun contractEditor(contractId: Long = 0): String =
        "contract_editor?${ContractEditorViewModel.ARG_CONTRACT_ID}=$contractId"

    fun settlementSummary(weekId: String): String = "settlement_summary/$weekId"
}

/** The four bottom-navigation destinations (`docs/03-screens.md`, navigation). */
private enum class BottomDestination(
    val route: String,
    val labelRes: Int,
) {
    BRIDGE(Routes.BRIDGE, R.string.nav_bridge),
    LOG(Routes.LOG, R.string.nav_log),
    STATION(Routes.STATION, R.string.nav_station),
    DRAINS(Routes.DRAINS, R.string.nav_drains),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Orbit7App(
    startWithOnboarding: Boolean,
    pendingSettlementWeekId: String?,
    deepLink: String?,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // The settlement summary is a full-screen takeover shown once, on the first open after a week
    // was settled.
    androidx.compose.runtime.LaunchedEffect(pendingSettlementWeekId) {
        if (pendingSettlementWeekId != null && !startWithOnboarding) {
            navController.navigate(Routes.settlementSummary(pendingSettlementWeekId))
        }
    }

    androidx.compose.runtime.LaunchedEffect(deepLink) {
        when (deepLink) {
            MainActivity.DESTINATION_QUICK_ADD -> navController.navigate(Routes.quickAdd())
            MainActivity.DESTINATION_DRAINS -> navController.navigate(Routes.DRAINS)
        }
    }

    Scaffold(
        topBar = {
            // Reports, Budgets and Settings live in the Bridge's top bar rather than in the bottom
            // navigation, which stays at four destinations (docs/03-screens.md, navigation).
            if (currentRoute == Routes.BRIDGE) {
                TopAppBar(
                    title = { Text(stringResource(CoreR.string.app_name)) },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                            titleContentColor = ch.no1hardy.orbit7.core.designsystem.theme.Orbit7Theme.colors.onSurface,
                        ),
                    actions = {
                        TextButton(
                            onClick = { navController.navigate(Routes.BUDGETS) },
                            modifier = Modifier.testTag("top_budgets"),
                        ) {
                            Text(stringResource(R.string.nav_budgets))
                        }
                        TextButton(
                            onClick = { navController.navigate(Routes.REPORTS) },
                            modifier = Modifier.testTag("top_reports"),
                        ) {
                            Text(stringResource(R.string.nav_reports))
                        }
                        TextButton(
                            onClick = { navController.navigate(Routes.SETTINGS) },
                            modifier = Modifier.testTag("top_settings"),
                        ) {
                            Text(stringResource(R.string.nav_settings))
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (currentRoute in BottomDestination.entries.map { it.route }) {
                NavigationBar {
                    BottomDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(Routes.BRIDGE) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Text(stringResource(destination.labelRes)) },
                            label = { Text(stringResource(destination.labelRes)) },
                            modifier = Modifier.testTag("nav_${destination.name.lowercase()}"),
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentRoute in listOf(Routes.BRIDGE, Routes.LOG)) {
                FloatingActionButton(
                    onClick = { navController.navigate(Routes.quickAdd()) },
                    modifier = Modifier.testTag(TestTags.FAB_QUICK_ADD),
                ) {
                    Text(stringResource(R.string.nav_quick_add))
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = if (startWithOnboarding) Routes.ONBOARDING else Routes.BRIDGE,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
            ) {
                composable(Routes.ONBOARDING) {
                    OnboardingRoute(
                        onCompleted = {
                            navController.navigate(Routes.BRIDGE) {
                                popUpTo(Routes.ONBOARDING) { inclusive = true }
                            }
                        },
                    )
                }

                composable(Routes.BRIDGE) {
                    BridgeRoute(
                        onOpenStation = { navController.navigate(Routes.STATION) },
                        onOpenSettlementSummary = { weekId ->
                            navController.navigate(Routes.settlementSummary(weekId))
                        },
                        onOpenDrains = { navController.navigate(Routes.DRAINS) },
                        onQuickAdd = { navController.navigate(Routes.quickAdd()) },
                    )
                }

                composable(Routes.LOG) {
                    LogRoute(onEditExpense = { id -> navController.navigate(Routes.quickAdd(id)) })
                }

                composable(Routes.STATION) { StationRoute() }

                composable(Routes.DRAINS) {
                    DrainsRoute(
                        onEditContract = { id -> navController.navigate(Routes.contractEditor(id)) },
                        onAddContract = { navController.navigate(Routes.contractEditor()) },
                    )
                }

                composable(Routes.BUDGETS) { BudgetsRoute() }

                composable(Routes.REPORTS) { ReportsRoute() }

                composable(Routes.SETTINGS) {
                    SettingsRoute(onExport = {}, onPickImportFile = {})
                }

                composable(
                    route = Routes.QUICK_ADD,
                    arguments =
                        listOf(
                            navArgument(QuickAddViewModel.ARG_EXPENSE_ID) {
                                type = NavType.LongType
                                defaultValue = 0L
                            },
                        ),
                ) {
                    QuickAddRoute(
                        onSaved = { _, _ -> navController.popBackStack() },
                        onDismiss = { navController.popBackStack() },
                    )
                }

                composable(
                    route = Routes.CONTRACT_EDITOR,
                    arguments =
                        listOf(
                            navArgument(ContractEditorViewModel.ARG_CONTRACT_ID) {
                                type = NavType.LongType
                                defaultValue = 0L
                            },
                        ),
                ) {
                    ContractEditorRoute(
                        onSaved = { navController.popBackStack() },
                        onDismiss = { navController.popBackStack() },
                    )
                }

                composable(
                    route = Routes.SETTLEMENT_SUMMARY,
                    arguments =
                        listOf(
                            navArgument(SettlementSummaryViewModel.ARG_WEEK_ID) { type = NavType.StringType },
                        ),
                ) {
                    SettlementSummaryRoute(
                        onDismiss = { navController.popBackStack() },
                        onOpenStation = {
                            navController.popBackStack()
                            navController.navigate(Routes.STATION)
                        },
                    )
                }
            }

            // Ambient scanline drift, above everything, decorative, and the first thing
            // reduceEffects removes.
            ScanlineOverlay()
        }
    }
}
