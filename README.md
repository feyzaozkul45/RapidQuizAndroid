# Rapid Quiz (Android)

Rapid Quiz, web uygulamasının mobil karşılığıdır: bir kategori seçersin, **20 soruyu soru başına 5 saniyeyle** cevaplarsın, adını skor tablosuna yazarsın ve kategori sıralamalarına bakarsın. Kayıt/giriş, profil, çoklu oyuncu veya çevrimdışı mod yoktur.

Kotlin · Jetpack Compose · Material 3 · MVVM (ViewModel + StateFlow) · Retrofit 3 / OkHttp 5 · kotlinx.serialization · DataStore · `minSdk 26`

## Ekranlar

Kategoriler → Quiz → Sonuç (isim kaydı) → Skor Tablosu. Skor tablosu ana ekrandaki kupa ikonundan da açılır.

- **Kategoriler:** `GET categories/` ile kartlar; karta basınca quiz oturumu başlar.
- **Quiz:** dairesel geri sayım, 4 seçenek, 1 sn doğru/yanlış geri bildirimi. Geri tuşu onay ister.
- **Sonuç:** puan, "N/20 doğru", takma ad formu, tekrar oyna.
- **Skor Tablosu:** kategori chip'leri, ilk 10, kendi satırın vurgulu, aşağı çekip yenileme.

## Önemli tasarım kararları

- Doğru cevap, puan ve süre **sunucuda** hesaplanır; uygulama sunucu yanıtını gösterir.
- Geri sayım `remaining_seconds` değerinden ve `SystemClock.elapsedRealtime` ile başlar. Süre dolunca `choice_id = null` gönderilir.
- `question_mismatch` / `question_not_served` / ağ hatasında `current-question` ile eşitlenir; `answers/` körlemesine tekrar edilmez.
- Sunucu ücretsiz planda uyuyabilir (ilk istek 30–60 sn). Açılışta `health/` atılır, uyanma sırasında yeniden denenir ve 3 sn'den uzun sürerse bilgi gösterilir.
- API adresi yalnızca `BuildConfig.API_BASE_URL` içindedir ([app/build.gradle.kts](app/build.gradle.kts)).
- Takma adlarda istemci tarafı kontrol (uzunluk, karakter, temel küfür filtresi) hızlı geri bildirim içindir; asıl doğrulama sunucudadır.

## Çalıştırma

Gereksinimler: Android Studio (JDK, Android SDK) ve bir emülatör ya da cihaz.

1. Projeyi Android Studio ile aç, Gradle senkronizasyonunu bekle.
2. Windows'ta komut satırından çalıştırırsan `JAVA_HOME` ayarla:
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
   ```
3. Debug sürümünü kur ve aç:
   ```bash
   ./gradlew installDebug
   ```

Uygulama varsayılan olarak yayındaki API'ye (`https://rapid-quiz-api-pqpc.onrender.com/api/v1/`) bağlanır; internet gerekir. Yerel backend ile denemek için `app/build.gradle.kts` içindeki yorum satırındaki `debug` `buildConfigField` örneğini kullan (emülatörde `10.0.2.2` bilgisayarın localhost'udur; düz HTTP için debug'a özel `network_security_config` gerekir).

> Gerçek API ile quiz oynamak sunucuda oturum açar. Skor tablosuna yalnızca isim kaydedersen girersin.

## Testler

```bash
./gradlew test                       # birim testleri (ViewModel, ağ/MockWebServer, isim kuralları)
./gradlew connectedDebugAndroidTest  # Compose UI testleri (emülatör/cihaz gerekir)
```

UI testleri sahte repository ile çalışır, gerçek API'ye gitmez. `ApiLatencyBenchmark` gerçek API'yi ölçer ve `latency=true` verilmedikçe atlanır (ayrıntı: [docs/HANDOFF.md](docs/HANDOFF.md) Bölüm 6).

## Release derleme ve imzalama

```bash
./gradlew assembleRelease   # APK
./gradlew bundleRelease     # Play için AAB
```

R8 release'de açıktır. `keystore.properties` yoksa çıktı imzasız olur. İmzalamak için [docs/SIGNING.md](docs/SIGNING.md) adımlarını izle; keystore ve parolalar repoya girmez.

## Proje yapısı

```
app/src/main/java/com/rapidquiz/app/
├── RapidQuizApp.kt, AppContainer.kt, MainActivity.kt   # elle DI, açılışta health ping
├── navigation/        # tip güvenli rotalar
├── data/              # api (Retrofit, interceptor'lar, ApiError), model, repository, DataStore
├── ui/                # home, quiz, result, leaderboard, common, theme
└── util/              # PlayerName, ProfanityFilter
app/src/test/          # birim testleri
app/src/androidTest/   # Compose UI testleri
docs/                  # HANDOFF.md (durum ve tuzaklar), SIGNING.md
```

## Dokümanlar

- [docs/HANDOFF.md](docs/HANDOFF.md): yapılanlar, kalan işler, teknik tuzaklar
- [docs/SIGNING.md](docs/SIGNING.md): release imzalama
- [CLAUDE.md](CLAUDE.md): geliştirme kuralları
- Gereksinimler: `docs/Rapid Quiz.docx`; API ayrıntıları backend deposundaki `docs/PROJECT.md`
