package JYBank.JYBank.notification.channel;

import JYBank.JYBank.notification.dto.NotificationEvent;
import org.springframework.stereotype.Component;

@Component
public class PushChannel implements NotificationChannel {
    @Override public String name() { return "PUSH"; }

    @Override
    public void send(String receiverId, String title, String body, NotificationEvent event) {
        System.out.printf("[PUSH] to=%s | %s - %s | event=%s%n", receiverId, title, body, event.type());
        // TODO: FCM/푸시 게이트웨이 연동
    }
}
