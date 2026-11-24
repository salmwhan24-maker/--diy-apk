package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.domain.InvoiceType
import com.example.ui.components.BrandTopHeader
import com.example.ui.components.FeedbackToastBanner
import com.example.ui.components.GoldShopBackground
import com.example.ui.screens.CreateInvoiceScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InvoiceHistoryScreen
import com.example.ui.screens.InvoicePreviewScreen
import com.example.ui.screens.MenuAndAboutScreen
import com.example.ui.screens.PricesAndCalculatorScreen
import kotlinx.coroutines.delay

object AppRoutes {
    const val DASHBOARD = "dashboard"
    const val PRICES = "prices"
    const val HISTORY = "history"
    const val MENU = "menu"
    const val CREATE_INVOICE = "create_invoice/{type}"
    const val INVOICE_PREVIEW = "invoice_preview/{invoiceId}"

    fun createInvoice(type: InvoiceType) = "create_invoice/${type.name}"
    fun invoicePreview(invoiceId: Long) = "invoice_preview/$invoiceId"
}

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

private val topLevelDestinations = listOf(
    TopLevelDestination(
        route = AppRoutes.DASHBOARD,
        label = "الرئيسية",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_dashboard",
    ),
    TopLevelDestination(
        route = AppRoutes.PRICES,
        label = "الأسعار والحاسبة",
        selectedIcon = Icons.Filled.Calculate,
        unselectedIcon = Icons.Outlined.Calculate,
        testTag = "nav_prices",
    ),
    TopLevelDestination(
        route = AppRoutes.HISTORY,
        label = "الفواتير",
        selectedIcon = Icons.AutoMirrored.Filled.ReceiptLong,
        unselectedIcon = Icons.AutoMirrored.Outlined.ReceiptLong,
        testTag = "nav_history",
    ),
    TopLevelDestination(
        route = AppRoutes.MENU,
        label = "القائمة وحول",
        selectedIcon = Icons.Filled.Menu,
        unselectedIcon = Icons.Outlined.Menu,
        testTag = "nav_menu",
    ),
)

