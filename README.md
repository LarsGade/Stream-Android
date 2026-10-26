# Stream til Android

Android-app til M3U-kanallister, afspilning med Media3 og Google Cast.

## Radio og widgets

Åbn **☰ → Radio** for danske stationer fra Radio Browser. Appen henter kun stationer med landekoden DK og gyldige HTTP(S)-streams, skjuler kendte defekte stationer og forsøger alternative Radio Browser-servere ved fejl. Søg efter stationsnavn, tryk på en station for at afspille, og brug stjernen for at tilføje den til **Favoritter → Radio**. Radio kan fortsætte i baggrunden med Androids mediebetjening, lydfokus og stop, når hovedtelefoner frakobles. Radio og lokal video afspilles ikke samtidigt.

I **☰ → Favoritter → Radio**: Hold og slip en station for at vælge **Omdøb** eller **Fjern fra favoritter**; hold og træk for at ændre rækkefølgen. Navn og rækkefølge gemmes separat fra **Favoritter → TV**. Et lokalt navn som **DR P1**, **DR P3** eller **Nova** bruger det tilsvarende indbyggede logo fra de uploadede filer, når der findes et match. Det oprindelige stations-id og stream-link bevares ved omdøbning.

Tilføj en widget på Androids startskærm: Hold på et tomt område → **Widgets → Stream Radio**, og vælg en station fra Radio-fanen under Favoritter. Hver widget kan have sin egen station. Widgetten er 1 × 1 felt med gennemsigtig baggrund og kun logo. Et kort afspil/stop-ikon vises oven på logoet, når du trykker. Tryk **Afspil/Stop** på widgetten uden at åbne appen; tryk hvor som helst på widgetten for at afspille eller stoppe den valgte station. Ændrede favoritnavne og indbyggede logoer opdateres på widgets. Hvis en favorit slettes, beder widgetten om en ny station. Radio kræver internet, og en station kan være utilgængelig, selvom den findes i Radio Browser.

## Brug

1. Installér debug-APK'en på Android 6 eller nyere. Appen hedder **Stream** og bruger det vedhæftede blå ikon i et sort, gråt og blåt tema.
2. Åbn **☰ → Kilder**. Vælg **Tilføj M3U** eller **Tilføj Xtream-login**, og angiv eventuelt et navn til kilden. Tryk **Gem og indlæs**.
3. Tilføj navngivne XMLTV- eller XMLTV.gz-links under **☰ → TV-Guide**. Tryk på en guide for at redigere navn/link eller slette den. Vælg den ønskede TV-guide i formularen, når en kilde tilføjes eller redigeres. Vælg **Ingen TV-Guide** for at fjerne tilknytningen.
4. Du kan tilføje flere kilder og skifte mellem dem under **Kilder**. Tryk længe på en kilde for at vælge **Redigér kilde** eller **Slet kilde**. Redigering bevarer kildens id og favoritter; opdatering fornyer kanaloplysninger og stream-links, når der findes et entydigt match. Sletning af en kilde sletter dens favoritter. Sletning af en TV-guide bevarer kilder og favoritter og fjerner kun deres EPG-tilknytning.
5. Skift mellem **TV**, **Film** og **Serier**, vælg en gruppe eller søg efter titel. Serverens rækkefølge bevares. Xtream-serier åbner en afsnitsliste med sæson- og afsnitsnumre.
6. I **Streamingtjeneste**: Tryk på **stjernen** ved et element for at tilføje eller fjerne det fra **Favoritter**. Favoritter kan komme fra flere kilder og bevarer den valgte rækkefølge.
7. Åbn **☰ → Favoritter**. Listen vises uden overskrift og uden stjerner. **Hold og slip** en kanal for at åbne menuen **Fjern fra favoritter**. **Hold og træk** kanalen til en ny placering for at ændre rækkefølgen; flytningen gemmes ved slip og bevares ved genstart. Træk tæt på over-/underkanten for automatisk rulning. Afbrydelse af trækket bevarer den tidligere rækkefølge. Her vises den aktuelle udsendelse med grå skrift under TV-kanalens navn, hvis dens kilde har en tilknyttet EPG. Kanal-id (M3U `tvg-id` / Xtream `epg_channel_id`) prioriteres; ellers bruges et entydigt XMLTV-kanalnavn eller M3U `tvg-name`. Ukendte/udløbne programmer vises ikke som aktuelle. Listen har ingen søgning, gruppevalg, TV/Film/Serier-valg, kildenavne eller antal. Direkte i **☰-menuen** kan du slå **Vis favoritter, når appen åbner** til. Listen gemmes mellem genstarter og kan vises uden at hente hele kataloget igen. Afspilning og nye logoer kræver netværk.
8. Afspilleren er et rent videovindue uden synlige knapper, tidslinje eller afspilningsstatus ved åbning. Det gælder både almindelig visning og fuldskærm. Tryk på videoen for at vise kontrolpanelet; det skjules automatisk efter 2,5 sekunder eller ved et nyt tryk på billedet. Panelet indeholder afspil/pause, stop, spoling, lyd fra/til, tidslinje og fuldskærm. Under indstillingsikonet findes hastighed, undertekster og lydspor. Spoling og tidslinje er aktive, når streamen understøtter det. Fuldskærm skifter til liggende visning og udfylder skærmen med hele billedet og bevaret billedformat; sorte kanter bruges ved behov. Kontrollerne ligger oven på videoen i begge visninger og optager ingen ekstra plads. Tryk tilbage for at afslutte fuldskærm uden at genstarte videoen. Telefonens statuslinje skjules under afspilning; navigationslinjen skjules også i fuldskærm. Lydstyrken styres med enhedens fysiske lydstyrkeknapper; der er ingen lydstyrkeskyder i appen.
9. Tryk på Cast-ikonet for at forbinde til en Google Cast-enhed, og vælg indhold.

