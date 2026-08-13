package com.sheshabiz.quickquote.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.sheshabiz.quickquote.R
import com.sheshabiz.quickquote.di.AppContainer
import com.sheshabiz.quickquote.di.viewModelFactory
import com.sheshabiz.quickquote.ui.businesssetup.BusinessSetupScreen
import com.sheshabiz.quickquote.ui.businesssetup.BusinessSetupViewModel
import com.sheshabiz.quickquote.ui.customers.CustomerEditScreen
import com.sheshabiz.quickquote.ui.customers.CustomerEditViewModel
import com.sheshabiz.quickquote.ui.customers.CustomersScreen
import com.sheshabiz.quickquote.ui.customers.CustomersViewModel
import com.sheshabiz.quickquote.ui.dashboard.DashboardScreen
import com.sheshabiz.quickquote.ui.dashboard.DashboardViewModel
import com.sheshabiz.quickquote.ui.invoice.create.CreateInvoiceScreen
import com.sheshabiz.quickquote.ui.invoice.create.CreateInvoiceViewModel
import com.sheshabiz.quickquote.ui.invoice.preview.InvoicePreviewScreen
import com.sheshabiz.quickquote.ui.invoice.preview.InvoicePreviewViewModel
import com.sheshabiz.quickquote.ui.invoices.InvoicesListScreen
import com.sheshabiz.quickquote.ui.invoices.InvoicesListViewModel
import com.sheshabiz.quickquote.ui.more.MoreScreen
import com.sheshabiz.quickquote.ui.onboarding.OnboardingScreen
import com.sheshabiz.quickquote.ui.pos.ReceiptPreviewScreen
import com.sheshabiz.quickquote.ui.pos.ReceiptPreviewViewModel
import com.sheshabiz.quickquote.ui.pos.SalesHistoryScreen
import com.sheshabiz.quickquote.ui.pos.SalesHistoryViewModel
import com.sheshabiz.quickquote.ui.pos.TillScreen
import com.sheshabiz.quickquote.ui.pos.TillViewModel
import com.sheshabiz.quickquote.ui.products.ProductEditScreen
import com.sheshabiz.quickquote.ui.products.ProductEditViewModel
import com.sheshabiz.quickquote.ui.products.ProductsScreen
import com.sheshabiz.quickquote.ui.products.ProductsViewModel
import com.sheshabiz.quickquote.ui.quote.create.CreateQuoteScreen
import com.sheshabiz.quickquote.ui.quote.create.CreateQuoteViewModel
import com.sheshabiz.quickquote.ui.quote.preview.QuotePreviewScreen
import com.sheshabiz.quickquote.ui.quote.preview.QuotePreviewViewModel
import com.sheshabiz.quickquote.ui.quotes.QuotesListScreen
import com.sheshabiz.quickquote.ui.quotes.QuotesListViewModel
import com.sheshabiz.quickquote.ui.settings.SettingsScreen
import com.sheshabiz.quickquote.ui.settings.SettingsViewModel
import com.sheshabiz.quickquote.ui.splash.SplashScreen
import kotlinx.coroutines.launch