@Composable
fun AbuIshaqNavigationApp(
    viewModel: GoldShopViewModel,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AppRoutes.DASHBOARD

    val prices by viewModel.prices.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val allInvoices by viewModel.allInvoices.collectAsStateWithLifecycle()
    val filteredInvoices by viewModel.filteredInvoices.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val feedback by viewModel.feedback.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterType by viewModel.filterType.collectAsStateWithLifecycle()
    val dateRange by viewModel.dateRange.collectAsStateWithLifecycle()
    val pendingPrefill by viewModel.pendingPrefill.collectAsStateWithLifecycle()

    LaunchedEffect(feedback) {
        if (feedback != null) {
            delay(5000)
            viewModel.dismissFeedback()
        }
    }

    val isTopLevel = topLevelDestinations.any { it.route == currentRoute }
    val subtitle = when {
        currentRoute == AppRoutes.DASHBOARD -> "الواجهة الرئيسية • نظام أوفلاين متكامل"
        currentRoute == AppRoutes.PRICES -> "تفاصيل الذهب والأسعار والحاسبة الفورية"
        currentRoute == AppRoutes.HISTORY -> "سجل فواتير البيع والشراء"
        currentRoute == AppRoutes.MENU -> "القائمة والنسخ الاحتياطي وحول التطبيق"
        currentRoute.startsWith("create_invoice") -> "إصدار فاتورة رسمية جديدة"
        currentRoute.startsWith("invoice_preview") -> "معاينة وطباعة الفاتورة الرسمية"
        else -> null
    }

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 680.dp

        GoldShopBackground {
            Row(modifier = Modifier.fillMaxSize()) {
                if (useRail && isTopLevel) {
                    NavigationRail(
                        modifier = Modifier
                            .fillMaxHeight()
                            .windowInsetsPadding(WindowInsets.navigationBars),
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    ) {
                        topLevelDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label,
                                    )
                                },
                                label = {
                                    Text(
                                        text = dest.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                                modifier = Modifier.testTag(dest.testTag),
                            )
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.weight(1f),
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    topBar = {
                        Column {
                            BrandTopHeader(
                                subtitle = subtitle,
                                showBackButton = !isTopLevel,
                                onBack = { navController.popBackStack() },
                            )
                            FeedbackToastBanner(
                                feedback = feedback,
                                onDismiss = viewModel::dismissFeedback,
                            )
                        }
                    },
                    bottomBar = {
                        if (!useRail && isTopLevel) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                                windowInsets = WindowInsets.navigationBars,
                            ) {
                                topLevelDestinations.forEach { dest ->
                                    val selected = currentRoute == dest.route
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateTopLevel(dest.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                contentDescription = dest.label,
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = dest.label,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        ),
                                        modifier = Modifier.testTag(dest.testTag),
                                    )
                                }
                            }
                        }
                    },
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .then(
                                if (!isTopLevel) Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                                else Modifier
                            ),
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = AppRoutes.DASHBOARD,
                        ) {
                            composable(AppRoutes.DASHBOARD) {
                                DashboardScreen(
                                    prices = prices,
                                    settings = settings,
                                    invoices = allInvoices,
                                    isSyncing = isSyncing,
                                    onRefreshOnline = viewModel::refreshPricesOnline,
                                    onNewSaleInvoice = {
                                        navController.navigate(AppRoutes.createInvoice(InvoiceType.SALE))
                                    },
                                    onNewPurchaseInvoice = {
                                        navController.navigate(AppRoutes.createInvoice(InvoiceType.PURCHASE))
                                    },
                                    onOpenHistory = { navigateTopLevel(AppRoutes.HISTORY) },
                                    onOpenPricesAndCalc = { navigateTopLevel(AppRoutes.PRICES) },
                                    onOpenMenuAndAbout = { navigateTopLevel(AppRoutes.MENU) },
                                )
                            }

                            composable(AppRoutes.PRICES) {
                                PricesAndCalculatorScreen(
                                    prices = prices,
                                    settings = settings,
                                    isSyncing = isSyncing,
                                    onRefreshOnline = viewModel::refreshPricesOnline,
                                    onEditPrice = viewModel::updateSinglePrice,
                                    onChangeCurrency = viewModel::changeCurrency,
                                    onUpdateExchangeRates = viewModel::updateExchangeRates,
                                    onCreateInvoiceFromCalc = { prefill ->
                                        viewModel.setPrefill(prefill)
                                        navController.navigate(AppRoutes.createInvoice(prefill.type))
                                    },
                                )
                            }

                            composable(AppRoutes.HISTORY) {
                                InvoiceHistoryScreen(
                                    invoices = filteredInvoices,
                                    searchQuery = searchQuery,
                                    filterType = filterType,
                                    dateRange = dateRange,
                                    activeCurrencyCode = settings.activeCurrencyCode,
                                    onSearchChange = viewModel::setSearchQuery,
                                    onFilterTypeChange = viewModel::setFilterType,
                                    onDateRangeChange = viewModel::setDateRange,
                                    onOpenInvoice = { id ->
                                        navController.navigate(AppRoutes.invoicePreview(id))
                                    },
                                    onDeleteInvoice = viewModel::deleteInvoice,
                                )
                            }

                            composable(AppRoutes.MENU) {
                                MenuAndAboutScreen(
                                    settings = settings,
                                    invoicesCount = allInvoices.size,
                                    onToggleTheme = viewModel::toggleTheme,
                                    onNavigateDashboard = { navigateTopLevel(AppRoutes.DASHBOARD) },
                                    onNavigatePrices = { navigateTopLevel(AppRoutes.PRICES) },
                                    onNavigateNewSale = {
                                        navController.navigate(AppRoutes.createInvoice(InvoiceType.SALE))
                                    },
                                    onNavigateNewPurchase = {
                                        navController.navigate(AppRoutes.createInvoice(InvoiceType.PURCHASE))
                                    },
                                    onNavigateHistory = { navigateTopLevel(AppRoutes.HISTORY) },
                                    onExportBackupJson = viewModel::exportBackupJson,
                                    onImportBackupJson = viewModel::importBackupJson,
                                    onShowMessage = viewModel::showFeedback,
                                )
                            }

                            composable(
                                route = AppRoutes.CREATE_INVOICE,
                                arguments = listOf(navArgument("type") { type = NavType.StringType }),
                            ) { backEntry ->
                                val typeArg = backEntry.arguments?.getString("type") ?: InvoiceType.SALE.name
                                val initialType = InvoiceType.fromName(typeArg)
                                CreateInvoiceScreen(
                                    initialType = initialType,
                                    prefill = pendingPrefill,
                                    prices = prices,
                                    settings = settings,
                                    onConsumePrefill = { viewModel.consumePrefill() },
                                    onShowValidationError = { msg -> viewModel.showFeedback(msg, true) },
                                    onSaveInvoice = { type, partyName, partyPhone, notes, currency, items, discount ->
                                        viewModel.saveInvoice(
                                            type = type,
                                            partyName = partyName,
                                            partyPhone = partyPhone,
                                            notes = notes,
                                            currencyCode = currency,
                                            items = items,
                                            discount = discount,
                                            onSaved = { savedId ->
                                                navController.navigate(AppRoutes.invoicePreview(savedId)) {
                                                    popUpTo(AppRoutes.DASHBOARD) { inclusive = false }
                                                }
                                            },
                                        )
                                    },
                                )
                            }

                            composable(
                                route = AppRoutes.INVOICE_PREVIEW,
                                arguments = listOf(navArgument("invoiceId") { type = NavType.LongType }),
                            ) { backEntry ->
                                val invoiceId = backEntry.arguments?.getLong("invoiceId") ?: 0L
                                val invoiceWithItems by viewModel.observeInvoice(invoiceId)
                                    .collectAsStateWithLifecycle(initialValue = null)
                                InvoicePreviewScreen(
                                    invoiceWithItems = invoiceWithItems,
                                    onShowMessage = viewModel::showFeedback,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
