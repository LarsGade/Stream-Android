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

Version 1.11.2 fjerner radioens spring-frem/-tilbage-kommandoer, som kunne optage de kompakte knappladser og skjule Stop i telefonens mediepanel. Knaplayoutet sendes også direkte til medienotifikationens controller, så Androids platformsession opdateres. En rigtig Media3-controller og den publicerede platform-PlaybackState kontrolleres på Android 9, 16 og 17: Stop er tilgængelig under afspilning, og Afspil efter stop. Telefonproducenten bestemmer knappens endelige placering; visningen er endnu ikke verificeret på en fysisk telefon.

Version 1.11.3 gendanner standardmediebetjeningen fra før radioens Stop-tilpasninger: Afspil/Pause i telefonens mediepanel, på låseskærmen og via headset/Android Auto. Pause er igen en almindelig pause. Den særlige Stop-knap og filtreringen af systemkommandoer er fjernet. Radioens notifikation har igen standard Afspil/Pause samt Stop. Widgettens særskilte Afspil/Stop bevares. Den eksisterende Android Auto-integration og understøttelse af Android 16/17 bevares.

Version 1.11.4 tilføjer **Live** ved siden af **Stop radio** på radiosiderne. Live genåbner den aktuelle radiostream med nulstillet position og afspiller igen, så en pausebuffer kasseres. Normal Afspil/Pause i telefonens medieafspiller bevares.

Version 1.12.0 bruger det indbyggede DR P4-logo til alle kanalnavne med “P4”, inklusive regionale stationer. En fast radiobar nederst på appens almindelige sider viser den valgte kanal og Afspil/Stop. Stop lukker afspilningen; Afspil åbner samme station fra live igen. Før en station er valgt, er Afspil inaktiv. Fuldskærmsvideo bevarer sit rene videovindue.

Version 1.12.1 viser stationens logo til venstre for navnet i radiobaren og skjuler baren efter Stop. Radioens medieoplysninger viser kanalnavnet med “Afspilles via Stream” som undertekst, inklusive Android Auto. Bilens endelige layout bestemmes af dens medieafspiller.

Version 1.12.2 fjerner de gamle Stop/Live-kontroller fra radiolisterne. Kun den faste radiobar bruges. Live vises efter pause i mediebetjeningen og forbliver synlig ved genoptagelse af den gamle buffer. Live genåbner streamen fra live-positionen og skjuler knappen igen; valg af en station på ny gør det samme. Stop skjuler hele baren.

## Lokale filer

**☰ → Lokale filer** ligger lige under Radio. Brug **Tilføj filer** til at vælge en eller flere lyd-/videofiler eller **Tilføj mappe** til at tilknytte en mappe via Androids filvælger. Appen gemmer kun dokumentlinks, navne, medietyper og varig læseadgang i det krypterede bibliotek. Mediefiler kopieres ikke til appens lager. Mapper indlæses fra deres oprindelige placering, så nye filer i en tilknyttet mappe kan ses, når mappen åbnes igen. Undermapper kan åbnes; brug Tilbage-knappen eller knappen til overordnet mappe for at gå op. Filvælgerens tilladte placeringer bestemmes af Android.

Brug stjernen ved en fil eller mappe for at tilføje/fjerne den under **Favoritter → Lokale filer**, til højre for Radio. Også enkelte filer og undermapper inde i en tilknyttet mappe kan tilføjes. Hold på et link på hovedsiden for at vælge **Fjern link**; dette sletter aldrig filen eller mappen på enheden og fjerner ikke en separat favorit.

Lyd afspilles med en baggrundsmediesession og den fælles bundbar. Tryk på navnet/barren for at åbne den fulde lydafspiller med tidslinje, Pause/Afspil og Stop. Stop skjuler baren. Videofiler bruger den samme afspiller og fuldskærmsvisning som TV, med spoling for søgbare filer. Lokale dokumentlinks afspilles på telefonen; de sendes ikke til en Cast-modtager. Radio, lokal lyd og video starter ikke samtidig.

Filerne skal fortsat findes på den oprindelige placering, og appen skal have adgang. Flyttede/slettede filer, frakoblede drev eller tilbagekaldt adgang kræver, at filen/mappen tilføjes igen. Understøttede codecs afhænger af enheden. Version 1.13.0 tilføjer denne funktion og bevarer eksisterende TV-/radiofavoritter.

Version 1.13.1 retter tryk på lokale mapper og filer: favoritknappen tager ikke længere fokus fra rækken. Når en mappe åbnes, er **Medtag filer fra alle undermapper** slået til; filer fra dybere mapper vises med deres relative mappenavn og afspilles fra det oprindelige dokumentlink. Fra version 1.13.2 medtages alle undermapper altid; valget er fjernet. Gennemgangen kører i baggrunden og beskytter mod cirkulære mappehenvisninger.

Åbn en tilknyttet mappe og tryk **Skift kildemappe**, eller hold på mappen på hovedsiden og vælg samme handling. Den nye mappe erstatter linket på samme plads, og en favorit til selve kildemappen opdateres. Andre links og favoritter bevares. Annullering eller en ugyldig mappe bevarer den gamle kilde. Der slettes ingen filer.


Version 1.13.2 medtager altid alle undermapper uden afkrydsningsfelt. Filrækken og bundbaren viser Pause for den aktive fil; tryk pauser/genoptager den samme afspilning. Ved valg af en ny fil fastholdes listens rækkefølge som en kø (uden mappeposter). Afspilleren fortsætter automatisk med næste fil, også på tværs af lyd og video, og stopper efter den sidste fil. Senere mappenavigation ændrer ikke den igangværende kø.

Den lokale MediaSessionService ejer nu både lyd- og videoafspilleren. Skærmskift kobler kun videovisningen fra og til; de frigiver ikke den aktive lokale afspiller. Tjenesten starter sin foreground-medienotifikation straks, før filen klargøres, og anvender decoder-fallback. En stor kø sendes med et lille token frem for som hele listen i en Intent. Afspilningsfejl logges med Media3-fejlkode og stopper afspilningen uden at kaste fejlen videre fra brugerhandlingen. Disse ændringer adresserer kendte stabilitetsrisici; brugerens generelle Android-besked om fejl/rydning af cache identificerer ikke en bekræftet crashårsag.

Version 1.14.0 bruger kun ikoner til Afspil/Pause/Stop. Bundbaren har ét transportikon, og tryk på baren åbner den fulde afspiller for radio, lokal lyd, lokal video, streams og Cast. Alle medielister har søgning, inklusive TV-, radio- og lokale favoritter. Omarrangering er slået fra under en aktiv søgning, så skjulte favoritter bevarer deres placering.

Lokale filer har én kildemappe med alle undermapper. Vælg eller skift den med **Vælg kildemappe**. De tidligere Tilføj filer/Tilføj mappe-knapper er fjernet, Tilbage-knappen hedder **Tilbage**, og mediefilendelser skjules i visningen. De oprindelige dokumentlinks og filnavne bevares til opslag og afspilning. Favoritter bevares, når en anden kilde vælges.

Radio, streams og lokale lyd-/videofiler kan sendes til en Google Cast-modtager. Lyd sendes med titel og kunstner/afsender, så modtagerens standardmedievisning kan vise navnet. Lokale køer sendes i samme rækkefølge som på telefonen. Lokale filer udleveres af en foreground-tjeneste på telefonens aktuelle netværksadresse, kun via tilfældige engangsadresser for de valgte dokumenter. HTTP GET, HEAD, CORS og byte-ranges understøttes, så modtageren kan spole. Serveren lukkes ved stop, afsluttet afspilning eller afsluttet Cast-session. På Android 17 kræves tilladelse til lokalt netværk for lokale filer.

