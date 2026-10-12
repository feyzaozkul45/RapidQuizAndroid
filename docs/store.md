# Mağaza metni ve Data safety (Rapid Quiz)

Google Play Console'a girilecek metinler. Dil: Türkçe (tr-TR). Uygulama kimliği: `com.rapidquiz.app`.
Kaynak: `docs/Rapid Quiz.docx` Bölüm 12 ve uygulamanın gerçek davranışı (kod). "Doğrula" işaretli yerler uygulama kodundan değil sunucu tarafından (backend) bağlıdır; formu göndermeden önce backend sahibiyle teyit et.

## 1. Uygulama bilgileri

| Alan | Değer |
|---|---|
| Uygulama adı (≤30 karakter) | Rapid Quiz |
| Kısa açıklama (≤80 karakter) | 5 saniyede bir soru, 20 soruda bir sıralama. |
| Kategori | Oyunlar > Bilgi yarışması (Trivia) |
| Etiketler | Bilgi yarışması, Quiz, Genel kültür |
| Fiyat / reklam | Ücretsiz, reklamsız, uygulama içi satın alma yok |

## 2. Tam açıklama (≤4000 karakter)

```
Rapid Quiz, hızlı düşünenlerin bilgi yarışması.

Bir kategori seç, 20 soruyu soru başına sadece 5 saniyede cevapla. Her doğru cevap 100 puan kazandırır (en fazla 2000), toplam puanın sıralamandaki yerini belirler. Oyun bittiğinde takma adını yazıp skor tablosuna girebilirsin.

NASIL OYNANIR?
• Ana ekrandan bir kategori seç.
• Her soruda dört şıktan birini 5 saniye içinde işaretle. Süre dolarsa soru cevapsız sayılır.
• Her cevaptan sonra doğru şıkkı görürsün.
• 20 soru bitince toplam puanını ve doğru sayını gör.
• İstersen takma adını kaydet ve kategori skor tablosunda ilk 10'a gir.

ÖZELLİKLER
• Fizik, yazılım, bilgisayar mühendisliği, yapay zeka, ülkeler ve daha fazlası
• Her kategori için ayrı skor tablosu
• Sıralamadaki yerini ve kendi satırını görme
• Aşağı çekerek skor tablosunu yenileme
• Koyu tema ve erişilebilirlik desteği (ekran okuyucu, büyük yazı tipi)
• Hesap açmak, giriş yapmak veya kişisel bilgi vermek gerekmez

Puanlar ve süreler sunucuda hesaplanır, bu yüzden herkes için adil bir yarış.

Not: Uygulama internet bağlantısı gerektirir. Sunucu bir süre kullanılmadıysa ilk açılışta kısa bir bekleme olabilir.
```

> Kategori listesi sunucudan geldiği için açıklamada yalnızca örnek verildi; sunucudaki gerçek kategorilerle uyumlu tut.

## 3. Ekran görüntüleri (sen alacaksın)

Doküman Bölüm 12: 5 kategori ekranı + quiz + skor tablosu. Önerilen sıra (telefon, dikey, en az 2, en çok 8 görüntü):

