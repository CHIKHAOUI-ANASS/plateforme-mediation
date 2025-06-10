package com.mediation.platform.config;

import com.mediation.platform.security.JwtAuthenticationEntryPoint;
import com.mediation.platform.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // ======== ENDPOINTS PUBLICS (PAS D'AUTH REQUISE) ========

                        // Endpoints d'authentification
                        .requestMatchers("/auth/**").permitAll()

                        // 🔥 ENDPOINTS DE TEST - PUBLICS POUR DÉVELOPPEMENT
                        .requestMatchers(HttpMethod.GET, "/test/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/test/**").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/test/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/test/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/test/**").permitAll()

                        // Documentation et outils de développement
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/api-docs/**").permitAll()
                        .requestMatchers("/swagger-ui.html").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()

                        // Endpoints publics pour consultation (sans auth)
                        .requestMatchers(HttpMethod.GET, "/projets").permitAll()
                        .requestMatchers(HttpMethod.GET, "/projets/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/associations").permitAll()
                        .requestMatchers(HttpMethod.GET, "/associations/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/statistiques/publiques").permitAll()

                        // ======== ENDPOINTS PROTÉGÉS (AUTH REQUISE) ========

                        // Endpoints admin
                        .requestMatchers("/admin/**").hasRole("ADMINISTRATEUR")

                        // Endpoints donateur
                        .requestMatchers("/donateur/**").hasRole("DONATEUR")

                        // Endpoints association
                        .requestMatchers("/associations/dashboard").hasRole("ASSOCIATION")
                        .requestMatchers("/associations/profil").hasRole("ASSOCIATION")
                        .requestMatchers("/associations/projets").hasRole("ASSOCIATION")

                        // Tout le reste nécessite une authentification
                        .anyRequest().authenticated()
                );

        // 🔥 IMPORTANT: Ajouter le filtre JWT APRÈS les endpoints publics
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Pour H2 console (développement)
        http.headers(headers -> headers.frameOptions().disable());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}