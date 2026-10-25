# Stream til Android

Android-app til M3U-kanallister, afspilning med Media3 og Google Cast.

## Brug

1. Installér debug-APK'en på Android 6 eller nyere. Appen hedder **Stream** og bruger det vedhæftede blå ikon i et sort, gråt og blåt tema.
2. Åbn **☰ → Indstillinger → Kilder**. Vælg **Tilføj M3U** eller **Tilføj Xtream-login**, og angiv eventuelt et navn til kilden. Tryk **Gem og indlæs**.
3. Du kan tilføje flere kilder og skifte mellem dem under **Kilder**. Tryk længe på en kilde for at slette den og dens favoritter.
4. Skift mellem **TV**, **Film** og **Serier**, vælg en gruppe eller søg efter titel. Serverens rækkefølge bevares. Xtream-serier åbner en afsnitsliste med sæson- og afsnitsnumre.
5. Tryk på **stjernen** ved et element for at tilføje eller fjerne det fra **Favoritter**. Favoritter kan komme fra flere kilder og bevarer rækkefølgen, de blev tilføjet i.
6. Åbn **☰ → Favoritter**. Denne visning samler alle dine favoritter uden søgefelt, grupper, TV/Film/Serier-valg, gruppetekster eller antal. Under **☰ → Indstillinger** kan du slå **Vis favoritter, når appen åbner** til. Listen gemmes mellem genstarter og kan vises uden at hente hele kataloget igen. Afspilning og nye logoer kræver netværk.
7. Afspilleren er et rent videovindue uden synlige knapper, tidslinje eller afspilningsstatus ved åbning. Det gælder både almindelig visning og fuldskærm. Tryk på videoen for at vise kontrolpanelet; det skjules automatisk efter 2,5 sekunder eller ved et nyt tryk på billedet. Panelet indeholder afspil/pause, stop, spoling, lyd fra/til, tidslinje og fuldskærm. Under indstillingsikonet findes hastighed, undertekster og lydspor. Spoling og tidslinje er aktive, når streamen understøtter det. Fuldskærm skifter til liggende visning og udfylder skærmen med hele billedet og bevaret billedformat; sorte kanter bruges ved behov. Kontrollerne ligger oven på videoen i begge visninger og optager ingen ekstra plads. Tryk tilbage for at afslutte fuldskærm uden at genstarte videoen. Telefonens statuslinje skjules under afspilning; navigationslinjen skjules også i fuldskærm. Lydstyrken styres med enhedens fysiske lydstyrkeknapper; der er ingen lydstyrkeskyder i appen.
8. Tryk på Cast-ikonet for at forbinde til en Google Cast-enhed, og vælg indhold.

Kilder, login, favoritter og startindstillingen gemmes lokalt med AES-GCM og en nøgle fra Android Keystore. Eksisterende gemte M3U-links flyttes fra den gamle indstilling til den krypterede lagring ved opdatering. Android-backup er slået fra. Den seneste kilde indlæses automatisk, medmindre appen starter på Favoritter.

De 47 PNG-logoer fra den vedhæftede `Kanallogoer.zip` er indbygget i appen og har prioritet ved match mellem kanalnavn og billedfilens navn uden filendelse. Matchning ignorerer store/små bogstaver og mellemrum (fx `DR 1` → `DR1.png`), men bevarer betydningsfulde tegn såsom `+`, så TV3 og TV3+ ikke forveksles. Disse logoer virker også uden netværk og på tidligere gemte favoritter.

Når intet indbygget logo matcher, læses logoer fra M3U's `tvg-logo` eller Xtreams `stream_icon`/`cover`, hentes asynkront og caches. Ved manglende, ugyldige eller utilgængelige logoer vises et standardikon. Logoer understøtter PNG, JPEG, WebP og SVG. Logo-adresser kan være relative, uden anførselstegn i M3U eller URL-kodede; logoanmodninger bruger en browserlignende User-Agent og kun kildens origin som Referer, aldrig kildeadressens loginparametre. Standardadvarsler, der kan indeholde logo-URL’er, er slået fra i billedbiblioteket.

