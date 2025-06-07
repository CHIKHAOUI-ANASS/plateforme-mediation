package com.mediation.platform.controller;

import com.mediation.platform.dto.request.*;
import com.mediation.platform.dto.response.ApiResponse;
import com.mediation.platform.dto.response.LoginResponse;
import com.mediation.platform.entity.Utilisateur;
import com.mediation.platform.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Authentification", description = "API d'authentification et gestion des comptes")
public class AuthController {

    @Autowired
    private AuthenticationService authenticationService;

    /**
     * Connexion utilisateur
     */
    @PostMapping("/login")
    @Operation(summary = "Connexion utilisateur", description = "Authentifie un utilisateur et retourne un token JWT")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            LoginResponse loginResponse = authenticationService.login(loginRequest);
            return ResponseEntity.ok(ApiResponse.success("Connexion réussie", loginResponse));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Échec de la connexion", e.getMessage()));
        }
    }

    /**
     * Inscription donateur
     */
    @PostMapping("/register/donateur")
    @Operation(summary = "Inscription donateur", description = "Créer un compte donateur")
    public ResponseEntity<ApiResponse<String>> registerDonateur(@Valid @RequestBody RegisterDonateurRequest request) {
        try {
            Utilisateur utilisateur = authenticationService.registerDonateur(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Compte donateur créé avec succès",
                            "Bienvenue " + utilisateur.getNomComplet() + " !"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Erreur lors de la création du compte", e.getMessage()));
        }
    }

    /**
     * Inscription association
     */
    @PostMapping("/register/association")
    @Operation(summary = "Inscription association", description = "Créer un compte association (en attente de validation)")
    public ResponseEntity<ApiResponse<String>> registerAssociation(@Valid @RequestBody RegisterAssociationRequest request) {
        try {
            Utilisateur utilisateur = authenticationService.registerAssociation(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Demande d'association créée avec succès",
                            "Votre demande est en cours de traitement. Vous recevrez un email de confirmation."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Erreur lors de la création de la demande", e.getMessage()));
        }
    }

    /**
     * Vérifier si un email existe
     */
    @GetMapping("/check-email")
    @Operation(summary = "Vérifier email", description = "Vérifie si un email est déjà utilisé")
    public ResponseEntity<ApiResponse<Boolean>> checkEmail(@RequestParam String email) {
        try {
            boolean exists = authenticationService.emailExists(email);
            String message = exists ? "Email déjà utilisé" : "Email disponible";
            return ResponseEntity.ok(ApiResponse.success(message, exists));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la vérification", e.getMessage()));
        }
    }

    /**
     * Changer le mot de passe
     */
    @PostMapping("/change-password")
    @Operation(summary = "Changer mot de passe", description = "Permet à un utilisateur connecté de changer son mot de passe")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @RequestHeader("Authorization") String token) {
        try {
            // Vérifier que les mots de passe correspondent
            if (!request.isPasswordMatch()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Les mots de passe ne correspondent pas"));
            }

            // Obtenir l'utilisateur connecté
            Utilisateur utilisateur = authenticationService.getCurrentUser(token);

            // Changer le mot de passe
            boolean success = authenticationService.changerMotDePasse(
                    utilisateur.getIdUtilisateur(),
                    request.getAncienMotDePasse(),
                    request.getNouveauMotDePasse()
            );

            if (success) {
                return ResponseEntity.ok(ApiResponse.success("Mot de passe modifié avec succès"));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Ancien mot de passe incorrect"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors du changement de mot de passe", e.getMessage()));
        }
    }

    /**
     * Réinitialiser le mot de passe
     */
    @PostMapping("/reset-password")
    @Operation(summary = "Réinitialiser mot de passe", description = "Envoie un nouveau mot de passe par email")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            authenticationService.resetMotDePasse(request.getEmail());
            return ResponseEntity.ok(ApiResponse.success(
                    "Un nouveau mot de passe a été envoyé à votre adresse email"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la réinitialisation", e.getMessage()));
        }
    }

    /**
     * Rafraîchir le token
     */
    @PostMapping("/refresh-token")
    @Operation(summary = "Rafraîchir token", description = "Génère un nouveau token d'accès à partir d'un refresh token")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(@RequestParam String refreshToken) {
        try {
            LoginResponse loginResponse = authenticationService.refreshToken(refreshToken);
            return ResponseEntity.ok(ApiResponse.success("Token rafraîchi avec succès", loginResponse));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Token de rafraîchissement invalide", e.getMessage()));
        }
    }

    /**
     * Déconnexion
     */
    @PostMapping("/logout")
    @Operation(summary = "Déconnexion", description = "Déconnecte l'utilisateur")
    public ResponseEntity<ApiResponse<String>> logout(@RequestHeader("Authorization") String token) {
        try {
            authenticationService.logout(token);
            return ResponseEntity.ok(ApiResponse.success("Déconnexion réussie"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la déconnexion", e.getMessage()));
        }
    }

    /**
     * Obtenir le profil de l'utilisateur connecté
     */
    @GetMapping("/me")
    @Operation(summary = "Profil utilisateur", description = "Retourne les informations de l'utilisateur connecté")
    public ResponseEntity<ApiResponse<Utilisateur>> getCurrentUser(@RequestHeader("Authorization") String token) {
        try {
            Utilisateur utilisateur = authenticationService.getCurrentUser(token);
            return ResponseEntity.ok(ApiResponse.success("Profil récupéré avec succès", utilisateur));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Token invalide", e.getMessage()));
        }
    }

    /**
     * Test endpoint pour vérifier que le contrôleur fonctionne
     */
    @GetMapping("/test")
    @Operation(summary = "Test", description = "Endpoint de test")
    public ResponseEntity<ApiResponse<String>> test() {
        return ResponseEntity.ok(ApiResponse.success("✅ AuthController fonctionne correctement!"));
    }

    /**
     * Endpoint pour les administrateurs - Valider une association
     */
    @PostMapping("/admin/validate-user/{userId}")
    @Operation(summary = "Valider utilisateur", description = "Valide un compte utilisateur (admin seulement)")
    public ResponseEntity<ApiResponse<String>> validateUser(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String token) {
        try {
            // Vérifier que l'utilisateur connecté est un admin
            Utilisateur admin = authenticationService.getCurrentUser(token);
            if (!"ADMINISTRATEUR".equals(admin.getRole().name())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Accès refusé"));
            }

            authenticationService.validerUtilisateur(userId);
            return ResponseEntity.ok(ApiResponse.success("Utilisateur validé avec succès"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Erreur lors de la validation", e.getMessage()));
        }
    }

    /**
     * Endpoint pour les administrateurs - Rejeter une association
     */
    @PostMapping("/admin/reject-user/{userId}")
    @Operation(summary = "Rejeter utilisateur", description = "Rejette un compte utilisateur (admin seulement)")
    public ResponseEntity<ApiResponse<String>> rejectUser(
            @PathVariable Long userId,
            @RequestParam(required = false) String motif,
            @RequestHeader("Authorization") String token) {
        try {
            // Vérifier que l'utilisateur connecté est un admin
            Utilisateur admin = authenticationService.getCurrentUser(token);
            if (!"ADMINISTRATEUR".equals(admin.getRole().name())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Accès refusé"));
            }

            authenticationService.rejeterUtilisateur(userId, motif);
            return ResponseEntity.ok(ApiResponse.success("Utilisateur rejeté"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Erreur lors du rejet", e.getMessage()));
        }
    }
}