package eu.cybershu.pocketstats.db;

public enum Source {
    POCKET("pocket"),
    INSTAPAPER("instapaper"),
    READER("reader");

    private final String sourceType;

    Source(String sourceType) {
        this.sourceType = sourceType;
    }
}
