package ltd.realquick.nitnem.data.model

enum class BaniLength(val prefValue: String) {
    SHORT("s"),
    MEDIUM("m"),
    LONG("l"),
    EXTRA_LONG("xl");

    companion object {
        fun fromPrefValue(value: String?): BaniLength {
            return entries.firstOrNull { it.prefValue == value } ?: MEDIUM
        }
    }
}

data class Bani(
    val id: Int,
    val nameEn: String,
    val slug: String,
    val verses: List<BaniVerse>
)

data class BaniVerse(
    val paragraphId: Int,
    val section: String? = null,
    val line: Line,
    val existsShort: Boolean,
    val existsMedium: Boolean,
    val existsLong: Boolean,
    val existsExtraLong: Boolean
) {
    fun existsIn(length: BaniLength): Boolean {
        return when (length) {
            BaniLength.SHORT -> existsShort
            BaniLength.MEDIUM -> existsMedium
            BaniLength.LONG -> existsLong
            BaniLength.EXTRA_LONG -> existsExtraLong
        }
    }
}

data class Paragraph(
    val id: Int,
    val section: String? = null,
    val lines: List<Line>
)

data class ReaderParagraph(
    val id: Int,
    val section: String? = null,
    val text: String
)

data class Line(
    val pn: String,
    val en: String,
    val hi: String
)

data class BaniInfo(
    val id: Int,
    val nameEn: String,
    val slug: String
)
