# RemoteBT

Aplikasi Android (Kotlin) untuk mengontrol satu HP dari HP lain lewat
Bluetooth Classic (RFCOMM). Satu HP berperan sebagai **Host** (yang
dikontrol), satu lagi sebagai **Client** (remote-nya).

## Fitur

- Volume naik/turun, mute
- Kontrol media: play/pause, next, prev
- Senter on/off
- Kunci layar jarak jauh (opsional, butuh izin Device Admin)

## Sisi keamanan

- Kedua HP **harus sudah pairing Bluetooth manual** lebih dulu lewat
  Pengaturan sistem — aplikasi ini tidak melakukan pairing sendiri.
- Setiap kali service host dinyalakan, dibuat **PIN 6 digit acak**
  yang tampil di layar host. Client wajib mengirim PIN itu sebelum
  host mau menerima perintah apa pun. PIN beda tiap sesi.
- Host hanya menerima satu koneksi aktif dalam satu waktu, dan hanya
  menjalankan perintah dari daftar tetap (whitelist) di
  `CommandProtocol.KNOWN_COMMANDS` — string lain langsung diabaikan.
- Saat service host aktif, selalu ada notifikasi foreground yang
  tidak bisa disembunyikan, jadi pemilik HP host selalu tahu kalau
  layanan sedang berjalan.
- Fitur kunci layar butuh aktivasi manual izin Device Admin lewat
  dialog sistem Android (tidak bisa diaktifkan diam-diam dari kode).

## Cara pakai

1. Pairing dua HP lewat Pengaturan Bluetooth seperti biasa.
2. Di HP yang mau dikontrol: buka app → **Jadi Perangkat yang
   Dikontrol** → Aktifkan Layanan → catat PIN yang muncul.
3. Di HP remote: buka app → **Kontrol Perangkat Lain** → pilih nama
   HP tadi → masukkan PIN → Hubungkan.
4. Tombol kontrol aktif setelah status berubah jadi "terhubung".

## Build lewat GitHub Actions

Push ke branch `main` (atau jalankan workflow manual lewat tab
Actions) otomatis men-build APK debug. Hasilnya bisa diunduh dari
artifact **RemoteBT-debug-apk** pada run workflow tersebut.

## Keterbatasan yang perlu diketahui

- `dispatchMediaKeyEvent` bergantung pada app pemutar media yang
  sedang aktif di HP host; beberapa app mungkin tidak merespons di
  versi Android tertentu.
- Kunci layar butuh HP host mengaktifkan izin Device Admin dulu
  lewat tombol di layar Host.
