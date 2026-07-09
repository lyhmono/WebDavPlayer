package com.webdav.player.feature.browser.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.webdav.player.feature.browser.SortField

/**
 * 排序栏组件
 *
 * 排序字段选择（名称/大小/修改时间/类型）
 * 升降序切换
 * 目录优先选项
 */
@Composable
fun SortBar(
    currentField: SortField,
    ascending: Boolean,
    directoriesFirst: Boolean,
    onFieldSelected: (SortField) -> Unit,
    onToggleOrder: () -> Unit,
    onToggleDirectoriesFirst: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 排序字段
        SortField.entries.forEach { field ->
            SortChip(
                label = field.displayName,
                isSelected = currentField == field,
                onClick = { onFieldSelected(field) }
            )
        }

        Spacer(Modifier.width(8.dp))

        // 升降序切换
        Icon(
            imageVector = if (ascending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
            contentDescription = if (ascending) "升序" else "降序",
            modifier = Modifier
                .size(20.dp)
                .clickable { onToggleOrder() },
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.width(8.dp))

        // 目录优先
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onToggleDirectoriesFirst() }
        ) {
            Icon(
                imageVector = if (directoriesFirst) Icons.Default.Check else Icons.Default.Folder,
                contentDescription = "目录优先",
                modifier = Modifier.size(18.dp),
                tint = if (directoriesFirst) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(Modifier.width(2.dp))
            Text(
                text = "目录优先",
                style = MaterialTheme.typography.labelSmall,
                color = if (directoriesFirst) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun SortChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
