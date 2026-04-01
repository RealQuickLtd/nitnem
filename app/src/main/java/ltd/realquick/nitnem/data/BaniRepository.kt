package ltd.realquick.nitnem.data

import android.content.Context
import ltd.realquick.nitnem.data.model.Bani
import ltd.realquick.nitnem.data.model.BaniInfo
import ltd.realquick.nitnem.data.model.Line
import ltd.realquick.nitnem.data.model.Paragraph
import org.json.JSONObject

class BaniRepository(private val context: Context) {

    fun loadBani(slug: String): Bani {
        val json = context.assets
            .open("banis/$slug.json")
            .bufferedReader()
            .use { it.readText() }
        return parseBani(JSONObject(json))
    }

    private fun parseBani(json: JSONObject): Bani {
        val id = json.getInt("id")
        val nameEn = json.getString("nameEn")
        val slug = json.getString("slug")
        val paragraphsArray = json.getJSONArray("paragraphs")

        val paragraphs = (0 until paragraphsArray.length()).map { i ->
            val pObj = paragraphsArray.getJSONObject(i)
            val section = if (pObj.has("section") && !pObj.isNull("section"))
                pObj.getString("section") else null
            val linesArray = pObj.getJSONArray("lines")
            val lines = (0 until linesArray.length()).map { j ->
                val lObj = linesArray.getJSONObject(j)
                Line(
                    pn = lObj.getString("pn"),
                    en = lObj.getString("en"),
                    hi = lObj.getString("hi")
                )
            }
            Paragraph(section = section, lines = lines)
        }

        return Bani(id, nameEn, slug, paragraphs)
    }

    companion object {
        val BANI_LIST = listOf(
            BaniInfo(2, "Japji Sahib", "japji-sahib"),
            BaniInfo(4, "Jaap Sahib", "jaap-sahib"),
            BaniInfo(31, "Sukhmani Sahib", "sukhmani-sahib"),
            BaniInfo(3, "Shabad Hazare", "shabad-hazare"),
            BaniInfo(9, "Chaupai Sahib", "chaupai-sahib"),
            BaniInfo(21, "Rehras Sahib", "rehras-sahib"),
            BaniInfo(23, "Kirtan Sohila", "kirtan-sohila"),
            BaniInfo(10, "Anand Sahib", "anand-sahib"),
            BaniInfo(6, "Tavprasad Savaiye (Sraavag Sudh)", "tavprasad-savaiye-sraavag-sudh"),
            BaniInfo(7, "Tavprasad Savaiye (Deenan Kee)", "tavprasad-savaiye-deenan-kee")
        )
    }
}
