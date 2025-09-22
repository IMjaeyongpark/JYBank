package JYBank.JYBank.aop;

import JYBank.JYBank.aop.annotation.Auditable;
import JYBank.JYBank.support.audit.AuditEvent;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Aspect
@Component
public class AuditAspect {

    private final KafkaTemplate<String, AuditEvent> kafka;
    private final String topic;

    public AuditAspect(
            KafkaTemplate<String, AuditEvent> kafka,
            @Value("${app.kafka.topics.audit}") String topic
    ) {
        this.kafka = kafka;
        this.topic = topic;
    }

    @AfterReturning(pointcut = "@annotation(aud)", returning = "ret")
    public void success(JoinPoint jp, Auditable aud, Object ret) {
        AuditEvent evt = new AuditEvent(
                aud.action(),
                "SUCCESS",
                principalOf(jp),
                referenceOf(ret),
                "ok",
                Instant.now()
        );
        // key는 action 또는 principal 기준 권장
        kafka.send(topic, aud.action(), evt);
    }

    @AfterThrowing(pointcut = "@annotation(aud)", throwing = "ex")
    public void fail(JoinPoint jp, Auditable aud, Throwable ex) {
        AuditEvent evt = new AuditEvent(
                aud.action(),
                "FAIL",
                principalOf(jp),
                null,
                ex.getMessage(),
                Instant.now()
        );
        kafka.send(topic, aud.action(), evt);
    }

    // 주체/참조 추출 로직은 필요에 맞게 보완
    private String principalOf(JoinPoint jp) {
        // 1) SecurityContext 우선
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() != null) {
            return String.valueOf(auth.getPrincipal()); // 보통 loginId/email
        }
        // 2) 파라미터에서 추정 (현재 구현 유지)
        Object[] args = jp.getArgs();
        return (args != null && args.length > 0) ? String.valueOf(args[0]) : "unknown";
    }

    private String referenceOf(Object ret) {
        // 예: 서비스가 TransferCreateRes{transferId,...} 반환 시 ID 추출하도록 커스텀
        return (ret != null) ? ret.toString() : null;
    }


}
