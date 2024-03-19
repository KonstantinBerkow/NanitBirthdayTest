package io.github.konstantinberkow.nanitbirthdaytest.network

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

private const val NAME_KEY = "name"
private const val DATE_KEY = "dob"
private const val THEME_KEY = "theme"

class BirthdayMessageGsonAdapter : JsonDeserializer<BirthdayMessage> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): BirthdayMessage {
        val jsonObject = json.asJsonObject
        val name = jsonObject.get(NAME_KEY).asString
        val dob = jsonObject.get(DATE_KEY).asLong
        val rawTheme = jsonObject.get(THEME_KEY).asString
        val theme = rawTheme?.let { BirthdayMessage.Theme.valueOf(it) }

        if (name != null && dob != 0L && theme != null) {
            return BirthdayMessage(
                name = name,
                dateOfBirth = dob,
                theme = theme
            )
        }

        throw JsonParseException("Failed to parse BirthdayMessage, name: $name, dob: $dob, theme: $rawTheme")
    }
}