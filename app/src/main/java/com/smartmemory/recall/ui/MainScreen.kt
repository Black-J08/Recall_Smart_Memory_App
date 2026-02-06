package com.smartmemory.recall.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.smartmemory.recall.ui.chat.ChatScreen
import com.smartmemory.recall.ui.feed.FeedScreen
import com.smartmemory.recall.ui.feed.FeedViewModel
import com.smartmemory.recall.ui.settings.SettingsScreen
import com.smartmemory.recall.ui.settings.SettingsViewModel

import androidx.compose.runtime.*
import com.smartmemory.recall.ui.chat.ChatViewModel
import com.smartmemory.recall.ui.chat.components.ChatHistoryDrawer
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainContent(
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        composable("settings") {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainContent(onNavigateToSettings: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val chatViewModel: ChatViewModel = hiltViewModel()
    val chatState by chatViewModel.uiState.collectAsState()
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Handle back button to close drawer
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ChatHistoryDrawer(
                sessions = chatState.sessions,
                currentSessionId = chatState.currentSessionId,
                onSessionSelected = { sessionId ->
                    chatViewModel.selectSession(sessionId)
                    scope.launch { drawerState.close() }
                },
                onNewChat = {
                    chatViewModel.createNewChat()
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                HorizontalPager(state = pagerState) { page ->
                    when (page) {
                        0 -> {
                            val viewModel: FeedViewModel = hiltViewModel()
                            FeedScreen(viewModel = viewModel)
                        }
                        1 -> ChatScreen(
                            viewModel = chatViewModel,
                            onMenuClick = { scope.launch { drawerState.open() } },
                            onSettingsClick = onNavigateToSettings
                        )
                    }
                }
            }
        }
    }
}
