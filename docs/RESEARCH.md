# Protokol araştırması — 31 Ekim 2026

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
