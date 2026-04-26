package com.weighttracker.app.data

import com.weighttracker.app.util.jinToKg
import com.weighttracker.app.util.round2
import com.weighttracker.app.util.startOfDayMillis
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

data class ImportResult(
    val records: List<WeightRecord>,
    val errors: List<String>,
)

object ExcelIO {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun buildExportBytes(records: List<WeightRecord>): ByteArray {
        XSSFWorkbook().use { wb ->
            val sheet = wb.createSheet("体重记录")
            val header = sheet.createRow(0)
            header.createCell(0).setCellValue("日期")
            header.createCell(1).setCellValue("体重(kg)")
            header.createCell(2).setCellValue("体重(斤)")

            records.sortedBy { it.dateMillis }.forEachIndexed { i, r ->
                val row = sheet.createRow(i + 1)
                val date = java.time.Instant.ofEpochMilli(r.dateMillis)
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                row.createCell(0).setCellValue(dateFormatter.format(date))
                row.createCell(1).setCellValue(r.weightKg)
                row.createCell(2).setCellValue(r.weightKg * 2.0)
            }

            for (c in 0..2) sheet.autoSizeColumn(c)

            val out = ByteArrayOutputStream()
            wb.write(out)
            return out.toByteArray()
        }
    }

    fun buildTemplateBytes(): ByteArray {
        XSSFWorkbook().use { wb ->
            val sheet = wb.createSheet("模板")
            val header = sheet.createRow(0)
            header.createCell(0).setCellValue("date")
            header.createCell(1).setCellValue("weight_kg")
            val example = sheet.createRow(1)
            example.createCell(0).setCellValue("2026-04-26")
            example.createCell(1).setCellValue(60.0)
            sheet.autoSizeColumn(0)
            sheet.autoSizeColumn(1)
            val out = ByteArrayOutputStream()
            wb.write(out)
            return out.toByteArray()
        }
    }

    fun parseImport(input: InputStream, today: LocalDate): ImportResult {
        val errors = mutableListOf<String>()
        val out = mutableListOf<WeightRecord>()
        XSSFWorkbook(input).use { wb ->
            val sheet = wb.getSheetAt(0) ?: return ImportResult(emptyList(), listOf("表格为空"))
            if (sheet.physicalNumberOfRows < 2) {
                return ImportResult(emptyList(), listOf("没有数据行"))
            }
            val fmt = DataFormatter()
            val headerRow = sheet.getRow(0) ?: return ImportResult(emptyList(), listOf("缺少表头"))
            val idx = resolveHeaderIndices(headerRow, fmt)
                ?: return ImportResult(emptyList(), listOf("表头无法识别：需要日期列 + 体重列"))

            for (r in 1..sheet.lastRowNum) {
                val row = sheet.getRow(r) ?: continue
                val line = r + 1
                val dateRaw = fmt.formatCellValue(row.getCell(idx.date)).trim()
                if (dateRaw.isEmpty()) continue

                val date = try {
                    LocalDate.parse(dateRaw, dateFormatter)
                } catch (_: DateTimeParseException) {
                    errors.add("第${line}行：日期格式错误，请使用 yyyy-MM-dd")
                    continue
                }

                if (date.isAfter(today)) {
                    errors.add("第${line}行：日期不能晚于今天")
                    continue
                }

                val weightKg = when {
                    idx.kg != null -> {
                        val w = fmt.formatCellValue(row.getCell(idx.kg)).trim().toDoubleOrNull()
                        if (w == null || w <= 0 || w > 500) {
                            errors.add("第${line}行：体重(kg)无效")
                            continue
                        }
                        w.round2()
                    }
                    idx.jin != null -> {
                        val j = fmt.formatCellValue(row.getCell(idx.jin)).trim().toDoubleOrNull()
                        if (j == null || j <= 0 || j > 1000) {
                            errors.add("第${line}行：体重(斤)无效")
                            continue
                        }
                        j.jinToKg()
                    }
                    else -> {
                        errors.add("第${line}行：缺少体重")
                        continue
                    }
                }

                out.add(
                    WeightRecord(
                        dateMillis = date.startOfDayMillis(),
                        weightKg = weightKg,
                    ),
                )
            }
        }
        return ImportResult(out, errors)
    }

    private data class HeaderIndices(
        val date: Int,
        val kg: Int?,
        val jin: Int?,
    )

    private fun resolveHeaderIndices(
        headerRow: org.apache.poi.ss.usermodel.Row,
        fmt: DataFormatter,
    ): HeaderIndices? {
        var dateIdx: Int? = null
        var kgIdx: Int? = null
        var jinIdx: Int? = null

        for (c in 0 until headerRow.lastCellNum.toInt().coerceAtLeast(0)) {
            val raw = fmt.formatCellValue(headerRow.getCell(c)).trim()
            if (raw.isEmpty()) continue
            when {
                isDateHeader(raw) -> dateIdx = c
                isKgHeader(raw) -> kgIdx = c
                isJinHeader(raw) -> jinIdx = c
            }
        }

        val d = dateIdx ?: return null
        if (kgIdx == null && jinIdx == null) return null
        return HeaderIndices(date = d, kg = kgIdx, jin = jinIdx)
    }

    private fun isDateHeader(s: String): Boolean {
        val t = s.trim()
        return t.equals("date", ignoreCase = true) || t == "日期"
    }

    private fun isKgHeader(s: String): Boolean {
        val t = s.trim()
        val l = t.lowercase()
        return l == "weight_kg" || l == "kg" || t == "体重(kg)" || t == "体重kg"
    }

    private fun isJinHeader(s: String): Boolean {
        val t = s.trim()
        return t == "体重(斤)" || t == "斤" || t.equals("weight_jin", ignoreCase = true)
    }
}