Telefon og Cast-modtager skal kunne nå hinanden på netværket. Cast bruger modtagerens understøttede codecs og kontrollerer ikke DRM-/licenskrav; versionen transkoder ikke. Cast-modtagerens endelige lydlayout bestemmes af dens receiver. Faktisk casting er ikke verificeret på en fysisk modtager i cloudmiljøet. Tests kontrollerer filserverens adgangsbeskyttelse, range/suffix/HEAD-svar, ugyldige ranges, lukning, køer, kildeskift og filnavne.

Version 1.14.1 bruger en venstrepil uden synlig tekst til Tilbage i lokale mapper og den fulde lyd-/Cast-afspiller. Skærmlæsere får fortsat beskrivelsen Tilbage. Den lokale Indlæser-tekst er fjernet. Når **Vis favoritter, når appen åbner** er aktiv, vises **Fane ved appstart** med TV, Radio og Lokale filer. Startfanen gemmes uafhængigt af senere faneskift. Eksisterende installationer får den tidligere valgte favoritfane som startværdi.

Version 1.14.2 fjerner favoritstjernen fra lokale favoritlister, som allerede var skjult i TV- og radiofavoritter. Hold på en favorit og vælg **Fjern fra favoritter** for at fjerne den. Stjernen bevares i almindelige kildelister til tilføjelse af favoritter. Cast-knappen i den fulde afspiller bruger nu 48 dp frem for et fast pixelmål, har ekstra margen til højre og tager højde for skærmens udskæring.

Version 1.14.3 viser kun **Udviklet af Lars Gade** nederst i burgermenuen. Krediteringen er fjernet fra appens øvrige sider. Menuen ruller fortsat på mindre skærme; ved ekstra plads skubbes krediteringen til bunden.

Version 1.14.4 giver Favoritter-knappen i burgermenuen en blå baggrund med hvid tekst og omdøber Kilder til **Kilder til streamingtjeneste**.

Version 1.14.5 bruger en ren blå playtrekant som launcherikon, uden indbygget ramme eller tekst. Android 8+ får separate adaptive baggrunds- og forgrundslag, så launcheren bestemmer formen. Baggrundsfarven kommer fra Androids neutrale systempalette på Android 12+; Android 13+ kan også bruge en separat monokrom playtrekant til tematiserede ikoner. Android 6/7 får samme playmotiv som et transparent vektorikon.

Version 1.14.6 omdøber **Lokale filer** til **Lokale** i menu, favoritfane, startfanevælger og overskrift.

Version 1.14.7 giver Streamingtjeneste, Radio og Lokale i burgermenuen blå baggrund med hvid tekst. Favoritter får gul baggrund med sort tekst.

## Video i bil – Android 9, version 1.15.0

Det praktiske forsøg på video fra en Android 9-telefon er **☰ → Video i bil**. Stream kan åbne den aktuelle TV-kanal/video i en installeret bilafspiller, overføre TV-/filmfavoritter som M3U eller åbne en lokal videofavorit med læseadgang til netop den valgte fil. Den lokale favoritliste kan søges. Stream sættes først på pause, når den valgte afspiller er startet.

