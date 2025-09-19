package JYBank.JYBank.support.audit;


import java.time.Instant;

public record AuditEvent(
        String action,       // e.g. TRANSFER_CREATE
        String result,       // SUCCESS | FAIL
        String principal,    // 사용자/지갑/주체 식별자
        String reference,    // 참조 ID(transferId 등)
        String message,      // 에러/요약 메시지
        Instant at           // 이벤트 시각
) { }