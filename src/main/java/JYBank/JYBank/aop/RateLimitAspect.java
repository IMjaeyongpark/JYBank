package JYBank.JYBank.aop;

import JYBank.JYBank.aop.annotation.RateLimited;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Aspect
@Component
public class RateLimitAspect {
    private final StringRedisTemplate redis;

    public RateLimitAspect(StringRedisTemplate redis) { this.redis = redis; }

    @Before("@annotation(anno)")
    public void before(JoinPoint jp, RateLimited anno) {
        String key = evalKey(jp, anno.key());
        String window = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        String rk = "rl:" + key + ":" + window;
        Long c = redis.opsForValue().increment(rk);
        if (c != null && c == 1L) redis.expire(rk, Duration.ofMinutes(1));
        if (c != null && c > anno.permitsPerMinute()) throw new RuntimeException("Too many requests");
    }

    private String evalKey(JoinPoint jp, String spel) {
        String[] names = ((MethodSignature) jp.getSignature()).getParameterNames();
        Object[] args = jp.getArgs();
        var parser = new org.springframework.expression.spel.standard.SpelExpressionParser();
        var ctx = new org.springframework.expression.spel.support.StandardEvaluationContext();
        for (int i = 0; i < names.length; i++) ctx.setVariable(names[i], args[i]);
        return parser.parseExpression(spel).getValue(ctx, String.class);
    }
}
