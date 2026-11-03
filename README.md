# HackLab

Aplikasi belajar hacking dan coding dari nol, untuk pemula sekalipun.
Sepenuhnya offline, tanpa WebView, tanpa dependensi pihak ketiga.

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

## Desain

Tema gelap datar: latar `#0F1115`, kartu `#1C1F27`, teks sekunder
`#9AA0A6`, satu aksen hijau terminal `#00C853` (prompt, sukses,
progres). Tanpa gradien, tanpa dekorasi, tanpa gambar AI. Satu aksi
utama per layar. Semua teks Bahasa Indonesia, kalimat pendek.

## Struktur kode

- `MainActivity.kt` — beranda: satu kartu jalur + progres + satu tombol.
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
