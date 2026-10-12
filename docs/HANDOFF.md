# HANDOFF — Rapid Quiz Android

Son güncelleme: 2026-10-12. Gereksinimler: `docs/Rapid Quiz.docx`. Kurallar: `CLAUDE.md`.

## 1. Tamamlanan ve commit'lenen işler
- Tüm ekranlar: Kategoriler, Quiz, Sonuç (isim formu), Skor Tablosu; Navigation Compose (tip güvenli rotalar), elle DI (`AppContainer`).
- Ağ katmanı: Retrofit 3 + OkHttp 5 + kotlinx.serialization, başlık ve soğuk başlangıç interceptor'ları, `ApiError` eşlemesi, DataStore ile `recent_question_ids`.
- Paket adı / applicationId `com.rapidquiz.app` (dokümanla aynı).
- R8 açık (tam uygulama kodu), keep kuralları `app/src/main/keepRules/rules.keep`. Release APK emülatörde 20 soruluk tur oynanarak doğrulandı.
- Koyu tema (`values-night`) ve erişilebilirlik (canlı bölge, ✓/✕ simgeleri, birleşik skor satırı açıklamaları).
- Bağımlılık güncellemeleri, 3 ayrı commit: (a) Kotlin 2.4.21 + serialization 1.11.0 + coroutines 1.11.0, (b) Compose BOM 2026.09.00 + Navigation 2.10.2 + Lifecycle 2.11.0 + core-ktx 1.19.1, (c) OkHttp 5.5.0 + DataStore 1.2.1. compileSdk/targetSdk (37) değişmedi.
- `ON_RESUME` düzeltmesi (bkz. Bölüm 5) ve `QuizLifecycleTest`.
- Sonuç ekranı / isim formu UI testleri (`ResultScreenTest`, 11 test).
- `.gitignore`: keystore, `*.jks`, `*.pem`, `.env`, `keystore.properties`, `.idea/`.

## 2. Commit'lenmemiş / doğrulanmamış değişiklikler
Yok. Çalışma ağacı temiz olmalı (`git status`).
Testlerin son durumu (bu dosya yazılmadan hemen önce koşuldu): `./gradlew test connectedDebugAndroidTest` → **38 birim testi + 23 UI testi, hepsi geçti**. Emülatör: `emulator-5554` (Medium_Phone).

## 3. Push durumu
- `origin/main` (https://github.com/feyzaozkul45/RapidQuizAndroid.git) şu commit'e kadar push edildi: `e3db0d3` (koyu tema ve erişilebilirlik).
- **Push EDİLMEDİ:** bağımlılık güncellemeleri (a, b, c), `ON_RESUME` düzeltmesi, Sonuç ekranı testleri, bu HANDOFF commit'i. Doğrulamak için: `git log origin/main..main`.

## 4. Kalan işler (sıralı)
Kullanıcının son iş mesajı 4 maddeydi (1: bağımlılıklar + release/R8 turu, 2: ON_RESUME, 3: Sonuç testleri, 4: imzalama). 1–3 bitti. Kalanlar:
1. **İmzalama (mesajdaki 4. madde):** `app/build.gradle.kts`'e kök dizindeki `keystore.properties`'i okuyan `signingConfig` ekle (dosya yoksa build bozulmasın, unsigned kalsın). `providers.fileContents(...)` ile oku (configuration cache dosya değişimini izlesin). `keystore.properties.example` (gerçek değer yok) commit'le. Geçici keystore ile `assembleRelease` + `apksigner verify` dene, sonra geçici dosyayı sil. Kullanıcı keystore'u kendisi oluşturacak: `keytool -genkeypair -v -keystore <yol>.jks -alias rapidquiz -keyalg RSA -keysize 2048 -validity 10000` adımlarını `docs/SIGNING.md`'ye ve yanıta adım adım yaz; keystore repo dışında (ör. `C:\Users\THINKPAD\keystores\`) tutulmalı, yedeklenmeli (kaybedilirse Play'de güncelleme çıkamaz).
2. Tüm commit'leri `origin/main`'e push et (kullanıcı ilk turda push'u onayladı; force push yok).
3. Doküman Bölüm 12 yayın hazırlığı kalanları: mağaza metni, ekran görüntüleri, Play "Data safety" bilgisi (hesap/kişisel veri yok, yalnızca takma ad herkese açık).
4. Elle kontrol listesi (doküman Bölüm 11): uçak modu (hata + tekrar dene), soru sırasında arka plana atma, 15 dk sonra soğuk başlangıç, 360 dp ekran, büyük yazı tipi, geri tuşu onayı. Koyu temada ✓/✕ renkleri ve TalkBack okuması gözle/gerçek cihazda doğrulanmadı.
5. Doküman Bölüm 11 / 13'te geçen ktlint/spotless henüz kurulmadı (CLAUDE.md'den `ktlintCheck` çıkarıldı). İstenirse ekle.
6. Compose UI tarafında henüz test yok: Ana ekran (kategori kartı → quiz başlatma). Dokümandaki "tam bir quiz turu" testi `QuizScreenTest`'te var ama Home → Quiz gezinmesi uçtan uca test edilmedi.

