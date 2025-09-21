package JYBank.JYBank.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

public class JwtUtil {

    private static Key key(String secret) {
        // 최소 32바이트 이상 권장
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private static JwtParser parser(String secret) {
        return Jwts.parserBuilder()
                .setSigningKey(key(secret))
                .setAllowedClockSkewSeconds(30) // 시계 드리프트 허용
                .build();
    }

    public static String getLoginId(String token, String secretKey) {
        Claims claims = parseClaims(token, secretKey);
        // 커스텀 클레임(loginId) 우선, 없으면 subject 사용
        String login = claims.get("loginId", String.class);
        return (login != null) ? login : claims.getSubject();
    }

    public static boolean isExpired(String token, String secretKey) {
        Claims c = parseClaims(token, secretKey);
        return c.getExpiration().before(new Date());
    }

    public static String createAccessToken(String loginId, String secretKey, long expiredMs) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(loginId)                 // 표준 subject 사용
                .claim("loginId", loginId)           // 기존 호환 위해 유지
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expiredMs))
                .signWith(key(secretKey), SignatureAlgorithm.HS256)
                .compact();
    }

    public static String createRefreshToken(String loginId, String secretKey, long expiredMs) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(loginId)                 // 누구의 토큰인지 명확히
                .claim("typ", "refresh")             // 타입 표시(검증 시 활용)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expiredMs))
                .signWith(key(secretKey), SignatureAlgorithm.HS256)
                .compact();
    }

    // ===== 내부 유틸 =====
    private static Claims parseClaims(String rawToken, String secretKey) {
        String token = stripBearer(rawToken);
        return parser(secretKey).parseClaimsJws(token).getBody();
    }

    private static String stripBearer(String token) {
        if (token == null) return null;
        String t = token.trim();
        return t.startsWith("Bearer ") ? t.substring(7) : t;
    }

    public static boolean isRefreshToken(String token, String secretKey) {
        var claims = Jwts.parserBuilder()
                .setSigningKey(key(secretKey))
                .setAllowedClockSkewSeconds(30)
                .build()
                .parseClaimsJws(stripBearer(token))
                .getBody();
        return "refresh".equals(claims.get("typ", String.class));
    }

}
