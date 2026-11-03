# DYO Keyboard RGB

Kotlin / Jetpack Compose Android uygulaması. Samsung Galaxy Tab S11 Ultra ve 156 Wireless Magic Keyboard için **donanım analizi ve deneysel uyku geciktirme**.

**Gerçek LED kontrolü ve Rainbow Wave henüz doğrulanmadı, etkin değil. Projenin nihai hedefi tamamlanmış değildir.** Bu sürüm protokolü araştırmak için tabletten teknik kanıt toplar; sahte RGB kontrol ekranı içermez.

## 0.3.0 — Uyku geciktirmeyi dene

Kullanıcının 156 model kılavuzu iki ayrı süre veriyor: ışık için 30 saniye, klavye uykusu için 10 dakika. Herhangi bir klavye tuşu uyandırır; touchpad uyandırmaz. Kılavuzda bu süreleri değiştiren komut belirtilmiyor.

Yeni **Uyku geciktirmeyi dene · 12 dk** düğmesi, BLE iletişiminin klavyenin hareketsizlik zamanlayıcısını etkileyip etkilemediğini sınar. Her 15 saniyede yalnızca standart Device Information / Manufacturer Name (`180A/2A29`) okunur. Bu alan kullanıcının önceki raporunda başarıyla okunmuştu. Süre ayarı değiştirilmez; FFB3, HID veya firmware komutu gönderilmez. Başarılı GATT yanıtları ışığın ya da yazı yazma işlevinin uyanık kaldığını kanıtlamaz.

1. Klavyeyi seç, ışığını aç, **Uyku geciktirmeyi dene · 12 dk** düğmesine bas.
2. İstenirse bildirim iznini ver; bildirimdeki **Durdur** düğmesiyle deney sonlandırılabilir. İzin reddi deneyi engellemez; uygulamadaki durdurma düğmesi kullanılabilir.
3. İlk 11 dakika klavyeye/touchpad'e dokunma. 30 saniye sonra ışığın durumunu gözle. Tablet üzerinde başka uygulamaya geçebilirsin.
4. 11. dakikada, deney hâlâ sürerken bir kez yazmayı dene; ilk tuşun gecikip gecikmediğini veya kaybolup kaybolmadığını not et. Bu tuş sonraki hareketsizlik süresini sıfırlayacağından yalnızca o andaki davranışı değerlendir.
5. Deney 12. dakikada kendiliğinden biter. **Uyku raporunu göster → Teknik raporu kaydet** ile okuma/bağlantı sonuçlarını kaydet. Işık ve ilk tuş gözlemini ayrıca belirt.

Deney görünür bir `connectedDevice` ön plan servisi ve süre sınırı olan kısmi uyanıklık kilidiyle yürür. En fazla 12 dakikalık ölçüm + 20 saniyelik kurulum süresi vardır. Yanıt 5 saniyede gelmezse veya bağlantı kesilirse durur; otomatik yeniden bağlanmaz. Donanım analizi ve ışık gözlemi aynı anda başlatılamaz. Kalıcı uyku süresi veya RGB kontrolü henüz sunulmuyor. Tabletteki fiziksel etkisi test edilmeden başarı iddia edilmez.

## Tablette kullanım

1. Bu depoyu CodeAssist ile clone et, `app` modülünü aç.
2. SDK 35 ve Compose derleyici desteği olan güncel CodeAssist kullan. Yerel proje modeli `.platform/workspace.json`, modül `app/module.toml` dosyasındadır. Gradle dosyaları aynı kaynakların alternatif derlemesidir.
3. Bağımlılıkları ilk derlemede indir; debug APK oluştur ve yükle. İlk bağımlılık indirmesi internet ister, uygulamanın normal kullanımı istemez. Uygulamada INTERNET izni yoktur.
4. Klavyeyi Android Bluetooth ayarlarında eşleştir ve bağlı tut.
5. Uygulamada **İzin / Yenile** → klavyeyi seç → **Donanımı incele**.
6. Servis keşfi en fazla yaklaşık 32 saniye; ardından her meta veri okuması en fazla 4 saniye sürer (en fazla 32 okuma). Tek bir okuma zaman aşımına uğrarsa kalan okumalar denenmeden bağlantı kapatılır. **Teknik raporu kaydet** ile JSON dosyasını kaydet. Rapor cihaz adresi ve ayrı giriş aygıtı gözlemleri içerir; paylaşmadan önce içeriğini kontrol edebilirsin.
7. JSON raporunu inceleme için gönder. Donanım üzerinde renk kontrolüne geçiş bu veriye ve doğrulanmış protokole bağlıdır.

