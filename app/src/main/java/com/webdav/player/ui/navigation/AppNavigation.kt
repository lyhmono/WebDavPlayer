package com.webdav.player.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.webdav.player.feature.browser.BrowserScreen
import com.webdav.player.feature.player.MiniPlayerBar
import com.webdav.player.feature.player.PlayerScreen
import com.webdav.player.feature.player.PlayerViewModel
import com.webdav.player.feature.playlist.PlaylistDetailScreen
import com.webdav.player.feature.playlist.PlaylistScreen
import com.webdav.player.ui.ServerConfigScreen

/**
 * 导航路由
 */
object Routes {
    /** 服务器配置页面 */
    const val SERVER_CONFIG = "server_config"

    /** 文件浏览器: browser/{serverId}/{encodedPath} */
    const val BROWSER = "browser/{serverId}/{encodedPath}"

    /** 全屏播放器 */
    const val PLAYER = "player"

    /** 播放列表管理 */
    const val PLAYLISTS = "playlists"

    /** 播放列表详情: playlist_detail/{playlistId} */
    const val PLAYLIST_DETAIL = "playlist_detail/{playlistId}"

    /**
     * 构建浏览器路由
     */
    fun browser(serverId: Long, path: String = "/"): String {
        val encodedPath = java.net.URLEncoder.encode(path, "UTF-8")
        return "browser/$serverId/$encodedPath"
    }

    /**
     * 构建播放列表详情路由
     */
    fun playlistDetail(playlistId: Long): String {
        return "playlist_detail/$playlistId"
    }
}

/**
 * 应用导航
 */
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val playerViewModel: PlayerViewModel = hiltViewModel()

    NavHost(
        navController = navController,
        startDestination = Routes.SERVER_CONFIG
    ) {
        // 服务器配置页面
        composable(Routes.SERVER_CONFIG) {
            ServerConfigScreen(
                onServerClick = { server ->
                    navController.navigate(Routes.browser(server.id, "/"))
                }
            )
        }

        // 文件浏览器
        composable(
            route = Routes.BROWSER,
            arguments = listOf(
                navArgument("serverId") { type = NavType.LongType },
                navArgument("encodedPath") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getLong("serverId") ?: 0L
            val encodedPath = backStackEntry.arguments?.getString("encodedPath") ?: ""

            Box(modifier = Modifier.fillMaxSize()) {
                BrowserScreen(
                    serverId = serverId,
                    encodedPath = encodedPath,
                    onBack = { navController.popBackStack() },
                    onPlayFile = { entry ->
                        playerViewModel.playWebDavEntry(entry, serverId)
                    },
                    onPlayAll = { entries, startIndex ->
                        playerViewModel.playAllWebDavEntries(entries, serverId, startIndex)
                    },
                    onNavigateToPlayer = {
                        navController.navigate(Routes.PLAYER)
                    },
                    onNavigateToPlaylists = {
                        navController.navigate(Routes.PLAYLISTS)
                    },
                    playerViewModel = playerViewModel
                )
            }
        }

        // 全屏播放器
        composable(Routes.PLAYER) {
            PlayerScreen(
                viewModel = playerViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // 播放列表管理
        composable(Routes.PLAYLISTS) {
            Scaffold { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        PlaylistScreen(
                            onBack = { navController.popBackStack() },
                            onPlaylistClick = { playlistId ->
                                navController.navigate(Routes.playlistDetail(playlistId))
                            }
                        )
                    }
                    // MiniPlayerBar
                    MiniPlayerBar(
                        viewModel = playerViewModel,
                        onClick = { navController.navigate(Routes.PLAYER) }
                    )
                }
            }
        }

        // 播放列表详情
        composable(
            route = Routes.PLAYLIST_DETAIL,
            arguments = listOf(
                navArgument("playlistId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: 0L
            PlaylistDetailScreen(
                playlistId = playlistId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
