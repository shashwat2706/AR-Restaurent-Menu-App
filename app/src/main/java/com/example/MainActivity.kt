package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ArStudioScreen
import com.example.ui.CartHistoryScreen
import com.example.ui.MenuExplorerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SlateBorderLight
import com.example.ui.theme.SlateGreyDark
import com.example.ui.theme.SlateMediumSurface
import com.example.viewmodel.MenuViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("CRASH_LOGGER", "FATAL EXCEPTION in thread ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppLayout()
            }
        }
    }
}

@Composable
fun MainAppLayout() {
    val viewModel: MenuViewModel = viewModel()
    var activeTab by remember { mutableStateOf("explore") } // "explore", "ar_studio", "cart_history"

    val cartItems by viewModel.cartItems.collectAsState()
    val totalCartItemsCount = cartItems.sumOf { it.quantity }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // High fidelity styled bottom navigation bar
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .height(68.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .border(1.dp, SlateBorderLight, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .testTag("app_navigation_bar"),
                containerColor = SlateMediumSurface,
                tonalElevation = 8.dp
            ) {
                // Explore Tab
                NavigationBarItem(
                    selected = activeTab == "explore",
                    onClick = { activeTab = "explore" },
                    icon = {
                        Icon(
                            imageVector = if (activeTab == "explore") Icons.Filled.RestaurantMenu else Icons.Outlined.RestaurantMenu,
                            contentDescription = "Explore dishes"
                        )
                    },
                    label = { Text("Menu", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("nav_explore_tab")
                )

                // AR Studio Camera Tab
                NavigationBarItem(
                    selected = activeTab == "ar_studio",
                    onClick = { activeTab = "ar_studio" },
                    icon = {
                        Icon(
                            imageVector = if (activeTab == "ar_studio") Icons.Filled.PhotoCamera else Icons.Outlined.PhotoCamera,
                            contentDescription = "AR Lenses"
                        )
                    },
                    label = { Text("AR Camera", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("nav_ar_studio_tab")
                )

                // Basket Cart Details Tab
                NavigationBarItem(
                    selected = activeTab == "cart_history",
                    onClick = { activeTab = "cart_history" },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (totalCartItemsCount > 0) {
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                        Text("$totalCartItemsCount", color = Color.White, fontSize = 10.sp)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (activeTab == "cart_history") Icons.Filled.ShoppingBag else Icons.Outlined.ShoppingBag,
                                contentDescription = "Order hub"
                            )
                        }
                    },
                    label = { Text("Checkout Hub", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("nav_cart_history_tab")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SlateGreyDark)
        ) {
            // Screen contents switching with crossfade animation transitions
            Crossfade(
                targetState = activeTab,
                animationSpec = tween(300),
                label = "screen_navigation"
            ) { screen ->
                when (screen) {
                    "explore" -> {
                        MenuExplorerScreen(
                            viewModel = viewModel,
                            onViewInAr = { dish ->
                                viewModel.startArSession(dish)
                                activeTab = "ar_studio"
                            }
                        )
                    }
                    "ar_studio" -> {
                        ArStudioScreen(
                            viewModel = viewModel
                        )
                    }
                    "cart_history" -> {
                        CartHistoryScreen(
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}
