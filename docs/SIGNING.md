# Release imzalama

Release APK/AAB, kök dizindeki `keystore.properties` varsa imzalanır; yoksa build bozulmaz, çıktı imzasız (`app-release-unsigned.apk`) olur. `keystore.properties` ve `*.jks` git'e girmez (`.gitignore`).

## 1. Keystore oluştur (bir kez)

PowerShell'de (keystore'u repo DIŞINDA tut):

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
New-Item -ItemType Directory -Force C:\Users\THINKPAD\keystores
& "$env:JAVA_HOME\bin\keytool.exe" -genkeypair -v -keystore C:\Users\THINKPAD\keystores\rapidquiz.jks -alias rapidquiz -keyalg RSA -keysize 2048 -validity 10000
```

keytool şifre ve kimlik bilgilerini (ad, kurum, ülke) sorar. Şifreleri bir şifre yöneticisine kaydet.

## 2. keystore.properties hazırla

```powershell
Copy-Item keystore.properties.example keystore.properties
```

İçini doldur: `storeFile` (ör. `C:/Users/THINKPAD/keystores/rapidquiz.jks`), `storePassword`, `keyAlias` (`rapidquiz`), `keyPassword`.

## 3. Derle ve doğrula

```powershell
.\gradlew assembleRelease
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\<sürüm>\apksigner.bat" verify --verbose app\build\outputs\apk\release\app-release.apk
```

Play Store için AAB: `.\gradlew bundleRelease`.

## 4. Yedekle

`.jks` dosyasını ve şifreleri en az iki ayrı yere yedekle (ör. şifreli USB + şifre yöneticisi). Anahtar kaybolursa Play'de bu uygulamaya güncelleme çıkarılamaz (Play App Signing'e kayıt olmak bu riski azaltır).