Appen indeholder ingen kanaler eller abonnementer. HTTP understøttes af hensyn til eksisterende IPTV-udbydere; brug HTTPS til links med loginoplysninger.

Cast kræver Google Play-tjenester og normalt samme Wi-Fi som modtageren. Modtageren henter streamen direkte; format, codec og adgang skal understøttes på TV'et. DRM og udbyderspecifikke HTTP-headere understøttes ikke. Xtream-login bruger serverens API til TV, VOD, serier og afsnit. For M3U udledes typen af metadata, grupper og URL-stier (`/live/`, `/movie/`, `/series/`) eller filtypen; lister uden typeoplysninger falder tilbage til TV. M3U kan kun vise indhold og afsnit, som er inkluderet i listen.

Version 1.2.1 gør ikonmotivet 10 % mindre med ekstra gennemsigtig kant for at undgå beskæring i launcherens maske. Versionen retter også et crash ved indlæsning af kilder: billedbibliotekets logningsværdi var ugyldig, så visning af den første kanal udløste en undtagelse.

Version 1.2.2 gør ikonmotivet yderligere 10 % mindre end i 1.2.1 (81 % af originalens størrelse).

Bibliotek er omdøbt til Streamingtjeneste. Favoritter viser kun logo, kanalnavn og stjerne uden kilde- eller gruppenavn.

Version 1.2.3 forbedrer M3U-logoindlæsning og SVG-understøttelse og forenkler Favoritter til en samlet liste uden filtre eller antal. Vælg din M3U-kilde igen under Indstillinger → Kilder for også at opdatere logo-adresser i allerede gemte favoritter.

Version 1.3.0 tilføjer synlige afspillerkontroller, fuldskærm og lydstyrke samt de 47 indbyggede kanallogoer.

Version 1.3.1 retter fuldskærm: videoen bruger hele skærmarealet, kontrolbjælker optager ikke videoplads, pauseknappen skjules automatisk, og systemlinjer holdes skjult i fuldskærm.

Version 1.4.0 genopbygger afspilleren med én samlet tryk-for-at-vise-overlay i både almindelig visning og fuldskærm. Media3’s separate midterknap og kontrolpanel er slået fra.

Version 1.4.1 fjerner lydstyrkeskyderen og bruger FIT i både almindelig visning og fuldskærm, så hele videoen vises uden beskæring.

Version 1.4.2 bruger det blå Stream-banner fra `banner.zip` i topbjælken. Kun tomme, gennemsigtige margener er fjernet; hele logoet skaleres med bevaret billedformat.

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

Tests kontrollerer metadata, relative links, specialtegn i links og Xtream-login, HTTP-redirects, gzip, UTF-16, store lister, serverfejl, API-kataloger, rækkefølgen for sektioner og grupper samt sæsoner og afsnit. Der er også tests af logo-adresser, gemte kilder, favoritter, startindstilling og krypteringens integritet. M3U-hentning accepterer op til 64 MB og viser konkrete fejl uden at vise loginoplysninger. Android-tests med Robolectric gennemgår tilføjelse af M3U- og Xtream-kilder, visning af kanallogoer og sektioner samt favoritter og startindstillingen. Billedtests kontrollerer desuden pixelindholdet i hentede PNG-, JPEG-, WebP- og SVG-logoer samt redirects og sikre headers. Favoritvisningen testes uden filtre og antal på tværs af indholdstyper. Afspiller-tests kontrollerer skjult starttilstand, tryk på videoen, automatisk skjulning uden ændring af videovinduets størrelse, afspillerindstillinger, pause/afspilning, stop, fuldskærm og tilbage uden udskiftning af afspilleren samt faktisk ændring af medielydstyrke og mute. Logo-tests kontrollerer de 47 pakkede filer, navnematch og indlæsning fra assets. De bruger en isoleret lagringsbackend til UI-testene; Android Keystore og faktisk afspilning/casting skal også afprøves på en Android-enhed. Cloudmiljøet har ingen fysisk Cast-modtager.
