# Stream til Android

Android-app til M3U-kanallister, afspilning med Media3 og Google Cast.

## Brug

1. Installér debug-APK'en på Android 6 eller nyere.
2. Indsæt et HTTP/HTTPS-link til en M3U-liste, og tryk **Indlæs kanaler**.
3. Søg på kanalnavn eller gruppe, og tryk på en kanal for at afspille.
4. Tryk på Cast-ikonet, forbind til en Google Cast-enhed, og vælg en kanal.

Det senest indlæste link gemmes lokalt. Appen indeholder ingen kanaler eller abonnementer. HTTP understøttes af hensyn til eksisterende IPTV-udbydere; brug HTTPS til links med loginoplysninger.

Cast kræver Google Play-tjenester og normalt samme Wi-Fi som modtageren. Modtageren henter streamen direkte; format, codec og adgang skal understøttes på TV'et. DRM, udbyderspecifikke HTTP-headere og loginflows understøttes ikke. Et M3U-link er en kanalliste; HLS-streams i listen kan være M3U8.

## Udvikling

Brug JDK 17 eller 21, Android SDK Platform 35 og Build Tools 34.0.0.

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

I det klargjorte cloudmiljø:

```sh
. /workspace/setup/android-env.sh
cd /workspace/Stream-Android
gradle assembleDebug testDebugUnitTest lintDebug
```

Parserens enhedstests kontrollerer metadata, relative links, ugyldige protokoller og tomme lister. Afspilning, brugergrænseflade og casting skal også afprøves på en Android-enhed. Cloudmiljøet har ingen fysisk Cast-modtager.
