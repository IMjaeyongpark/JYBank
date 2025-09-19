package JYBank.JYBank.notification.channel;

import JYBank.JYBank.notification.dto.NotificationEvent;

public interface NotificationChannel {
    String name(); // "PUSH", "EMAIL", "SMS" ...
    void send(String receiverId, String title, String body, NotificationEvent event);
}
