package ltd.realquick.nitnem.data.model

data class Bani(
    val id: Int,
    val nameEn: String,
    val slug: String,
    val paragraphs: List<Paragraph>
)

data class Paragraph(
    val section: String? = null,
    val lines: List<Line>
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
