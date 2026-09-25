package com.evox.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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

import java.util.List;

/**
 * Configuracion central de seguridad: define que rutas son publicas,
 * cuales requieren estar autenticado y cuales requieren rol ADMINISTRADOR.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CORS: permitir que el frontend local (Vite/Next) consuma la API
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // API sin sesiones de servidor: cada peticion se autentica con su propio token.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Preflight CORS: el navegador envía OPTIONS antes de cada petición
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Rutas públicas
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/productos/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/categorias/**").permitAll()

                        // Solo administradores pueden crear/editar/eliminar productos
                        .requestMatchers(HttpMethod.POST, "/api/v1/productos").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/productos/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/productos/**").hasRole("ADMINISTRADOR")

                        // Solo administradores pueden consultar la lista de usuarios
                        .requestMatchers(HttpMethod.GET, "/api/v1/perfiles").hasRole("ADMINISTRADOR")

                        // Todo lo demas requiere estar autenticado (carrito, pedidos, comentarios)
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** CORS abierto para desarrollo local: frontend en http://localhost:3000, 5173, etc. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
