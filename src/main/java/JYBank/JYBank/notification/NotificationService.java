package JYBank.JYBank.notification;

import JYBank.JYBank.notification.channel.NotificationChannel;
import JYBank.JYBank.notification.dto.NotificationEvent;
import JYBank.JYBank.notification.template.NotificationTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class NotificationService {
    private final List<NotificationTemplate> templates;
    private final List<NotificationChannel> channels;
    private final StringRedisTemplate redis;

    public NotificationService(List<NotificationTemplate> templates,
                               List<NotificationChannel> channels,
                               StringRedisTemplate redis) {
        this.templates = templates;
        this.channels = channels;
        this.redis = redis;
    }

    // 중복방지(컨슈머 멱등성): eventId로 24시간 동안 한 번만 알림
    private boolean acquire(String eventId) {
        if (eventId == null) return true; // eventId 없으면 스킵
        Boolean ok = redis.opsForValue().setIfAbsent("notif:" + eventId, "1", Duration.ofHours(24));
        return !Boolean.FALSE.equals(ok);
    }

    public void notify(NotificationEvent event) {
        if (!acquire(event.eventId())) return; // 이미 보낸 이벤트면 종료

        var template = templates.stream().filter(t -> t.supports(event.type())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No template for type=" + event.type()));
        var title = template.title(event);
        var body  = template.body(event);
        var channelName = template.channel();

        var channel = channels.stream().filter(c -> c.name().equals(channelName)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No channel=" + channelName));

        channel.send(event.receiverId(), title, body, event);
    }
}
