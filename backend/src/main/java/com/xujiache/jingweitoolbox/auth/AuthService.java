package com.xujiache.jingweitoolbox.auth;

import com.xujiache.jingweitoolbox.auth.dto.AuthDtos.AuthData;
import com.xujiache.jingweitoolbox.auth.dto.AuthDtos.LoginRequest;
import com.xujiache.jingweitoolbox.auth.dto.AuthDtos.PrincipalData;
import com.xujiache.jingweitoolbox.auth.dto.AuthDtos.RefreshRequest;
import com.xujiache.jingweitoolbox.auth.security.JwtService;
import com.xujiache.jingweitoolbox.common.api.ApiException;
import com.xujiache.jingweitoolbox.common.api.ErrorCode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证会话服务：游客会话、登录、刷新、注销与当前身份。
 * 会话撤销实时生效：SessionCheckFilter 对每个已认证请求核对会话状态。
 */
@Service
public class AuthService {

    private static final String SESSION_TYPE_GUEST = "GUEST";

    private final JwtService jwt;
    private final UserRepository users;
    private final SessionRepository sessions;
    private final PasswordEncoder passwordEncoder;

    public AuthService(JwtService jwt, UserRepository users, SessionRepository sessions,
            PasswordEncoder passwordEncoder) {
        this.jwt = jwt;
        this.users = users;
        this.sessions = sessions;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthData createGuestSession() {
        Instant now = Instant.now();
        Instant expiry = now.plus(jwt.guestTtl());
        long sessionId = sessions.create(null, SESSION_TYPE_GUEST, "pending:" + UUID.randomUUID(), expiry);
        String token = jwt.issueToken(String.valueOf(sessionId), "guest-" + sessionId, SESSION_TYPE_GUEST,
                null, jwt.guestTtl());
        sessions.updateTokenHash(sessionId, jwt.hash(token));
        return new AuthData(token, null, "Bearer", expiry, SESSION_TYPE_GUEST);
    }

    @Transactional
    public AuthData login(LoginRequest request) {
        UserRepository.UserRow user = users.findByIdentifier(request.identifier())
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED, "账号或密码不正确"));
        if (!"ACTIVE".equals(user.status())) {
            throw new ApiException(ErrorCode.UNAUTHENTICATED, "账号不可用");
        }
        if (user.passwordHash() == null || !passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new ApiException(ErrorCode.UNAUTHENTICATED, "账号或密码不正确");
        }
        String principalType = user.role();

        Instant now = Instant.now();
        Instant accessExpiry = now.plus(jwt.accessTtl());
        Instant sessionExpiry = now.plus(jwt.refreshTtl());
        long sessionId = sessions.create(user.id(), principalType, "pending:" + UUID.randomUUID(), sessionExpiry);
        String access = jwt.issueToken(String.valueOf(sessionId), String.valueOf(user.id()), principalType,
                user.displayName(), jwt.accessTtl());
        String refresh = jwt.issueToken(String.valueOf(sessionId), String.valueOf(user.id()), principalType,
                user.displayName(), jwt.refreshTtl());
        sessions.updateTokenHash(sessionId, jwt.hash(access));
        return new AuthData(access, refresh, "Bearer", accessExpiry, principalType);
    }

    public AuthData refresh(RefreshRequest request) {
        Jwt claims;
        try {
            claims = jwt.decode(request.refreshToken());
        } catch (JwtException ex) {
            throw new ApiException(ErrorCode.UNAUTHENTICATED, "刷新令牌无效或已过期");
        }
        String principalType = claims.getClaimAsString(JwtService.CLAIM_PRINCIPAL_TYPE);
        if (SESSION_TYPE_GUEST.equals(principalType)) {
            throw new ApiException(ErrorCode.UNAUTHENTICATED, "游客会话不支持刷新");
        }
        long sessionId = parseSessionId(claims);
        if (!sessions.isUsable(sessionId)) {
            throw new ApiException(ErrorCode.UNAUTHENTICATED, "会话已失效");
        }
        String access = jwt.issueToken(claims.getClaimAsString(JwtService.CLAIM_SESSION_ID),
                claims.getSubject(), principalType,
                claims.getClaimAsString(JwtService.CLAIM_DISPLAY_NAME), jwt.accessTtl());
        sessions.updateTokenHash(sessionId, jwt.hash(access));
        return new AuthData(access, request.refreshToken(), "Bearer",
                Instant.now().plus(jwt.accessTtl()), principalType);
    }

    public void logout(Jwt claims) {
        sessions.revoke(parseSessionId(claims));
    }

    public PrincipalData currentUser(Jwt claims) {
        String principalType = claims.getClaimAsString(JwtService.CLAIM_PRINCIPAL_TYPE);
        boolean guest = SESSION_TYPE_GUEST.equals(principalType);
        return new PrincipalData(principalType, guest ? null : claims.getSubject(),
                claims.getClaimAsString(JwtService.CLAIM_DISPLAY_NAME), List.of());
    }

    private long parseSessionId(Jwt claims) {
        String sid = claims.getClaimAsString(JwtService.CLAIM_SESSION_ID);
        if (sid == null) {
            throw new ApiException(ErrorCode.UNAUTHENTICATED, "令牌缺少会话标识");
        }
        try {
            return Long.parseLong(sid);
        } catch (NumberFormatException ex) {
            throw new ApiException(ErrorCode.UNAUTHENTICATED, "令牌会话标识非法");
        }
    }
}