Kilder, EPG-link/navne, EPG-tilknytninger, login, favoritter og startindstillingen gemmes lokalt med AES-GCM og en nøgle fra Android Keystore. Eksisterende gemte M3U-links flyttes fra den gamle indstilling til den krypterede lagring ved opdatering. Android-backup er slået fra. Den seneste kilde indlæses automatisk, medmindre appen starter på Favoritter.

De 47 PNG-logoer fra den vedhæftede `Kanallogoer.zip` er indbygget i appen og har prioritet ved match mellem kanalnavn og billedfilens navn uden filendelse. Matchning ignorerer store/små bogstaver og mellemrum (fx `DR 1` → `DR1.png`), men bevarer betydningsfulde tegn såsom `+`, så TV3 og TV3+ ikke forveksles. Disse logoer virker også uden netværk og på tidligere gemte favoritter.

Når intet indbygget logo matcher, læses logoer fra M3U's `tvg-logo` eller Xtreams `stream_icon`/`cover`, hentes asynkront og caches. Ved manglende, ugyldige eller utilgængelige logoer vises et standardikon. Logoer understøtter PNG, JPEG, WebP og SVG. Logo-adresser kan være relative, uden anførselstegn i M3U eller URL-kodede; logoanmodninger bruger en browserlignende User-Agent og kun kildens origin som Referer, aldrig kildeadressens loginparametre. Standardadvarsler, der kan indeholde logo-URL’er, er slået fra i billedbiblioteket.

Appen indeholder ingen kanaler eller abonnementer. HTTP understøttes af hensyn til eksisterende IPTV-udbydere; brug HTTPS til links med loginoplysninger.

Cast kræver Google Play-tjenester og normalt samme Wi-Fi som modtageren. Modtageren henter streamen direkte; format, codec og adgang skal understøttes på TV'et. DRM og udbyderspecifikke HTTP-headere understøttes ikke. Xtream-login bruger serverens API til TV, VOD, serier og afsnit. For M3U udledes typen af metadata, grupper og URL-stier (`/live/`, `/movie/`, `/series/`) eller filtypen; lister uden typeoplysninger falder tilbage til TV. M3U kan kun vise indhold og afsnit, som er inkluderet i listen.

Version 1.2.1 gør ikonmotivet 10 % mindre med ekstra gennemsigtig kant for at undgå beskæring i launcherens maske. Versionen retter også et crash ved indlæsning af kilder: billedbibliotekets logningsværdi var ugyldig, så visning af den første kanal udløste en undtagelse.

Version 1.2.2 gør ikonmotivet yderligere 10 % mindre end i 1.2.1 (81 % af originalens størrelse).

Bibliotek er omdøbt til Streamingtjeneste. Favoritter viser logo, kanalnavn og stjerne uden kilde- eller gruppenavn; fra version 1.5.0 vises også det aktuelle program ved tilknyttet EPG.

