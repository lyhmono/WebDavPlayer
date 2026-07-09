package com.webdav.player.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.webdav.player.feature.browser.BrowserScreen
import com.webdav.player.ui.ServerConfigScreen

/**
 * 导航路由
 */
object Routes {
    /** 服务器配置页面 */
    const val SERVER_CONFIG = "server_config"

    /** 文件浏览器: browser/{serverId}/{encodedPath} */
    const val BROWSER = "browser/{serverId}/{encodedPath}"

    /**
     * 构建浏览器路由
     */
    fun browser(serverId: Long, path: String = "/"): String {
        val encodedPath = java.net.URLEncoder.encode(path, "UTF-8")
        return "browser/$serverId/$encodedPath"
    }
}

/**
 * 应用导航
 */
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
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

            BrowserScreen(
                serverId = serverId,
                encodedPath = encodedPath,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
