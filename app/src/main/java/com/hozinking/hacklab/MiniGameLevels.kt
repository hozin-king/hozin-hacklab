package com.hozinking.hacklab

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONArray

// Level definitions for the five offline mini-games. Every level is data:
// the engines only interpret it, so new levels can arrive later through
// the online catalog (LessonCatalog) and parse into these same classes.
// Online level JSON uses the same field names as the data classes below.

// Code Combat ("Misi Kode"): a grid. Legend: '#' wall, '.' empty,
// 'H' hero start, 'G' gem, 'T' target. The hero starts facing east.
data class CombatLevel(
    val id: String,
    val name: String,
    val grid: List<String>,
    val hint: String
)

// Bitburner ("Retas Server"): guess the 4-digit PIN after connecting
// to an open port. maxTries caps the guesses per attempt.
data class HackLevel(
    val id: String,
    val name: String,
    val server: String,
    val pin: String,
    val ports: List<Int>,
    val maxTries: Int,
    val hint: String
)

// CodinGame ("Debug Kode"): a short program with exactly one bug.
// bugLine is 1-based. The player picks which line is wrong.
data class DebugLevel(
    val id: String,
    val name: String,
    val code: List<String>,
    val bugLine: Int,
    val explain: String
)

// Screeps ("Program Bot"): a grid. Legend: '#' wall, '.' empty,
// 'B' bot start, 'R' resource, 'H' home base. The bot carries at most
// 2 resources. budget is the total cell moves allowed per run.
data class BotLevel(
    val id: String,
    val name: String,
    val grid: List<String>,
    val budget: Int,
    val hint: String
)

// Opus Magnum ("Mesin Alkimia"): transform start into target by tapping
// operations in order. A rule only fires on its exact input material.
// maxSteps caps the taps per attempt.
data class AlchemyRule(val op: String, val from: String, val to: String)

data class AlchemyLevel(
    val id: String,
    val name: String,
    val start: String,
    val target: String,
    val ops: List<String>,
    val rules: List<AlchemyRule>,
    val maxSteps: Int
)

// One row in the level picker: the same for every game.
data class LevelRef(val id: String, val name: String)

object MiniGameLevels {

    private val mainHandler = Handler(Looper.getMainLooper())

    // Same as levelRefs, but the online-level parse runs on a background
    // thread and the result is posted to the main thread.
    fun levelRefsAsync(context: Context, gameId: String, onDone: (List<LevelRef>) -> Unit) {
        Thread {
            val refs = levelRefs(context, gameId)
            mainHandler.post { onDone(refs) }
        }.start()
    }

    // Finds one level by id for a game, with the parse on a background
    // thread. Returns the typed level as Any (the caller casts it in
    // its when on game id), or null when the level does not exist.
    fun findLevelAsync(context: Context, gameId: String, levelId: String, onDone: (Any?) -> Unit) {
        Thread {
            val level: Any? = when (gameId) {
                "code-combat" -> combatLevels(context).firstOrNull { it.id == levelId }
                "bitburner" -> hackLevels(context).firstOrNull { it.id == levelId }
                "codingame" -> debugLevels(context).firstOrNull { it.id == levelId }
                "screeps" -> botLevels(context).firstOrNull { it.id == levelId }
                "opus-magnum" -> alchemyLevels(context).firstOrNull { it.id == levelId }
                else -> null
            }
            mainHandler.post { onDone(level) }
        }.start()
    }

    // Code Combat: 3 built-in levels. Solutions were verified offline
    // with a simulator before shipping (see the v1.3 build notes).
    private val BUILTIN_COMBAT = listOf(
        CombatLevel(
            "cc-1", "Ambil Permata Pertama",
            listOf("#####", "#H.G#", "#...#", "#..T#", "#####"),
            "Perintah: maju, kiri, kanan, ambil. Ambil permata dulu, baru ke target."
        ),
        CombatLevel(
            "cc-2", "Belok dan Ambil",
            listOf("#####", "#H..#", "#.G.#", "#..T#", "#####"),
            "Kadang jalan lurus bukan jawabannya. Putar dulu, baru maju."
        ),
        CombatLevel(
            "cc-3", "Dua Permata",
            listOf("#####", "#H#G#", "#...#", "#G.T#", "#####"),
            "Dua permata, satu dinding di tengah. Rencanakan rutenya dulu."
        )
    )

