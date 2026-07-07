package com.example.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object LocalStorage {
    private const val PREFS_NAME = "web_apk_local_storage"
    private const val RECENT_CONFIGS_KEY = "recent_website_configurations"

    fun setItem(context: Context, key: String, value: String) {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPref.edit().putString(key, value).apply()
    }

    fun getItem(context: Context, key: String): String? {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPref.getString(key, null)
    }

    fun removeItem(context: Context, key: String) {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPref.edit().remove(key).apply()
    }

    fun clear(context: Context) {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPref.edit().clear().apply()
    }

    // High-level helper specifically for saving recent website configurations offline
    fun saveRecentConfiguration(context: Context, name: String, url: String, packageName: String, apkFileName: String) {
        try {
            val currentData = getItem(context, RECENT_CONFIGS_KEY)
            val jsonArray = if (currentData != null) {
                JSONArray(currentData)
            } else {
                JSONArray()
            }

            // Create new entry
            val newConfig = JSONObject().apply {
                put("name", name)
                put("url", url)
                put("packageName", packageName)
                put("apkFileName", apkFileName)
                put("timestamp", System.currentTimeMillis())
            }

            // Remove any existing duplicate by url
            val newList = ArrayList<JSONObject>()
            newList.add(newConfig)
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                if (item.getString("url") != url) {
                    newList.add(item)
                }
            }

            // Keep only top 10 configurations
            val limitedList = newList.take(10)
            val updatedArray = JSONArray()
            limitedList.forEach { updatedArray.put(it) }

            setItem(context, RECENT_CONFIGS_KEY, updatedArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getRecentConfigurations(context: Context): List<RecentConfig> {
        val list = mutableListOf<RecentConfig>()
        try {
            val data = getItem(context, RECENT_CONFIGS_KEY) ?: return emptyList()
            val jsonArray = JSONArray(data)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    RecentConfig(
                        name = obj.getString("name"),
                        url = obj.getString("url"),
                        packageName = obj.getString("packageName"),
                        apkFileName = obj.optString("apkFileName", ""),
                        timestamp = obj.optLong("timestamp", 0L)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}

data class RecentConfig(
    val name: String,
    val url: String,
    val packageName: String,
    val apkFileName: String,
    val timestamp: Long
)
