package JYBank.JYBank.notification.template;

import JYBank.JYBank.notification.dto.NotificationEvent;

public interface NotificationTemplate {
    boolean supports(String type);                      // 어떤 이벤트 타입을 처리할지
    String title(NotificationEvent event);              // 제목
    String body(NotificationEvent event);               // 내용(국제화, 통화포맷 등 여기서)
    default String channel() { return "PUSH"; }         // 기본 채널 (필요시 타입별 다르게)
}
