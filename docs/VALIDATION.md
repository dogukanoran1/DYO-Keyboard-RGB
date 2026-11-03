# Doğrulama — 31 Ekim 2026

## 0.3.0 — 4 Kasım 2026

- Kod commit'i `66e95de08ad7fd03fb19c75bba2b273cc5fd520f` için GitHub Actions'ta JDK 17 / SDK 35 ile `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug` başarılı.
- Derleme kaydı: https://github.com/dogukanoran1/DYO-Keyboard-RGB/actions/runs/41736874614
- 8 yeni test: başarılı okumaların 12 dakikalık son tarihi uzatmaması, okuma ve kurulum zaman aşımları, iptal sonrası geç gelen callback'ler, okuma başlatma/yanıt hataları, çift başlatma ve önceki oturum callback'lerinin yeni oturuma sızmaması. Mevcut 3 meta veri testi de aynı görevde çalıştırıldı.
- APK üretildi ve Actions artifact olarak yüklendi. Kaynakların CodeAssist'in belirli tablet sürümünde derlenmesi ve fiziksel Bluetooth davranışı burada test edilmedi.
- Donanım sonucu bilinmiyor: periyodik üretici bilgisi okumasının klavyenin ışık veya uyku zamanlayıcısını etkilediği henüz doğrulanmadı. Yazılım başarılı GATT yanıtlarını fiziksel başarı saymaz; raporda `sleepPrevented` ve `backlightStayedOn` null kalır.
- Kalıcı uyku ayarı, FFB3 komutu, HID yazımı, firmware güncellemesi veya RGB/bağımsız LED kontrolü eklenmedi.

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

