# Protokol araştırması — 31 Ekim 2026

## 4 Kasım 2026 — Uyku zamanlayıcısı için ölçülebilir deney

156 modeline ait kullanıcı kılavuzu doğrudan incelendi: ışık 30 saniye hareketsizlikte söner, klavye 10 dakikada uyur. Touchpad uyandıramaz; herhangi bir klavye tuşu uyandırabilir. F6 aydınlatma parlaklığı, F7 renk içindir. Süre değişikliği veya bağımsız LED adresleme komutu belirtilmiyor.

0.3.0, standart üretici metnini 15 saniyede bir okuyarak harici BLE trafiğinin klavyenin hareketsizlik sayacını etkileyip etkilemediğini denemek için 12 dakikalık bir mod ekler. Bu bir test hipotezidir; Hailuck firmware'inde işe yaradığına dair kanıt yoktur. BLE bağlantısı, HID işlevi ve ışık zamanlayıcısı birbirinden ayrı davranabilir. Kalıcı süre değişikliği veya LED komutu uygulanmaz; FFB3 ve korunan HID erişimine dokunulmaz. SleepTrial raporları fiziksel sonucu otomatik başarılı saymaz.

Ön plan servisi yalnızca kullanıcı başlattığında çalışır; Bluetooth bağlantı izni, connectedDevice türü ve süreli kısmi uyanıklık kilidi kullanır. Kurulum/okuma zaman aşımları, durdurma, servis kapanışı ve maksimum deney süresi kaynakları serbest bırakır. Protokol belgesi veya aynı cihazda doğrulanmış bir sürücü olmadan gerçek renk/RGB kontrolü henüz uygulanamaz.

Birincil kaynaklar:
- https://developer.android.com/reference/android/bluetooth/BluetoothGatt
- https://developer.android.com/develop/background-work/services/fgs/service-types#connected-device
- https://source.android.com/docs/core/connect/bluetooth/verifying_debugging

## Önceki protokol araştırması

156 Wireless Magic Keyboard adı tek başına üretici, denetleyici veya firmware kimliğini doğrulamıyor. Bu adla yapılan araştırmada cihazla eşleştirilebilen üretici protokolü veya açık kaynak sürücü bulunamadı. Bu, böyle bir protokolün bulunmadığını kanıtlamaz.

Kullanıcının gözlemleri: 7 renk, sabit renk, nefes efekti, parlaklık ve fiziksel tuşlarla kontrol. Bunlar kullanıcı beyanıdır; uygulama tarafından doğrulanmış donanım ölçümü değildir. 7 renk, RGB karışımı veya bağımsız tuş/bölge kontrolünün kanıtı değildir. Firmware'in içinde çalışan efektlerin harici bir kanaldan yönetilebildiği varsayılmaz.

## Android erişim sınırları

- BluetoothDevice.type Android'in bildirdiği türdür; o anda kullanılan HID taşımasını göstermez.
- Bonded listesi bağlantı listesi değildir. Başlangıç HID bağlantısı normal public host API üzerinden kesinleştirilmez. Sonradan gözlenen ACL olayları ayrı kaydedilir.
- fetchUuidsWithSdp sonucu ACTION_UUID ile gelir; sonuçlar önbellekten gelebilir. UUID eksikliği servis yokluğunun kanıtı değildir.
- BLE varsa discoverServices servis/karakteristik/descriptor meta verisini alır. WRITE özelliği bilinmeyen bir kanala LED baytı gönderme izni veya protokol kanıtı değildir.
- BluetoothHidDevice API, Android'i HID aygıtı olarak kullanır; harici klavyeye host olarak keyfi HID Output/Feature raporu gönderme çözümü değildir.
- InputDevice üzerinden sunulan VID/PID sıfırsa bilinmiyor olarak raporlanır. Bu kayıtlar seçilen Bluetooth adresiyle otomatik eşleştirilmez. Bir klavye birden fazla InputDevice üretebilir.
- InputDevice.lightsManager yalnızca sistemin sunduğu ışık listesini gözlemler. Işık bulunması klavye RGB protokolünün çözüldüğü anlamına gelmez. Bu sürüm ışık oturumu açmaz ve ışıkları değiştirmez.

## Sonraki kanıt

Tablette üretilecek JSON raporunda cihaz adı, Android taşıma türü, SDP UUID'leri ve varsa GATT UUID'leri incelenmeli. InputDevice kayıtlarından VID/PID için ayrıca kimlik eşleştirme kanıtı gerekir. Üretici etiketi/model numarası bulunursa belge araması bununla daraltılır. Kontrol kanalı + komut biçimi + cihaz/firmware eşleşmesi doğrulanmadan ilk LED komutu eklenmez. Rainbow Wave için ayrıca bağımsız LED bölgesi veya donanımsal wave komutu gerekir.

## Birincil kaynaklar

- https://developer.android.com/reference/android/bluetooth/BluetoothDevice
- https://developer.android.com/reference/android/bluetooth/BluetoothGatt
- https://developer.android.com/reference/android/bluetooth/BluetoothHidDevice
- https://developer.android.com/reference/android/view/InputDevice
- https://developer.android.com/develop/connectivity/bluetooth/ble/transfer-ble-data
- https://github.com/tyron12233/CodeAssist/blob/main/docs/build-system.md
- https://github.com/tyron12233/CodeAssist/blob/main/docs/github-action.md

## İlk cihaz raporunun değerlendirmesi

Kullanıcının 0.1 raporunda analiz kesilmeden bitmiş, GATT servis keşfi başarılı ve yazma sayısı sıfırdır. Ham rapor ve cihaz adresi depoya dahil edilmemiştir.

