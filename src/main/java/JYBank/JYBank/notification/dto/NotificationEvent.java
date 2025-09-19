package JYBank.JYBank.notification.dto;

import java.time.Instant;
import java.util.Map;

public record NotificationEvent(
        String eventId,        // 멱등/중복 방지 키 (예: pgTrxId, payoutId, transferId)
        String type,           // TRANSFER_COMPLETED, DEPOSIT_COMPLETED, PAYOUT_FAILED, SECURITY_LOGIN_ALERT ...
        String receiverId,     // 누구에게 알릴지 (userId or walletId)
        Map<String, Object> data, // 템플릿에 넣을 데이터 {amount, currency, fromWalletId ...}
        Instant occurredAt
) {}