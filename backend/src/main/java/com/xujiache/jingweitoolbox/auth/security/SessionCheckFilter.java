package com.xujiache.jingweitoolbox.auth.security;

import tools.jackson.databind.ObjectMapper;
import com.xujiache.jingweitoolbox.auth.SessionRepository;
import com.xujiache.jingweitoolbox.common.api.ErrorCode;
import com.xujiache.jingweitoolbox.common.api.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 会话实时校验：注销或令牌轮换后，旧访问令牌立即失效，不等自然过期。
 * 仅作用于已通过 JWT 认证的请求。
 */
public class SessionCheckFilter extends OncePerRequestFilter {

    private final SessionRepository sessions;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public SessionCheckFilter(SessionRepository sessions, JwtService jwtService, ObjectMapper objectMapper) {
        this.sessions = sessions;
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            String sid = jwtAuth.getToken().getClaimAsString(JwtService.CLAIM_SESSION_ID);
            long sessionId = parseSessionId(sid);
            boolean active = sessionId > 0
                    && sessions.isActive(sessionId, jwtService.hash(jwtAuth.getToken().getTokenValue()));
            if (!active) {
                response.setStatus(ErrorCode.UNAUTHENTICATED.httpStatus().value());
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                objectMapper.writeValue(response.getWriter(),
                        ErrorResponse.of(ErrorCode.UNAUTHENTICATED, "会话已失效，请重新认证"));
                response.getWriter().flush();
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private long parseSessionId(String sid) {
        if (sid == null) {
            return -1;
        }
        try {
            return Long.parseLong(sid);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }
}
