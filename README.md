# HackLab

Aplikasi belajar hacking dan coding dari nol, untuk pemula sekalipun.
Fase 1 dan mini-game Fase 2 sepenuhnya offline; video pendamping dan
sinkronisasi katalog pelajaran memakai internet. Tanpa dependensi
pihak ketiga — pemutar video YouTube adalah satu-satunya WebView di
aplikasi ini.

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

## Fase 2: belajar ngoding lewat game (mini-game offline)

Lima game dari video Rayden "5 game bikin jago ngoding & hacking",
masing-masing jadi mini-game yang benar-benar bisa dimainkan di dalam
aplikasi, tanpa internet. Tiap game punya 3 level, progres level
tersimpan di HP. Daftar tampil ala Play Store: ikon + nama pelajaran.

1. Code Combat — "Misi Kode": ketik perintah (maju/kiri/kanan/ambil),
   hero mengeksekusi selangkah demi selangkah di grid 5x5. Ambil semua
   permata dan sampai ke target.
2. Bitburner — "Retas Server": terminal palsu. `scan` lihat port,
   `connect <port>`, lalu `crack <PIN>` menebak PIN 4 digit dengan
   petunjuk lebih besar/kecil.
3. CodinGame — "Debug Kode": program pendek berisi 1 bug; pilih baris
   mana yang salah.
4. Screeps: World — "Program Bot": susun urutan aksi (kumpul/antar/
   ulang), bot menjalankannya sendiri di grid kecil sampai semua
   resource diantar sebelum langkah habis.
5. Opus Magnum — "Mesin Alkimia": susun urutan operasi (panaskan/
   dinginkan/dsb.) untuk mengubah bahan awal menjadi produk target.

Detail tiap game tetap menampilkan penjelasan, skill yang dilatih,
bahasa, dan tingkat; tombol utama "MAIN" membuka mini-game, tombol
sekunder "Buka situs resmi" membuka situs aslinya di browser.

Level didefinisikan sebagai data (MiniGameLevels), bukan kode khusus
per level, supaya level baru bisa ditambah lewat katalog online.

## Katalog online

Daftar Fase 2 bisa bertambah lewat katalog di
`https://hozinking.my.id/api/hacklab/lessons` (GET). Aplikasi
mengambilnya di thread background saat beranda dibuka (timeout 8
detik), menyimpan JSON mentah sebagai cache di SharedPreferences,
dan menggabungkannya dengan game bawaan:

- id cocok dengan game bawaan + kind=minigame → level online
  ditambahkan ke game itu (dedupe per id level);
- id baru + kind=link → entri baru di daftar (ketuk → buka di browser);
- id baru + kind=minigame tanpa engine lokal → diabaikan.

Bila fetch gagal atau tidak ada internet, aplikasi diam-diam memakai
cache + bawaan (tanpa pesan error). Pelajaran online yang sudah
sekali tersinkron tetap tampil saat offline. Data katalog disimpan
di D1 `stopwatch-db` milik user (satu database dengan stopwatch yang
sudah ada, bukan database baru).

Semua game terbuka tanpa kunci dan tanpa progres. Video Rayden
sendiri tidak disematkan (video TikTok tidak bisa diputar di pemutar
YouTube dalam aplikasi).

## Desain

Tema gelap datar: latar `#0F1115`, kartu `#1C1F27`, teks sekunder
`#9AA0A6`, satu aksen hijau terminal `#00C853` (prompt, sukses,
progres). Tanpa gradien, tanpa dekorasi, tanpa gambar AI. Satu aksi
utama per layar. Semua teks Bahasa Indonesia, kalimat pendek.

## Struktur kode

- `MainActivity.kt` — beranda: satu kartu jalur + progres + satu tombol,
  ditambah panel Fase 2 (daftar ala Play Store: ikon + nama, dari
  katalog gabungan bawaan + online).
- `Games.kt` — definisi 5 game Fase 2 (deskripsi, skill, bahasa,
  tingkat, URL situs resmi, id + ikon + warna tile).
- `GameActivity.kt` — layar detail satu pelajaran + tombol MAIN
  (mini-game) dan tombol buka situs resmi di browser.
- `MiniGameActivity.kt` — host mini-game: daftar level + area main.
- `GameEngines.kt` — 5 engine mini-game offline (Misi Kode, Retas
  Server, Debug Kode, Program Bot, Mesin Alkimia).
- `MiniGameLevels.kt` — definisi level sebagai data (bawaan + parse
  level online dari cache katalog).
- `GameStore.kt` — SharedPreferences untuk progres level mini-game
  (key `level_done_<game>_<level>`, terpisah dari key Fase 1).
- `LessonCatalog.kt` — fetch + cache + gabung katalog pelajaran
  online (endpoint `hozinking.my.id/api/hacklab/lessons`).
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
Manifest hanya punya satu permission: INTERNET (video YouTube +
sinkronisasi katalog). Versi saat ini: 1.3 (versionCode 4).
