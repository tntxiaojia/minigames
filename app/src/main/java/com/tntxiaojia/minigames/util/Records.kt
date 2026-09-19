package com.tntxiaojia.minigames.util

import android.content.Context

/** 各游戏历史最高分（简单的 SharedPreferences 存取）。 */
object Records {
    private const val PREFS = "minigames_records"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun best(context: Context, key: String): Int = prefs(context).getInt(key, 0)

    /** 写入并返回更新后的最高分。 */
    fun submit(context: Context, key: String, score: Int): Int {
        val p = prefs(context)
        val best = maxOf(p.getInt(key, 0), score)
        p.edit().putInt(key, best).apply()
        return best
    }
}
