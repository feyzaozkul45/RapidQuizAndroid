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
- Release imzalama: `keystore.properties` (kök, git'e girmez) varsa `signingConfig` oluşur, yoksa release imzasız kalır. `keystore.properties.example` ve `docs/SIGNING.md` var. Keystore'u kullanıcı kendisi oluşturacak (henüz oluşturulmadı).
- Skor tablosu chip satırı seçili chip'e kaydırılır (`rememberLazyListState` + `LaunchedEffect`); `LeaderboardScreenTest.selectedChipIsScrolledIntoView`.
- Takma ad filtresi: `util/ProfanityFilter.kt` (TR+EN kısa liste, leet/aksan/tekrar katlama, kısa kelimeler yalnızca tam sözcük). `PlayerName.validate` → `Problem.INAPPROPRIATE`. Yalnızca istemci tarafı; asıl koruma sunucuda olmalı.
- API süre ölçümü: `androidTest/.../ApiLatencyBenchmark.kt` (bkz. Bölüm 6).

## 2. Commit'lenmemiş / doğrulanmamış değişiklikler
Yok. Çalışma ağacı temiz olmalı (`git status`).
Testlerin son durumu: `./gradlew spotlessCheck test connectedDebugAndroidTest` → **44 birim testi + 28 UI testi geçti** (29. test `ApiLatencyBenchmark`, `latency=true` verilmedikçe bilerek atlanır; raporda "failure" gibi görünür ama Assume atlamasıdır). Emülatör: `emulator-5554` (Medium_Phone).

## 3. Push durumu
- `origin/main` (https://github.com/feyzaozkul45/RapidQuizAndroid.git) güncel; push durumunu doğrulamak için: `git log origin/main..main`. Kullanıcı push'u her seferinde sohbette açıkça onaylar (force push yok).

## 4. Kalan işler (sıralı)
İmzalama, push, chip kaydırma, isim filtresi ve süre ölçümü bitti. Kalanlar:
1. **Soğuk başlangıç süresi ölçülmedi — bilerek.** Sunucu eğitim projesi için hocanın sunucusu; kullanıcı prod'a ek ölçüm isteği atılmamasını istedi. Tekrar ölçülmesin (kullanıcı yeniden istemedikçe).
2. Doküman Bölüm 12 yayın hazırlığı kalanları: mağaza metni, ekran görüntüleri, Play "Data safety" bilgisi (hesap/kişisel veri yok, yalnızca takma ad herkese açık).
3. Elle kontrol listesi (doküman Bölüm 11): uçak modu (hata + tekrar dene), soru sırasında arka plana atma, 15 dk sonra soğuk başlangıç, 360 dp ekran, büyük yazı tipi, geri tuşu onayı. Koyu temada ✓/✕ renkleri ve TalkBack okuması gözle/gerçek cihazda doğrulanmadı.
4. Kod biçimi: Spotless + ktlint 1.8.0 kuruldu (`./gradlew spotlessCheck` / `spotlessApply`, kurallar `.editorconfig` + kök `build.gradle.kts`). `ktlint_official` stili, sondaki virgüller korunur (`editorConfigOverride`), max satır 140.
5. Home → Quiz gezinmesi `HomeNavigationTest` ile test edildi (3 test).
6. Mağaza metni ve Data safety: `docs/store.md`. Ekran görüntülerini kullanıcı alacak; elle kontrol listesi kullanıcıda.

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

## 6. API süre ölçümü (sıcak sunucu)
- Kod: `app/src/androidTest/.../ApiLatencyBenchmark.kt` — yalnızca debug test APK'sında bulunur, release'e girmez. Gerçek API'ye gider (her turda bir deneme oturumu açar), bu yüzden `latency=true` verilmedikçe atlanır:
  `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.rapidquiz.app.ApiLatencyBenchmark -Pandroid.testInstrumentationRunnerArguments.latency=true` (turu `-P...rounds=N` ile değiştir). Sonuç: `adb logcat -s ApiLatency`.
- Sonuçlar (2026-10-12, emülatör, 5 tur, sunucu SICAKTI; ms):

| Çağrı | İlk | Sıcak medyan | Min–maks |
|---|---|---|---|
| categories | 1185 | 119 | 111–492 |
| create_session | 160 | 124 | 118–128 |
| current_question | 141 | 169 | 139–318 |
| answer | 139 | 221 | 129–241 |
| leaderboard | 111 | 125 | 110–128 |

- "İlk" categories değeri DNS+TLS kurulumunu içerir. Soğuk başlangıç (sunucu uykudayken) ölçülmedi, bkz. Bölüm 4 madde 1.
- Yükleme göstergeleri: `CircularProgressIndicator` Home/Quiz/Result/`LoadingView`'de; sunucu uyanıyorsa 3 sn sonra `WakingBanner`.
