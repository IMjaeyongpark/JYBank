package JYBank.JYBank.aop;

import JYBank.JYBank.aop.annotation.Auditable;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuditAspect {
    @AfterReturning(pointcut = "@annotation(aud)", returning = "ret")
    public void success(JoinPoint jp, Auditable aud, Object ret) {
        // TODO: Kafka 등으로 감사 이벤트 발행
        System.out.printf("[AUDIT] action=%s result=SUCCESS ret=%s\n", aud.action(), ret);
    }

    @AfterThrowing(pointcut = "@annotation(aud)", throwing = "ex")
    public void fail(JoinPoint jp, Auditable aud, Throwable ex) {
        System.out.printf("[AUDIT] action=%s result=FAIL err=%s\n", aud.action(), ex.getMessage());
    }
}
