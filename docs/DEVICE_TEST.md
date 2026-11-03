# Fiziksel cihaz testi

Bu liste henüz çalıştırılmadı. Test cihazı: Samsung Galaxy Tab S11 Ultra / Android 16.

## 0.3 uyku deneyi

- Deneyin etkisini fiziksel klavyede doğrulamak gerekiyor. Uygulamasız bir denemeyle 30 saniyelik ışık sönmesi ve 10 dakikalık uykudan sonraki ilk tuş davranışını karşılaştır.
- Klavyeyi seç, ışığını aç ve uyku deneyini başlat. İlk 11 dakika klavyeye/touchpad'e dokunma. 30 saniye sonra ışığın durumunu gözle. 11. dakikada deney devam ederken bir kez yazıp ilk tuşun gecikmesini/kaybolmasını değerlendir; o andan sonraki zamanlayıcı davranışı bu ölçüme dahil değildir.
- 12. dakikada otomatik bitişi, rapordaki successfulReads sayısını ve fiziksel gözlemi ayrı değerlendir. Başarılı GATT okumaları HID uykusunu veya LED kontrolünü kanıtlamaz.
- Başka uygulamaya geç, ekranı kapat/aç ve döndür; aynı deney sürmeli, yeni oturum başlamamalı.
- Uygulamadan veya bildirimden Durdur → yeni okuma gönderilmemeli; kısmi uyanıklık kilidi ve GATT istemcisi bırakılmalı.
- Klavyeyi kapat veya Bluetooth iznini kaldır → bağlantı/okuma hatasıyla durmalı, yeniden bağlanma döngüsü başlamamalı.
- Üretici bilgisi alanı olmayan BLE cihazda deney uygulanamadı demeli; farklı karakteristiklere komut göndermemeli.
- Bildirim izni reddinde çökme olmamalı; uygulamadaki durdurma düğmesi çalışmalı.
- İşlem Android tarafından sonlandırıldıktan sonra yeniden aç → eski running kaydı INTERRUPTED olarak gösterilmeli; deney kendiliğinden yeniden başlamamalı.
- JSON'da characteristicWritesSent 0, sleepSettingChanged false, sleepPrevented ve backlightStayedOn null kalmalı.
- Deney sırasında yazı/touchpad işlevlerinin korunması ayrıca doğrulanmalı. Firmware'in hareketsizlik zamanlayıcısı BLE okumalarından etkilenmiyorsa bu yöntem çözüm değildir.

## Donanım analizi

- İzin reddi → boş liste ve açıklama; tekrar izin ver → liste yenilenir.
- Bluetooth kapalı → sistem ayarlarını aç; geri dönünce liste yenilenir.
- Birden fazla eşleştirilmiş aygıtta doğru adresin seçildiğini doğrula.
- Classic cihazda BLE bağlantısı denenmediğini doğrula.
- BLE aygıtta başarı, bağlantı hatası ve 20 saniye zaman aşımını gözle.
- SDP 12 saniye içinde dönmezse belirsizlik raporlandığını doğrula.
- Başka aygıt seçildiğinde eski raporun kendi ad/adres/zaman damgasıyla ayırt edildiğini doğrula.
- Analizi durdur / arka plana geç / ekranı döndür; GATT kapanmalı, otomatik döngü başlamamalı. Kesilmiş rapor kesin sonuç sayılmamalı.
- Analiz sırasında ve sonrasında klavyede yazı/touchpad/işlev tuşları çalışmalı.
- JSON'u kaydet, dosyayı aç; writesSent 0 ve lightingVerdict UNKNOWN_NOT_VERIFIED kalmalı.
- Yeniden açınca son rapor ve seçili adres hatırlanmalı.
- Uçak modu / izin iptali / klavye kapatma durumlarında çökme olmamalı.

Donanım değerlendirmesi: UUID ve WRITE bayrağı aydınlatma desteğini kanıtlamaz. Olumsuz bağlantı sonucu da donanımın imkânsız olduğunu kanıtlamaz. Nihai hedef fiziksel Rainbow Wave, bu sürüm yalnızca ilk kanıt toplama adımıdır.

## 0.2 meta veri testleri

- metadataReads içinde her alan SUCCESS, GATT_STATUS, ACCESS_DENIED, NOT_STARTED veya TIMEOUT olarak ayrı raporlanmalı.
- PnP ID yalnızca tam 7 baytsa çözülmeli; vendorIdSource (Bluetooth SIG / USB-IF) korunmalı.
- Aynı 2A4D UUID'sinin dört örneği characteristicInstanceId ile ayrılmalı.
- Report Reference sadece rapor kimliği/türünü içerir; canlı giriş raporları (2A4D, 2A22) okunmamalı.
- 4 saniye zaman aşımında kalan GATT istekleri gönderilmemeli; bağlantı kapanmalı.
- Analiz sırasında arka plana geçince meta veri kuyruğu ve zamanlayıcısı temizlenmeli.
- writesSent sıfır kalmalı; notify/CCCD yazımı yapılmamalı.

