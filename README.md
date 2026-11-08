# HackLab

Aplikasi belajar hacking dan coding dari nol, untuk pemula sekalipun.
Fase 1 sepenuhnya offline; video pendamping dan situs game Fase 2
memakai internet. Tanpa dependensi pihak ketiga — pemutar video
YouTube adalah satu-satunya WebView di aplikasi ini.

## Fase 1: satu jalur lengkap

**Terminal & Linux Dasar** — terminal simulasi gaya Bandit (bukan shell
beneran, tidak menyentuh filesystem asli), 6 langkah berurutan:

1. Kenalan dengan Terminal — misi: `pwd` dan `whoami`
2. Melihat Isi Folder — misi: `ls` di home
3. Pindah Folder — misi: `cd dokumen`
4. Membaca File — misi: `cat catatan.txt` di folder dokumen
5. Jejak dan Bantuan — misi: `help` lalu `clear`
6. Misi Akhir: Temukan Flag — misi: baca `brankas/flag.txt`

Langkah N+1 terkunci sampai langkah N selesai. Progres tersimpan di
SharedPreferences. Setiap misi punya predikat keberhasilan yang dicek
program dari sesi terminal — langkah tidak pernah ditandai selesai lebih
awal. Pesan penutup jalur memuat prinsip etika: skill ini hanya dipakai
di lab sendiri atau target yang memberi izin.

## Fase 2: belajar ngoding lewat game

Lima game dari video Rayden "5 game bikin jago ngoding & hacking",
masing-masing dengan penjelasan, skill yang dilatih, bahasa, tingkat
kesulitan, dan tombol ke situs resminya (dibuka di browser):

1. Code Combat — pemula, Python/JavaScript, perintahin karakter selesaikan misi
2. Bitburner — menengah, JavaScript, script hack sistem virtual + kelola resource
3. CodinGame — pemula–menengah, banyak bahasa, puzzle programming melatih logika
4. Screeps: World — lanjutan, JavaScript, AI kontrol unit + strategi
5. Opus Magnum — menengah, logika visual, algoritma + otomatisasi + optimasi

Semua game terbuka tanpa kunci dan tanpa progres. Game butuh internet
untuk dibuka di situsnya; Fase 1 tetap sepenuhnya offline. Video
Rayden sendiri tidak disematkan (video TikTok tidak bisa diputar di
pemutar YouTube dalam aplikasi).

## Desain

Tema gelap datar: latar `#0F1115`, kartu `#1C1F27`, teks sekunder
`#9AA0A6`, satu aksen hijau terminal `#00C853` (prompt, sukses,
progres). Tanpa gradien, tanpa dekorasi, tanpa gambar AI. Satu aksi
utama per layar. Semua teks Bahasa Indonesia, kalimat pendek.

## Struktur kode

- `MainActivity.kt` — beranda: satu kartu jalur + progres + satu tombol,
  ditambah panel Fase 2 (5 game, tanpa kunci).
- `Games.kt` — definisi 5 game Fase 2 (deskripsi, skill, bahasa,
  tingkat, URL situs resmi).
- `GameActivity.kt` — layar detail satu game + tombol buka situs resmi
  di browser.
- `StepActivity.kt` — pelajaran satu langkah + kartu misi + tombol simulator.
- `TerminalActivity.kt` — simulator: banner misi, log monospace, input,
  tombol Petunjuk, kotak penyelesaian misi.
- `Steps.kt` — definisi 6 langkah + filesystem virtual (hardcode).
- `TerminalEngine.kt` — parser perintah + state sesi + predikat misi.
- `ProgressStore.kt` — SharedPreferences untuk progres.
- `Ui.kt` — seluruh bahasa visual (warna, tombol, teks) di satu tempat.

## Build

Build hanya lewat GitHub Actions (aturan proyek). Toolchain: Gradle
6.7.1, AGP 4.0.1, compileSdk 30, minSdk 26, targetSdk 30.
Manifest tanpa permission apa pun — termasuk tanpa INTERNET.
