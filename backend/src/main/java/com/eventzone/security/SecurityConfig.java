package com.eventzone.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                    HandlerMappingIntrospector introspector) throws Exception {
        // The H2 console registers its own servlet alongside the DispatcherServlet,
        // so plain String patterns are ambiguous here. MVC endpoints are matched
        // through the introspector; the console is matched by servlet path.
        MvcRequestMatcher.Builder mvc = new MvcRequestMatcher.Builder(introspector);

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        // Must precede the public GET rule below, which would
                        // otherwise make the organiser's own-events list public.
                        // Writes to /api/events are not matched by those GET-only
                        // rules, so they fall through to anyRequest().authenticated()
                        // and are further constrained by @PreAuthorize on the methods.
                        .requestMatchers(
                                mvc.pattern(HttpMethod.GET, "/api/events/mine"),
                                mvc.pattern(HttpMethod.GET, "/api/events/all")
                        ).authenticated()
                        .requestMatchers(
                                mvc.pattern(HttpMethod.GET, "/api/events"),
                                mvc.pattern(HttpMethod.GET, "/api/events/**"),
                                mvc.pattern(HttpMethod.GET, "/api/categories")
                        ).permitAll()
                        .requestMatchers(mvc.pattern(HttpMethod.POST, "/api/auth/**")).permitAll()
                        .requestMatchers(mvc.pattern("/api/monitoring/**")).permitAll()
                        .requestMatchers(
                                mvc.pattern("/actuator/health"),
                                mvc.pattern("/actuator/health/**"),
                                mvc.pattern("/actuator/info"),
                                mvc.pattern("/actuator/metrics"),
                                mvc.pattern("/actuator/metrics/**"),
                                mvc.pattern("/actuator/prometheus")
                        ).permitAll()
                        .requestMatchers(mvc.pattern("/actuator/**")).authenticated()
                        .requestMatchers(
                                mvc.pattern("/swagger-ui/**"),
                                mvc.pattern("/swagger-ui.html"),
                                mvc.pattern("/v3/api-docs/**")
                        ).permitAll()
                        .requestMatchers(PathRequest.toH2Console()).permitAll()
                        // Anything under /api not explicitly allowed above needs a
                        // token. This must stay ahead of the anyRequest() rule.
                        .requestMatchers(mvc.pattern("/api/**")).authenticated()
                        // Everything else is the built single-page app served from
                        // classpath:/static -- index.html, hashed assets, and the
                        // client-side routes that fall back to it. Public by nature.
                        .anyRequest().permitAll()
                )
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
