# Fiziksel cihaz testi

Bu liste henüz çalıştırılmadı. Test cihazı: Samsung Galaxy Tab S11 Ultra / Android 16.

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
