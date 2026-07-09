package com.webdav.player.feature.browser.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.webdav.player.feature.browser.Breadcrumb

/**
 * 面包屑导航组件
 *
 * 显示当前路径层级，支持点击跳转
 */
@Composable
fun BreadcrumbNav(
    breadcrumbs: List<Breadcrumb>,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        breadcrumbs.forEachIndexed { index, breadcrumb ->
            if (index == 0) {
                // 根目录用 Home 图标
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "根目录",
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onNavigate(breadcrumb.path) },
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = breadcrumb.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (index == breadcrumbs.lastIndex) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { onNavigate(breadcrumb.path) }
                )
            }

            if (index < breadcrumbs.lastIndex) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
