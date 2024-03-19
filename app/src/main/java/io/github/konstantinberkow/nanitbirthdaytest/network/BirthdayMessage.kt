package io.github.konstantinberkow.nanitbirthdaytest.network

data class BirthdayMessage(
    val name: String,
    val dateOfBirth: Long,
    val theme: Theme
) {

    enum class Theme(val rawValue: String) {
        Fox("fox"),
        Elephant("elephant"),
        Pelican("pelican");

        fun fromString(raw: String): Theme? {
            return values().firstOrNull { it.rawValue == raw }
        }
    }
}
