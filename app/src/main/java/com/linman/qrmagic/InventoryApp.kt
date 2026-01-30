/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.linman.qrmagic

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.linman.qrmagic.R.string
import com.linman.qrmagic.ui.home.HomeDestination
import com.linman.qrmagic.ui.navigation.InventoryNavHost
import com.linman.qrmagic.ui.settings.SettingsDestination

/**
 * Top level composable that represents screens for the application.
 */
@Composable
fun InventoryApp(navController: NavHostController = rememberNavController()) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
             val topLevelDestinations = listOf(HomeDestination.route, SettingsDestination.route)
             if (currentRoute in topLevelDestinations) {
                 NavigationBar {
                     NavigationBarItem(
                         icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                         selected = currentRoute == HomeDestination.route,
                         onClick = {
                             navController.navigate(HomeDestination.route) {
                                 popUpTo(navController.graph.findStartDestination().id) {
                                     saveState = true
                                 }
                                 launchSingleTop = true
                                 restoreState = true
                             }
                         }
                     )
                     NavigationBarItem(
                         icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                         selected = currentRoute == SettingsDestination.route,
                         onClick = {
                             navController.navigate(SettingsDestination.route) {
                                 popUpTo(navController.graph.findStartDestination().id) {
                                     saveState = true
                                 }
                                 launchSingleTop = true
                                 restoreState = true
                             }
                         }
                     )
                 }
             }
        }
    ) { innerPadding ->
        InventoryNavHost(navController = navController, modifier = Modifier.padding(innerPadding))
    }
}

/**
 * App bar to display title and conditionally display the back navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryTopAppBar(
    title: String,
    canNavigateBack: Boolean,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    navigateUp: () -> Unit = {}
) {
    CenterAlignedTopAppBar(
        title = { Text(title) },
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = navigateUp) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(string.back_button)
                    )
                }
            }
        }
    )
}
