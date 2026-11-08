package com.hozinking.hacklab

/**
 * Fase 2: belajar ngoding lewat game.
 *
 * Lima game dari video Rayden "5 game bikin jago ngoding & hacking"
 * (92 detik, ditranskrip 4 Nov 2026). Bukan misi berurutan seperti
 * Fase 1 — semua game terbuka, tanpa kunci. Sejak v1.3 tiap game
 * adalah mini-game offline yang bisa dimainkan di dalam aplikasi
 * (lihat MiniGameLevels); tombol situs resmi tetap ada sebagai
 * pelengkap, bukan satu-satunya jalan main.
 *
 * icon adalah 2 huruf untuk tile di daftar ala Play Store, color
 * warna tile itu. id dipakai katalog online untuk menambah level.
 */
data class GameDef(
    val index: Int,
    val name: String,
    val tagline: String,
    val about: List<String>,
    val skills: String,
    val language: String,
    val level: String,
    val url: String,
    val id: String,
    val icon: String,
    val color: String
)

object Games {

    const val COUNT = 5

    val ALL: List<GameDef> = listOf(
        GameDef(
            index = 0,
            name = "Code Combat",
            tagline = "Belajar ngoding sambil main game petualangan.",
            about = listOf(
                "Kamu menggerakkan karakter dengan baris kode, bukan dengan " +
                    "tombol. Tiap level adalah misi kecil: ambil permata, " +
                    "hindari jebakan, kalahkan musuh — semua dengan perintah " +
                    "yang kamu tulis sendiri.",
                "Cocok untuk yang baru pertama kali melihat kode. Kalau " +
                    "salah ketik, gamenya langsung menunjukkan apa yang " +
                    "terjadi, jadi kamu belajar dari kesalahanmu sendiri."
            ),
            skills = "Dasar Python/JavaScript, logika urutan perintah, " +
                "dan pemecahan masalah selangkah demi selangkah.",
            language = "Python / JavaScript",
            level = "Pemula",
            url = "https://codecombat.com",
            id = "code-combat",
            icon = "CC",
            color = "#00C853"
        ),
        GameDef(
            index = 1,
            name = "Bitburner",
            tagline = "Jadi hacker di dunia virtual lewat script.",
            about = listOf(
                "Di dunia Bitburner, kamu menulis script JavaScript untuk " +
                    "meretas server virtual, mencuri data, dan mengotomatisasi " +
                    "semuanya supaya berjalan sendiri.",
                "Sambil bermain kamu belajar mengelola resource dan menghadapi " +
                    "tantangan teknis yang makin sulit — seperti hacker " +
                    "sungguhan, tapi di dunia yang aman."
            ),
            skills = "JavaScript, menulis script otomatisasi, " +
                "dan berpikir sistem.",
            language = "JavaScript",
            level = "Menengah",
            url = "https://bitburner.com",
            id = "bitburner",
            icon = "BB",
            color = "#7C4DFF"
        ),
        GameDef(
            index = 2,
            name = "CodinGame",
            tagline = "Latihan logika lewat puzzle dan duel kode.",
            about = listOf(
                "CodinGame memberi teka-teki programming yang makin lama " +
                    "makin sulit. Kamu menulis kode untuk memecahkan tiap " +
                    "puzzle, lalu melihat solusimu berjalan.",
                "Ada juga mode duel: adu kode dengan pemain lain dalam " +
                    "pertandingan singkat. Cocok untuk melatih logika sampai tajam."
            ),
            skills = "Logika, algoritma, " +
                "dan latihan banyak bahasa pemrograman.",
            language = "Banyak bahasa",
            level = "Pemula – Menengah",
            url = "https://www.codingame.com",
            id = "codingame",
            icon = "CG",
            color = "#2962FF"
        ),
        GameDef(
            index = 3,
            name = "Screeps: World",
            tagline = "Tulis AI yang memainkan gamenya untukmu.",
            about = listOf(
                "Di Screeps, kamu tidak menggerakkan unit satu per satu. " +
                    "Kamu menulis AI dalam JavaScript yang mengontrol unitmu: " +
                    "mengumpulkan resource, membangun, dan bertahan dari " +
                    "pemain lain.",
                "Dunianya jalan terus, bahkan saat kamu offline. Jadi yang " +
                    "kamu latih adalah strategi jangka panjang, bukan refleks."
            ),
            skills = "JavaScript, AI dan otomatisasi, " +
                "serta strategi jangka panjang.",
            language = "JavaScript",
            level = "Lanjutan",
            url = "https://screeps.com",
            id = "screeps",
            icon = "SC",
            color = "#FF6D00"
        ),
        GameDef(
            index = 4,
            name = "Opus Magnum",
            tagline = "Rancang mesin alkimia yang paling efisien.",
            about = listOf(
                "Opus Magnum adalah game puzzle dari Zachtronics. Kamu " +
                    "merancang mesin dari lengan mekanik dan jalur produksi " +
                    "untuk membuat bahan alkimia.",
                "Tidak ada kode yang ditulis — tantangannya murni logika: " +
                    "bagaimana membuat mesin yang paling ringkas, cepat, dan " +
                    "murah. Cocok untuk melatih cara berpikir seperti programmer."
            ),
            skills = "Logika algoritma, otomatisasi, dan optimasi.",
            language = "Logika visual (tanpa kode)",
            level = "Menengah",
            url = "https://www.zachtronics.com/opus-magnum/",
            id = "opus-magnum",
            icon = "OM",
            color = "#C51162"
        )
    )

    fun get(index: Int): GameDef? {
        if (index < 0 || index >= ALL.size) return null
        return ALL[index]
    }
}
