package com.eggplant.detector.data.history

import java.time.LocalDateTime

const val HISTORY_RETENTION_DAYS: Long = 30

fun historyExpiryCutoff(now: LocalDateTime): LocalDateTime = now.minusDays(HISTORY_RETENTION_DAYS)