Version 1.2.3 forbedrer M3U-logoindlæsning og SVG-understøttelse og forenkler Favoritter til en samlet liste uden filtre eller antal. Vælg din M3U-kilde igen under ☰ → Kilder for også at opdatere logo-adresser i allerede gemte favoritter.

Version 1.3.0 tilføjer synlige afspillerkontroller, fuldskærm og lydstyrke samt de 47 indbyggede kanallogoer.

Version 1.3.1 retter fuldskærm: videoen bruger hele skærmarealet, kontrolbjælker optager ikke videoplads, pauseknappen skjules automatisk, og systemlinjer holdes skjult i fuldskærm.

Version 1.4.0 genopbygger afspilleren med én samlet tryk-for-at-vise-overlay i både almindelig visning og fuldskærm. Media3’s separate midterknap og kontrolpanel er slået fra.

Version 1.4.1 fjerner lydstyrkeskyderen og bruger FIT i både almindelig visning og fuldskærm, så hele videoen vises uden beskæring.

Version 1.4.2 bruger det blå Stream-banner fra `banner.zip` i topbjælken. Kun tomme, gennemsigtige margener er fjernet; hele logoet skaleres med bevaret billedformat.

## Opdatering og nulstilling

**☰ → Opdatér alt** genindlæser alle gemte kilder og alle TV-guides, også guides der endnu ikke er knyttet til en kilde. Opdateringen fortsætter efter individuelle fejl og viser en samlet rapport. Mislykkede opdateringer overskriver ikke eksisterende kanal- eller programdata.

EPG parses i baggrunden med XMLTV-tidszoner og understøttelse af gzip samt HTTP-redirects. Aktuelle og kommende programmer caches i appens private lager uden rå EPG-links. Link-fingerprints forhindrer brug af en gammel cache til et ændret link. Tilknyttede guides indlæses ved start, og favoritternes aktuelle programlinje opdateres mindst hvert minut i forgrunden. Aktuelle data kan vises offline fra cachen; gamle udsendelser vises ikke efter deres sluttid. Guiden genhentes normalt efter cirka 30 minutter. Standardgrænser: 256 MB udpakket XML og 250.000 relevante programmer.

**☰ → Nulstil app** kræver **Ja** i en særskilt bekræftelse. **Nej** bevarer data. Ja stopper afspilning/casting og kalder Androids rydning af denne apps egne brugerdata. Appen lukker, og ved næste åbning er kilder, login, favoritter, TV-guides, indstillinger og cache væk. De indbyggede logoer og appens programfiler følger stadig installationen.

Version 1.5.0 tilføjer TV-Guide med navngivne EPG-links, EPG-valg pr. kilde, aktuelle programmer på Favoritter, redigering/sletning, Opdatér alt og Nulstil app med Ja/Nej.

Version 1.5.1 flytter alle indstillingspunkter og start-på-favoritter-kontakten direkte til en scrollbar burgermenu. Kildevælgerens knapper står lodret i rækkefølgen Tilføj M3U, Tilføj Xtream-login og Annuller nederst.

Version 1.5.2 fjerner overskriften “Stream” i burgermenuen og de faste vejledningstekster under overskrifterne og i kilde-/guideformularer. Fejl og indlæsningsstatus bevares. En lille grå kreditering, “Udviklet af Lars Gade”, står nederst på appens almindelige sider. EPG-programtitlerne på Favoritter bevares.

Version 1.6.0 fjerner Favoritter-overskriften og favoritstjernerne på selve favoritlisten. Et stille langt tryk åbner en fjern-menu ved slip, og et langt tryk efterfulgt af bevægelse flytter elementet på listen. Den manuelle rækkefølge gemmes med de øvrige krypterede data. Stjerner i Streamingtjeneste bruges fortsat til at tilføje kanaler.

## Udvikling

Brug JDK 21, Android SDK Platform 37.0 og Build Tools 36.0.0 eller nyere. Projektet bruger Android Gradle Plugin 9.3.2, Gradle Wrapper 9.7.1 og targetSdk 37.

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

I det klargjorte cloudmiljø:

```sh
. /workspace/setup/android-env.sh
cd /workspace/Stream-Android
./gradlew assembleDebug testDebugUnitTest lintDebug
```

