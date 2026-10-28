package com.fzu.wuyu.util

import java.util.TimeZone

/**
 * 日期工具：minSdk 24 不使用 java.time，用 epochDay（自 1970-01-01 起的天数）自行换算。
 * 算法来自 Howard Hinnant 的 civil_from_days / days_from_civil。
 */
object DateUtils {

    /** epochDay -> 年/月/日 */
    fun epochDayToYmd(epochDay: Long): Triple<Int, Int, Int> {
        val z = epochDay + 719468
        val era = (if (z >= 0) z else z - 146096) / 146097
        val doe = z - era * 146097                       // [0, 146096]
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365 // [0, 399]
        val y = yoe + era * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)               // [0, 365]
        val mp = (5 * doy + 2) / 153                                     // [0, 11]
        val d = doy - (153 * mp + 2) / 5 + 1                             // [1, 31]
        val m = if (mp < 10) mp + 3 else mp - 9                          // [1, 12]
        return Triple((y + if (m <= 2) 1 else 0).toInt(), m.toInt(), d.toInt())
    }

    /** 年/月/日 -> epochDay */
    fun ymdToEpochDay(year: Int, month: Int, day: Int): Long {
        val y = if (month <= 2) year - 1 else year
        val era = (if (y >= 0) y else y - 399) / 400
        val yoe = y - era * 400
        val doy = (153 * (if (month > 2) month - 3 else month + 9) + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097L + doe - 719468
    }

    /** 格式化为 "2026-10-26" */
    fun format(epochDay: Long): String {
        val (y, m, d) = epochDayToYmd(epochDay)
        return "%04d-%02d-%02d".format(y, m, d)
    }

    /** 当前日期（按本地时区）对应的 epochDay */
    fun todayEpochDay(): Long {
        val now = System.currentTimeMillis()
        // currentTimeMillis 基于 UTC，直接整除在东八区凌晨 0–8 点会算成"昨天"，
        // 因此先加上本地时区偏移再取天数
        val offset = TimeZone.getDefault().getOffset(now).toLong()
        return Math.floorDiv(now + offset, 86_400_000L)
    }

    /** 根据当前日期推断默认学期名，如 "2026-2027学年第一学期" */
    fun currentSemesterName(): String {
        val (year, month, _) = epochDayToYmd(todayEpochDay())
        return if (month >= 8) "${year}-${year + 1}学年第一学期"
        else "${year - 1}-${year}学年第二学期"
    }
}
