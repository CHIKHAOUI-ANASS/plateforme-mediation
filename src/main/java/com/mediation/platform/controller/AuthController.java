package com.mediation.platform.controller;

import com.mediation.platform.dto.mapper.AuthMapper;
import com.mediation.platform.dto.request.*;
import com.mediation.platform.dto.response.ApiResponse;
import com.mediation.platform.dto.response.LoginResponse;
import com.mediation.platform.entity.Association;
import com.mediation.platform.entity.Donateur;
import com.mediation.platform.entity.Utilisateur;
import com.mediation.platform.enums.StatutUtilisateur;
import com.mediation.platform.exception.ValidationException;
import com.mediation.platform.security.UserPrincipal;
import com.mediation.platform.service.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UtilisateurService utilisateurService;

    @Autowired
    private DonateurService donateurService;

    @Autowired
    private AssociationService associationService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private AuthMapper authMapper;

    /**
     * Test endpoint
     */
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("AuthController avec JWT fonctionne !");
    }

    /**
     * Connexion d'un utilisateur avec JWT
     * POST /auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest) {

        try {
            // Authentification avec Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getMotDePasse()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Générer les tokens JWT
            Map<String, Object> tokenResponse = jwtService.generateTokenResponse(authentication);

            // Récupérer l'utilisateur authentifié
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            Optional<Utilisateur> utilisateur = utilisateurService.findByEmail(userPrincipal.getEmail());

            if (utilisateur.isPresent()) {
                Utilisateur user = utilisateur.get();

                // Mettre à jour la dernière connexion
                user.marquerConnexion();
                utilisateurService.save(user);

                // Créer la réponse de connexion
                LoginResponse loginResponse = authMapper.toLoginResponse(user);
                loginResponse.setToken((String) tokenResponse.get("accessToken"));
                loginResponse.setRefreshToken((String) tokenResponse.get("refreshToken"));
                loginResponse.setTokenType((String) tokenResponse.get("tokenType"));
                loginResponse.setExpiresIn((Integer) tokenResponse.get("expiresIn"));

                return ResponseEntity.ok(
                        ApiResponse.success("Connexion réussie", loginResponse)
                );
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Utilisateur non trouvé"));
            }

        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Email ou mot de passe incorrect"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la connexion", e.getMessage()));
        }
    }

    /**
     * Renouvellement du token d'accès
     * POST /auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refreshToken(
            @RequestBody RefreshTokenRequest refreshTokenRequest) {

        try {
            String refreshToken = refreshTokenRequest.getRefreshToken();

            if (!jwtService.validateJwtToken(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Refresh token invalide"));
            }

            String newAccessToken = jwtService.refreshAccessToken(refreshToken);

            Map<String, Object> tokenResponse = Map.of(
                    "accessToken", newAccessToken,
                    "tokenType", "Bearer",
                    "expiresIn", jwtService.getTimeToExpiration(newAccessToken)
            );

            return ResponseEntity.ok(
                    ApiResponse.success("Token renouvelé avec succès", tokenResponse)
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Erreur lors du renouvellement du token", e.getMessage()));
        }
    }

    /**
     * Déconnexion (côté client principalement)
     * POST /auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(
                ApiResponse.success("Déconnexion réussie", "User logged out successfully")
        );
    }

    /**
     * Inscription d'un donateur
     * POST /auth/register/donateur
     */
    @PostMapping("/register/donateur")
    public ResponseEntity<ApiResponse<LoginResponse>> registerDonateur(
            @Valid @RequestBody RegisterDonateurRequest registerRequest) {

        try {
            // Vérifier si l'email existe déjà
            if (utilisateurService.existsByEmail(registerRequest.getEmail())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error("Un compte avec cet email existe déjà"));
            }

            // Convertir DTO vers entité
            Donateur donateur = authMapper.toDonateurEntity(registerRequest);
            donateur.setStatut(StatutUtilisateur.ACTIF); // Donateur actif immédiatement
            donateur.setMotDePasse(passwordEncoder.encode(donateur.getMotDePasse()));

            // Sauvegarder le donateur
            Donateur savedDonateur = donateurService.save(donateur);

            // Envoyer email de bienvenue
            try {
                emailService.envoyerEmailBienvenue(
                        savedDonateur.getEmail(),
                        savedDonateur.getNomComplet()
                );
            } catch (Exception emailException) {
                System.err.println("Erreur envoi email de bienvenue: " + emailException.getMessage());
            }

            // Authentifier automatiquement l'utilisateur
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            registerRequest.getEmail(),
                            registerRequest.getMotDePasse()
                    )
            );

            Map<String, Object> tokenResponse = jwtService.generateTokenResponse(authentication);

            // Retourner la réponse de connexion
            LoginResponse loginResponse = authMapper.toLoginResponse(savedDonateur);
            loginResponse.setToken((String) tokenResponse.get("accessToken"));
            loginResponse.setRefreshToken((String) tokenResponse.get("refreshToken"));
            loginResponse.setTokenType((String) tokenResponse.get("tokenType"));
            loginResponse.setExpiresIn((Integer) tokenResponse.get("expiresIn"));

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Compte donateur créé avec succès", loginResponse));

        } catch (ValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Données invalides", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la création du compte", e.getMessage()));
        }
    }

    /**
     * Inscription d'une association
     * POST /auth/register/association
     */
    @PostMapping("/register/association")
    public ResponseEntity<ApiResponse<String>> registerAssociation(
            @Valid @RequestBody RegisterAssociationRequest registerRequest) {

        try {
            // Vérifier si l'email existe déjà
            if (utilisateurService.existsByEmail(registerRequest.getEmail())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error("Un compte avec cet email existe déjà"));
            }

            // Convertir DTO vers entité
            Association association = authMapper.toAssociationEntity(registerRequest);
            association.setStatut(StatutUtilisateur.EN_ATTENTE); // Association en attente de validation
            association.setMotDePasse(passwordEncoder.encode(association.getMotDePasse()));

            // Sauvegarder l'association
            Association savedAssociation = associationService.save(association);

            // Envoyer email de confirmation
            try {
                emailService.envoyerEmailPersonnalise(
                        savedAssociation.getEmail(),
                        "Demande d'association reçue",
                        "Bonjour " + savedAssociation.getNomComplet() + ",\n\n" +
                                "Votre demande d'inscription pour l'association '" + savedAssociation.getNomAssociation() +
                                "' a été reçue.\nElle sera examinée par nos équipes dans les plus brefs délais.\n\n" +
                                "Vous recevrez un email de confirmation une fois la validation effectuée.\n\n" +
                                "Cordialement,\nL'équipe de la plateforme"
                );
            } catch (Exception emailException) {
                System.err.println("Erreur envoi email de confirmation: " + emailException.getMessage());
            }

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(
                            "Demande d'association créée avec succès. Vous recevrez un email une fois la validation effectuée.",
                            "Demande en cours de traitement"
                    ));

        } catch (ValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Données invalides", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la création de la demande", e.getMessage()));
        }
    }

    /**
     * Vérifier si un email existe
     * GET /auth/check-email?email=test@example.com
     */
    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<Boolean>> checkEmail(@RequestParam String email) {

        try {
            boolean exists = utilisateurService.existsByEmail(email);

            return ResponseEntity.ok(
                    ApiResponse.success(
                            exists ? "Email déjà utilisé" : "Email disponible",
                            exists
                    )
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la vérification", e.getMessage()));
        }
    }

    /**
     * Changement de mot de passe
     * PUT /auth/change-password
     */
    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest changePasswordRequest,
            Authentication authentication) {

        try {
            // Vérifier que les mots de passe correspondent
            if (!changePasswordRequest.isPasswordMatch()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Les mots de passe ne correspondent pas"));
            }

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            Optional<Utilisateur> utilisateurOpt = utilisateurService.findByEmail(userPrincipal.getEmail());

            if (utilisateurOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Utilisateur non trouvé"));
            }

            Utilisateur utilisateur = utilisateurOpt.get();

            // Vérifier l'ancien mot de passe
            if (!passwordEncoder.matches(changePasswordRequest.getAncienMotDePasse(), utilisateur.getMotDePasse())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Ancien mot de passe incorrect"));
            }

            // Changer le mot de passe
            utilisateur.setMotDePasse(passwordEncoder.encode(changePasswordRequest.getNouveauMotDePasse()));
            utilisateurService.save(utilisateur);

            return ResponseEntity.ok(
                    ApiResponse.success("Mot de passe modifié avec succès", "Changement effectué")
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors du changement de mot de passe", e.getMessage()));
        }
    }

    /**
     * Réinitialisation de mot de passe
     * POST /auth/reset-password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest resetPasswordRequest) {

        try {
            Optional<Utilisateur> utilisateur = utilisateurService.findByEmail(resetPasswordRequest.getEmail());

            if (utilisateur.isPresent()) {
                // Générer un nouveau mot de passe temporaire
                String nouveauMotDePasse = genererMotDePasseTemporaire();

                // Réinitialiser le mot de passe
                Utilisateur user = utilisateur.get();
                user.setMotDePasse(passwordEncoder.encode(nouveauMotDePasse));
                utilisateurService.save(user);

                // Envoyer email avec le nouveau mot de passe
                emailService.envoyerEmailResetMotDePasse(
                        user.getEmail(),
                        nouveauMotDePasse
                );

                return ResponseEntity.ok(
                        ApiResponse.success("Un nouveau mot de passe a été envoyé à votre adresse email", "Email envoyé")
                );
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Aucun compte trouvé avec cet email"));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la réinitialisation", e.getMessage()));
        }
    }

    /**
     * Vérification du token JWT
     * GET /auth/verify
     */
    @GetMapping("/verify")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyToken(Authentication authentication) {
        try {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

            Map<String, Object> userInfo = Map.of(
                    "id", userPrincipal.getId(),
                    "email", userPrincipal.getEmail(),
                    "nom", userPrincipal.getNom(),
                    "prenom", userPrincipal.getPrenom(),
                    "nomComplet", userPrincipal.getNomComplet(),
                    "authorities", userPrincipal.getAuthorities()
            );

            return ResponseEntity.ok(
                    ApiResponse.success("Token valide", userInfo)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Token invalide"));
        }
    }

    /**
     * Informations sur le token
     * GET /auth/token-info
     */
    @GetMapping("/token-info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTokenInfo(
            @RequestHeader("Authorization") String authHeader) {

        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                Map<String, Object> tokenInfo = jwtService.getTokenInfo(token);

                return ResponseEntity.ok(
                        ApiResponse.success("Informations du token", tokenInfo)
                );
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Token manquant ou format invalide"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Token invalide", e.getMessage()));
        }
    }

    // ========== MÉTHODES UTILITAIRES ==========

    private String genererMotDePasseTemporaire() {
        return "Temp" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String getMessageStatut(StatutUtilisateur statut) {
        switch (statut) {
            case EN_ATTENTE:
                return "Votre compte est en attente de validation";
            case REFUSE:
                return "Votre compte a été refusé";
            case SUSPENDU:
                return "Votre compte est suspendu";
            case INACTIF:
                return "Votre compte est inactif";
            default:
                return "Votre compte n'est pas actif";
        }
    }
}