Tests kontrollerer metadata, relative links, specialtegn i links og Xtream-login, HTTP-redirects, gzip, UTF-16, store lister, serverfejl, API-kataloger, rækkefølgen for sektioner og grupper samt sæsoner og afsnit. Der er også tests af logo-adresser, gemte kilder, favoritter, startindstilling og krypteringens integritet. M3U-hentning accepterer op til 64 MB og viser konkrete fejl uden at vise loginoplysninger. Android-tests med Robolectric gennemgår tilføjelse af M3U- og Xtream-kilder, visning af kanallogoer og sektioner samt favoritter og startindstillingen. Billedtests kontrollerer desuden pixelindholdet i hentede PNG-, JPEG-, WebP- og SVG-logoer samt redirects og sikre headers. Favoritvisningen testes uden filtre og antal på tværs af indholdstyper. Afspiller-tests kontrollerer skjult starttilstand, tryk på videoen, automatisk skjulning uden ændring af videovinduets størrelse, afspillerindstillinger, pause/afspilning, stop, fuldskærm og tilbage uden udskiftning af afspilleren samt faktisk ændring af medielydstyrke og mute. Logo-tests kontrollerer de 47 pakkede filer, navnematch og indlæsning fra assets. De bruger en isoleret lagringsbackend til UI-testene; Android Keystore og faktisk afspilning/casting skal også afprøves på en Android-enhed. EPG-tests kontrollerer id-/navnematch, tvetydige navne, tidszoner og sommertid, programskift, cache og gzip/redirects. UI-tests dækker navngivne guides, valg pr. kilde, grå programtekst, kilde-/guide-redigering og sletning, opdatering af alle poster med delvise fejl samt Ja/Nej til nulstilling. Nulstilling testes via Androids simulerede datarydnings-API og udføres ikke på brugerens faktiske appdata under bygningen. Cloudmiljøet har ingen fysisk Cast-modtager.

Version 1.6.1 læser Xtream-kataloger løbende uden at gemme hele serversvaret og JSON-træet i hukommelsen. Unødvendige metadata springes over. Favoritopdatering indekserer kun relevante poster. Kataloger over 100.000 poster eller 64 MiB pr. API-svar afvises med en fejlbesked; login- og afsnitssvar begrænses til 4 MiB. Dette reducerer risikoen for hukommelsescrash på store kilder.

Version 1.6.2 fjerner alle faste størrelses- og antalgrænser for Xtream-kilder, herunder 64 MiB, 100.000 poster og grænserne for login-/afsnitssvar og tekstfelter. Katalogerne læses fortsat løbende og i serverens rækkefølge.

Version 1.7.0 bruger blå trykmarkeringer og tidslinje i afspilleren samt et tandhjul til afspillerindstillinger. Burgermenuen har to nye valg, der som standard er slået fra: **Afspil automatisk seneste** starter den senest afspillede video ved næste appstart (film/afsnit fra den gemte position, TV på live-positionen). Oplysningerne gemmes krypteret sammen med biblioteket. **Mini-afspiller** åbner Androids billede-i-billede, når appen forlades med Hjem eller Tilbage under lokal afspilning. Fra fuldskærm afslutter første Tilbage fuldskærm. Valget kræver Android 8 eller nyere og en enhed med understøttelse af billede-i-billede; Androids tilladelse til funktionen skal være aktiv. Casting bruger fortsat Cast-modtageren. At tvangslukke appen eller fjerne mini-vinduet stopper lokal afspilning.

Version 1.7.1 åbner en kildevælger ved tryk på Streamingtjeneste i burgermenuen, når der er flere kilder. Vælg en kilde for at indlæse den; Annuller bevarer den aktuelle visning. Med én kilde åbnes den direkte. Søgefeltet hedder nu Søg, og logoet øverst vises 10 % mindre.

Version 1.8.0 omdøber Favoritter til TV-Favoritter, tilføjer Radio og Radio-Favoritter lige nedenunder i burgermenuen samt konfigurerbare Radio-widgets med afspil/pause. Radio-favoritter gemmes krypteret sammen med det eksisterende bibliotek. Stationerne kommer fra den offentlige Radio Browser-tjeneste.

