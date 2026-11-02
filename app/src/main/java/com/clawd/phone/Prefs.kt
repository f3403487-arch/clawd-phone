package com.clawd.phone

import android.content.Context
import android.content.SharedPreferences

/** Все настройки в одном месте: ключ → значение по умолчанию, как get_setting в плагине. */
class Prefs(context: Context) {
    val sp: SharedPreferences = context.getSharedPreferences("clawd", Context.MODE_PRIVATE)

    companion object {
        val DEFAULTS: Map<String, Any> = mapOf(
            "enabled" to false,
            "size" to 1,
            "color" to 0,
            "gravity" to false,
            "pos_x" to 800,
            "pos_y" to 720,
            "rotate" to true,
            "wander" to true,
            "phrases" to true,
            "sleep" to 1,
            "eat" to true,
            "poop" to true,
            "shake" to true,
            "events" to true,
            "battery" to true,
            "notify" to true,
            "dance" to true,
            "haptics" to true,
            "moods_chat" to true,
            "hide_fullscreen" to true,
            "merge" to true,
            "screen_react" to true,
            "screen_text" to true,
            "screen_time" to true,
            "acc" to 0,
            "hunger" to true,
            "buddy" to true,
            "buddy_name" to "Клодик",
            "buddy_color" to 2,
            "commands" to true,
            "suggest_auto" to false,
            "brain_enabled" to true,
            "brain_name" to "",
            "brain_about" to "",
            "brain_learn" to true,
            "brain_share" to true,
            "ai_enabled" to false,
            "ai_provider" to 0,
            "ai_key" to "",
            "ai_model" to "",
            "ai_url" to "",
            "ai_chat" to 1,
            "ai_moods" to true,
            "ai_move" to true,
            "ai_text" to false,
            "ai_persona" to "",
            "ai2_url" to "",
            "ai2_key" to "",
            "ai2_model" to "",
            "ai_fallback" to true,
            "pets_count" to 0,
            "eaten_count" to 0,
            "poops_cleaned" to 0,
            "food_level" to 700,
            "food_ts" to 0L,
            "acc_seen" to -1,
            "sync" to true,
            "pc_link" to true,
            "pc_notify" to true,
            "pc_notify_text" to false,
            "pc_ip" to "",
            "pc_port" to 8769,
            "pc_seen" to 0L,
            "pc_pets" to 0,
            "pc_clean" to 0,
            "pc_throws" to 0,
            "sync_code" to "",
            "sync_last" to 0L,
            "ach" to "",
            "ball_kicks" to 0,
            "pomo_done" to 0,
            "remind_done" to 0,
            "tag_wins" to 0,
            "seek_found" to 0,
            "bath_count" to 0,
            "mood_level" to 700,
            "energy_level" to 800,
            "clean_level" to 800,
            "stats_ts" to 0L,
            "home" to true,
            "weather" to false,
            "city" to "",
            "geo_city" to "",
            "lat" to "",
            "lon" to "",
            "tts" to false,
            "reminders" to "[]",
            "morning_day" to "",
        )
    }

    fun b(key: String) = sp.getBoolean(key, DEFAULTS[key] as? Boolean ?: false)
    fun i(key: String) = sp.getInt(key, DEFAULTS[key] as? Int ?: 0)
    fun s(key: String) = sp.getString(key, DEFAULTS[key] as? String ?: "") ?: ""
    fun l(key: String) = sp.getLong(key, DEFAULTS[key] as? Long ?: 0L)

    fun put(key: String, value: Any) {
        val e = sp.edit()
        when (value) {
            is Boolean -> e.putBoolean(key, value)
            is Int -> e.putInt(key, value)
            is Long -> e.putLong(key, value)
            is Float -> e.putFloat(key, value)
            else -> e.putString(key, value.toString())
        }
        e.apply()
    }

    fun inc(key: String) = put(key, i(key) + 1)

    var enabled: Boolean
        get() = b("enabled")
        set(value) = put("enabled", value)
}
