package JYBank.JYBank.config;

import JYBank.JYBank.domain.user.AppUser;
import JYBank.JYBank.repository.AppUserRepository;
import JYBank.JYBank.service.auth.AuthService;
import JYBank.JYBank.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final AuthService authService;            // 필요 시 역할/권한 조회에 사용
    private final AppUserRepository userRepo;
    private final String secretKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 이미 인증됐으면 패스
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        // 토큰 없거나 형식 아님 → 무인증 요청으로 패스
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authorization.substring(7);

        try {
            // 만료면 패스(원하면 401 바로 내려도 됨)
            if (JwtUtil.isExpired(token, secretKey)) {
                log.debug("JWT expired");
                filterChain.doFilter(request, response);
                return;
            }

            // 주체(loginId/email) & 발급시각(iat) 추출
            final String loginId = JwtUtil.getLoginId(token, secretKey);
            if (loginId == null || loginId.isBlank()) {
                log.debug("JWT has no subject/loginId");
                filterChain.doFilter(request, response);
                return;
            }

            final Instant iat = JwtUtil.getIssuedAt(token, secretKey); // ⬅ 하드 로그아웃 비교용(없으면 JwtUtil에 추가)

            // 🔒 하드 로그아웃(즉시 무효화) 체크: lastLogoutAt 이후 발급된 토큰만 허용
            AppUser u = userRepo.findByEmailIgnoreCase(loginId).orElse(null);
            if (u != null && u.getLastLogoutAt() != null &&
                    (iat == null || iat.isBefore(u.getLastLogoutAt()))) {
                log.debug("Access token invalidated by global logout: iat < lastLogoutAt");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            // (선택) 토큰 타입이 access인지 확인하고 싶으면 여기에 추가:
            // if (!JwtUtil.isAccessToken(token, secretKey)) { ... }

            // 권한 부여 (임시 USER 고정; 필요 시 role 클레임/DB로 확장)
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            loginId,
                            null,
                            List.of(new SimpleGrantedAuthority("USER"))
                    );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            log.debug("JWT parse/verify failed: {}", e.getMessage());
            // 원하면 여기서 401로 끊고 return; 해도 됨
        }

        filterChain.doFilter(request, response);
    }
}
