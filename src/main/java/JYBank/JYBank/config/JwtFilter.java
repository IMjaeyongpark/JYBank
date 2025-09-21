package JYBank.JYBank.config;

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
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final AuthService authService; // 추후 role 조회 등 확장에 사용
    private final String secretKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 이미 인증된 상태면 그냥 다음으로
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Authorization 헤더 없거나 Bearer 아님 → 무인증 요청으로 패스
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // "Bearer " 제거
        final String token = authorization.substring(7);

        try {
            // 만료 토큰은 무시(혹은 response 401로 바꿔도 됨)
            if (JwtUtil.isExpired(token, secretKey)) {
                log.debug("JWT expired");
                filterChain.doFilter(request, response);
                return;
            }

            // subject/loginId 추출
            final String loginId = JwtUtil.getLoginId(token, secretKey);
            if (loginId == null || loginId.isBlank()) {
                log.debug("JWT has no subject/loginId");
                filterChain.doFilter(request, response);
                return;
            }

            // 권한 부여 (임시로 USER 고정; 필요 시 role 클레임/DB 조회로 확장)
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            loginId,
                            null,
                            List.of(new SimpleGrantedAuthority("USER"))
                    );

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            // 서명 오류/형식 오류 등 → 조용히 패스(필요시 401 처리로 변경 가능)
            log.debug("JWT parse/verify failed: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
