package JYBank.JYBank.exception;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import JYBank.JYBank.service.auth.AuthService.EmailAlreadyUsedException;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> bad(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new ApiError("BAD_REQUEST", e.getMessage(), null));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> tooMany(RuntimeException e) {
        if ("Too many requests".equals(e.getMessage())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(new ApiError("RATE_LIMIT", e.getMessage(), null));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiError("INTERNAL", e.getMessage(), null));
    }



    @ExceptionHandler(EmailAlreadyUsedException.class)
    public ResponseEntity<?> handleDupEmail(EmailAlreadyUsedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "code", "EMAIL_IN_USE",
                "message", ex.getMessage(),
                "timestamp", Instant.now().toString()
        ));
    }

}
