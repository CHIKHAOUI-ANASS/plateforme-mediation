package com.mediation.platform.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret:myVerySecretKeyForJWTTokenGenerationThatIsAtLeast32CharactersLong}")
    private String secret;

    @Value("${jwt.expiration:86400}")
    private Long expiration; // 24 heures en secondes

    @Value("${jwt.refresh.expiration:604800}")
    private Long refreshExpiration; // 7 jours en secondes

    // 🔧 CORRECTION : Utilisation d'une clé plus robuste
    private SecretKey getSigningKey() {
        // S'assurer que la clé est assez longue
        String finalSecret = secret.length() >= 32 ? secret :
                "myVerySecretKeyForJWTTokenGenerationThatIsAtLeast32CharactersLong";
        return Keys.hmacShaKeyFor(finalSecret.getBytes());
    }

    public String extractUsername(String token) {
        try {
            // 🔥 AJOUT DE LOGS POUR DEBUGGING
            System.out.println("🔍 Extraction username du token: " + token.substring(0, Math.min(50, token.length())) + "...");
            String username = extractClaim(token, Claims::getSubject);
            System.out.println("✅ Username extrait: " + username);
            return username;
        } catch (Exception e) {
            System.err.println("❌ Erreur extraction username: " + e.getMessage());
            throw e;
        }
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            // 🔥 VERIFICATION PRÉALABLE
            if (token == null || token.trim().isEmpty()) {
                throw new IllegalArgumentException("Token est null ou vide");
            }

            // Vérifier le format du token (doit avoir 2 points)
            long periodCount = token.chars().filter(ch -> ch == '.').count();
            if (periodCount != 2) {
                throw new IllegalArgumentException("Token JWT invalide - doit contenir exactement 2 points. Trouvé: " + periodCount);
            }

            System.out.println("🔍 Parsing token avec " + periodCount + " points");

            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            System.err.println("❌ Erreur parsing JWT: " + e.getMessage());
            System.err.println("Token problématique: [" + token + "]");
            throw e;
        }
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userDetails.getUsername());
    }

    public String generateToken(String username, String role, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("userId", userId);

        String token = createToken(claims, username);

        // 🔥 LOG DU TOKEN GÉNÉRÉ
        System.out.println("✅ Token généré pour " + username + " (longueur: " + token.length() + ")");
        System.out.println("Token: " + token.substring(0, Math.min(50, token.length())) + "...");

        return token;
    }

    public String generateRefreshToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tokenType", "refresh");
        return createRefreshToken(claims, username);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private String createRefreshToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpiration * 1000))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            boolean isValid = (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
            System.out.println("🔍 Validation token pour " + username + ": " + (isValid ? "✅ VALIDE" : "❌ INVALIDE"));
            return isValid;
        } catch (Exception e) {
            System.err.println("❌ Erreur validation token: " + e.getMessage());
            return false;
        }
    }

    public Boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            boolean isValid = !isTokenExpired(token);
            System.out.println("🔍 Validation token simple: " + (isValid ? "✅ VALIDE" : "❌ EXPIRÉ"));
            return isValid;
        } catch (Exception e) {
            System.err.println("❌ Erreur validation token simple: " + e.getMessage());
            return false;
        }
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }

    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("tokenType", String.class));
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(extractTokenType(token));
    }
}