package JYBank.JYBank.notification.template;

import JYBank.JYBank.notification.dto.NotificationEvent;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TransferCompletedTemplate implements NotificationTemplate {
    @Override public boolean supports(String type) { return "TRANSFER_COMPLETED".equals(type); }

    @Override public String title(NotificationEvent e) { return "송금이 도착했어요"; }

    @Override public String body(NotificationEvent e) {
        var amount = new BigDecimal(String.valueOf(e.data().get("amount")));
        var currency = String.valueOf(e.data().getOrDefault("currency", "KRW"));
        var from = String.valueOf(e.data().get("fromWalletId"));
        return "지갑 " + from + "에서 " + amount + " " + currency + "가 입금되었습니다.";
    }
}
