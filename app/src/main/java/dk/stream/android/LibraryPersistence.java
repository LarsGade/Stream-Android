package dk.stream.android;
/** Keeps source-loading and UI behavior independent of the persistence backend. */
interface LibraryPersistence {
 Library load() throws Exception;
 void save(Library library) throws Exception;
}
