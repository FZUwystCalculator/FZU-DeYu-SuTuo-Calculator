package com.fzu.wuyu.data

import android.content.Context
import android.content.SharedPreferences

/** 主题模式：浅色 / 深色 / 跟随系统 */
enum class ThemeMode {
    LIGHT, DARK, SYSTEM;

    val displayName: String
        get() = when (this) {
            LIGHT -> "浅色"
            DARK -> "深色"
            SYSTEM -> "跟随系统"
        }
}

/** 主题偏好持久化（SharedPreferences，本地保存，重启后保持） */
class ThemePrefs(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("wuyu_settings", Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() = try {
            ThemeMode.valueOf(prefs.getString(KEY_MODE, null) ?: ThemeMode.SYSTEM.name)
        } catch (_: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
        set(value) {
            prefs.edit().putString(KEY_MODE, value.name).apply()
        }

    /** 记录排序：true = 按活动时间倒序（新→旧），false = 正序（旧→新） */
    var recordSortDescending: Boolean
        get() = prefs.getBoolean(KEY_SORT_DESC, true)
        set(value) {
            prefs.edit().putBoolean(KEY_SORT_DESC, value).apply()
        }

    /** 记录页「按类别」分组开关：true = 按 A–F、S 分层展示，false = 全部记录平铺 */
    var recordsGroupByCategory: Boolean
        get() = prefs.getBoolean(KEY_GROUP_BY_CATEGORY, true)
        set(value) {
            prefs.edit().putBoolean(KEY_GROUP_BY_CATEGORY, value).apply()
        }

    private companion object {
        const val KEY_MODE = "theme_mode"
        const val KEY_SORT_DESC = "record_sort_desc"
        const val KEY_GROUP_BY_CATEGORY = "records_group_by_category"
    }
}
