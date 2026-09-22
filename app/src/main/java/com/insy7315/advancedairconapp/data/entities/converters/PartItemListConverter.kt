// app/src/main/java/com/insy7315/advancedairconapp/data/converters/PartItemListConverter.kt
package com.insy7315.advancedairconapp.data.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.insy7315.advancedairconapp.data.entities.PartItem

class PartItemListConverter {

    private val gson = Gson()

    @TypeConverter
    fun fromPartItemList(parts: List<PartItem>): String {
        return gson.toJson(parts)
    }

    @TypeConverter
    fun toPartItemList(json: String): List<PartItem> {
        val type = object : TypeToken<List<PartItem>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
}