    private val BUILTIN_HACK = listOf(
        HackLevel(
            "bb-1", "Server Web", "web-srv-01", "1234",
            listOf(22, 80), 8,
            "PIN-nya urutan angka yang paling gampang ditebak."
        ),
        HackLevel(
            "bb-2", "Server Database", "db-srv-02", "4826",
            listOf(21, 3306, 8080), 7,
            "Tebak pakai petunjuk lebih besar / lebih kecil."
        ),
        HackLevel(
            "bb-3", "Vault", "vault-03", "9021",
            listOf(443, 1337), 6,
            "PIN-nya dimulai angka 9."
        )
    )

    private val BUILTIN_DEBUG = listOf(
        DebugLevel(
            "cg-1", "Titik Dua Hilang",
            listOf(
                "hero = \"pemula\"",
                "for i in range(3)",
                "    print(hero)",
                "print(\"selesai\")"
            ),
            2,
            "Baris for harus diakhiri titik dua (:) sebelum blok indentasi."
        ),
        DebugLevel(
            "cg-2", "Else Tanpa Titik Dua",
            listOf(
                "skor = 10",
                "if skor > 5:",
                "    print(\"lolos\")",
                "else",
                "    print(\"gagal\")"
            ),
            4,
            "else juga harus diakhiri titik dua (:)."
        ),
        DebugLevel(
            "cg-3", "Satu Terlalu Banyak",
            listOf(
                "for (let i = 0; i <= 3; i++) {",
                "    console.log(i);",
                "}",
                "// diharapkan: 0 1 2"
            ),
            1,
            "i <= 3 membuat loop jalan 4 kali (0 1 2 3). Pakai i < 3 supaya berhenti di 2."
        )
    )

    private val BUILTIN_BOT = listOf(
        BotLevel(
            "sc-1", "Antar Satu Resource",
            listOf(".....", ".B.R.", ".....", "...H.", "....."),
            10,
            "Susun: kumpul, lalu antar."
        ),
        BotLevel(
            "sc-2", "Dua Resource Sekaligus",
            listOf(".....", ".B.R.", ".....", ".R.H.", "....."),
            16,
            "Bot bisa membawa 2 resource. Kumpul dua kali, baru antar."
        ),
        BotLevel(
            "sc-3", "Tiga Resource, Satu Ulang",
            listOf(".....", ".B.R.", "..#..", ".R.H.", "..R.."),
            20,
            "Tiga resource, bot cuma bawa 2. Pakai ulang untuk mengulang program."
        )
    )

    private val BUILTIN_ALCHEMY = listOf(
        AlchemyLevel(
            "om-1", "Baja Pertama",
            "Bijih Besi", "Baja",
            listOf("panaskan", "tempa", "dinginkan"),
            listOf(
                AlchemyRule("panaskan", "Bijih Besi", "Besi"),
                AlchemyRule("tempa", "Besi", "Baja"),
                AlchemyRule("dinginkan", "Besi", "Besi Dingin")
            ),
            4
        ),
        AlchemyLevel(
            "om-2", "Air Murni",
            "Air", "Air Murni",
            listOf("panaskan", "dinginkan", "saring", "bekukan"),
            listOf(
                AlchemyRule("panaskan", "Air", "Uap"),
                AlchemyRule("dinginkan", "Uap", "Air Suling"),
                AlchemyRule("saring", "Air Suling", "Air Murni"),
                AlchemyRule("bekukan", "Air", "Es")
            ),
            5
        ),
        AlchemyLevel(
            "om-3", "Kaca Optik",
            "Pasir", "Kaca Optik",
            listOf("panaskan", "dinginkan", "poles", "campur", "tekan"),
            listOf(
                AlchemyRule("panaskan", "Pasir", "Kaca Cair"),
                AlchemyRule("dinginkan", "Kaca Cair", "Kaca"),
                AlchemyRule("poles", "Kaca", "Kaca Optik"),
                AlchemyRule("campur", "Pasir", "Pasir Campur"),
                AlchemyRule("tekan", "Kaca", "Kaca Retak")
            ),
            6
        )
    )

