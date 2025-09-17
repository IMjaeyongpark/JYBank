package JYBank.JYBank.aop;

import JYBank.JYBank.aop.annotation.Idempotent;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Duration;

@Aspect
@Component
public class IdempotencyAspect {
    private final StringRedisTemplate redis;
    private final SpelExpressionParser parser = new SpelExpressionParser();

    public IdempotencyAspect(StringRedisTemplate redis) { this.redis = redis; }

    @Around("@annotation(anno)")
    public Object around(ProceedingJoinPoint pjp, Idempotent anno) throws Throwable {
        String key = evalKey(pjp, anno.key());
        String redisKey = "idem:" + key;
        Boolean ok = redis.opsForValue().setIfAbsent(redisKey, "1", Duration.ofSeconds(anno.ttlSeconds()));
        if (Boolean.FALSE.equals(ok)) {
            throw new IllegalStateException("Duplicate request: " + key);
        }
        try {
            return pjp.proceed();
        } catch (Throwable t) {
            redis.delete(redisKey); // 정책에 따라 보존할 수도 있음
            throw t;
        }
    }

    private String evalKey(ProceedingJoinPoint pjp, String spel) {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        String[] paramNames = ((MethodSignature) pjp.getSignature()).getParameterNames();
        Object[] args = pjp.getArgs();
        EvaluationContext ctx = new StandardEvaluationContext();
        for (int i = 0; i < paramNames.length; i++) ctx.setVariable(paramNames[i], args[i]);
        return parser.parseExpression(spel).getValue(ctx, String.class);
    }
}
