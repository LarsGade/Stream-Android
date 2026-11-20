# Stream Cast-modtager

Denne modtager er klargjort, men endnu ikke aktiveret. Standard-APK'en bruger fortsat Googles Default Media Receiver, indtil Stream har et registreret Cast-app-id.

1. Host index.html på en offentligt tilgængelig HTTPS-adresse.
2. Registrer en Custom Web Receiver med navnet Stream i Google Cast SDK Developer Console, og angiv sidens adresse.
3. Registrer test-enheder eller publicer receiveren, så den kan bruges på de ønskede enheder.
4. Byg APK'en med `./gradlew assembleDebug -PcastReceiverAppId=DIT_APP_ID`.

App-id'et er offentligt og må deles. Adgangskoder eller kontonøgler skal ikke deles. Navnet Default Media Receiver styres af Googles registrerede receiver-app og kan ikke fjernes med mediemetadata.
