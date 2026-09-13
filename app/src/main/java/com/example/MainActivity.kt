package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.example.ui.DonsaelViewModel
import com.example.ui.ScreenTab
import com.example.ui.components.AppDrawerContent
import com.example.ui.components.AppHeader
import com.example.ui.components.ToastSnackbar
import com.example.ui.screens.CreditScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.PosScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SellerManagementScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.DonsaelTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: DonsaelViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            DonsaelTheme(darkTheme = isDarkMode) {
                val currentUser by viewModel.currentUser.collectAsState()
                val conflictMsg by viewModel.deviceConflictMessage.collectAsState()
                val toast by viewModel.toast.collectAsState()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    if (currentUser == null) {
                        LoginScreen(
                            deviceConflictMessage = conflictMsg,
                            onLogin = { u, p -> viewModel.login(u, p) },
                            onDismissConflict = { viewModel.dismissDeviceConflict() }
                        )
                    } else {
                        MainAppContent(viewModel = viewModel)
                    }

                    // Bottom sliding toast with semantic colors
                    ToastSnackbar(
                        toast = toast,
                        onDismiss = { viewModel.clearToast() }
                    )
                }
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: DonsaelViewModel) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val currentUser by viewModel.currentUser.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val exchangeRate by viewModel.exchangeRate.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val paperWidthMm by viewModel.paperWidthMm.collectAsState()
    val messageOfTheDay by viewModel.messageOfTheDay.collectAsState()

    val products by viewModel.products.collectAsState()
    val trashProducts by viewModel.trashProducts.collectAsState()
    val sales by viewModel.sales.collectAsState()
    val creditSales by viewModel.creditSales.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()
    val sellerAccount by viewModel.sellerAccount.collectAsState()
    val exportedDocs by viewModel.exportedDocs.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val posCurrency by viewModel.posCurrency.collectAsState()
    val completedSale by viewModel.completedSale.collectAsState()

    val lowStockCount = products.count { it.quantity <= it.alertThreshold }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent(
                currentTab = activeTab,
                currentUser = currentUser,
                onTabSelected = { tab -> viewModel.selectTab(tab) },
                onCloseDrawer = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                AppHeader(
                    currentUser = currentUser,
                    isOnline = isOnline,
                    exchangeRate = exchangeRate,
                    isDarkMode = isDarkMode,
                    alertCount = lowStockCount,
                    onToggleDarkMode = { viewModel.toggleDarkMode() },
                    onToggleOnline = { viewModel.toggleOnlineStatus() },
                    onUpdateExchangeRate = { r -> viewModel.updateExchangeRate(r) },
                    onOpenDownloads = { viewModel.selectTab(ScreenTab.DOWNLOADS) },
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onLogout = { viewModel.logout() }
                )
            }
        ) { paddingValues ->
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                label = "ScreenTransition"
            ) { targetTab ->
                when (targetTab) {
                    ScreenTab.DASHBOARD -> DashboardScreen(
                        products = products,
                        sales = sales,
                        creditSales = creditSales,
                        exchangeRate = exchangeRate,
                        messageOfTheDay = messageOfTheDay
                    )

                    ScreenTab.POS -> PosScreen(
                        products = products,
                        cart = cart,
                        currency = posCurrency,
                        exchangeRate = exchangeRate,
                        completedSale = completedSale,
                        onSetCurrency = { viewModel.setPosCurrency(it) },
                        onAddToCart = { viewModel.addToCart(it) },
                        onUpdateQuantity = { pId, d -> viewModel.updateCartQuantity(pId, d) },
                        onRemoveFromCart = { viewModel.removeFromCart(it) },
                        onClearCart = { viewModel.clearCart() },
                        onFinalizeSale = { pType, pMethod, amtTendered, cName, cPhone, cPhone2, cAddress, cDeposit, cDepMethod, cRem, cDue, onSucc ->
                            viewModel.finalizeSale(
                                paymentType = pType,
                                paymentMethod = pMethod,
                                amountTendered = amtTendered,
                                customerName = cName,
                                customerPhone = cPhone,
                                customerPhone2 = cPhone2,
                                customerAddress = cAddress,
                                creditDepositAmount = cDeposit,
                                creditDepositMethod = cDepMethod,
                                creditRemainingBalance = cRem,
                                creditDueDate = cDue,
                                onComplete = onSucc
                            )
                        },
                        onDismissCompletedSale = { viewModel.dismissCompletedSale() },
                        onReprintReceipt = { viewModel.reprintSaleReceipt(it) }
                    )

                    ScreenTab.INVENTORY -> InventoryScreen(
                        products = products,
                        trashProducts = trashProducts,
                        currentUser = currentUser,
                        onAddProduct = { viewModel.addProduct(it) },
                        onUpdateProduct = { viewModel.updateProduct(it) },
                        onSoftDeleteProduct = { viewModel.softDeleteProduct(it) },
                        onRestoreProduct = { viewModel.restoreProduct(it) },
                        onPermanentDeleteProduct = { viewModel.permanentDeleteProduct(it) },
                        onExportPdf = { viewModel.exportInventoryPdf() }
                    )

                    ScreenTab.CREDITS -> CreditScreen(
                        creditSales = creditSales,
                        exchangeRate = exchangeRate,
                        onPayCredit = { s, amt, curr, meth, note -> viewModel.payCredit(s, amt, curr, meth, note) },
                        onReprintReceipt = { viewModel.reprintSaleReceipt(it) },
                        onExportPdf = { viewModel.exportFinancialReportPdf("Crédits Clients", "TOUT") }
                    )

                    ScreenTab.SELLER_MANAGEMENT -> SellerManagementScreen(
                        seller = sellerAccount,
                        onCreateSeller = { fn, un, pw -> viewModel.createSeller(fn, un, pw) },
                        onUpdatePermissions = { viewModel.updateSellerPermissions(it) },
                        onBlockSeller = { viewModel.blockSeller(it) },
                        onUnblockSeller = { viewModel.unblockSeller() },
                        onResetPassword = { viewModel.resetSellerPassword(it) },
                        onClearDevice = { viewModel.clearSellerDevice(it) }
                    )

                    ScreenTab.REPORTS -> ReportsScreen(
                        sales = sales,
                        products = products,
                        exchangeRate = exchangeRate,
                        onExportPdf = { pLabel, cFilter -> viewModel.exportFinancialReportPdf(pLabel, cFilter) }
                    )

                    ScreenTab.HISTORY -> HistoryScreen(
                        sales = sales,
                        auditLogs = auditLogs,
                        onReprintReceipt = { viewModel.reprintSaleReceipt(it) }
                    )

                    ScreenTab.SETTINGS -> SettingsScreen(
                        currentUser = currentUser,
                        exchangeRate = exchangeRate,
                        paperWidthMm = paperWidthMm,
                        messageOfTheDay = messageOfTheDay,
                        onUpdateExchangeRate = { viewModel.updateExchangeRate(it) },
                        onUpdatePaperWidth = { viewModel.setPaperWidthMm(it) },
                        onUpdateMessageOfTheDay = { viewModel.setMessageOfTheDay(it) }
                    )

                    ScreenTab.DOWNLOADS -> DownloadsScreen(
                        documents = exportedDocs,
                        onOpenDocument = { viewModel.openDocument(it) }
                    )
                }
            }
        }
    }
}

