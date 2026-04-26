package com.weighttracker.app.ui.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.weighttracker.app.WeightApplication
import com.weighttracker.app.util.formatDisplay
import com.weighttracker.app.util.startOfDayMillis
import com.weighttracker.app.util.today
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecordScreen(
    onDone: () -> Unit,
    vm: AddViewModel = viewModel(
        factory = AddViewModel.factory(LocalContext.current.applicationContext as WeightApplication),
    ),
) {
    val date by vm.selectedDate.collectAsStateWithLifecycle()
    val weight by vm.weightText.collectAsStateWithLifecycle()
    val unit by vm.displayUnit.collectAsStateWithLifecycle()

    var showDatePicker by remember { mutableStateOf(false) }
    var showReplace by remember { mutableStateOf(false) }

    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val zone = remember { ZoneId.systemDefault() }
    val todayDate = remember { today() }

    val selectableDates = remember(zone, todayDate) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val d = Instant.ofEpochMilli(utcTimeMillis).atZone(zone).toLocalDate()
                return !d.isAfter(todayDate)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            TopAppBar(
                title = { Text("添加记录") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                when (val o = vm.save(allowReplace = false)) {
                                    SaveOutcome.Success -> onDone()
                                    SaveOutcome.NeedsReplaceConfirm -> showReplace = true
                                    is SaveOutcome.Error -> snack.showSnackbar(o.message)
                                }
                            }
                        },
                    ) { Text("保存") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("日期：${date.formatDisplay()}")
            }

            OutlinedTextField(
                value = weight,
                onValueChange = vm::setWeightText,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("体重（${unit.label}）") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                supportingText = { Text("精确到小数点后两位") },
            )

            Text("快速调节（${unit.label}）", style = MaterialTheme.typography.titleSmall)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(-0.5, -0.3, -0.1).forEach { d ->
                    Button(
                        onClick = { vm.adjustBy(d) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                        ),
                    ) {
                        Text(d.toString())
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.1, 0.3, 0.5).forEach { d ->
                    Button(
                        onClick = { vm.adjustBy(d) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text("+$d")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "说明：不可选择今天之后的日期；默认体重为上次记录（首次为 120 斤）。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showDatePicker) {
        key(date) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = date.startOfDayMillis(),
                selectableDates = selectableDates,
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val millis = datePickerState.selectedDateMillis ?: return@TextButton
                            val picked = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
                            vm.setDate(picked)
                            showDatePicker = false
                        },
                    ) { Text("确定") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("取消") }
                },
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }

    if (showReplace) {
        AlertDialog(
            onDismissRequest = { showReplace = false },
            title = { Text("覆盖记录？") },
            text = { Text("该日期已有记录，是否覆盖？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            when (val o = vm.save(allowReplace = true)) {
                                SaveOutcome.Success -> {
                                    showReplace = false
                                    onDone()
                                }
                                is SaveOutcome.Error -> {
                                    showReplace = false
                                    snack.showSnackbar(o.message)
                                }
                                else -> showReplace = false
                            }
                        }
                    },
                ) { Text("覆盖") }
            },
            dismissButton = {
                TextButton(onClick = { showReplace = false }) { Text("取消") }
            },
        )
    }
}
