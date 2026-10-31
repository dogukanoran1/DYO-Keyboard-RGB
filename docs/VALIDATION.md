# Doğrulama — 31 Ekim 2026

Kaynak commit: f3c447a7215f5b94aaec2315fbdcbc7d4412b25e.

- JDK 17, Gradle 8.7, Android SDK 35 ile `:app:assembleDebug :app:lintDebug` başarılı.
- `apksigner verify app-debug.apk` başarılı.
- Lint hata yok; iki uyarı: uygulamaya özel ikon henüz yok ve Android 12+ veri çıkarma kuralları ayrıca tanımlanmamış. Manifest allowBackup=false; OEM cihazlar arası transfer davranışı ayrıca test edilmeli.
- CodeAssist için app/module.toml ve .platform proje modeli sağlandı; belirli tablet IDE sürümünün kendi motorunda doğrulanmadı.
- Fiziksel Bluetooth klavye, cihazdaki izin akışı, ekran yerleşimi, klavye/touchpad işlevleri ve LED erişimi burada test edilmedi.
- Gerçek LED kontrolü / Rainbow Wave başarısı iddia edilmiyor. Donanım raporu bekleniyor.
