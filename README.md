# Stream til Android

Android-app til M3U-kanallister, afspilning med Media3 og Google Cast.

## Brug

1. Installér debug-APK'en på Android 6 eller nyere.
2. Indsæt et HTTP/HTTPS-link til en M3U-liste, og tryk **Indlæs kanaler**.
3. Alternativt: Tryk **Tilføj Xtream-login**, angiv server (med HTTP/HTTPS og eventuel port), brugernavn og adgangskode. Appen henter TV, film, serier og grupper via Xtreams `player_api.php`. Login gemmes ikke.
4. Skift mellem **TV**, **Film** og **Serier**. Vælg en gruppe eller søg på titel. Serverens rækkefølge for grupper og indhold bevares. Tryk på indhold for at afspille; Xtream-serier åbner en afsnitsliste med sæson- og afsnitsnumre.
5. Tryk på Cast-ikonet, forbind til en Google Cast-enhed, og vælg en kanal.

Det senest indlæste link gemmes lokalt. Appen indeholder ingen kanaler eller abonnementer. HTTP understøttes af hensyn til eksisterende IPTV-udbydere; brug HTTPS til links med loginoplysninger.

Cast kræver Google Play-tjenester og normalt samme Wi-Fi som modtageren. Modtageren henter streamen direkte; format, codec og adgang skal understøttes på TV'et. DRM og udbyderspecifikke HTTP-headere understøttes ikke. Xtream-login bruger serverens API til TV, VOD, serier og afsnit. For M3U udledes indholdstypen af metadata, grupper og URL-stier (`/live/`, `/movie/`, `/series/`) eller filtypen; lister uden oplysninger om typen kan ikke altid inddeles sikkert og falder tilbage til TV. M3U-lister kan kun vise det indhold og de afsnit, der faktisk er inkluderet. Et M3U-link er en kanalliste; HLS-streams i listen kan være M3U8.

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

Tests kontrollerer metadata, relative links, specialtegn i links og Xtream-login, HTTP-redirects, gzip, UTF-16, store lister, serverfejl, API-kataloger, rækkefølgen for sektioner og grupper samt sæsoner og afsnit. M3U-hentning accepterer op til 64 MB og viser konkrete fejl uden at vise loginoplysninger. Afspilning, brugergrænseflade og casting skal også afprøves på en Android-enhed. Cloudmiljøet har ingen fysisk Cast-modtager.
