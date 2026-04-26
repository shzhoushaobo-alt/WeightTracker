package com.weighttracker.app.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.weighttracker.app.WeightApplication
import com.weighttracker.app.data.ExcelIO
import com.weighttracker.app.util.DisplayUnit
import com.weighttracker.app.util.formatDisplay
import com.weighttracker.app.util.millisToLocalDate
import com.weighttracker.app.util.toDisplayString
import com.weighttracker.app.util.toDisplayValue
import com.weighttracker.app.util.today
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAdd: () -> Unit,
    vm: HomeViewModel = viewModel(factory = HomeViewModel.factory(LocalContext.current.applicationContext as WeightApplication)),
) {
    val records by vm.records.collectAsStateWithLifecycle()
    val unit by vm.displayUnit.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()

    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val resolver = context.contentResolver

    var menuOpen by remember { mutableStateOf(false) }
    var rangeSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val zone = remember { ZoneId.systemDefault() }
    val today = remember { today() }

    val selectableDates = remember(zone, today) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val d = Instant.ofEpochMilli(utcTimeMillis).atZone(zone).toLocalDate()
                return !d.isAfter(today)
            }
        }
    }

    val rangePickerState = rememberDateRangePickerState(selectableDates = selectableDates)

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val bytes = ExcelIO.buildExportBytes(records)
            withContext(Dispatchers.IO) {
                resolver.openOutputStream(uri)?.use { it.write(bytes) }
            }
            snack.showSnackbar("已导出当前筛选数据")
        }
    }

    val templateLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val bytes = ExcelIO.buildTemplateBytes()
            withContext(Dispatchers.IO) {
                resolver.openOutputStream(uri)?.use { it.write(bytes) }
            }
            snack.showSnackbar("模板已保存")
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    resolver.openInputStream(uri).use { input ->
                        requireNotNull(input) { "无法读取文件" }
                        ExcelIO.parseImport(input, today())
                    }
                }
                vm.importRecords(result.records)
                val detail = if (result.errors.isNotEmpty()) {
                    "；示例：" + result.errors.take(2).joinToString("；")
                } else {
                    ""
                }
                snack.showSnackbar(
                    "导入完成：写入 ${result.records.size} 条" +
                        (if (result.errors.isNotEmpty()) "，问题 ${result.errors.size} 条" else "") +
                        detail,
                )
            } catch (e: Exception) {
                snack.showSnackbar("导入失败：${e.message ?: "未知错误"}")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("体重记录") },
                actions = {
                    FilterChip(
                        selected = false,
                        onClick = { vm.toggleUnit() },
                        label = { Text(if (unit == DisplayUnit.KG) "公斤" else "斤") },
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("下载导入模板") },
                            onClick = {
                                menuOpen = false
                                templateLauncher.launch("weight_import_template.xlsx")
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("从 Excel 导入") },
                            onClick = {
                                menuOpen = false
                                importLauncher.launch(
                                    arrayOf(
                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                        "*/*",
                                    ),
                                )
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("导出 Excel") },
                            onClick = {
                                menuOpen = false
                                exportLauncher.launch("weight_export.xlsx")
                            },
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("添加记录") },
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = filter is HomeViewModel.DateFilter.Last7,
                    onClick = { vm.setFilter(HomeViewModel.DateFilter.Last7) },
                    label = { Text("近7天") },
                )
                FilterChip(
                    selected = filter is HomeViewModel.DateFilter.Last30,
                    onClick = { vm.setFilter(HomeViewModel.DateFilter.Last30) },
                    label = { Text("近30天") },
                )
                FilterChip(
                    selected = filter is HomeViewModel.DateFilter.Custom,
                    onClick = { rangeSheet = true },
                    label = { Text("自定义") },
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                text = "趋势",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(8.dp))
                            if (records.isEmpty()) {
                                Text(
                                    text = "该时间段暂无记录。点击下方「添加记录」开始记录。",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            } else {
                                WeightLineChart(records = records, unit = unit)
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "明细（${unit.label}）",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                items(records.asReversed()) { r ->
                    val date = millisToLocalDate(r.dateMillis).formatDisplay()
                    val w = r.weightKg.toDisplayValue(unit).toDisplayString()
                    Card {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(date, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                w,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }

    if (rangeSheet) {
        ModalBottomSheet(
            onDismissRequest = { rangeSheet = false },
            sheetState = sheetState,
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("选择日期范围", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                DateRangePicker(state = rangePickerState)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = { rangeSheet = false }) { Text("取消") }
                    TextButton(
                        onClick = {
                            val s = rangePickerState.selectedStartDateMillis ?: return@TextButton
                            val e = rangePickerState.selectedEndDateMillis ?: return@TextButton
                            val start = Instant.ofEpochMilli(s).atZone(zone).toLocalDate()
                            val end = Instant.ofEpochMilli(e).atZone(zone).toLocalDate()
                            vm.setCustomRange(start, end)
                            rangeSheet = false
                        },
                    ) { Text("确定") }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
