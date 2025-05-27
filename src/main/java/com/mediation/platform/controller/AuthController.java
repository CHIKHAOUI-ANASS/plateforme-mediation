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
import com.mediation.platform.service.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/auth")  // CHANGÉ: Supprimé /api
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    @Autowired
    private AuthenticationService authenticationService;

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
        return ResponseEntity.ok("AuthController fonctionne !");
    }

    /**
     * Connexion d'un utilisateur
     * POST /auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest) {

        try {
            Optional<Utilisateur> utilisateur = authenticationService.authenticate(
                    loginRequest.getEmail(),
                    loginRequest.getMotDePasse()
            );

            if (utilisateur.isPresent()) {
                Utilisateur user = utilisateur.get();

                // Vérifier le statut de l'utilisateur
                if (user.getStatut() != StatutUtilisateur.ACTIF) {
                    String message = getMessageStatut(user.getStatut());
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(ApiResponse.error(message));
                }

                LoginResponse loginResponse = authMapper.toLoginResponse(user);

                return ResponseEntity.ok(
                        ApiResponse.success("Connexion réussie", loginResponse)
                );
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Email ou mot de passe incorrect"));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la connexion", e.getMessage()));
        }
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

            // Sauvegarder le donateur
            Donateur savedDonateur = donateurService.save(donateur);

            // Envoyer email de bienvenue
            try {
                emailService.envoyerEmailBienvenue(
                        savedDonateur.getEmail(),
                        savedDonateur.getPrenom() + " " + savedDonateur.getNom()
                );
            } catch (Exception emailException) {
                System.err.println("Erreur envoi email de bienvenue: " + emailException.getMessage());
            }

            // Retourner la réponse de connexion
            LoginResponse loginResponse = authMapper.toLoginResponse(savedDonateur);

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

            // Sauvegarder l'association
            Association savedAssociation = associationService.save(association);

            // Envoyer email de confirmation
            try {
                emailService.envoyerEmailPersonnalise(
                        savedAssociation.getEmail(),
                        "Demande d'association reçue",
                        "Bonjour " + savedAssociation.getPrenom() + " " + savedAssociation.getNom() + ",\n\n" +
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
     * PUT /auth/change-password/{userId}
     */
    @PutMapping("/change-password/{userId}")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @PathVariable Long userId,
            @Valid @RequestBody ChangePasswordRequest changePasswordRequest) {

        try {
            // Vérifier que les mots de passe correspondent
            if (!changePasswordRequest.isPasswordMatch()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Les mots de passe ne correspondent pas"));
            }

            // Changer le mot de passe
            boolean success = authenticationService.changerMotDePasse(
                    userId,
                    changePasswordRequest.getAncienMotDePasse(),
                    changePasswordRequest.getNouveauMotDePasse()
            );

            if (success) {
                return ResponseEntity.ok(
                        ApiResponse.success("Mot de passe modifié avec succès", "Changement effectué")
                );
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Ancien mot de passe incorrect"));
            }

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
                authenticationService.resetMotDePasse(
                        resetPasswordRequest.getEmail(),
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