package com.mf650.manager.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.CellTower
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.mf650.manager.data.repository.Mf650Repository
import com.mf650.manager.ui.screens.cellular.CellularScreen
import com.mf650.manager.ui.screens.home.HomeScreen
import com.mf650.manager.ui.screens.messages.MessagesScreen
import com.mf650.manager.ui.screens.more.MoreScreen
import com.mf650.manager.ui.screens.more.MoreSubRoute
import com.mf650.manager.ui.screens.wifi.WifiScreen
import com.mf650.manager.ui.theme.CyanPrimary
import com.mf650.manager.ui.theme.GlassTokens
import com.mf650.manager.ui.viewmodel.CellularViewModel
import com.mf650.manager.ui.viewmodel.DashboardViewModel
import com.mf650.manager.ui.viewmodel.MoreViewModel
import com.mf650.manager.ui.viewmodel.SmsViewModel
import com.mf650.manager.ui.viewmodel.WifiViewModel

enum class MainDestination(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("首页", Icons.Filled.Home, Icons.Outlined.Home),
    CELLULAR("基站", Icons.Filled.CellTower, Icons.Outlined.CellTower),
    WIFI("Wi-Fi", Icons.Filled.Wifi, Icons.Outlined.Wifi),
    MESSAGES("短信", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline),
    MORE("更多", Icons.Filled.Tune, Icons.Outlined.Tune)
}

@Composable
fun MainAppScaffold(
    repository: Mf650Repository,
    dashboardViewModel: DashboardViewModel = remember { DashboardViewModel(repository) },
    cellularViewModel: CellularViewModel = remember { CellularViewModel(repository) },
    wifiViewModel: WifiViewModel = remember { WifiViewModel(repository) },
    smsViewModel: SmsViewModel = remember { SmsViewModel(repository) },
    moreViewModel: MoreViewModel = remember { MoreViewModel(repository) }
) {
    var currentDestination by remember { mutableStateOf(MainDestination.HOME) }
    var targetMoreSubRoute by remember { mutableStateOf<MoreSubRoute?>(null) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = GlassTokens.PanelAlpha),
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                MainDestination.entries.forEach { destination ->
                    val isSelected = currentDestination == destination
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            currentDestination = destination
                            if (destination != MainDestination.MORE) {
                                targetMoreSubRoute = null
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                                contentDescription = destination.label,
                                tint = if (isSelected) CyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        label = {
                            Text(
                                text = destination.label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) CyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = CyanPrimary.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        when (currentDestination) {
            MainDestination.HOME -> HomeScreen(viewModel = dashboardViewModel, modifier = modifier)
            MainDestination.CELLULAR -> CellularScreen(viewModel = cellularViewModel, modifier = modifier)
            MainDestination.WIFI -> WifiScreen(viewModel = wifiViewModel, modifier = modifier)
            MainDestination.MESSAGES -> MessagesScreen(
                viewModel = smsViewModel,
                onNavigateToForward = {
                    targetMoreSubRoute = MoreSubRoute.SMS_FORWARD
                    currentDestination = MainDestination.MORE
                },
                modifier = modifier
            )
            MainDestination.MORE -> MoreScreen(
                viewModel = moreViewModel,
                routerIp = "192.168.100.1",
                initialSubRoute = targetMoreSubRoute,
                modifier = modifier
            )
        }
    }
}
