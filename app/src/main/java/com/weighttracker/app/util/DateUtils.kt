package com.weighttracker.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val zone: ZoneId get() = ZoneId.systemDefault()

fun LocalDate.startOfDayMillis(): Long =
    atStartOfDay(zone).toInstant().toEpochMilli()

fun LocalDate.formatDisplay(): String =
    DateTimeFormatter.ISO_LOCAL_DATE.format(this)

fun millisToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

fun today(): LocalDate = LocalDate.now(zone)