| Gözlem | Yorum |
|---|---|
| Android türü BLE, HID servisi 1812 | BLE ve HID servis keşfi çalışıyor |
| 180A / 2A29, 2A28, 2A50 okunabilir | Üretici adı, yazılım sürümü, PnP ID için standart meta veri okuması denenebilir |
| HID 2A4B okunabilir, 2A4D üzerinde 2908 descriptor'ları var | Report Map ve Report Reference meta verisi incelenebilir; Android erişimi yine engelleyebilir |
| FFE0 / FFB3 write + write-without-response | Bir yazma kanalı listelenmiş; işlevi ve uygulamadan yazma erişimi doğrulanmadı |
| FFE0 / FFB4 notify | Bildirim özelliği listelenmiş; hangi veriyi taşıdığı bilinmiyor; abone olunmuyor |
| Ayrı Wireless Keyboard InputDevice VID 0x1915 / PID 0x0040 | Bluetooth adresiyle kesin eşleştirme ve üretici/denetleyici kimliği çıkarılamaz |
| Sistemin sunduğu ışık listesi boş | Android ışık listesinde kontrol bulunmadı; bu bulgu donanımın RGB yeteneğini dışlamaz |

FFE0/FFB3/FFB4 kombinasyonu için bu klavye ve firmware ile eşleştirilebilen güvenilir LED komut protokolü bulunamadı. Başka aygıtlarda aynı kısa UUID'ler farklı amaçlarla kullanılabilir; bunlardan komut ödünç alınmaz. 0.2 sürümü salt okunur kimlik ve HID tanımı toplar. Sonuç hâlâ UNKNOWN_NOT_VERIFIED.

Ek birincil kaynak: https://www.bluetooth.com/wp-content/uploads/Files/Specification/HTML/Assigned_Numbers/out/en/index-en.html

## İkinci cihaz raporu — 0.2.0

Standart Device Information okumaları SUCCESS ile dönmüştür:

| Alan | Doğrudan cihazdan okunan değer |
|---|---|
| Manufacturer Name | Hailuck |
| Software Revision | HL_NEW_129S_US_UK_01 |
| PnP Vendor ID Source | 2 — USB-IF |
| PnP VID / PID | 0x1915 / 0x0040 |
| PnP Product Version | 0x0005 |

Üretici metni firmware'in kendi beyanıdır. USB VID tek başına gerçek denetleyici veya ticari model tespiti değildir; firmware içindeki 129S metniyle başka bir perakende ürünün aynı donanım olduğu varsayılmaz. Software Revision, Firmware Revision karakteristiği yerine geçmez.

İlk HID Report Reference okuması BLUETOOTH_PRIVILEGED eksikliğiyle ACCESS_DENIED dönmüştür. Sonraki üç Report Reference ve Report Map NOT_STARTED'dır: bu kayıtlar ayrı ayrı erişim reddi veya donanım desteği yokluğu olarak yorumlanmaz. HID rapor baytları elde edilmemiştir. Erişim engeli normal Yakındaki cihazlar izni reddi değildir.

Android'in resmi izin belgesi BLUETOOTH_PRIVILEGED iznini üçüncü taraf uygulamalar için tanımlamaz. AOSP GattService korunan HID UUID/handle erişiminde bu izni kontrol eder. Bildirime izin veren UUID bulmak, bu kontrolü aşma yetkisi sağlamaz. Uygulamaya yalnızca bu izin adını eklemek çözüm değildir.

Hailuck üretici adı, tam yazılım kimliği ve FFE0/FFB3/FFB4 kombinasyonuyla arama yapıldı. Hailuck'un incelenen resmi Download ve FAQ sayfalarında bu yazılım için eşleştirilebilen LED komut belgesi bulunmadı. Bulunan fwupd Hailuck eklentisi başka USB cihazlar için firmware güncellemesidir (örnek VID/PID 0603:1020); bu BLE klavyenin RGB kontrol protokolü sayılmaz ve kullanılmaz.

### Mevcut karar ve sonraki koşul

Kimlik ve erişim sınırları hakkında teknik kanıt elde edildi. LED kontrolü ve gerçek Rainbow Wave hâlâ UNKNOWN_NOT_VERIFIED. FFE0/FFB3/FFB4 işlevi bilinmiyor; aydınlatma, firmware güncelleme veya başka amaç olduğu iddia edilmiyor. Bu kanalda yazma erişimi de denenmedi.

Aynı servis taramasını tekrarlatmak yeni protokol kanıtı sağlamayacak. Gerçek LED komutu için Hailuck/tedarikçiden bu yazılıma uyan kontrol protokolü veya aynı cihaz üzerinde güvenilir şekilde doğrulanmış bir sürücü gerekiyor. Bağımsız LED bölgeleri veya native wave komutu ayrıca doğrulanmalı. Şu aşamada imkânsızlık ya da başarı kararı verilemez.

Kaynaklar:
- https://developer.android.com/reference/android/Manifest.permission#BLUETOOTH_PRIVILEGED
- https://android.googlesource.com/platform/packages/modules/Bluetooth/+/refs/heads/master/android/app/src/com/android/bluetooth/gatt/GattService.java
- https://www.hailuck.com/download/
- https://www.hailuck.com/faq/
- https://chromium.googlesource.com/chromiumos/third_party/fwupd/+/refs/heads/factory-brya-14517.B/plugins/hailuck/README.md

Ham rapor, cihaz MAC adresi ve uygulama UID'si depoya eklenmemiştir.