Version 1.8.1 fjerner Radio Browser-teksten nederst i Radio, markerer faktisk afspilning i TV- og radiolister med en hvid prik og erstatter samtlige indbyggede logoer med de 47 PNG-filer fra den nye upload. Radio-widgetten er som standard 2 × 2 felter med større logo, navn og afspil/stop-ikon. Bredden er fast; højden kan justeres. Eksisterende widgets kan bevare deres tidligere størrelse i launcheren: fjern dem og tilføj dem igen for at bruge den nye standardstørrelse.

Version 1.8.2 fordobler afspilningsprikkens diameter fra 8 til 16 dp. Radio-widgets er nu 2 × 1 felter uden stationsnavn, med logo og afspil/stop-ikon ved siden af hinanden. Tryk på logoet for at vælge en anden favorit. Fjern og tilføj eksisterende widgets igen for at få den nye størrelse.

Version 1.9.0 fjerner overskriften fra Radio-Favoritter og gør hele den konfigurerede radio-widget til afspil/stop, inklusive logoet. Stationsvalg sker kun ved tilføjelse af widgetten (eller hvis dens favorit er slettet). Radio og video i mini-afspilleren offentliggør en Android-mediesession med stations-/kanalnavn og logo samt MediaStyle-notifikation med afspil/pause og stop. Logoer indlæses først fra de indbyggede filer, ellers fra serveren i baggrunden; appikonet bruges ved manglende logo. Android kan bruge logoet som baggrund i systemets medieafspiller. Notifikationen tillader offentlig visning på låseskærmen; telefonens egne låseskærms- og notifikationsindstillinger bestemmer det endelige udseende og synligheden. På Android 13+ beder appen om notifikationstilladelse ved første afspilning.


Version 1.10.0 samler TV- og radiofavoritter under **☰ → Favoritter**, hvor der vælges **TV** eller **Radio**. Eksisterende favoritter, omdøbninger og rækkefølger bevares. Valgt fane gemmes, og **Vis favoritter, når appen åbner** åbner favoritsiden på den gemte fane. Versionen indeholder også den blå medienotifikation og den gennemsigtige 1 × 1-logo-widget med kort afspil/stop-animation. Tilføj gamle widgets igen for at få 1 × 1-størrelsen. Androids endelige mediepanel kan stadig variere mellem telefoner.

Version 1.10.1 placerer Favoritter under Radio i burgermenuen og centrerer TV/Radio-valget. Medielogoet bevarer den originale gennemsigtighed uden pålagt baggrund; notifikationens blå temafarve bevares, mens Android kan tilpasse sit eget mediepanel. Widget-feedback sendes kun én gang ved tryk, mens afspillerens statusopdateringer afventer, at den korte fade-animation er færdig.

Version 1.11.0 bygger til Android 17 (API 37) og omfatter kompatibilitetstests på Android 16 (API 36) og 17. Telefonens TV-, film- og serieafspiller bevares som video. Android Auto registreres som en officiel medieapp med radiofavoritter, danske radiostationer, søgning, logoer og bilens mediebetjening via MediaLibraryService/MediaBrowserService. Telefon- og bilafspiller deler den samme radioafspiller. Der tilbydes ikke TV som ren lyd i bilen: TV-billede på alle Android Auto-enheder er ikke en funktion, som appen kan garantere eller gennemtvinge.

Radio kan kun afspilles eller stoppes. En pausekommando fra telefonens medieafspiller, bil, headset eller lydfokus bliver til et stop. Næste afspilning åbner en ny forbindelse fra live-positionen. Radiosøgning og katalogindlæsning kører uden for mediesessionens hovedtråd; cache kan bruges ved netværksfejl. Bilens bibliotek deler radiofavoritternes navne og rækkefølge, og ingen TV-kilder eller loginoplysninger offentliggøres til bilen.

Ved manuel APK-installation kan Android Auto kræve **Ukendte kilder** under sine udviklerindstillinger, og Stream kan skulle vælges under **Tilpas applisten**. Offentlig distribution og godkendelse i Android Auto via Google Play er en separat proces. Der er endnu ikke udført test i en fysisk bil eller med Android Auto Desktop Head Unit.

Version 1.11.1 fjerner også pause fra selve radioafspillerens annoncerede systemkommandoer under afspilning. Kommandoændringer sendes korrekt til medieklienterne, og Stop offentliggøres både i det klassiske og nyere medieknapsæt. Efter stop annonceres Afspil igen. Dette retter det tilbageværende pauseikon i Androids medieafspiller.