    fun levelRefs(context: Context, gameId: String): List<LevelRef> = when (gameId) {
        "code-combat" -> combatLevels(context).map { LevelRef(it.id, it.name) }
        "bitburner" -> hackLevels(context).map { LevelRef(it.id, it.name) }
        "codingame" -> debugLevels(context).map { LevelRef(it.id, it.name) }
        "screeps" -> botLevels(context).map { LevelRef(it.id, it.name) }
        "opus-magnum" -> alchemyLevels(context).map { LevelRef(it.id, it.name) }
        else -> emptyList()
    }

    fun combatLevels(context: Context): List<CombatLevel> {
        val online = parseOnline(context, "code-combat") { o ->
            val grid = jsonStrings(o.optJSONArray("grid"))
            if (grid.isEmpty()) null else CombatLevel(
                o.optString("id", ""),
                o.optString("name", ""),
                grid,
                o.optString("hint", "")
            )
        }
        return BUILTIN_COMBAT + online.filter { o -> BUILTIN_COMBAT.none { it.id == o.id } }
    }

    fun hackLevels(context: Context): List<HackLevel> {
        val online = parseOnline(context, "bitburner") { o ->
            val ports = ArrayList<Int>()
            val arr = o.optJSONArray("ports")
            if (arr != null) for (i in 0 until arr.length()) ports.add(arr.optInt(i))
            if (ports.isEmpty()) null else HackLevel(
                o.optString("id", ""),
                o.optString("name", ""),
                o.optString("server", ""),
                o.optString("pin", ""),
                ports,
                o.optInt("maxTries", 6),
                o.optString("hint", "")
            )
        }
        return BUILTIN_HACK + online.filter { o -> BUILTIN_HACK.none { it.id == o.id } }
    }

    fun debugLevels(context: Context): List<DebugLevel> {
        val online = parseOnline(context, "codingame") { o ->
            val code = jsonStrings(o.optJSONArray("code"))
            if (code.isEmpty()) null else DebugLevel(
                o.optString("id", ""),
                o.optString("name", ""),
                code,
                o.optInt("bugLine", 0),
                o.optString("explain", "")
            )
        }
        return BUILTIN_DEBUG + online.filter { o -> BUILTIN_DEBUG.none { it.id == o.id } }
    }

    fun botLevels(context: Context): List<BotLevel> {
        val online = parseOnline(context, "screeps") { o ->
            val grid = jsonStrings(o.optJSONArray("grid"))
            if (grid.isEmpty()) null else BotLevel(
                o.optString("id", ""),
                o.optString("name", ""),
                grid,
                o.optInt("budget", 20),
                o.optString("hint", "")
            )
        }
        return BUILTIN_BOT + online.filter { o -> BUILTIN_BOT.none { it.id == o.id } }
    }

    fun alchemyLevels(context: Context): List<AlchemyLevel> {
        val online = parseOnline(context, "opus-magnum") { o ->
            val ops = jsonStrings(o.optJSONArray("ops"))
            val rules = ArrayList<AlchemyRule>()
            val arr = o.optJSONArray("rules")
            if (arr != null) for (i in 0 until arr.length()) {
                val r = arr.optJSONObject(i) ?: continue
                rules.add(
                    AlchemyRule(
                        r.optString("op", ""),
                        r.optString("from", ""),
                        r.optString("to", "")
                    )
                )
            }
            if (ops.isEmpty() || rules.isEmpty()) null else AlchemyLevel(
                o.optString("id", ""),
                o.optString("name", ""),
                o.optString("start", ""),
                o.optString("target", ""),
                ops,
                rules,
                o.optInt("maxSteps", 6)
            )
        }
        return BUILTIN_ALCHEMY + online.filter { o -> BUILTIN_ALCHEMY.none { it.id == o.id } }
    }

    // Parses the cached online "levels" array for one game id into typed
    // levels. Bad entries are skipped; a bad payload yields an empty list.
    private fun <T> parseOnline(
        context: Context,
        gameId: String,
        make: (org.json.JSONObject) -> T?
    ): List<T> {
        val raw = LessonCatalog.onlineLevelsJson(context, gameId) ?: return emptyList()
        val out = ArrayList<T>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val id = o.optString("id", "")
                if (id.isEmpty()) continue
                val made = make(o) ?: continue
                out.add(made)
            }
        } catch (e: Exception) {
            // Corrupt cache: ignore, built-ins still work.
        }
        return out
    }
}