1. Kategoriler ekranı (kategori kartları görünür)
2. Quiz ekranı: soru ve geri sayım halkası
3. Quiz ekranı: doğru/yanlış geri bildirimi
4. Sonuç ekranı (puan, "N/20 doğru", isim alanı)
5. Skor Tablosu (chip'ler ve kendi satırın vurgulu)
6. (isteğe bağlı) Koyu temada kategoriler

Ayrıca Play'in istediği grafikler: uygulama ikonu 512×512 PNG (`ic_launcher` tasarımından dışa aktar) ve öne çıkan grafik 1024×500.

## 4. Gizlilik özeti (mağaza sayfası için)

> Hesap ve kişisel veri toplanmaz. Yalnızca kullanıcının kendi girdiği takma ad, skor tablosunda herkese açık gösterilir.

Play "Gizlilik politikası" alanı **zorunludur**; henüz bir gizlilik politikası sayfası yok. Yayından önce herkese açık bir URL'de yayınla (örnek metin Bölüm 6).

## 5. Data safety formu önerisi

Form uygulamanın gerçek veri akışına göre dolduruldu. Veri akışı:
- Uygulama yalnızca HTTPS ile `https://rapid-quiz-api-pqpc.onrender.com/api/v1/` adresine bağlanır.
- Gönderilenler: seçilen kategori, quiz cevapları (soru ID + seçenek ID), istemci türü/sürümü başlıkları (`X-Client-Type: android`, `X-Client-Version`), kategori başına son görülen en fazla 40 soru ID'si, ve kullanıcı isim yazarsa takma ad.
- Cihazda tutulanlar: yalnızca DataStore'daki son soru ID'leri (hesap, kişisel veri yok).
- Reklam, analitik, çökme raporlama veya üçüncü taraf SDK'sı yoktur; konum, rehber, kamera vb. izin istenmez (yalnızca `INTERNET`).

| Soru | Cevap |
|---|---|
| Uygulama kullanıcıdan veya cihazdan veri topluyor mu? | Evet |
| Toplanan tüm veriler aktarım sırasında şifreleniyor mu? | Evet (HTTPS/TLS) |
| Kullanıcılar verilerinin silinmesini isteyebiliyor mu? | **Doğrula.** Uygulamada hesap ve silme ekranı yok. Silme talebi için bir iletişim yolu (e-posta) belirleyip Play'e "silme talebi için URL/e-posta" olarak gir. |

Veri türleri:

| Kategori > Tür | Toplanıyor mu | Paylaşılıyor mu | Amaç | İsteğe bağlı mı |
|---|---|---|---|---|
| Kişisel bilgiler > Ad (takma ad) | Evet | Hayır (üçüncü tarafa değil; skor tablosunda uygulama kullanıcılarına gösterilir) | Uygulama işlevselliği (skor tablosu) | Evet, isim girmek zorunlu değil |
| Uygulama etkinliği > Uygulama etkileşimleri (quiz cevapları, puan) | Evet | Hayır | Uygulama işlevselliği | Hayır (oyunun parçası) |
| Uygulama bilgisi ve performans > (yok) | Hayır | – | – | – |
| Konum, finansal, sağlık, mesajlar, fotoğraf/video, ses, kişiler, takvim | Hayır | – | – | – |
| Cihaz veya diğer kimlikler | Hayır (uygulama kimlik toplamaz) | – | – | – |

Notlar:
- **Doğrula (backend):** Sunucu istek günlüklerinde IP adresi tutuyor olabilir (barındırma sağlayıcısı Render). Tutuluyorsa ve kullanıcıyla ilişkilendirilebiliyorsa "Cihaz veya diğer kimlikler" ya da ilgili türü forma ekle.
- **Doğrula (backend):** Takma ad ve skorun ne kadar süre saklandığını belirle (silme politikası için).
- Takma ad herkese açık gösterildiği için formda "kullanıcı tarafından oluşturulan içerik gibi herkese açık" olduğunu belirt. Uygulama içi küfür/hakaret kontrolü yalnızca istemcidedir; asıl denetim sunucuda olmalı.
- Veriler "paylaşılan" sayılmaz çünkü yalnızca kendi sunucuya gider; barındırma sağlayıcısı işleyici (service provider) konumundadır.

## 6. Örnek gizlilik politikası metni

```
Rapid Quiz Gizlilik Politikası

Rapid Quiz hesap veya giriş gerektirmez; ad, e-posta, telefon, konum, rehber veya benzeri kişisel bilgi toplamaz.

Topladığımız veriler
• Takma ad (isteğe bağlı): Oyun sonunda yazarsan skor tablosunda uygulamanın diğer kullanıcılarına herkese açık gösterilir. Gerçek adını yazmak zorunda değilsin.
• Oyun verisi: seçtiğin kategori, cevapların ve puanın skor tablosunu oluşturmak için sunucuya gönderilir.
• Teknik veri: uygulama sürümü ve istemci türü gönderilir; barındırma sağlayıcısı olağan sunucu günlüklerinde IP adresini tutabilir.

Cihazında saklananlar: daha önce gördüğün soruların kimlikleri (sorular tekrar etmesin diye).

Verilerin şifrelenmesi: Tüm iletişim HTTPS ile yapılır.
Üçüncü taraflar: Reklam, analitik veya takip aracı kullanmıyoruz.
Silme talebi: <iletişim e-postası> adresine yazarak takma adının ve skorunun silinmesini isteyebilirsin.
Çocuklar: Uygulama 13 yaş altı çocuklara yönelik değildir.
Değişiklikler bu sayfada yayımlanır.

İletişim: <iletişim e-postası>
```

## 7. Diğer Play Console formları (öneri)

- **Hedef kitle:** 13+ (çocuklara yönelik değil; Families politikasından kaçınmak için).
- **İçerik derecelendirme (IARC):** Şiddet, cinsel içerik, kumar, uyuşturucu: yok. "Kullanıcılar etkileşimde bulunur / içerik paylaşır": takma ad skor tablosunda herkese göründüğü için "Evet" (serbest metin değil, kısa takma ad) işaretle.
- **Reklamlar:** Yok.
- **Uygulama erişimi:** Özel erişim gerekmiyor (giriş yok).
- **Sağlık/finans/haber uygulaması:** Hayır.