CodeAssist'in belirli tablet sürümünde derleme ve fiziksel klavyede çalışma burada doğrulanamaz. Compose desteği, modül modeli ve ilk bağımlılık çözümlemesi kullanılan IDE sürümüne bağlıdır. Derleme hatasını gizlemek için boş/işlevsiz fallback ekranı yoktur.

## Bu sürümde mevcut

- Eşleştirilmiş cihaz seçimi, son seçimi hatırlama; Bluetooth kapalı/izin yok/boş liste durumları.
- Ad/adres/tür/sınıf, önbellekteki UUID'ler ve zaman sınırlı Classic SDP sorgusu.
- BLE/Dual/bilinmeyen tür için zaman sınırlı GATT bağlantısı ve salt meta veri keşfi; servisler, karakteristik özellikleri ve descriptor UUID'leri.
- v0.2: standart Device Information kimlik alanları, PnP ID, HID Report Map ve Report Reference descriptor değerlerini sırayla okuma; ham HEX, hata veya erişim reddini kaydetme. Canlı HID giriş raporları okunmaz ve bildirim aboneliği açılmaz.
- Ayrı dış InputDevice listesi, mevcutsa VID/PID ve sistemin sunduğu ışık meta verisi. Bluetooth cihazıyla kesin kimlik eşleştirmesi yapılmaz.
- ACL bağlantı olayları, hata durumları, belirsizlikler; son raporu yerelde saklama, Android dosya seçicisiyle JSON dışa aktarma.
- Siyah Compose analiz ekranı; geniş tablette iki sütun, dar ekranda kaydırılabilir tek sütun.
- Protokol doğrulanana kadar tüm aydınlatma komutlarını reddeden LightingController sınırı.

## Güvenlik ve yaşam döngüsü

Üreticiye özel karakteristik, HID, firmware güncelleme, yeniden eşleştirme veya unpair komutu yoktur. Işık gözlemi yalnızca standart CCCD aboneliği kurar. Donanım analizi ve ışık gözlemi arka plana geçince durur; yeni uyku deneyi süreli ön plan servisiyle devam eder. Uygulama yalnızca kendi GATT istemcisini kapatır ve sınırsız yeniden bağlanmaz. BLE trafiği veya servis keşfi cihazın güç davranışını etkileyebilir. Normal yazı/touchpad işlevlerinin korunması fiziksel tablette ayrıca test edilmelidir.

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

JDK 17, SDK 35, Gradle 8.7. `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`.
GitHub Actions aynı kaynakları derler; APK Actions artifacts altında oluşturulur. Bu doğrulama CodeAssist'in kendi derleme motoru veya fiziksel Bluetooth testi yerine geçmez.

Ayrıntılar: [Araştırma ve Android sınırları](docs/RESEARCH.md), [Cihaz test listesi](docs/DEVICE_TEST.md).

## 0.2 güncellemesi

0.1 raporunda BLE keşfi ve FFE0 altında FFB3/FFB4 kanalları gözlendi. Bu kanalların işlevi henüz belirlenmedi. 0.2 aynı Donanımı incele düğmesiyle üretici/yazılım/PnP kimliği ve erişilebilirse HID tanımı toplar. Sadece UUID benzerliğiyle başka cihazların komutları kullanılmaz. Android HID okumasını engellerse erişim reddi raporlanır; engel aşılmaya çalışılmaz.

