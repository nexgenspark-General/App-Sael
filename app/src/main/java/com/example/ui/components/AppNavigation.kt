package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.UserAccount
import com.example.ui.ScreenTab
import com.example.ui.theme.DonsaelNavy
import com.example.ui.theme.DonsaelTeal

data class NavItem(
    val tab: ScreenTab,
    val icon: ImageVector,
    val adminOnly: Boolean = false,
    val permissionCheck: ((UserAccount) -> Boolean)? = null
)

@Composable
fun AppDrawerContent(
    currentTab: ScreenTab,
    currentUser: UserAccount?,
    onTabSelected: (ScreenTab) -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        NavItem(ScreenTab.DASHBOARD, Icons.Default.Dashboard),
        NavItem(ScreenTab.POS, Icons.Default.PointOfSale, permissionCheck = { it.canAccessPos }),
        NavItem(ScreenTab.INVENTORY, Icons.Default.Inventory2, permissionCheck = { it.canAccessInventory }),
        NavItem(ScreenTab.CREDITS, Icons.Default.CreditCard, permissionCheck = { it.canAccessCredits }),
        NavItem(ScreenTab.SELLER_MANAGEMENT, Icons.Default.People, adminOnly = true),
        NavItem(ScreenTab.REPORTS, Icons.Default.Assessment, permissionCheck = { it.canAccessReports }),
        NavItem(ScreenTab.HISTORY, Icons.Default.History, permissionCheck = { it.canAccessHistory }),
        NavItem(ScreenTab.DOWNLOADS, Icons.Default.Download),
        NavItem(ScreenTab.SETTINGS, Icons.Default.Settings)
    )

    ModalDrawerSheet(
        modifier = modifier.width(300.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DonsaelNavy)
                    .padding(20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_dcm_icon),
                                contentDescription = "Logo DCM DONSAEL",
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "DONSAEL (DCM)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "COMMUNICATION",
                                color = DonsaelTeal,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "& MULTI-SERVICES",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (currentUser != null) {
                        Surface(
                            color = Color.White.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = currentUser.fullName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Rôle : ${if (currentUser.role == "ADMIN") "Administrateur Principal" else "Vendeur Caisse"}",
                                    color = Color.White.copy(alpha = 0.75f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Navigation Items with RBAC visibility filter
            Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                items.forEach { item ->
                    val isVisible = if (currentUser == null) false
                    else if (currentUser.role == "ADMIN") true
                    else if (item.adminOnly) false
                    else item.permissionCheck?.invoke(currentUser) ?: true

                    if (isVisible) {
                        NavigationDrawerItem(
                            icon = { Icon(item.icon, contentDescription = item.tab.title) },
                            label = { Text(item.tab.title, fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                            selected = currentTab == item.tab,
                            onClick = {
                                onTabSelected(item.tab)
                                onCloseDrawer()
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .padding(vertical = 2.dp)
                                .testTag("nav_item_${item.tab.name}")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

            // Footer NexGen Spark credits
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Conçu par NexGen Spark",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Belle-Vue, L'Asile, Nippes, Haïti",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "support@nexgns.net | (+509) 4200-0315",
                    fontSize = 8.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
