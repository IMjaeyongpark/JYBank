package JYBank.JYBank.notification;

import JYBank.JYBank.notification.dto.NotificationEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationConsumer {
    private final NotificationService service;
    public NotificationConsumer(NotificationService s) { this.service = s; }

    @KafkaListener(topics = "${app.kafka.topics.notification}", groupId = "jybank-notification")
    public void onEvent(NotificationEvent event) {
        service.notify(event);
    }
}
