package ltd.realquick.nitnem.data

import android.content.Context
import ltd.realquick.nitnem.data.model.Bani
import ltd.realquick.nitnem.data.model.BaniInfo
import ltd.realquick.nitnem.data.model.BaniLength
import ltd.realquick.nitnem.data.model.BaniVerse
import ltd.realquick.nitnem.data.model.Line
import ltd.realquick.nitnem.data.model.Paragraph
import ltd.realquick.nitnem.data.model.ReaderParagraph
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

class BaniRepository(private val context: Context) {

    fun loadBani(slug: String): Bani {
        return cache.getOrPut(slug) {
            val json = context.assets
                .open("banis/$slug.json")
                .bufferedReader()
                .use { it.readText() }
            parseBani(JSONObject(json))
        }
    }

    fun loadReaderParagraphs(
        slug: String,
        length: BaniLength,
        language: String
    ): List<ReaderParagraph> {
        return buildParagraphs(loadBani(slug), length).map { paragraph ->
            ReaderParagraph(
                id = paragraph.id,
                section = paragraph.section,
                text = paragraph.lines.joinToString("\n") { line ->
                    when (language) {
                        "hi" -> line.hi
                        "pn" -> line.pn
                        else -> line.en
                    }
                }
            )
        }
    }

    fun buildParagraphs(bani: Bani, length: BaniLength): List<Paragraph> {
        val paragraphs = linkedMapOf<Int, MutableParagraph>()

        bani.verses.asSequence()
            .filter { it.existsIn(length) }
            .forEach { verse ->
                val paragraph = paragraphs.getOrPut(verse.paragraphId) {
                    MutableParagraph(id = verse.paragraphId)
                }
                if (paragraph.section == null && !verse.section.isNullOrBlank()) {
                    paragraph.section = verse.section
                }
                paragraph.lines += verse.line
            }

        return paragraphs.values.map { paragraph ->
            Paragraph(
                id = paragraph.id,
                section = paragraph.section,
                lines = paragraph.lines
            )
        }
    }

    private fun parseBani(json: JSONObject): Bani {
        val id = json.getInt("id")
        val nameEn = json.getString("nameEn")
        val slug = json.getString("slug")
        val versesArray = json.getJSONArray("verses")

        val verses = (0 until versesArray.length()).map { i ->
            val vObj = versesArray.getJSONObject(i)
            BaniVerse(
                paragraphId = vObj.getInt("p"),
                section = vObj.optString("s").takeIf { it.isNotBlank() },
                line = Line(
                    pn = vObj.getString("pn"),
                    en = vObj.getString("en"),
                    hi = vObj.getString("hi")
                ),
                existsShort = vObj.optBoolean("es", true),
                existsMedium = vObj.optBoolean("em", true),
                existsLong = vObj.optBoolean("el", true),
                existsExtraLong = vObj.optBoolean("ex", true)
            )
        }

        return Bani(id, nameEn, slug, verses)
    }

    companion object {
        val BANI_LIST = listOf(
            BaniInfo(2, "Japji Sahib", "japji-sahib"),
            BaniInfo(3, "Shabad Hazare", "shabad-hazare"),
            BaniInfo(4, "Jaap Sahib", "jaap-sahib"),
            BaniInfo(6, "Tavprasad Savaiye (Sraavag Sudh)", "tavprasad-savaiye-sraavag-sudh"),
            BaniInfo(7, "Tavprasad Savaiye (Deenan Kee)", "tavprasad-savaiye-deenan-kee"),
            BaniInfo(9, "Chaupai Sahib", "chaupai-sahib"),
            BaniInfo(10, "Anand Sahib", "anand-sahib"),
            BaniInfo(21, "Rehras Sahib", "rehras-sahib"),
            BaniInfo(22, "Aarti", "aarti"),
            BaniInfo(23, "Kirtan Sohila", "kirtan-sohila"),
            BaniInfo(24, "Ardas", "ardas"),
            BaniInfo(31, "Sukhmani Sahib", "sukhmani-sahib"),
            BaniInfo(90, "Asa Di Vaar", "asa-di-vaar")
        )

        private val cache = ConcurrentHashMap<String, Bani>()
    }

    private data class MutableParagraph(
        val id: Int,
        var section: String? = null,
        val lines: MutableList<Line> = mutableListOf()
    )
}
