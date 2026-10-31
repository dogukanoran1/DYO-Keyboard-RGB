# DYO Keyboard RGB

Kotlin / Jetpack Compose Android uygulaması. Samsung Galaxy Tab S11 Ultra ve 156 Wireless Magic Keyboard için **Aşama 1: Donanım Analizi**.

**Gerçek LED kontrolü ve Rainbow Wave henüz doğrulanmadı, etkin değil. Projenin nihai hedefi tamamlanmış değildir.** Bu sürüm protokolü araştırmak için tabletten teknik kanıt toplar; sahte RGB kontrol ekranı içermez.

## Tablette kullanım

1. Bu depoyu CodeAssist ile clone et, `app` modülünü aç.
2. SDK 35 ve Compose derleyici desteği olan güncel CodeAssist kullan. Yerel proje modeli `.platform/workspace.json`, modül `app/module.toml` dosyasındadır. Gradle dosyaları aynı kaynakların alternatif derlemesidir.
3. Bağımlılıkları ilk derlemede indir; debug APK oluştur ve yükle. İlk bağımlılık indirmesi internet ister, uygulamanın normal kullanımı istemez. Uygulamada INTERNET izni yoktur.
4. Klavyeyi Android Bluetooth ayarlarında eşleştir ve bağlı tut.
5. Uygulamada **İzin / Yenile** → klavyeyi seç → **Donanımı incele**.
6. İşlem en fazla yaklaşık 32 saniye sürer. **Teknik raporu kaydet** ile JSON dosyasını kaydet. Rapor cihaz adresi ve ayrı giriş aygıtı gözlemleri içerir; paylaşmadan önce içeriğini kontrol edebilirsin.
7. JSON raporunu inceleme için gönder. Donanım üzerinde renk kontrolüne geçiş bu veriye ve doğrulanmış protokole bağlıdır.

CodeAssist'in belirli tablet sürümünde derleme ve fiziksel klavyede çalışma burada doğrulanamaz. Compose desteği, modül modeli ve ilk bağımlılık çözümlemesi kullanılan IDE sürümüne bağlıdır. Derleme hatasını gizlemek için boş/işlevsiz fallback ekranı yoktur.

## Bu sürümde mevcut

- Eşleştirilmiş cihaz seçimi, son seçimi hatırlama; Bluetooth kapalı/izin yok/boş liste durumları.
- Ad/adres/tür/sınıf, önbellekteki UUID'ler ve zaman sınırlı Classic SDP sorgusu.
- BLE/Dual/bilinmeyen tür için zaman sınırlı GATT bağlantısı ve salt meta veri keşfi; servisler, karakteristik özellikleri ve descriptor UUID'leri.
- Ayrı dış InputDevice listesi, mevcutsa VID/PID ve sistemin sunduğu ışık meta verisi. Bluetooth cihazıyla kesin kimlik eşleştirmesi yapılmaz.
- ACL bağlantı olayları, hata durumları, belirsizlikler; son raporu yerelde saklama, Android dosya seçicisiyle JSON dışa aktarma.
- Siyah Compose analiz ekranı; geniş tablette iki sütun, dar ekranda kaydırılabilir tek sütun.
- Protokol doğrulanana kadar tüm aydınlatma komutlarını reddeden LightingController sınırı.

## Güvenlik ve yaşam döngüsü

Karakteristik/descriptor yazma, bildirim aboneliği, raw HID/L2CAP/RFCOMM komutları, yeniden eşleştirme veya unpair yoktur. Uygulama yalnızca kendi GATT istemcisini kapatır. Arka plana geçince analiz durur; sınırsız yeniden bağlanma ve arka plan servisi yoktur. BLE servis keşfi cihazı kısa süreli uyandırabilir. Normal klavye işlevlerinin korunması fiziksel tablette ayrıca test edilmelidir.

## Geliştirme sırası

| Aşama | Durum / geçiş koşulu |
|---|---|
| 1. Donanım analizi | Uygulama kaynakları hazır; fiziksel rapor bekleniyor |
| 2. Protokol | Model adına göre doğrulanmış protokol bulunamadı; raporla araştırma daraltılacak |
| 3. İlk gerçek LED komutu | Doğrulanmış cihaz/firmware ve komut belgesi gerekli |
| 4. Efekt motoru | Öncelik gerçek Rainbow Wave; bağımsız bölge veya native wave komutu gerekli |
| 5. Önizleme ve profiller | Gerçek kontrolden ayrı tutulacak; kontrol yeteneği netleşince geliştirilecek |
| 6. Cihaz testleri | Android 16 izinleri, yazı/touchpad, bağlantı, batarya ve gerçek LED davranışı test edilecek |

İleride sabit renk/HEX/favoriler, parlaklık, breathing hızları, dört wave yönü ve diğer efektler yalnızca doğrulanmış yetenekler ölçüsünde sunulacak. Tüm klavye tek kanalsa bağımsız akan wave vaat edilmeyecek. Reactive için sistem genelinde tuş dinleme varsayılmayacak.

## Derleme

JDK 17, SDK 35, Gradle 8.7. `./gradlew :app:assembleDebug :app:lintDebug`.
GitHub Actions aynı kaynakları derler; APK Actions artifacts altında oluşturulur. Bu doğrulama CodeAssist'in kendi derleme motoru veya fiziksel Bluetooth testi yerine geçmez.

Ayrıntılar: [Araştırma ve Android sınırları](docs/RESEARCH.md), [Cihaz test listesi](docs/DEVICE_TEST.md).