Der kræves en separat videoafspiller, som faktisk kan vises i brugerens Android Auto-opsætning, fx [Fermata Auto](https://github.com/AndreyPavlenko/Fermata/releases). Vælgeren prioriterer Fermata Auto og viser kun matchende videoafspillere med bilintegration eller den kendte Fermata Auto-pakke. Det er stadig Android Auto-værten, der afgør, om bilafspilleren må vises. En almindelig telefonafspiller er ikke tilstrækkelig til billede på bilens skærm. Løsningen kræver afprøvning på den konkrete telefon og bil; generel kompatibilitet og faktisk bilafspilning er ikke verificeret.

Favoritlisten indeholder de gemte streamlinks, inklusive eventuelle adgangsoplysninger i linksene. Listen udleveres kun efter brugerens valg af en modtagerapp og via en ikke-eksporteret FileProvider med midlertidig læseadgang. Cacheområdet er afgrænset til `car-playlists/`; gamle eksporter fjernes ved næste eksport efter 24 timer. Lokale dokumentlinks indlejres ikke i M3U-listen, da de kræver særskilte Android-læsegrants. Intents giver aldrig skriveadgang eller varig adgang. Ingen tredjepartsafspiller eller privat Auto-SDK er pakket ind i APK'en, og Android Auto-værtens kontrol af afspillere ændres ikke.

Kompatibilitetsgrundlaget er standard Android ACTION_VIEW og de [filtyper, Fermata selv registrerer](https://github.com/AndreyPavlenko/Fermata/blob/master/fermata/src/main/AndroidManifest.xml). Den eksisterende indbyggede Android Auto-radiointegration bevares. **Video i bil** er en overførsel til en ekstern bilafspiller og tilføjer ikke TV/video til Streams eget radiokatalog i bilen.

Verifikation af version 1.15.0: APK og lint bygges med projektets konfiguration. Modeltests dækker linktyper, seriemapper, duplikater, danske navne, M3U-linjeinjektion og afvisning af lokale links uden særskilte grants. Android 9-intenttestene er tilføjet, men testkørslen blev blokeret ved hentning af Robolectrics Android 9-bibliotek (HTTP 403); de er ikke rapporteret som bestået. Der er ikke testet med en tilsluttet bil eller en installeret Fermata Auto.

Version 1.15.1 gør logoet i appens top 20 % mindre end i den foregående version. Logoets visuelle skala reduceres fra 0,90 til 0,72, og topområdets højde reduceres tilsvarende fra 64 dp til cirka 51 dp. Menu- og Cast-knapperne bevarer mindst 48 × 48 dp trykflade.

Version 1.15.2 fjerner menupunktet Video i bil og hele forbindelsen til eksterne bilafspillere, inklusive eksport, FileProvider og app-forespørgsler. Den eksisterende Android Auto-medietjeneste for radio bevares. TV/video tilføjes ikke til bilens radiobibliotek. Toplogoet er yderligere 10 % mindre end i version 1.15.1: visuel skala 0,648 og logoområde cirka 46 dp. Menu- og Cast-knappernes trykflader er fortsat 48 dp.

Toppen i version 1.15.2 viser kun det eksisterende playmotiv fra logoet, uden Stream-teksten. Appens launcherikon ændres ikke.

Version 1.15.3 omdøber menupunktet Streamingtjeneste til **Streaming (TV, Film, Serier)**.

Version 1.15.4 indsætter et fast linjeskift i Streaming-knappen, så **(TV, Film, Serier)** står centreret på linjen under **Streaming**.

Version 1.15.5 erstatter de resterende forekomster af Streamingtjeneste med **Streaming** i appen. Kildeindstillingerne hedder nu **Kilder til Streaming**, og fejlhenvisningen bruger samme navn. Streaming-knappens linjeskift og kategorier bevares.

Version 1.15.6 fjerner faste vejledningstekster fra tomme lokale lister og radiofavoritter, herunder teksten om at bruge stjernen. Tomme tekstfelter skjules, så de ikke optager plads. Overflødige instruktioner i afsluttet Cast-status og slettebekræftelsen er også fjernet. Fejl, statusoplysninger, feltnavne og selve bekræftelsen på sletning bevares. En radio-widget uden favoritter viser kun den korte status Ingen radiofavoritter.

## Pcloud – version 1.16.0

**☰ → Pcloud** åbner login og WebDAV-mapper. Vælg EU eller USA, og indtast pCloud-kontoens e-mail og adgangskode. Login gemmes først efter et vellykket WebDAV-opslag, i samme AES-GCM-krypterede bibliotek og Android Keystore som de øvrige loginoplysninger. Log ud fjerner de gemte pCloud-loginoplysninger og stopper aktiv pCloud-afspilning/casting. Der gemmes ingen adgangskode i dokumentlinks, afspilningsmetadata, Intents eller Cast-adresser.

Mappevisningen viser alle undermapper og understøttede lyd-/videofiler i den aktuelle mappe. Mapper åbnes med et tryk, venstrepilen går tilbage, og Søg filtrerer den aktuelle liste. Filendelser skjules som i Lokale. En fil kan føjes til **Favoritter → Lokale**; der er fortsat tre favoritfaner. Gemte links knyttes til region og konto, så en anden konto ikke overtager dem. Links kan bruges igen efter nyt login på samme konto.

Filerne streames gennem den fælles Media3-afspiller med samme bundbar, ikonknapper, fuld lydafspiller, fuldskærmsvideo, spoling og automatisk næste fil i listens rækkefølge. Hele mediefiler kopieres ikke til appens lager først. Cast af pCloud-filer bruger telefonens eksisterende lokale Cast-server, som udleverer de valgte filer via tilfældige adresser og videresender byte-ranges til WebDAV. Loginoplysninger sendes ikke til Cast-modtageren. Telefonen skal derfor fortsat være tændt og forbundet til netværket under casting.

WebDAV-klienten bruger kun læseoperationerne PROPFIND og GET. Login sendes kun til den konfigurerede HTTPS-server. Mappeomdirigeringer til en anden server afvises, og ved filomdirigeringer til en CDN fjernes Authorization før forespørgslen. XML-svar med DTD afvises. Filnavne, UTF-8, XML-entiteter, kontoidentitet, fejlstatus, omdirigeringer og Range-spoling testes med lokale WebDAV-testservere; en rigtig pCloud-konto eller fysisk Cast-modtager er ikke tilgængelig i cloudmiljøet.

pClouds officielle [WebDAV-vejledning](https://help.pcloud.com/article/connect-to-pcloud-using-webdav-and-rsync) angiver `https://ewebdav.pcloud.com/` for EU og `https://webdav.pcloud.com/` for USA. Ved 2FA kan forbindelsen skulle godkendes via en mail fra pCloud; 2FA behøver ikke slås fra. Crypto-mappen kan ikke tilgås gennem WebDAV.

Version 1.16.1 fjerner Log på/Konto, Log ud og Opdatér fra selve Pcloud-filvisningen. Før login åbner Pcloud i burgermenuen login. Efter login åbner samme menupunkt handlingerne **Åbn Pcloud**, **Opdatér**, **Redigér konto** og **Log ud**. Opdatér genindlæser den aktuelle mappe, hvis Pcloud allerede er åbent. Kontoredigering viser Pcloud-visningen efter et vellykket login. Lokale mapper, lokale mappefavoritter og Pcloud-mapper bruger samme egentlige mappeikon.


### 1.17.0 – Billeder
- Lokale og Pcloud viser nu også billedfiler. En fuld billedviser har swipe samt forrige/næste i den viste listes rækkefølge.
- Favoritter har fanen Billeder, som også kan vælges ved opstart. Hold på et favoritbillede for at fjerne det.
- Billeder kan castes og skiftes manuelt frem/tilbage. Cast klargør et JPEG/PNG på op til 1920×1080. Understøttede originalformater afhænger af telefonens billeddecoder.
- Lyd/video-køer springer billeder over. Eksisterende billedfavoritter flyttes til billedfanen. Pcloud-billeder bruger kontoens godkendte WebDAV-forbindelse og gemmes ikke i Glides diskcache.
- Verificeret med APK-build, lint og model-/netværkstests. Billedvisning på fysisk Android 9, rigtig Pcloud-konto og fysisk Cast-modtager er ikke afprøvet her.


### 1.17.1 – Pcloud-menu og indstillinger
- Regionvælgeren viser kun EU og USA. Ved login vises “Bekræft login på din mail”; hvis godkendelsen afvises, kan man bekræfte mailen og trykke Log på igen.
- Pcloud-kontoens handlinger vises som almindelige trykbare valg i en separat dialog. Burgermenuen lukkes først, så Favoritter-knappen og indstillingerne ikke står bag kontoens valgliste.
- Burgermenuens tre indstillinger bruger afkrydsningsfelter med samme lagrede indstillinger og funktioner.


### 1.17.2 – Favoritfaner
Favoritter viser kun faner med indhold. Når den sidste favorit fjernes fra den valgte fane, vælges den første tilbageværende fane. Uden favoritter skjules hele fanelinjen. En tom valgt startfane falder tilsvarende tilbage til en fane med indhold; den gemte startpræference bevares.


### 1.17.3 – Fjern billedfavoritter med langt tryk
Billedfavoritter fjernes kun ved langt tryk på filen og valget “Fjern fra favoritter”. Billedviseren kan kun tilføje favoritter, og gemte billeder viser ingen stjerneknap i Lokale eller Pcloud.


### 1.18.0 – Favoritter efter kilde
Favoritter har separate faner Lokale og Pcloud for alle mapper, lyd-, video- og billedfiler fra hver kilde. Den tidligere billedfane erstattes af kildeopdelingen; eksisterende favoritter bevares og fordeles efter deres URI uden flytning af filer. Pcloud-mapper kan tilføjes og åbnes direkte fra favoritter. Tomme faner skjules stadig; fjernelse sker med langt tryk i favoritlisten.


### 1.18.1 – Almindelig skrivemåde
Knapper og dialogknapper viser teksten uden automatisk versalisering. Menutekster bruger almindelig skrivemåde, herunder “Streaming (tv, film, serier)”, “Tv-guide” og “Kilder til streaming”. Filnavne, kanalnavne og brugernes indhold bevares.


### 1.18.2 – Gendan oprindeligt appikon
Startskærmsikonet er gendannet fra versionen umiddelbart før ændringen til den enkle blå playknap, med de oprindelige ikoner til både Android 9 og adaptive launchere.


### 1.18.3 – Pcloud-overskrift
Loginvinduets overskrift hedder nu kun “Pcloud”. Forkortelsen TV står igen med store bogstaver i alle appens tekster.


### 1.19.0 – Separate streamingfavoritter og fleksible faner
Streamingfavoritter opdeles i TV, Film og Serier efter indholdets type. Radio, Lokale og Pcloud bevares som selvstændige faner. Fanerne ombrydes automatisk til flere linjer efter den tilgængelige skærmbredde og tekststørrelse. Tomme faner skjules stadig. Film og Serier kan også vælges som startfane. Eksisterende favoritter og startpræferencer bevares.


### 1.19.1 – Logo som adaptivt appikon
Appikonet bruger det eksisterende gennemsigtige S/play-logo som forgrund. Der er ingen tegnet firkant eller baggrund i logobilledet. Android styrer ikonmasken og systemfarven i det obligatoriske adaptive baggrundslag (systempalettens neutrale farve på Android 12+). Ældre launchere får logoet med gennemsigtighed.


### 1.19.2 – Mindre logo uden baggrundsfarve
Ikonets forgrund er præcis 10 % mindre (inset 10 % → 14 %, så den tilgængelige størrelse går fra 80 % til 72 %). Det adaptive baggrundslag er gennemsigtigt; appen angiver ingen baggrundsfarve. En eventuel baggrund tilføjet af telefonens launcher styres af telefonen.


### 1.19.3 – Telefonens ikonindramning
Appen leverer nu et almindeligt gennemsigtigt ikon uden adaptivt baggrundslag, så launcherens egen indramning kan anvendes. Logoet er yderligere 10 % mindre (tilgængelig størrelse 72 % → 64,8 %). Den præcise ydre baggrund og indramning afhænger af telefonens launcher og ikontema.


### 1.19.4 – Mørk ikonbaggrund som referencen
Appikonet har nu en mørkegrå lodret overgang (#2C2C2C → #1A1A1A), tilpasset Stremio-ikonet på brugerens skærmbillede. Android 8+ bruger launcherens adaptive maske, ældre launchere får en afrundet baggrund. Logoets størrelse er bevaret.


### 1.19.5 – Logo 5 % større
Logoets bredde og højde i appikonet er øget med 5 %. Den mørkegrå baggrund bevares.


### 1.19.6 – Logo yderligere 5 % større
Logoets bredde og højde i appikonet er øget med yderligere 5 % i forhold til 1.19.5.


### 1.19.7 – Logo yderligere 10 % større
Logoets bredde og højde i appikonet er øget med yderligere 10 % i forhold til 1.19.6. Baggrunden bevares.


### 1.19.8 – Logo tilpasset referenceikonernes størrelse
Play-logoet er tilpasset referencens visuelle højde, så det fylder cirka samme andel af ikonfladen som Stremio. Forgrunden på adaptive ikoner er cirka 34 % større end i 1.19.7. Den mørkegrå baggrund bevares.


### 1.19.9 – Logo 5 % mindre
Play-logoets bredde og højde er reduceret med 5 % i forhold til 1.19.8. Baggrunden bevares.


### 1.19.10 – Logo yderligere 5 % mindre
Play-logoets bredde og højde er reduceret med yderligere 5 % i forhold til 1.19.9. Baggrunden bevares.


### 1.19.11 – Logo yderligere 5 % mindre
Play-logoets bredde og højde er reduceret med yderligere 5 % i forhold til 1.19.10. Baggrunden bevares.


### 1.19.12 – Logo yderligere 8 % mindre
Play-logoets bredde og højde er reduceret med yderligere 8 % i forhold til 1.19.11. Baggrunden bevares.


### 1.19.13 – Streaming-overskrift
Streamingsiden viser kun “Streaming” som standardoverskrift. Burgermenuen beholder “Streaming” med “(TV, Film, Serier)” på næste linje. Rettet fanernes kobling til deres indhold ved automatisk skjul af tomme favoritfaner.


### 1.20.0 – Dropdown i Favoritter
Favoritkategorierne TV, Film, Serier, Radio, Lokale og Pcloud vælges nu i en dropdownmenu. Kun kategorier med indhold vises, og den tomme menu skjules, hvis der ingen favoritter er. Fjernes sidste favorit fra den valgte kategori, vælges en anden kategori med indhold.
Startvælgeren bruger samme rækkefølge: TV, Film, Serier, Radio, Lokale, Pcloud. Gemte kategori-id’er bevares.


### 1.21.0 – Logo og cover i fuld lydafspiller
Den fulde lydafspiller viser radiostationens lokale eller eksterne logo og lydfilens indlejrede cover fra afspillerens metadata, også ved afspilning fra Pcloud. Billeder indlæses og skaleres via Glide; uden billede eller ved fejl skjules billedfeltet. Visningen følger automatisk skift til næste fil. Stationens eller filens navn bruges i stedet for udsendelsestitler. “Afspilles via Stream” er fjernet fra media-sessioner, Android Auto og Cast-metadata. Ved casting vises modtagerens billede, stationens logo eller det kendte cover for den aktuelle fil.
Build, lint og model-/netværkstests er kontrolleret. Robolectric-testen af coverfeltet kan ikke afvikles her, fordi download af Android-testpakken afvises af proxyen (HTTP 403). Billedvisningen er ikke afprøvet på en fysisk telefon.


### 1.21.1 – Logo i afspilningsbaren
Afspilningsbaren viser stationens logo eller lydfilens cover til venstre for navnet. Billedet opdateres ved skift af fil/station og skjules, hvis der ikke er et billede, eller hvis det ikke kan hentes. Ved casting bruges det tilgængelige logo eller cover fra Cast-oplysningerne.


### 1.21.2 – Play/pause til højre
Afspilningsbarens play/pause-knap er flyttet til højre kant. Logo/cover og navn står til venstre.


### 1.22.0 – Live-knap og stop ved lukning
- Live vises som to ringe under radio- og TV-afspilning i baren og den fulde afspiller. Ikonet er hvidt, når afspilningen er pauset, buffer eller er bagud, og rødt, når afspilningen kører live. Et tryk genindlæser radiostreamen, går til TV-kanalens livepunkt eller søger til live på en Cast-modtager. Almindelige lyd-/videofiler har ikke et livepunkt.
- Afspilningsbaren er sort med en hvid kant øverst.
- Fjernes appen fra seneste apps eller afsluttes hovedvinduet, stoppes afspilningstjenesterne og Cast-sessionen, og afspilningsmeddelelserne fjernes. Home, skærmlås og almindeligt skift mellem appens vinduer er ikke fuld lukning.
På Favoritter-siden vises kun søgefelt for Lokale og Pcloud. TV, Film, Serier og Radio viser ikke søgefelt. Billed-/lifecycle-tests på Android er fortsat blokeret af download af Robolectric-testpakken (HTTP 403).


### 1.22.1 – Swipe pauset afspiller væk
Pausede afspilningsmeddelelser kan fjernes med swipe. Meddelelsens delete-intent stopper den tilhørende afspiller og rydder køen, så den ikke genopretter meddelelsen. Radiomeddelelsen og den lokale/Pcloud-afspiller understøtter dette; Media3s eksisterende transportknapper og asynkrone coveropdateringer bevares.
Radiomeddelelsen har desuden en live-cirkel, der sender samme Live-kommando som appens afspillere. Knappens tilgængelighedstekst viser “Live” eller “Gå til live”; Android styrer selve ikonfarven i systemets medieafspiller.


### 1.22.2 – Live i systemafspiller og swipe straks efter pause
Radiomediesessionen registrerer Live som en rigtig custom session command med live-cirkel, så systemafspillere, der bruger sessionsknapper frem for meddelelsens actions, også kan vise den. Ved pause fjernes foreground-låsningen med det samme, og en ny pauset meddelelse indsættes uden ongoing-flag, mens afspilning og resume bevares. Det gælder radio samt lokale/Pcloud-filer. Swipe sender fortsat stop og rydder køen. Den konkrete visning og swipe skal efterprøves på brugerens telefon, da Android/Samsung SystemUI ikke kan afprøves i dette miljø.


### 1.22.3 – Stabil pause, Live-plads og åbn fuld afspiller
Pause opdaterer samme meddelelse uden cancel/remove eller genindsættelse. Foreground-laget frakobles, og ongoing/foreground/no-clear-flag ryddes på den pausede meddelelse. Radio registrerer Live både som custom layout og som media-button preference med en konkret knapplads; meningsløse forrige/næste-/spoleknapper for radiostationen fjernes fra systemcontrollerens kommandoer, så Live kan få plads. Tryk på radiomeddelelsen åbner fuld lydafspiller; lokal video og streamingmeddelelser beder om fuldskærmsvisning. SystemUI-visning og swipe er endnu ikke verificeret på en fysisk telefon.


### 1.23.0 – Kildemappe, konto og JSON-opsætning
Under TV-guide findes nu Kildemappe til lokale, Pcloud-konto og Importér/Eksportér i denne rækkefølge. Lokale og Pcloud er rene filvisninger; valg af rodmappe og konto ligger i burgermenuen.
Eksport gemmer hele Library-opsætningen (streamingkilder inkl. logins, Pcloud-konto, TV-guides, alle favoritter, lokale mappelinks, afspilnings- og startindstillinger) som læsbar UTF-8-JSON. Import kontrollerer format/version, størrelse og kildehenvisninger, før den gemmer en erstatning i det krypterede lokale lager. Ugyldige filer eller fejl ved lagring bevarer den gamle opsætning. Aktuel afspilning stoppes ved vellykket import, og siden genoprettes med de importerede indstillinger. Ingen gamle asynkrone biblioteksskrivninger må overskrive importen.
Filen indeholder logins i klartekst. Androids mappeadgang kan ikke flyttes med JSON; ved import på en anden telefon skal lokal kildemappe vælges igen. Systemtilladelser og Androids widget-placeringer er enhedsindstillinger. Swipe-adfærden og de nuværende live-kontroller bevares.


### 1.23.1 – Foldbare indstillinger
Favoritter står øverst i burgermenuen. Alle afkrydsningsfelter, startfanevælgeren og de grå menupunkter ligger under Indstillinger, som starter foldet sammen og åbnes/lukkes ved tryk. Menupunktet TV-guide hedder nu Kilder til TV-Guide. Krediteringen står fortsat nederst uden for indstillingerne.


### 1.23.2 – Rød nulstillingsknap
Nulstil app under Indstillinger har nu rød baggrund og hvid tekst.


### 1.23.3 – Opdatér radiostationer
Knappen Opdatér radio hedder nu Opdatér radiostationer.


### 1.23.4 – Lokale-overskrift over søgning
Lokale-siden har en fast overskrift “Lokale” over søgefeltet som på radiosiden. Den aktuelle mappesti vises stadig ved mappenavigation. Favoritter beholder kategori-dropdown uden en ekstra Lokale-overskrift.


### 1.23.5 – Pcloud-overskrift over søgning
Pcloud-siden har nu en fast overskrift over søgefeltet, som Lokale og Radio. Mappestien vises fortsat ved navigation i undermapper.


### 1.24.0 – Navigation og videobar
- Indstillinger foldes sammen, når burgermenuen lukkes. Favoritter har blå baggrund og fed hvid tekst.
- Ved sideskift skjules videovinduet øverst, mens afspilningen fortsætter i den nederste bar. Tryk på baren åbner videoen i fuldskærm; en tilbagepil øverst til venstre vender tilbage til siden. Baggrundsopdateringer fra den lokale afspilning åbner ikke automatisk det skjulte videovindue igen.
- Den valgte lokale kildemappe er roden i filvisningen. Tilbage går ikke op over den; en favoritmappe kan stadig forlades for at vende tilbage til favoritlisten.
- Søgning i lokale/Pcloud-lister viser navne uden relative stier, og mappestien skjules under søgning.


### 1.24.1 – Lokale filer
Lokale er omdøbt til Lokale filer i navigation, sideoverskrift, favoritkategori og startvælger. Indstillingen hedder tilsvarende Kildemappe til lokale filer.


### 1.25.0 – Ekstra radio-widget 2 × 1
Der er nu en ekstra widget, Stream radio 2 × 1, med stationslogo til venstre og en fast play/pause-knap til højre på mørkegrå baggrund med afrundede hjørner. Vælg en station fra radiofavoritter, når widgetten tilføjes. Tryk på logoet for at skifte den valgte station. Knappen til højre pauser/genoptager den valgte station eller starter den, hvis en anden station spiller. Den eksisterende 1 × 1-widget bevares.
Logoer hentes fra appens stationslogoer eller stationens billedadresse, indlæses i begrænset størrelse og opdateres separat, så et sent billede ikke ændrer play/pause-tilstanden. Størrelsen er deklareret som 2 kolonner og 1 række med en minWidth/minHeight-fallback til Android 9. Widgetplacering og betjening skal stadig afprøves på en fysisk launcher.


### 1.25.1 – Ens størrelse i 2 × 1-widget
Stationslogo og play/pause-knap har nu hvert sit centrerede felt på 48 × 48 dp i lige brede kolonner. Bag knappen er en mørkegrå cirkel med diameter 48 dp, svarende til logofeltets størrelse.


### 1.25.2 – Widgetgrafik 30 % større
Logo og den cirkulære play/pause-knap i 2 × 1-widgetten er øget fra 48 dp til 62,4 dp i bredde og højde. Knappens indvendige afstand er skaleret tilsvarende, så play/pause-symbolet også bliver 30 % større. Lodret indvendig margen fjernes for at give plads i én række.


### 1.25.3 – Afrundet firkant bag widget-knappen
Baggrunden bag play/pause i 2 × 1-widgetten er nu en mørkegrå firkant med afrundede hjørner. Størrelsen bevares.


### 1.25.4 – Mørkere widgetbaggrund
Hele 2 × 1-widgettens baggrund er nu #202020, tilpasset den mørke referencefarve. Afrundede hjørner og knapbaggrund bevares.


### 1.25.5 – Hele widgetten styrer afspilning
Tryk på baggrund, logo eller play/pause styrer nu samme valgte station. Kanalvalget låses efter tilføjelse; en anden station vælges ved at tilføje en ny widget. Logoet er samtidig øget 5 % fra 62,4 dp til 65,52 dp; kontrolknappens størrelse bevares.


### 1.25.6 – Næsten sort widgetbaggrund
Baggrunden på 2 × 1-widgetten er ændret til #0D0D0D efter den nye farvereference.


### 1.25.7 – Vælg radiofavorit ved tilføjelse
Kanalvælgeren vises altid ved tilføjelse af en radio-widget, også hvis telefonen genbruger et widget-id. Vælgerens titel angiver, at stationen vælges fra favoritter. Den brede widgets trykflader styrer fortsat kun play/pause; kanalskift kræver en ny widget.


### 1.25.8 – Lysere mørkegrå widgetbaggrund
Baggrunden på 2 × 1-widgetten er ændret fra #0D0D0D til #202020 efter den nye farvereference.


### 1.25.9 – Play/stop på begge radio-widgets
Begge widgettyper bruger nu fuldt stop, når den valgte station spiller. Stop rydder kø og station, stopper tjenesten og fjerner radiomeddelelsen. Den brede widget viser et stopikon under afspilning. Afspillerens almindelige pausefunktion inde i appen og meddelelsen bevares.


### 1.25.10 – Store play/stop-ikoner uden knapbaggrund
Den brede widgets play/stop-knap har nu gennemsigtig baggrund og større symboler uden indvendig padding. Widgettens samlede baggrund og logo bevares.


### 1.25.11 – Mindre play/stop-ikoner
Play- og stopikonets størrelse på 2 × 1-widgetten er reduceret med 25 % i forhold til 1.25.10. Logo og widgetbaggrund bevares.


### 1.25.12 – Play/stop yderligere 25 % mindre
Play- og stopikonets størrelse på 2 × 1-widgetten er reduceret med yderligere 25 % i forhold til 1.25.11.


### 1.25.13 – Let afrundede widget-ikoner
Play og stop på den brede radio-widget har nu let afrundede hjørner. Ikonernes felter og farve bevares.


### 1.26.0 – Samlede streamingkilder og P5/P6-logoer
Alle tilføjede streamingkilder indlæses i en samlet liste og fælles TV-/Film-/Seriegrupper uden kildevælger. Hver række beholder sin kildeidentitet, så favoritter, TV-guide, logins og serieafsnit fortsat knyttes til den rette kilde. Kilder hentes parallelt med højst tre samtidige forespørgsler; fejl i én kilde fjerner ikke de øvriges indhold. Allerede indlæste data fra samme uændrede forbindelse kan bevares ved en opdateringsfejl.
Kanaler med P5 eller P6 i navnet bruger nu henholdsvis P5.png og P6.png fra appens logoer, også for navnevarianter og i radioafspiller, radio-lister, widgets og Android Auto. Kildemappens navn vises ikke længere under søgefeltet i roden af Lokale filer; overskriften er kun Lokale filer.


### 1.26.1 – Samme mappestruktur som kildemappen
Lokale filer viser nu kun den aktuelle mappes direkte undermapper og mediefiler. Indhold fra dybere undermapper blandes ikke ind i listen. Åbn en undermappe for at se dens indhold; tilbage går stadig højst til den valgte kildemappe.


### 1.26.2 – Naturlig navnesortering
Mapper og filer i Lokale filer, Pcloud og deres favoritlister sorteres nu efter navn med tal i numerisk rækkefølge: Fil 2 kommer før Fil 10. Mapper står først. Sorteringen ignorerer store/små bogstaver og bruger dansk bogstavrækkefølge. Lange tal understøttes uden heltalsoverløb; ved samme tal kommer den korteste nuludfyldning først. Afspilning og billedviser følger den viste rækkefølge.

### 1.26.3

Kildemappe til lokale filer tilbyder nu “Vælg kildemappe” og “Ingen lokale filer”. Sidstnævnte fjerner den valgte kildemappe og tømmer lokale filers mappevisning. Gemte favoritter bevares.

### 1.26.4

Lokale filer, Pcloud og filfavoritter viser ingen play/pause-knap ved titlen. Den afspillende lyd- eller videofil markeres med en hvid prik yderst til højre. Tryk på den aktuelle fil ændrer ikke afspilningen; brug baren nederst. Mappe- og billedikoner bevares. Favoritstjernerne er fjernet fra alle fil- og mappelister. Hold på en fil eller mappe for at tilføje eller fjerne den fra favoritter.

### 1.26.5

Hold-menuer har grå baggrund. Sideoverskrifter i Streaming, Radio, Lokale filer og Pcloud samt mappetitler er fede. Mappetitler i Lokale filer og Pcloud har mappeikon til venstre.

### 1.26.6

Favoritkategoriens overskrift og dropdown-valg står med fed skrift. Hold-menuer i streaming- og radiofavoritter åbner straks ved langt tryk uden at vente på slip. Rækkefølgen kan ændres via Flyt op/Flyt ned i menuen.

### 1.26.7

Appens hovedvisning bevarer altid telefonens status- og navigationslinjer, også ved indlejret video. Headerens afstand beregnes ud fra systemlinjernes faste indryk samt skærmudskæringer, så logo, menu og cast ikke flytter op under statuslinjen. Fuldskærmsvideo kan stadig skjule systemlinjerne.

### 1.26.8

Afspilningsprikken i lokale filer, Pcloud og filfavoritter bruger samme 16 dp markør som streaming- og radiofavoritter. Alle fil- og mapperækker har ens højde med plads til to tekstlinjer og lodret centreret tekst; lange titler afkortes efter to linjer. Rækkehøjden tager højde for større systemtekst.

### 1.26.9

Bundafspilleren har altid samme faste højde som rækkerne i Lokale filer og Pcloud, uanset titel, afspilningskilde, logo og live-knap. Højden deler samme beregning og tilpasses systemets tekststørrelse.

### 1.26.10

Medielister bruger mørke rækker og grå separatorer i Streaming, Radio, Lokale filer, Pcloud, favoritter og serieafsnit. Radio- og streaminglister har ingen favoritstjerner; hold på en række for at tilføje eller fjerne en favorit.

### 1.26.11

Den afspillende række vises med fed tekst i Streaming, Radio, Lokale filer, Pcloud og favoritter. Fed skrift følger samme afspilningsstatus som den hvide prik og fjernes igen ved pause, stop eller skift til en anden række.

### 1.26.12

Logoet i bundbaren er 25 % større. Live- og play/pause-ikoner vises i 36 dp i stedet for 24 dp (50 % større), med samme berøringsområder og uændret barhøjde.

### 1.26.13

Live-ikonet i bundbaren er 20 % mindre (28,8 dp), og play/pause-ikonet er 20 % større (43,2 dp). Logo, barhøjde og berøringsområder er uændrede.

### 1.26.14

Opdatér radiostationer er flyttet fra radiosiden til burgermenuens Indstillinger, umiddelbart efter Kildemappe til lokale filer.

### 1.26.15

I den fulde lydafspiller er play/pause og stop forstørret fra 24 til 48 dp (100 % større). Live-ikonet er forstørret fra 24 til 36 dp (50 % større). Bundbarens ikoner er uændrede.

### 1.26.16

Lokale filer med Anders og Anders, Bæltestedet, r8dio eller Den Korte Radioavis i navnet bruger de tilhørende medfølgende logoer. Logoerne vises i fil- og favoritlister, bundbaren, fuld lydafspiller og den lokale mediesessions metadata.

### 1.26.17

Live-knappen i den fulde lydafspiller er flyttet til venstre kant og gjort 20 % mindre. Play/pause og stop er centreret som en gruppe uafhængigt af live-knappens synlighed.

### 1.26.18

Live-knappen i den fulde lydafspiller er flyttet til højre kant. Play/pause og stop forbliver centreret.

### 1.27.0

Streamingkataloger gemmes krypteret og genåbnes fra telefonen uden netværksopdatering ved appstart eller sideskift. Radio og TV-guide genbruges også uden automatisk udløb. Brug Opdatér alt, kildens opdatering eller Opdatér radiostationer for nye lister. På eksisterende installationer skal streaming opdateres én gang for at oprette den nye lokale kopi. Tilføjelse eller redigering af en kilde henter fortsat dens indhold.

### 1.27.1

Kanal- og programlogoer vises med kvadratisk beskæring i streaming- og radiolister, lokale fillister, favoritter, bundbaren, den fulde lydafspiller og begge radiowidgets. Appikon, mappeikoner og billedviseren er uændrede.

### 1.27.2

Streaminglister gemmes nu løbende i krypteret binært format i stedet for at opbygge flere komplette JSON-kopier i hukommelsen. Det reducerer hukommelsesforbruget under Opdatér alt. Ældre gemte kataloger kan fortsat læses. Identiske kataloger gemmes ikke længere to gange efter kildeopdatering.

### 1.27.3

Retter langsom lagring, som kunne få streamingopdatering til at blive stående ved fx 1/3. Krypterede kataloger skrives nu i blokke direkte til filen frem for enkeltbytes. Atomisk lagring og kryptering bevares.

### 1.27.4

Favoritter-knappen er hvid med fed sort tekst. Alle dropdown-menuer har samme grå baggrund som hold-menuerne. Logoer vises med deres oprindelige proportioner uden kvadratisk eller rund beskæring i lister, afspillere og widgets.

### 1.27.5

Genåbning af gemte kataloger har sin egen arbejdskø, så Opdatér alt ikke venter bag den. Manuel streamingopdatering afbryder en igangværende gendannelse, og forældede resultater kan ikke overskrive opdateringen. Dekryptering læser 64 KiB ad gangen i stedet for mange små Android Keystore-kald og verificerer krypteringens integritet.

### 1.27.6

Favoritter-knappen i burgermenuen er blå som de øvrige hovedknapper, med fed hvid tekst i 25 % større skriftstørrelse.

### 1.28.0

Radio har tre landemapper: Danmark (DK), England (GB i Radio Browser) og USA (US). Manuel radioopdatering henter alle tre lande og bevarer tidligere data for lande, der fejler. Favoritter bevarer deres land og vises fortsat samlet.

### 1.28.1

Play/stop-ikonerne i begge radiowidgets bruger nu den grå farve #555555. Medieafspillerens undertekst er “Afspilles fra Stream” for radio, streaming og lokale/Pcloud-filer, også i Cast-metadata.

### 1.28.2

Widgetternes play/stop-ikoner bruger nu #989898, midt mellem den tidligere lyse #DADADA og den mørke #555555.

### 1.28.3

Play, pause og stop har let afrundede hjørner. Øvrige tegnede appikoner har fået en diskret afrunding af konturerne. Farver og knapstørrelser er bevaret; widgetternes allerede afrundede ikoner er uændrede.

### 1.28.4

De fire lokale programlogoer er erstattet med de nye filer anders.png, korte.png, balte.png og roder.png. Logoernes oprindelige form og gennemsigtighed bevares.

### 1.28.5

De fire programlogoer matches nu også på Pcloud-filer. Logoerne vises i Pcloud-listen, favoritter, bundbaren og den fulde lydafspiller på samme måde som ved lokale filer.

### 1.28.6

Afstanden mellem ikoner/logoer og tekst er samlet til 12 dp, samme afstand som TV-favoritter. Gælder radio, landemapper, lokale filer, Pcloud, filfavoritter, mappeoverskrifter og bundbaren.

### 1.28.7

Mappeikoner bruger nu den gule farve #FFC400 i lister og mappeoverskrifter, inklusive radiolandemapper, lokale filer, Pcloud og favoritter.

### 1.28.8

Bundbaren viser nu også logoet for TV, film og serier til venstre for navnet. Den bruger medfølgende kanallogo, kildens logo eller indlejret artwork, når tilgængeligt. Radio, lokale filer og Pcloud bevarer deres logoer; TV-logoet bruges også i telefonens bar under casting.

### 1.29.0

Favoritter åbner en forside med store knapper til Streaming, Radio, Podcast og Musik. Streaming indeholder TV, Film og Serier. Nye favoritter kan placeres i en valgt liste uanset kilde, og eksisterende favoritter bevares og fordeles automatisk. Flytning mellem lister og rækkefølge findes i hold-menuen. Mapper fra lokale filer og Pcloud kan åbnes inde i favoritlisten. Listevalg og rækkefølge følger med JSON-eksport/import.

Bluetooth-knappen til venstre for cast findes i appens top og den fulde lydafspiller. Den viser tilsluttede Bluetooth-lydudgange og telefonens højttaler og sender valget til appens lydafspillere via Androids offentlige preferred-device-API. Valgt Bluetooth-udgang markeres blå. Parring og tilslutning af nye enheder sker i telefonens Bluetooth-indstillinger; casting afspilles fortsat på cast-enheden. Enheders faktiske routing skal verificeres på telefonen.

### 1.29.1

Streaming-favoritter bruger nu en vandret række med radiovalgene TV, Film og Serier i stedet for dropdown-menuen, ligesom den almindelige Streaming-side. Fane ved appstart har Forside som øverste mulighed; valget åbner favorittens forside og bevares i import/eksport. Stream-logoet i toppen er centreret på skærmen med Bluetooth- og cast-knapperne til højre.

### 1.30.0

Når en favorit placeres i Streaming, vælges også TV, Film eller Serier. Det valgte underafsnit gemmes uafhængigt af indholdets kilde. Ved Streaming som startfane kan et af de samme tre underafsnit vælges under Indstillinger. Valgene følger med JSON-eksport/import.

Afspilningsposition og varighed gemmes for ikke-live lyd/video fra lokale filer, Pcloud og streaming. En tynd blå progressbar vises i bunden af bundbaren og under mediets række i fillister, favoritter, film- og afsnitslister. Ny afspilning genoptages fra den gemte position; færdigafspillet indhold starter forfra. Positionerne gemmes løbende og ved pause, stop og lukning. Cast-afspilning bruger samme historik, når telefonen er tilsluttet sessionen.

Nulstil lyttede/sete ligger lige over Nulstil app. Den rydder alle gemte positioner og sætter en igangværende ikke-live afspilning tilbage til starten. Livekanaler og billeder har ingen afspilningsprogress. Historikken gemmes separat fra opsætningen på telefonen og eksporteres ikke med opsætningsfilen.

### 1.30.1

Tilbagepilen står nu på samme række til venstre for mappeikon og mappetitel i Lokale filer, Pcloud, radiolandemapper og favoritter. Overskrifter i favoritlister har også tilbagepilen på samme række for at spare lodret plads.

### 1.30.2

Begge dropdown-menuer for opstart viser mulighederne med fed skrift, både den valgte værdi og punkterne i den åbne menu.

### 1.30.3

Favoritforsidens fire knapper har nyt design efter referencen: mørke afrundede kort, blå glød og kant, blå kategoriikoner, lodret skillelinje og stor fed hvid tekst. Kortene skalerer med skærmbredden og har trykfeedback.

### 1.30.4

Favoritforsidens knapper følger den opdaterede reference: neutral mørkegrå baggrund uden blå glød, diskret grå kant og skillelinje, hvid-blå ikoner og Stream-logoet på Streaming-knappen.

### 1.30.5

Streaming-knappen på favoritforsiden har nu et afrundet play-ikon med samme hvid-blå gradient som de øvrige kategoriikoner.

### 1.30.6

Overskriften Favoritter er fjernet fra favoritforsiden, som nu kun viser de fire kategoriknapper.

### 1.30.7

Hold på en af favoritforsidens fire knapper for at åbne Flyt op/Flyt ned. Rækkefølgen gemmes mellem appstarter og følger med JSON-eksport/import. Ikon og handling følger altid den tilhørende kategori.

### 1.31.0

Importér/Eksportér medtager hele den gemte opsætning: kilder og logins, TV-guide-links, Pcloud-konto, lokale mappereferencer, alle favoritter og deres inddeling/rækkefølge, forsideknappernes rækkefølge, startfaner, afspillerindstillinger, seneste afspilning, lyttede/sete-positioner, widgetternes kanalvalg og valgt lydudgang. Aktiv afspilningsposition gemmes inden eksport. Import erstatter opsætningen; filen valideres først, og ved skrivefejl forsøges den tidligere opsætning gendannet. Ældre JSON-backups understøttes fortsat og nulstiller indstillinger, de ikke indeholder.

JSON-filen indeholder logins i læsbar form. Mediefiler og hentede katalog-/billed-/TV-guide-caches er ikke opsætning og medfølger ikke. Androids mappetilladelser, Bluetooth-parringer og placeringer af widgets kan ikke overføres via JSON; på en ny telefon skal disse oprettes igen. Widgetkanalvalg gendannes for eksisterende widget-id'er.

### 1.31.1

Tilbage-knappen er fjernet fra de øverste favoritlister (Streaming, Radio, Podcast og Musik). Den vises kun inde i en mappe og går ét mappeniveau tilbage. Telefonens tilbagefunktion bevarer navigationen til favoritsidens forside.

### 1.31.2

Forsidens fire favoritknapper har halvt så afrundede hjørner: radius 9 dp i stedet for 18 dp, også på trykeffekten.

### 1.32.0

Streaming-knappen på favoritforsiden er erstattet af TV, Film og Serier, som åbner deres respektive favoritlister direkte. Radio, Podcast og Musik er bevaret. Alle seks knapper kan flyttes med hold → Flyt op/Flyt ned. Tidligere gemt rækkefølge og JSON-backups migreres ved at erstatte Streaming med de tre knapper på samme plads. Favoritternes inddeling og startindstillinger bevares.

### 1.32.1

Favoritforsidens TV, Film og Serier har nu forskellige ikoner: fjernsyn, filmklaptræ og stablede afsnitsskærme. De bruger samme hvid-blå gradient og afrundede streger som Radio, Podcast og Musik.

### 1.33.0

Indstillinger → Forsideknapper, lige under Pcloud-konto, giver afkrydsningsfelter til TV, Film, Serier, Radio, Podcast og Musik. Valget gemmes og medtages i import/eksport; skjulte knappers rækkefølge og favoritter bevares. Nulstil lyttede/sete og Opdatér alt har byttet plads, så rækkefølgen er Importér/Eksportér, Nulstil lyttede/sete, Opdatér alt, Nulstil app.

### 1.33.1

Fane ved opstart har nu valgene Forside, TV, Film, Serier, Radio, Podcast og Musik. Det separate valg af Streaming-fane er fjernet. Eksisterende startvalg og JSON-backups bevarer den valgte TV-/Film-/Serier-liste.

### 1.33.2

Forsideknappernes hjørner er mindre afrundede (5 dp). Ikonerne har mere kantede rammer, stregender og former, mens hvid-blå gradient, størrelse og placering bevares.

### 1.34.0

Rækker med afspilningsprogress viser gul prik ved delvis afspilning og grøn prik ved afsluttet afspilning. Aktiv afspilning vises fortsat hvidt. Prikkerne deler størrelse og placering med den eksisterende afspilningsmarkør på favorit-, lokale-, Pcloud-, streaming- og afsnitslister og følger den gemte historik samt Nulstil lyttede/sete.

### 1.34.1

I fuldskærmsafspilleren for lyd er Bluetooth og Cast flyttet fra toppen til nederste venstre hjørne. I videoafspilleren vises de i kontrollinjen sammen med tidslinjen og følger kontrollernes vis/skjul. Cast bruger samme enhedsvælger og tilladelseshåndtering som før.

### 1.34.2

Bluetooth og Cast er fjernet fra hovedappens øverste højre hjørne; Stream-logoet er stadig centreret. I lydafspillerens fulde visning ligger Bluetooth og Cast til venstre på samme kontrollinje som play/pause, stop og Live. Billedvisningens udgangsvalg er også flyttet til kontrollinjen. Video beholder udgangsvalgene ved tidslinjen.

### 1.34.3

Cast-ikonerne bliver blå, når en Cast-session er tilsluttet. Tryk på ikonet under casting for at vælge Stop cast; dette stopper modtagerens afspilning og afslutter forbindelsen. Uden forbindelse åbnes enhedsvælgeren som før. De justerede placeringer fra 1.34.2 er medtaget.

### 1.34.4

Den valgte lydudgang vises med blå tekst i Bluetooth-/højttalervælgeren. Cast-menuen viser den tilsluttede enheds navn med blå tekst over Stop cast.

### 1.34.5

Logoet i appens top er erstattet med brugerens banner.png fra banner.zip. Den eksisterende størrelse, skalering og centrering af topfeltet er bevaret. Appens startskærmsikon er uændret.

### 1.34.6

Topbannerets skalering er fordoblet, og headeren giver plads til det større logo. Standardopsætningen er nu Vis favoritter ved appstart, Forside som startfane, Afspil automatisk seneste og Mini-afspiller. Gemte valg og importerede indstillinger bevares; standarderne gælder nye/nulstillede opsætninger og manglende værdier.

### 1.34.7

Toplogoet er 25 % mindre end i 1.34.6. Forsideknapperne har 12 dp ekstra afstand over den første knap.

### 1.34.8

Forsideikonernes farveforløb er justeret til logoets klare cyan, mættede blå og dybere blå skygger med en lys top. Ikonernes former, størrelser og placeringer er bevaret.

### 1.34.9

På favoritforsiden ligger burgermenuen centreret nederst, over en eventuel afspilningsbar. Toplogoet bevarer centreringen. På øvrige sider ligger burgermenuen fortsat øverst til venstre.

### 1.35.0

Appikonets farver er opdateret til klar cyan og blå med lyse højdepunkter og mørkeblå S-bånd, så det passer til bannerets logo. De eksisterende launcher-indrykninger og baggrunde er bevaret.

### 1.35.1

Burgermenuikonet nederst på forsiden bruger nu samme mørkegrå farve (#252525) som forsideknapperne.

### 1.35.2

Appikonet bruger igen den oprindelige lyse blå/hvide palette med mørkeblåt S-bånd. Topbannerets cyan er tonet til blå, og forsideikonerne har et hvidt/lyseblåt/blåt farveforløb efter den nye reference. Størrelser og placeringer er bevaret.

### 1.35.3

Toplogoets S-symbol tegnes nu direkte fra samme originale blå/hvide billede som appikonet. Stream-ordmærkets oprindelige kontur bevares og får samme hvid/lyseblå/blå palette som forsideikonerne. Det tidligere cyanfarvede banner og farvefilter bruges ikke længere til symbolet. Størrelse og placering er bevaret.

### 1.35.4

Farveændringerne fra 1.35.2 og 1.35.3 er rullet tilbage. Appikon, topbanner og forsideikoner svarer igen til 1.35.1, versionen før ønsket om den blå/hvide palette. Versionsnummeret er øget, så APK'en kan installeres som en almindelig opdatering.

### 1.35.5

Logoet i toppen er 25 % større end i 1.35.4. Farver og centrering er bevaret.

### 1.35.6

Cast og Bluetooth ligger nu øverst til højre i videoafspilleren og følger kontrollernes vis/skjul samt skærmens udskæring. Cast-knappen åbner enhedsvælgeren fra sin egen tilknyttede visning i stedet for en løs, usynlig knap uden vinduestilknytning. Tilladelsessvaret åbner samme knap igen. Blå forbindelsesmarkering og Stop cast bevares.

### 1.35.7

I mini-videoafspilleren ligger Bluetooth og Cast helt oppe ved afspillerens øverste højre kant. Telefonens skærmudskæring tilføjer kun afstand i fuldskærm, og placeringen nulstilles straks ved skift tilbage til mini-afspilleren.

### 1.35.8

Toplogoet bruger de originale billedpixels fra det uploadede skærmbillede fra 23:28:53. Appikonets symbol er hentet fra samme billede, og forsideikonernes gradient bruger tilsvarende lyse/hvide og blå nuancer. Skærmbilledets baggrund og overskydende kanter er fjernet. Kilden er kun 202 × 65 pixels, så skarpheden er begrænset af originalen.

### 1.35.9

Det lille skærmbillede i toppen er erstattet med brugerens banner.jpg i 1005 × 378 pixels fra banner _1_.zip. Sort baggrund er fjernet, og eksisterende skalering og centrering er bevaret. Appikon og forsideikonernes farver er ikke ændret.

### 1.36.0

Appikonets lille 52 × 50-pixel skærmbillede er erstattet med symbolet fra det større banner. Symbolet er nu 217 × 245 pixels på en transparent flade; forholdet mellem symbol og flade samt launcher-indrykningerne er bevaret. Topbanner og forsideikoner er uændrede.

### 1.36.1

Rutinemeddelelser om stoppet afspilning, afsluttet casting og vellykket casting er fjernet. Stop skjuler og rydder statusfeltet, så der heller ikke står et tomt felt tilbage. Fejl ved afspilning, forbindelser og indlæsning vises fortsat.

### 1.37.0

Bluetooth-genforbindelse genoptager den afspiller, som faktisk blev afbrudt, uden at prioritere radio over podcast, Pcloud, lokale filer eller andre aktive medier. Afspilningen pauser ved mistet Bluetooth-lydudgang og fortsætter fra sin eksisterende kø/position, når Bluetooth kommer tilbage. Et nyt medievalg, manuelt stop/pause, valg af telefonhøjttaler eller lukning af appen annullerer den ventende genoptagelse. Bilens generelle play-kommando til radiosessionen videresendes til den senest aktive afspiller, hvis det var et andet medie. Tilstanden gælder den igangværende app-proces; Androids Bluetooth- og baggrundsbegrænsninger gælder fortsat.

### 1.37.1

Begge widgets har særskilte forhåndsvisninger med en cirkel mærket Radio i stedet for et kanallogo/appikon. Den brede forhåndsvisning beholder play-knappen og den mørke baggrund. Der er både previewLayout og previewImage til forskellige Android-/launcher-versioner. Installerede widgets viser fortsat den valgte stations logo.
