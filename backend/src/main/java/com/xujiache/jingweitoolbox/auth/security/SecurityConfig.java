package com.xujiache.jingweitoolbox.auth.security;

import tools.jackson.databind.ObjectMapper;
import com.xujiache.jingweitoolbox.auth.AuthProperties;
import com.xujiache.jingweitoolbox.auth.SessionRepository;
import com.xujiache.jingweitoolbox.common.api.ErrorCode;
import com.xujiache.jingweitoolbox.common.api.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * 无状态安全配置：公开接口放行，admin 接口要求 ADMIN 角色，其余一律要求认证；
 * 认证与授权失败均以契约错误封套返回。
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtDecoder jwtDecoder(JwtService jwtService) {
        return jwtService::decode;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService,
            SessionRepository sessions, ObjectMapper objectMapper, JwtDecoder jwtDecoder) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/guest-session", "/api/v1/auth/login",
                                "/api/v1/auth/refresh", "/api/v1/tools/**", "/actuator/health").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(entryPoint(objectMapper))
                        .accessDeniedHandler(deniedHandler(objectMapper)))
                .oauth2ResourceServer(oauth -> oauth
                        .authenticationEntryPoint(entryPoint(objectMapper))
                        .accessDeniedHandler(deniedHandler(objectMapper))
                        .jwt(jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(principalConverter())))
                .addFilterAfter(new SessionCheckFilter(sessions, jwtService, objectMapper),
                        BearerTokenAuthenticationFilter.class);
        return http.build();
    }

    /**
     * typ claim 映射为 ROLE_GUEST/ROLE_USER/ROLE_ADMIN。
     */
    private Converter<org.springframework.security.oauth2.jwt.Jwt, AbstractAuthenticationToken> principalConverter() {
        return jwt -> {
            String principalType = jwt.getClaimAsString(JwtService.CLAIM_PRINCIPAL_TYPE);
            List<GrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + principalType));
            return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
        };
    }

    private org.springframework.security.web.AuthenticationEntryPoint entryPoint(ObjectMapper objectMapper) {
        return (request, response, exception) -> writeError(response, objectMapper,
                ErrorCode.UNAUTHENTICATED, "未认证或凭证无效");
    }

    private AccessDeniedHandler deniedHandler(ObjectMapper objectMapper) {
        return (request, response, exception) -> writeError(response, objectMapper,
                ErrorCode.FORBIDDEN, "没有访问该资源的权限");
    }

    private static void writeError(HttpServletResponse response, ObjectMapper objectMapper,
            ErrorCode errorCode, String message) throws IOException {
        response.setStatus(errorCode.httpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(errorCode, message));
        response.getWriter().flush();
    }
}
