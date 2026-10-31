# Doğrulama — 31 Ekim 2026

## 0.2.0

- JDK 17, Gradle 8.7, Android SDK 35 ile `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug` başarılı.
- 3 birim testi başarılı: PnP ID alanlarında little-endian/unsigned çözümleme ve ad alanının korunması; eksik/fazla uzun PnP değerinin reddi; ham HEX değerinde sıfır/yüksek bitlerin korunması.
- Lint hata yok. İlk sürümden kalan uyarılar: uygulamaya özel ikon yok; Android 12+ dataExtractionRules ayrıca tanımlanmamış. Kotlin eski callback override uyarıları, Android 12 uyumluluğu için tutulan callback'lerden geliyor.
- Kod yalnızca standart kimlik ve HID tanım meta verisini okur. FFB3 yazımı, FFB4 aboneliği, HID giriş raporu okuması ve CCCD yazımı yoktur.
- Kullanıcının Android 16 tablette ürettiği ikinci raporda üretici, Software Revision ve PnP ID okumaları SUCCESS. İlk HID descriptor okuması BLUETOOTH_PRIVILEGED eksikliğiyle ACCESS_DENIED; kalan HID okumaları NOT_STARTED. Analiz kesilmedi, writesSent 0. HID tanımı ve gerçek LED kontrolü elde edilmedi.
- CodeAssist için yerel model ve module.toml sağlandı; bu sürümün belirli tablet IDE motorunda derlenmesi burada doğrulanmadı.

## 0.1.0

- APK derlemesi, lint ve apksigner verify başarılıydı.
- Kullanıcı tablette uygulamayı çalıştırıp JSON raporunu iletti: BLE servis keşfi tamamlandı, analiz kesilmedi, writesSent 0.
- Bu test gerçek renk kontrolünü, Rainbow Wave'i, tüm hata durumlarını veya batarya davranışını doğrulamaz.

Ham kullanıcı raporu ve cihaz adresi GitHub'a yüklenmemiştir. Nihai hedef gerçek klavye aydınlatma kontrolüdür; henüz tamamlanmadı.