private data class BottomTab(val route: String, val labelRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomTabs = listOf(
    BottomTab(Routes.DASHBOARD, R.string.nav_dashboard, Icons.Filled.Dashboard),
    BottomTab(Routes.QUOTES, R.string.nav_quotes, Icons.Filled.ReceiptLong),
    BottomTab(Routes.INVOICES, R.string.nav_invoices, Icons.Filled.RequestQuote),
    BottomTab(Routes.POS, R.string.nav_pos, Icons.Filled.PointOfSale),
    BottomTab(Routes.MORE, R.string.nav_more, Icons.Filled.MoreHoriz)
)

@Composable
fun QuickQuoteNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = bottomTabs.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    bottomTabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding())
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(onFinished = {
                    val target = when {
                        !container.preferences.onboardingComplete.value -> Routes.ONBOARDING
                        !container.preferences.businessSetupComplete.value -> Routes.BUSINESS_SETUP
                        else -> Routes.DASHBOARD
                    }
                    navController.navigate(target) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                })
            }

            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onSkip = {
                        container.preferences.setOnboardingComplete(true)
                        navController.navigate(Routes.BUSINESS_SETUP) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                    },
                    onGetStarted = {
                        container.preferences.setOnboardingComplete(true)
                        navController.navigate(Routes.BUSINESS_SETUP) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                    },
                    onTryDemo = {
                        scope.launch {
                            container.demoDataSeeder.seed()
                            navController.navigate(Routes.DASHBOARD) { popUpTo(0) }
                        }
                    }
                )
            }

            composable(Routes.BUSINESS_SETUP) {
                val vm = viewModel<BusinessSetupViewModel>(
                    factory = viewModelFactory {
                        BusinessSetupViewModel(
                            businessRepository = container.businessRepository,
                            preferences = container.preferences,
                            copyLogoToInternalStorage = { uri -> container.copyLogo(uri) }
                        )
                    }
                )
                BusinessSetupScreen(
                    viewModel = vm,
                    isEditing = false,
                    onDone = {
                        navController.navigate(Routes.DASHBOARD) { popUpTo(0) }
                    }
                )
            }

            composable(Routes.EDIT_BUSINESS_PROFILE) {
                val vm = viewModel<BusinessSetupViewModel>(
                    factory = viewModelFactory {
                        BusinessSetupViewModel(
                            businessRepository = container.businessRepository,
                            preferences = container.preferences,
                            copyLogoToInternalStorage = { uri -> container.copyLogo(uri) }
                        )
                    }
                )
                BusinessSetupScreen(
                    viewModel = vm,
                    isEditing = true,
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.DASHBOARD) {
                val vm = viewModel<DashboardViewModel>(
                    factory = viewModelFactory { DashboardViewModel(container.businessRepository, container.quoteRepository) }
                )
                DashboardScreen(
                    viewModel = vm,
                    onNewQuote = { navController.navigate(Routes.createQuote()) },
                    onOpenQuote = { id -> navController.navigate(Routes.quotePreview(id)) },
                    onSeeAllQuotes = {
                        navController.navigate(Routes.QUOTES) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(Routes.QUOTES) {
                val vm = viewModel<QuotesListViewModel>(
                    factory = viewModelFactory { QuotesListViewModel(container.quoteRepository) }
                )
                QuotesListScreen(
                    viewModel = vm,
                    onOpenQuote = { id -> navController.navigate(Routes.quotePreview(id)) },
                    onNewQuote = { navController.navigate(Routes.createQuote()) }
                )
            }

            composable(Routes.INVOICES) {
                val vm = viewModel<InvoicesListViewModel>(
                    factory = viewModelFactory { InvoicesListViewModel(container.invoiceRepository) }
                )
                InvoicesListScreen(
                    viewModel = vm,
                    onOpenInvoice = { id -> navController.navigate(Routes.invoicePreview(id)) },
                    onNewInvoice = { navController.navigate(Routes.createInvoice()) }
                )
            }

            composable(Routes.CUSTOMERS) {
                val vm = viewModel<CustomersViewModel>(
                    factory = viewModelFactory { CustomersViewModel(container.customerRepository) }
                )
                CustomersScreen(
                    viewModel = vm,
                    onAddCustomer = { navController.navigate(Routes.customerEdit()) },
                    onOpenCustomer = { id -> navController.navigate(Routes.customerEdit(id)) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.SETTINGS) {
                val vm = viewModel<SettingsViewModel>(
                    factory = viewModelFactory {
                        SettingsViewModel(
                            container.businessRepository,
                            container.preferences,
                            container.backupService,
                            container.reminderScheduler
                        )
                    }
                )
                SettingsScreen(
                    viewModel = vm,
                    onEditBusinessProfile = { navController.navigate(Routes.EDIT_BUSINESS_PROFILE) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.MORE) {
                MoreScreen(
                    onOpenCustomers = { navController.navigate(Routes.CUSTOMERS) },
                    onOpenProducts = { navController.navigate(Routes.PRODUCTS) },
                    onOpenSalesHistory = { navController.navigate(Routes.SALES_HISTORY) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }

            composable(Routes.PRODUCTS) {
                val vm = viewModel<ProductsViewModel>(
                    factory = viewModelFactory { ProductsViewModel(container.productRepository) }
                )
                ProductsScreen(
                    viewModel = vm,
                    onAddProduct = { navController.navigate(Routes.productEdit()) },
                    onOpenProduct = { id -> navController.navigate(Routes.productEdit(id)) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.PRODUCT_EDIT_PATTERN,
                arguments = listOf(navArgument(Routes.PRODUCT_ARG) { type = NavType.LongType; defaultValue = -1L })
            ) { entry ->
                val productIdArg = entry.arguments?.getLong(Routes.PRODUCT_ARG) ?: -1L
                val productId = productIdArg.takeIf { it > 0 }
                val vm = viewModel<ProductEditViewModel>(
                    key = "product_edit_$productIdArg",
                    factory = viewModelFactory { ProductEditViewModel(container.productRepository, productId) }
                )
                ProductEditScreen(
                    viewModel = vm,
                    isEditing = productId != null,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                    onDeleted = { navController.popBackStack() }
                )
            }

            composable(Routes.POS) {
                val vm = viewModel<TillViewModel>(
                    factory = viewModelFactory {
                        TillViewModel(container.productRepository, container.saleRepository, container.preferences)
                    }
                )
                TillScreen(
                    viewModel = vm,
                    customerRepository = container.customerRepository,
                    onSaleCompleted = { saleId ->
                        navController.navigate(Routes.salePreview(saleId))
                    }
                )
            }

            composable(
                route = Routes.SALE_PREVIEW_PATTERN,
                arguments = listOf(navArgument(Routes.SALE_ARG) { type = NavType.LongType })
            ) { entry ->
                val saleId = entry.arguments?.getLong(Routes.SALE_ARG) ?: 0L
                val vm = viewModel<ReceiptPreviewViewModel>(
                    key = "sale_preview_$saleId",
                    factory = viewModelFactory {
                        ReceiptPreviewViewModel(saleId, container.saleRepository, container.businessRepository, container.pdfGenerator)
                    }
                )
                ReceiptPreviewScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onNewSale = {
                        navController.navigate(Routes.POS) {
                            popUpTo(Routes.POS) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.SALES_HISTORY) {
                val vm = viewModel<SalesHistoryViewModel>(
                    factory = viewModelFactory { SalesHistoryViewModel(container.saleRepository) }
                )
                SalesHistoryScreen(
                    viewModel = vm,
                    onOpenSale = { id -> navController.navigate(Routes.salePreview(id)) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.CREATE_QUOTE_PATTERN,
                arguments = listOf(navArgument(Routes.QUOTE_ARG) { type = NavType.LongType; defaultValue = -1L })
            ) { entry ->
                val quoteIdArg = entry.arguments?.getLong(Routes.QUOTE_ARG) ?: -1L
                val quoteId = quoteIdArg.takeIf { it > 0 }
                val vm = viewModel<CreateQuoteViewModel>(
                    key = "create_quote_$quoteIdArg",
                    factory = viewModelFactory {
                        CreateQuoteViewModel(container.quoteRepository, container.customerRepository, container.preferences, quoteId)
                    }
                )
                CreateQuoteScreen(
                    viewModel = vm,
                    customerRepository = container.customerRepository,
                    productRepository = container.productRepository,
                    isEditing = quoteId != null,
                    onBack = { navController.popBackStack() },
                    onSaved = { savedId ->
                        navController.navigate(Routes.quotePreview(savedId)) {
                            popUpTo(Routes.CREATE_QUOTE_PATTERN) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Routes.QUOTE_PREVIEW_PATTERN,
                arguments = listOf(navArgument(Routes.QUOTE_ARG) { type = NavType.LongType })
            ) { entry ->
                val quoteId = entry.arguments?.getLong(Routes.QUOTE_ARG) ?: 0L
                val vm = viewModel<QuotePreviewViewModel>(
                    key = "quote_preview_$quoteId",
                    factory = viewModelFactory {
                        QuotePreviewViewModel(
                            quoteId = quoteId,
                            quoteRepository = container.quoteRepository,
                            businessRepository = container.businessRepository,
                            invoiceRepository = container.invoiceRepository,
                            preferences = container.preferences,
                            pdfGenerator = container.pdfGenerator
                        )
                    }
                )
                QuotePreviewScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(Routes.createQuote(id)) },
                    onDuplicated = { newId ->
                        navController.navigate(Routes.quotePreview(newId)) {
                            popUpTo(Routes.QUOTE_PREVIEW_PATTERN) { inclusive = true }
                        }
                    },
                    onConvertedToInvoice = { invoiceId ->
                        navController.navigate(Routes.invoicePreview(invoiceId))
                    },
                    onDeleted = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.CUSTOMER_EDIT_PATTERN,
                arguments = listOf(navArgument(Routes.CUSTOMER_ARG) { type = NavType.LongType; defaultValue = -1L })
            ) { entry ->
                val customerIdArg = entry.arguments?.getLong(Routes.CUSTOMER_ARG) ?: -1L
                val customerId = customerIdArg.takeIf { it > 0 }
                val vm = viewModel<CustomerEditViewModel>(
                    key = "customer_edit_$customerIdArg",
                    factory = viewModelFactory { CustomerEditViewModel(container.customerRepository, customerId) }
                )
                CustomerEditScreen(
                    viewModel = vm,
                    isEditing = customerId != null,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.CREATE_INVOICE_PATTERN,
                arguments = listOf(navArgument(Routes.INVOICE_ARG) { type = NavType.LongType; defaultValue = -1L })
            ) { entry ->
                val invoiceIdArg = entry.arguments?.getLong(Routes.INVOICE_ARG) ?: -1L
                val invoiceId = invoiceIdArg.takeIf { it > 0 }
                val vm = viewModel<CreateInvoiceViewModel>(
                    key = "create_invoice_$invoiceIdArg",
                    factory = viewModelFactory {
                        CreateInvoiceViewModel(container.invoiceRepository, container.customerRepository, container.preferences, invoiceId)
                    }
                )
                CreateInvoiceScreen(
                    viewModel = vm,
                    customerRepository = container.customerRepository,
                    productRepository = container.productRepository,
                    isEditing = invoiceId != null,
                    onBack = { navController.popBackStack() },
                    onSaved = { savedId ->
                        navController.navigate(Routes.invoicePreview(savedId)) {
                            popUpTo(Routes.CREATE_INVOICE_PATTERN) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Routes.INVOICE_PREVIEW_PATTERN,
                arguments = listOf(navArgument(Routes.INVOICE_ARG) { type = NavType.LongType })
            ) { entry ->
                val invoiceId = entry.arguments?.getLong(Routes.INVOICE_ARG) ?: 0L
                val vm = viewModel<InvoicePreviewViewModel>(
                    key = "invoice_preview_$invoiceId",
                    factory = viewModelFactory {
                        InvoicePreviewViewModel(
                            invoiceId = invoiceId,
                            invoiceRepository = container.invoiceRepository,
                            businessRepository = container.businessRepository,
                            pdfGenerator = container.pdfGenerator
                        )
                    }
                )
                InvoicePreviewScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(Routes.createInvoice(id)) },
                    onDeleted = { navController.popBackStack() }
                )
            }
        }
    }
}