## 5. Teknik kararlar ve tuzaklar
- **ON_RESUME:** `LifecycleEventEffect(ON_RESUME)` ilk composition'da da tetiklenir; eskiden `resync()` bu yüzden current-question'ı ikinci kez çağırıyordu. Şimdi `QuizScreen` ON_STOP'ta `viewModel.onAppBackgrounded()` çağırır; `resync()` bayrak yoksa hiçbir şey yapmaz, varsa bayrağı sıfırlar. Yalnızca ON_PAUSE (ör. dialog) eşitleme tetiklemez. Testler: `QuizViewModelTest` (resyncWithoutBackgrounding…, resyncRunsOncePerBackgroundTrip), `QuizLifecycleTest`.
- **Fake repo (`androidTest/.../FakeQuizRepository.kt`):** `currentQuestion` sunucu gibi davranır — cevaplanana kadar AYNI soruyu döndürür (kuyruktan tüketmez; `answer()` tüketir). Aksi halde resync/ON_RESUME ikinci çağrıda `NoSuchElementException` fırlatıp Error durumuna düşürür (ilk UI test hatalarının nedeni buydu). `questionCalls`, `savedNames`, `nameError`, `sessionError` ile çağrılar izlenir/hata enjekte edilir. Quiz UI testlerinde geri tuşu testleri `remaining = 60.0` kullanır; yoksa süre dolup Feedback/Error'a geçer.
- **R8 / packageScope:** AGP şablonu `optimization { packageScope = setOf("androidx.**","kotlin.**","kotlinx.**") }` koymuştu; bu yüzden R8 uygulama kodunu hiç küçültmüyordu (mapping.txt'de 0 `com.rapidquiz` sınıfı). `packageScope` kaldırıldı. Release'i değiştirirsen mapping.txt'de `com.rapidquiz.app` sınıflarını kontrol et ve emülatörde gerçek API ile bir tur oyna (serialization keep kuralları yalnızca release'de sınanır).
- **Release APK'yı yerelde denemek:** unsigned çıkar. Debug anahtarıyla imzala: `apksigner sign --ks ~/.android/debug.keystore --ks-pass pass:android` (JAVA_HOME ve PATH'te jbr/bin gerekir). Bu yalnızca yerel deneme içindir.
- **Skor tablosu satırı** tek erişilebilirlik düğümüdür (`clearAndSetSemantics`); UI testlerinde metinle değil `contentDescription` ile aranır (`rowExists`, `onNodeWithContentDescription(..., substring = true)`).
- **Sunucu zamanlaması:** süre sunucuda ölçülür; geri sayım `remaining_seconds`'tan `SystemClock.elapsedRealtime` ile başlar. `answers/` ağ hatasında körlemesine tekrar edilmez, `current-question` ile eşitlenir (`question_mismatch`, `question_not_served`, `network_error`).
- **session_id yalnızca o oturumun kategorisi için** skor tablosuna gönderilir (başka chip seçilince gönderilmez).
- **libs.versions.toml tuzağı:** `[versions]` ve `[libraries]`'de aynı isimli satırlar var (`okhttp`, `retrofit`…). Toplu `sed` ile sürüm değiştirirken `^okhttp = "5.1.0"` gibi sürüm değerini de içeren tam desen kullan; yoksa kütüphane satırı bozulur (bir kez oldu).
- **Araç tuzakları (Windows):** `JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"` gerekir (PATH'te java yok). Bash aracında uzun heredoc'lar bazen "unexpected EOF" ile çöküyor; dosyaları Write ile yaz. `python -I` ortam değişkenlerini yok sayar (stdout cp1254 → Unicode yazdırmada hata), çıktıyı dosyaya yaz. `git add -A` yerine açık yol ver (`.idea/` takip edilmiyor).
- **Flaky:** bir seferinde UI test koşusunda (emülatör süreci) tek test boş hatayla düştü, tekrar koşunca 9/9 geçti. Tekrarlarsa `adb logcat` bak.
- **Gerçek API:** quiz oynamak prod sunucuda oturum açar (Render, ilk istek 30–60 sn sürebilir). İsim kaydedilmedikçe skor tablosuna girmez.
- **Skor tablosu (bilinçli tercih):** Aynı isim tekrarı ve vurgu bilinçli tercih. Her oyun ayrı satır olarak gösterilir, aynı isim birden fazla kez görünebilir. Vurgu yalnızca bu oturumun satırındadır (`isMe`), 0 puanlı olsa bile. Bu davranış kod değişikliği gerektirmez.
- **Sürüm kontrolü:** son kontrol edilen en güncel kararlı sürümler bu dosyadaki listelerdir; Retrofit 3.0.0, Turbine 1.2.1, activity-compose 1.13.0, AGP 9.4.1 zaten güncel idi.
