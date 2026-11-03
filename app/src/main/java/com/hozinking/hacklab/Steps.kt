package com.hozinking.hacklab

/**
 * The virtual filesystem and the six steps of the only learning path.
 *
 * The filesystem is hardcoded here and never reads the real device:
 * the simulator is a safe sandbox, not a real shell. Directories keep
 * insertion order so `ls` output is stable and easy to follow.
 */
sealed class VNode {
    class Dir(val children: LinkedHashMap<String, VNode>) : VNode()
    class File(val content: String) : VNode()
}

private fun dir(vararg entries: Pair<String, VNode>): VNode.Dir {
    val map = LinkedHashMap<String, VNode>()
    for ((name, node) in entries) {
        map[name] = node
    }
    return VNode.Dir(map)
}

private fun file(content: String): VNode.File {
    return VNode.File(content)
}

/**
 * One step: short lessons, one mission, one hint, and a predicate that
 * decides — from the terminal session alone — whether the mission is done.
 * A step is never marked finished unless its predicate is really true.
 */
data class StepDef(
    val index: Int,
    val title: String,
    val lessons: List<String>,
    val mission: String,
    val hint: String,
    val check: (TerminalEngine) -> Boolean
)

object Steps {

    const val COUNT = 6

    const val HOME_DIR = "/home/pemula"

    /** The whole virtual world. Root "/" contains only home/pemula. */
    fun buildFileSystem(): VNode.Dir {
        return dir(
            "home" to dir(
                "pemula" to dir(
                    "dokumen" to dir(
                        "catatan.txt" to file(
                            "Halo, pemula!\n" +
                                "Selamat datang di terminal. Semua hal besar " +
                                "di dunia komputer dimulai dari satu perintah " +
                                "kecil yang kamu ketik sendiri."
                        )
                    ),
                    "latihan" to dir(
                        "latihan1.txt" to file(
                            "Ini file latihan.\n" +
                                "Coba baca file ini dengan cat kalau kamu mau berlatih."
                        )
                    ),
                    "brankas" to dir(
                        "flag.txt" to file(
                            "FLAG{terminal_pertamaku}\n" +
                                "Kamu menemukannya! Ingat: skill ini hanya dipakai " +
                                "di lab sendiri atau target yang memberi izin."
                        )
                    )
                )
            )
        )
    }

    val ALL: List<StepDef> = listOf(
        StepDef(
            index = 0,
            title = "Kenalan dengan Terminal",
            lessons = listOf(
                "Terminal adalah cara ngobrol dengan komputer lewat tulisan. " +
                    "Kamu mengetik perintah, komputer menjawab dengan tulisan juga. " +
                    "Tidak ada tombol, tidak ada ikon — hanya teks.",
                "Dua perintah pertama: pwd (kependekan dari print working directory) " +
                    "menunjukkan folder tempat kamu berada sekarang. whoami " +
                    "menunjukkan nama pengguna yang sedang memakai terminal.",
                "Di simulator ini, kamu adalah pengguna bernama pemula, dan kamu " +
                    "mulai dari folder /home/pemula. Folder home adalah folder pribadimu."
            ),
            mission = "Jalankan pwd dan whoami.",
            hint = "Ketik pwd, tekan Kirim, lalu ketik whoami.",
            check = { engine ->
                engine.ran.contains("pwd") && engine.ran.contains("whoami")
            }
        ),
        StepDef(
            index = 1,
            title = "Melihat Isi Folder",
            lessons = listOf(
                "Folder bisa berisi file dan folder lain. Untuk melihat isinya, " +
                    "pakai perintah ls (kependekan dari list).",
                "ls menampilkan nama-nama di folder tempat kamu berada. Nama yang " +
                    "diakhiri garis miring (/) adalah folder, sisanya adalah file.",
                "Kamu sedang berada di home. Lihat sendiri apa saja yang ada di dalamnya."
            ),
            mission = "Jalankan ls saat berada di folder home.",
            hint = "Kamu sudah di home. Ketik saja ls.",
            check = { engine ->
                engine.lsDirs.contains(HOME_DIR)
            }
        ),
        StepDef(
            index = 2,
            title = "Pindah Folder",
            lessons = listOf(
                "Untuk masuk ke folder lain, pakai cd (kependekan dari change directory), " +
                    "diikuti nama foldernya. Contoh: cd dokumen.",
                "Setelah pindah, pwd akan menunjukkan folder barumu. Perintah lain " +
                    "seperti ls juga akan berlaku di folder baru itu."
            ),
            mission = "Pindah ke folder dokumen (akhiri di dalam folder dokumen).",
            hint = "Ketik cd dokumen.",
            check = { engine ->
                engine.cwd == "$HOME_DIR/dokumen"
            }
        ),
        StepDef(
            index = 3,
            title = "Membaca File",
            lessons = listOf(
                "File teks bisa dibaca langsung dari terminal dengan cat, diikuti " +
                    "nama file. Contoh: cat catatan.txt.",
                "cat hanya bisa membaca file, bukan folder. Kalau kamu mencoba " +
                    "membaca folder, simulator akan memberitahumu.",
                "Di dalam folder dokumen ada file bernama catatan.txt. Bacalah isinya."
            ),
            mission = "Baca file catatan.txt di dalam folder dokumen.",
            hint = "Pastikan kamu di folder dokumen dulu, lalu ketik cat catatan.txt.",
            check = { engine ->
                engine.readFiles.contains("$HOME_DIR/dokumen/catatan.txt")
            }
        ),
        StepDef(
            index = 4,
            title = "Jejak dan Bantuan",
            lessons = listOf(
                "Dua perintah penolong: help menampilkan daftar perintah yang dikenal " +
                    "simulator ini, beserta penjelasan singkatnya.",
                "clear membersihkan layar dari tulisan-tulisan lama, supaya kamu bisa " +
                    "mulai dari layar kosong lagi. Riwayat perintahmu tetap tersimpan " +
                    "untuk misi."
            ),
            mission = "Jalankan help, lalu clear.",
            hint = "Ketik help dulu, setelah itu ketik clear.",
            check = { engine ->
                // True when a "clear" runs at any point after a "help".
                // (First-occurrence index comparison would be wrong if the
                // user cleared once before ever asking for help.)
                var seenHelp = false
                var done = false
                for (cmd in engine.history) {
                    if (cmd == "help") seenHelp = true
                    if (cmd == "clear" && seenHelp) done = true
                }
                done
            }
        ),
        StepDef(
            index = 5,
            title = "Misi Akhir: Temukan Flag",
            lessons = listOf(
                "Dalam dunia keamanan komputer, flag adalah potongan teks rahasia yang " +
                    "harus ditemukan — biasanya sebagai bukti kamu berhasil " +
                    "menyelesaikan tantangan.",
                "Flag di simulator ini tersembunyi di folder brankas. Untuk sampai " +
                    "ke sana, gabungkan semua yang sudah kamu pelajari: pindah folder " +
                    "dengan cd, lihat isi dengan ls, baca file dengan cat."
            ),
            mission = "Baca file brankas/flag.txt.",
            hint = "Dari home, ketik cd brankas, lalu cat flag.txt.",
            check = { engine ->
                engine.readFiles.contains("$HOME_DIR/brankas/flag.txt")
            }
        )
    )

    fun get(index: Int): StepDef? {
        if (index < 0 || index >= ALL.size) return null
        return ALL[index]
    }
}
