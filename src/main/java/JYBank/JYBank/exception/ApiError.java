package JYBank.JYBank.exception;

public record ApiError(String code, String message, String traceId) { }