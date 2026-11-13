package com.hozinking.hacklab

/**
 * Fase 3: Lab Keamanan. Tujuh lab simulasi offline — semuanya menyerang
 * target PALSU di dalam aplikasi sendiri. Tidak ada paket yang dikirim ke
 * jaringan nyata, tidak ada situs nyata yang dibuka, tidak ada kredensial
 * nyata yang disentuh. Lab WiFi menegaskan hal ini di teorinya: Android
 * tanpa hardware mode-monitor memang tidak bisa menginjeksi frame 802.11
 * mentah.
 *
 * Lab terbuka semua seperti Fase 2 (tanpa kunci berurutan). Progres
 * disimpan LabStore dengan kunci baru, tidak menyentuh kunci Fase 1/2.
 */
data class LabDef(
    val index: Int,
    val id: String,
    val name: String,
    val focus: String,
    val title: String,
    val tagline: String,
    val icon: String,
    val color: String,
    val theory: List<String>,
    val mission: String,
    val hint: String,
    val mitigation: List<String>,
    val flag: String
)

object Labs {

    const val COUNT = 7

    val ALL: List<LabDef> = listOf(
        LabDef(
            index = 0,
            id = "xss",
            name = "Lab XSS",
            focus = "<script>alert(1)</script>",
            title = "Curi Cookie dari Buku Tamu",
            tagline = "Suntik JavaScript ke buku tamu palsu dan curi cookie sesi.",
            icon = "XS",
            color = "#C51162",
            theory = listOf(
                "Buku tamu ini menyimpan komentarmu lalu menampilkannya lagi " +
                    "ke semua pengunjung — tanpa membersihkan isinya dulu. " +
                    "Itulah XSS tersimpan (stored XSS): kode yang kamu kirim " +
                    "disimpan di server, lalu dijalankan di browser korban.",
                "Browser tidak bisa membedakan mana teks biasa dan mana kode. " +
                    "Kalau aplikasi menampilkan komentarmu apa adanya sebagai " +
                    "HTML, tag script atau atribut onerror akan dijalankan " +
                    "begitu saja saat halaman dibuka.",
                "Serangan klasiknya: pencuri cookie sesi. Sekali JavaScript " +
                    "penyerang jalan di halaman korban, ia bisa membaca " +
                    "document.cookie lalu mengirimnya ke server penyerang — " +
                    "dan masuk sebagai korban tanpa perlu password."
            ),
            mission = "Buat alert palsu muncul dan curi cookie sesi palsu dari buku tamu ini.",
            hint = "Coba kirim komentar berisi <script>alert(1)</script> — atau <img src=x onerror=alert(1)> kalau tag script diblokir.",
            mitigation = listOf(
                "Encode output: ubah < menjadi &lt; sebelum ditampilkan, " +
                    "supaya browser menganggapnya teks, bukan kode.",
                "Content-Security-Policy: batasi dari mana script boleh " +
                    "dimuat; inline script diblokir.",
                "Tandai cookie sesi dengan HttpOnly: JavaScript tidak bisa " +
                    "membacanya, jadi walau XSS lolos, sesi tidak tercuri."
            ),
            flag = "FLAG{xss_cookie_stolen}"
        ),
        LabDef(
            index = 1,
            id = "css",
            name = "Lab CSS Injection",
            focus = ".profil { display: none }",
            title = "Deface Halaman Profil",
            tagline = "Suntik CSS ke profil palsu: ubah tampilan, lalu pasang login palsu.",
            icon = "CS",
            color = "#7C4DFF",
            theory = listOf(
                "Kalau aplikasi membiarkan pengguna menyuntik CSS sendiri ke " +
                    "halaman, penyerang bisa mengubah tampilan halaman korban: " +
                    "menyembunyikan konten asli, mengubah warna, atau menimpa " +
                    "halaman dengan form login palsu.",
                "Yang paling berbahaya: overlay login palsu. Korban mengira " +
                    "ia login ke situs asli, padahal passwordnya diketik ke " +
                    "form milik penyerang.",
                "Teknik lanjut (teori saja di lab ini): exfiltrasi lewat " +
                    "attribute selector. CSS seperti " +
                    "input[value^=\"a\"] { background: url(...) } bisa " +
                    "membocorkan isi atribut satu huruf demi satu huruf, " +
                    "karena server penyerang melihat URL mana yang dimuat."
            ),
            mission = "Deface halaman profil ini dengan CSS suntikanmu, lalu tampilkan form login palsu di atasnya.",
            hint = "Coba .profil { background: red; } — lalu buat aturan .login-palsu { position: fixed; } untuk memunculkan overlay login.",
            mitigation = listOf(
                "Jangan pernah menampilkan CSS kiriman pengguna apa adanya; " +
                    "bersihkan (sanitasi) sebelum dirender.",
                "Content-Security-Policy dengan style-src yang ketat.",
                "Pisahkan konten pengguna dari style: izinkan hanya pilihan " +
                    "warna atau tema yang sudah ditentukan, bukan CSS bebas."
            ),
            flag = "FLAG{css_deface_overlay}"
        ),
        LabDef(
            index = 2,
            id = "sqli",
            name = "Lab SQL Injection",
            focus = "' OR '1'='1' --",
            title = "Bobol Login Admin",
            tagline = "Bangun query palsu dan bocorkan tabel users lewat form login.",
            icon = "SQ",
            color = "#2962FF",
            theory = listOf(
                "Form login ini membangun query SQL dengan menempelkan " +
                    "langsung apa yang kamu ketik. Tidak ada pemisah antara " +
                    "data dan perintah — jadi inputmu bisa menjadi bagian " +
                    "dari perintah SQL itu sendiri.",
                "Kuncinya ada dua: OR membuat kondisi selalu benar, dan -- " +
                    "adalah komentar di SQL yang mematikan sisa query, " +
                    "termasuk pengecekan password. Tanpa komentar itu, " +
                    "password tetap diperiksa karena AND mengikat lebih " +
                    "kuat daripada OR.",
                "UNION memungkinkanmu menempelkan query kedua: SELECT lain " +
                    "yang hasilnya digabung — begitu isi tabel bisa " +
                    "dibocorkan lewat halaman yang sama."
            ),
            mission = "Masuk sebagai admin tanpa password, lalu bocorkan isi tabel users.",
            hint = "Di kolom username ketik ' OR '1'='1' -- (perhatikan spasi setelah --). Untuk dump tabel, coba ' UNION SELECT id, username, password FROM users --",
            mitigation = listOf(
                "Prepared statement / parameter binding: query dan data " +
                    "dikirim terpisah, input tidak pernah ditafsir sebagai " +
                    "perintah.",
                "Akun database dengan hak minimal (least privilege): akun " +
                    "aplikasi tidak boleh bisa membaca tabel sensitif atau " +
                    "menjalankan DROP.",
                "Jangan tampilkan pesan error SQL mentah ke pengguna — itu " +
                    "memberi peta gratis ke penyerang."
            ),
            flag = "FLAG{sqli_admin_dump}"
        ),
        LabDef(
            index = 3,
            id = "wifi",
            name = "Lab WiFi Deauther",
            focus = "aireplay-ng --deauth",
            title = "Putuskan Klien dari Jaringan",
            tagline = "Simulasi aircrack-ng: scan jaringan palsu lalu kirim deauth.",
            icon = "WF",
            color = "#FF6D00",
            theory = listOf(
                "Frame deauthentication adalah management frame 802.11 — dan " +
                    "di WiFi lama, management frame TIDAK diautentikasi. " +
                    "Siapa pun yang bisa mengirim frame mentah bisa " +
                    "berpura-pura menjadi access point dan menyuruh klien: " +
                    "kamu terputus.",
                "Serangannya: kartu WiFi masuk mode monitor, memindai " +
                    "jaringan (airodump-ng), lalu mengirim frame deauth " +
                    "berulang ke klien target (aireplay-ng). Klien terputus " +
                    "dan harus menghubungkan ulang.",
                "Jujur soal batas: lab ini 100% simulasi. Android tanpa " +
                    "hardware mode-monitor memang tidak bisa menginjeksi " +
                    "frame 802.11 mentah — tidak ada satu frame pun yang " +
                    "benar-benar dikirim dari HP ini.",
                "Penangkalnya: Protected Management Frames (PMF / 802.11w) — " +
                    "management frame ikut ditandatangani, jadi deauth palsu " +
                    "ditolak. WPA3 mewajibkan PMF; di WPA2 PMF masih opsional " +
                    "dan sering mati."
            ),
            mission = "Masuk mode monitor, pindai jaringan, lalu putuskan satu klien dari access point target.",
            hint = "airmon-ng start wlan0, lalu airodump-ng wlan0mon, lalu aireplay-ng --deauth 10 -a <BSSID> -c <STATION> wlan0mon.",
            mitigation = listOf(
                "Nyalakan PMF (Protected Management Frames / 802.11w) di " +
                    "access point — deauth palsu akan ditolak.",
                "Pakai WPA3, yang mewajibkan PMF, bukan WPA2 yang " +
                    "opsional.",
                "Pantau banjir deauth di sisi access point: deauth yang " +
                    "wajar tidak pernah datang puluhan kali per detik."
            ),
            flag = "FLAG{wifi_deauth_sim}"
        ),
        LabDef(
            index = 4,
            id = "ddos",
            name = "Lab DDoS",
            focus = "SYN flood",
            title = "Tumbangkan Server dengan Botnet",
            tagline = "Banjiri server palsu sampai tumbang, lalu pulihkan dengan mitigasi.",
            icon = "DD",
            color = "#D32F2F",
            theory = listOf(
                "DDoS bukan meretas masuk — tapi membuat server terlalu " +
                    "sibuk untuk melayani yang sah. Botnet: ribuan perangkat " +
                    "yang sudah terinfeksi malware, diperintah serentak " +
                    "membanjiri satu target.",
                "L3/L4 (SYN flood, UDP flood): membanjiri koneksi dan " +
                    "bandwidth — server kehabisan slot koneksi atau pipanya " +
                    "penuh. L7 (HTTP flood): membanjiri aplikasi dengan " +
                    "request yang tampak sah — lebih sulit dibedakan dari " +
                    "pengunjung asli.",
                "Amplifikasi: penyerang memakai server pihak ketiga " +
                    "(misalnya DNS atau NTP) yang membalas jauh lebih besar " +
                    "dari permintaan, jadi banjirnya berlipat ganda dari " +
                    "modal kecil.",
                "Semua trafik di lab ini simulasi. Tidak ada satu paket pun " +
                    "yang dikirim ke host mana pun — yang tumbang hanya " +
                    "server palsu di dalam aplikasi ini."
            ),
            mission = "Luncurkan flood sampai server palsu tumbang, lalu pulihkan dengan mitigasi.",
            hint = "Pilih SYN Flood dan saksikan CPU naik. Setelah server DOWN, nyalakan Rate Limit, lalu WAF/CDN, lalu Anycast — saksikan server pulih.",
            mitigation = listOf(
                "Rate limiting: batasi request per IP — bot yang membabi " +
                    "buta langsung kena potong.",
                "WAF/CDN: serap dan saring trafik jauh dari server asal; " +
                    "pola serangan diblokir di tepi jaringan.",
                "Anycast: sebar server ke banyak lokasi — banjir terpecah, " +
                    "tidak ada satu titik yang menanggung semuanya."
            ),
            flag = "FLAG{ddos_down_recovered}"
        ),
        LabDef(
            index = 5,
            id = "server",
            name = "Lab Hack Server",
            focus = "nmap -sV",
            title = "Dari Recon ke Flag",
            tagline = "Pindai host palsu, temukan celahnya, rebut shell, ambil flag.",
            icon = "HS",
            color = "#00897B",
            theory = listOf(
                "Serangan ke server hampir selalu berantai: recon dulu (apa " +
                    "yang terbuka?), cari celah (versi lama? kredensial " +
                    "lemah? konfigurasi salah?), eksploitasi, lalu rebut flag.",
                "nmap -sV memindai port yang terbuka dan menebak layanan " +
                    "beserta versinya. Versi lama yang terkenal rentan adalah " +
                    "pintu masuk paling umum.",
                "Kredensial default (admin:admin) dan layanan yang tidak " +
                    "perlu terekspos ke internet (misalnya database yang bisa " +
                    "diakses publik) adalah kesalahan klasik yang berulang " +
                    "di insiden nyata.",
                "Target di lab ini palsu: 203.0.113.10 adalah alamat " +
                    "dokumentasi (TEST-NET) yang tidak mengarah ke siapa " +
                    "pun. Semua layanan dan flag hanya ada di simulator."
            ),
            mission = "Pindai host palsu, temukan celahnya, rebut shell, lalu ambil flag di /root/flag.txt.",
            hint = "nmap -sV 203.0.113.10 — perhatikan versi layanan yang sudah tua. Lalu exploit wordpress, dan di dalam shell coba sudo cat /root/flag.txt.",
            mitigation = listOf(
                "Patch dan update rutin; matikan layanan yang tidak perlu; " +
                    "jangan ekspos database ke internet.",
                "SSH key-only, matikan login password; fail2ban untuk " +
                    "memblokir brute force.",
                "Least privilege: layanan jalan dengan hak minimal, bukan " +
                    "root; sudo tanpa password adalah miskonfigurasi."
            ),
            flag = "FLAG{recon_ke_root}"
        ),
        LabDef(
            index = 6,
            id = "crypto",
            name = "Lab Hack Crypto",
            focus = "trace",
            title = "Jejak Dana yang Hilang",
            tagline = "Dua insiden fiktif: kunci hot wallet bocor dan reentrancy.",
            icon = "CR",
            color = "#F9A825",
            theory = listOf(
                "Semua nama, alamat, dan angka di lab ini FIKTIF — hanya " +
                    "polanya yang terinspirasi berita publik tentang insiden " +
                    "kripto. Tidak ada satu pun detail insiden nyata " +
                    "tertentu yang dipakai.",
                "Skenario A — kebocoran kunci hot wallet: kunci privat " +
                    "wallet panas (yang tersambung internet untuk melayani " +
                    "penarikan) bocor lewat repo yang salah konfigurasi. " +
                    "Sekali kunci bocor, penyerang memindahkan dana — dan " +
                    "blockchain yang transparan membuat alirannya bisa " +
                    "dilacak hop demi hop.",
                "Skenario B — reentrancy: smart contract yang mengirim dana " +
                    "SEBELUM memperbarui saldo memungkinkan penyerang " +
                    "memanggil penarikan berulang di dalam satu transaksi — " +
                    "menguras dana sebelum saldo sempat berkurang.",
                "Pelajaran: hot wallet hanya untuk dana operasional kecil; " +
                    "mayoritas dana di cold wallet + multisig. Kontrak harus " +
                    "mengikuti checks-effects-interactions dan diaudit " +
                    "sebelum tayang."
            ),
            mission = "A: lacak aliran dana dari hot wallet yang bocor sampai ke alamat penyerang, lalu bekukan sisanya. B: buktikan reentrancy menguras kontrak, lalu tambal.",
            hint = "Skenario A: audit lalu trace TX-7f3a lalu freeze. Skenario B: audit kontrak, lalu exploit, lalu patch.",
            mitigation = listOf(
                "Cold wallet + multisig untuk mayoritas dana; hot wallet " +
                    "hanya menyimpan dana operasional kecil.",
                "Audit kontrak oleh pihak ketiga sebelum tayang, dan " +
                    "jangan pernah menyimpan kunci privat di repo.",
                "Pola checks-effects-interactions dan reentrancy guard di " +
                    "setiap fungsi yang mengirim dana."
            ),
            flag = "FLAG{crypto_jejak_dana}"
        )
    )

    fun get(index: Int): LabDef? {
        if (index < 0 || index >= ALL.size) return null
        return ALL[index]
    }

    fun getById(id: String): LabDef? {
        for (lab in ALL) {
            if (lab.id == id) return lab
        }
        return null
    }
}
