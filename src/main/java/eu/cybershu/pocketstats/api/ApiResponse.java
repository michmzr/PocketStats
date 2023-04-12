package eu.cybershu.pocketstats.api;

public record ApiResponse<T>(int status, String message, T data) {
}
