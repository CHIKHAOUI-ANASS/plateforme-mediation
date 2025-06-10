package com.mediation.platform.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        // 🔥 AJOUT DE LOGS POUR DEBUGGING
        System.out.println("=== JWT FILTER DEBUG ===");
        System.out.println("Authorization Header: " + authorizationHeader);
        System.out.println("Request URI: " + request.getRequestURI());
        System.out.println("Request Method: " + request.getMethod());

        String username = null;
        String jwt = null;

        // 🔧 CORRECTION : Vérification plus robuste du header
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7); // Retire "Bearer "

            // 🔥 LOG DU TOKEN EXTRAIT
            System.out.println("Token JWT extrait (longueur: " + jwt.length() + "): " + jwt.substring(0, Math.min(50, jwt.length())) + "...");

            try {
                // 🔧 VÉRIFICATION : Le token a-t-il bien 2 points ?
                long periodCount = jwt.chars().filter(ch -> ch == '.').count();
                System.out.println("Nombre de points dans le token: " + periodCount);

                if (periodCount != 2) {
                    System.err.println("❌ TOKEN INVALIDE - Doit contenir exactement 2 points. Trouvé: " + periodCount);
                    // 🔥 AFFICHER LE TOKEN COMPLET POUR DEBUG
                    System.err.println("Token complet reçu: [" + jwt + "]");
                } else {
                    username = jwtUtil.extractUsername(jwt);
                    System.out.println("✅ Username extrait du token: " + username);
                }
            } catch (Exception e) {
                System.err.println("❌ Erreur lors de l'extraction du username: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("❌ Pas de header Authorization ou ne commence pas par 'Bearer '");
        }

        // Si on a un username et qu'aucune authentification n'est déjà présente
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                System.out.println("✅ UserDetails chargé pour: " + username);

                // Valider le token
                if (jwtUtil.validateToken(jwt, userDetails)) {
                    System.out.println("✅ Token valide pour: " + username);

                    // Créer l'objet d'authentification
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Définir l'authentification dans le contexte de sécurité
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    System.out.println("✅ Authentification définie dans le contexte de sécurité");
                } else {
                    System.err.println("❌ Token invalide pour: " + username);
                }
            } catch (Exception e) {
                System.err.println("❌ Erreur lors de la validation: " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("=== FIN JWT FILTER DEBUG ===\n");
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        // 🔧 CORRECTION : Liste mise à jour des endpoints publics
        boolean shouldSkip = path.startsWith("/api/auth/") ||
                path.startsWith("/api/test/") ||
                path.startsWith("/h2-console") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/api-docs") ||
                path.equals("/swagger-ui.html") ||
                path.startsWith("/v3/api-docs") ||
                (path.startsWith("/api/projets") && "GET".equals(request.getMethod())) ||
                (path.startsWith("/api/associations") && "GET".equals(request.getMethod())) ||
                path.startsWith("/api/statistiques/publiques");

        if (shouldSkip) {
            System.out.println("🚫 Filtre JWT ignoré pour: " + path);
        }

        return shouldSkip;
    }
}