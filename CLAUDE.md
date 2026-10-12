# rapid-quiz-android

Proje gereksinimleri: docs/Rapid Quiz.docx (API: rapid-quiz-backend/docs/PROJECT.md)

## Kurallar
- Kotlin + Jetpack Compose + Material 3, MVVM (ViewModel + StateFlow)
- API taban adresi yalnızca BuildConfig.API_BASE_URL; koda başka yerde adres yazılmaz
- Doğru cevap, puan ve süre hesabı istemcide yapılmaz; sunucu yanıtı gösterilir
- Geri sayım remaining_seconds'tan ve SystemClock.elapsedRealtime ile başlar
- Süre dolunca choice_id=null gönderilir; question_mismatch/question_not_served'da current-question ile eşitle
- Her yeni ViewModel/yardımcı birim testiyle gelir
- Metinler strings.xml'de (Türkçe)
- Paket adı / applicationId: com.rapidquiz.app

## Komutlar
- ./gradlew test · ./gradlew connectedAndroidTest · ./gradlew spotlessCheck (biçim: spotlessApply)
- Windows'ta JAVA_HOME: C:\Program Files\Android\Android Studio\jbr

## Oturum devri
- Çalışmaya başlamadan önce docs/HANDOFF.md dosyasını oku (durum, kalan işler, teknik tuzaklar orada).
