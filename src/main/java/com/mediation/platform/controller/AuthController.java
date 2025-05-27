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
@RequestMapping("/api/auth")
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
     * Connexion d'un utilisateur
     * POST /api/auth/login
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
     * POST /api/auth/register/donateur
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
     * POST /api/auth/register/association
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
     * Changement de mot de passe
     * PUT /api/auth/change-password/{userId}
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
                // Envoyer email de confirmation
                try {
                    Utilisateur user = utilisateurService.findById(userId);
                    emailService.envoyerEmailPersonnalise(
                            user.getEmail(),
                            "Mot de passe modifié",
                            "Bonjour " + user.getPrenom() + ",\n\n" +
                                    "Votre mot de passe a été modifié avec succès.\n" +
                                    "Si vous n'êtes pas à l'origine de cette modification, contactez-nous immédiatement.\n\n" +
                                    "Cordialement,\nL'équipe de la plateforme"
                    );
                } catch (Exception emailException) {
                    System.err.println("Erreur envoi email de confirmation: " + emailException.getMessage());
                }

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
     * POST /api/auth/reset-password
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

                // Envoyer email avec le nouveau mot de passe
                try {
                    emailService.envoyerEmailResetMotDePasse(
                            utilisateur.get().getEmail(),
                            nouveauMotDePasse
                    );
                } catch (Exception emailException) {
                    System.err.println("Erreur envoi email de reset: " + emailException.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ApiResponse.error("Erreur lors de l'envoi de l'email"));
                }

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
     * Vérifier si un email existe
     * GET /api/auth/check-email?email=test@example.com
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
     * Déconnexion
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout() {
        // Pour l'instant, la déconnexion est gérée côté client
        return ResponseEntity.ok(
                ApiResponse.success("Déconnexion réussie", "Logged out")
        );
    }

    /**
     * Profil utilisateur connecté
     * GET /api/auth/profile/{userId}
     */
    @GetMapping("/profile/{userId}")
    public ResponseEntity<ApiResponse<LoginResponse>> getProfile(@PathVariable Long userId) {

        try {
            Utilisateur utilisateur = utilisateurService.findById(userId);
            LoginResponse profile = authMapper.toLoginResponse(utilisateur);

            return ResponseEntity.ok(
                    ApiResponse.success("Profil récupéré avec succès", profile)
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Utilisateur non trouvé", e.getMessage()));
        }
    }

    /**
     * Valider un compte utilisateur (pour les administrateurs)
     * PUT /api/auth/validate-account/{userId}
     */
    @PutMapping("/validate-account/{userId}")
    public ResponseEntity<ApiResponse<String>> validateAccount(@PathVariable Long userId) {

        try {
            Utilisateur utilisateur = utilisateurService.findById(userId);
            utilisateur.setStatut(StatutUtilisateur.ACTIF);
            utilisateurService.update(userId, utilisateur);

            // Si c'est une association, envoyer email de validation
            if (utilisateur instanceof Association) {
                Association association = (Association) utilisateur;
                try {
                    emailService.envoyerEmailValidationAssociation(
                            association.getEmail(),
                            association.getNomAssociation()
                    );
                } catch (Exception emailException) {
                    System.err.println("Erreur envoi email de validation: " + emailException.getMessage());
                }
            }

            return ResponseEntity.ok(
                    ApiResponse.success("Compte validé avec succès", "Compte activé")
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Utilisateur non trouvé", e.getMessage()));
        }
    }

    /**
     * Refuser un compte utilisateur (pour les administrateurs)
     * PUT /api/auth/reject-account/{userId}?motif=...
     */
    @PutMapping("/reject-account/{userId}")
    public ResponseEntity<ApiResponse<String>> rejectAccount(
            @PathVariable Long userId,
            @RequestParam(required = false) String motif) {

        try {
            Utilisateur utilisateur = utilisateurService.findById(userId);
            utilisateur.setStatut(StatutUtilisateur.REFUSE);
            utilisateurService.update(userId, utilisateur);

            // Si c'est une association, envoyer email de refus
            if (utilisateur instanceof Association) {
                Association association = (Association) utilisateur;
                try {
                    emailService.envoyerEmailRefusAssociation(
                            association.getEmail(),
                            association.getNomAssociation(),
                            motif
                    );
                } catch (Exception emailException) {
                    System.err.println("Erreur envoi email de refus: " + emailException.getMessage());
                }
            }

            return ResponseEntity.ok(
                    ApiResponse.success("Compte refusé", "Compte rejeté")
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Utilisateur non trouvé", e.getMessage()));
        }
    }

    // ========== MÉTHODES UTILITAIRES ==========

    /**
     * Générer un mot de passe temporaire
     */
    private String genererMotDePasseTemporaire() {
        return "Temp" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Obtenir le message selon le statut de l'utilisateur
     */
    private String getMessageStatut(StatutUtilisateur statut) {
        switch (statut) {
            case EN_ATTENTE:
                return "Votre compte est en attente de validation. Vous recevrez un email une fois validé.";
            case REFUSE:
                return "Votre compte a été refusé. Contactez l'administration pour plus d'informations.";
            case SUSPENDU:
                return "Votre compte est temporairement suspendu. Contactez l'administration.";
            case INACTIF:
                return "Votre compte est inactif. Contactez l'administration pour le réactiver.";
            default:
                return "Votre compte n'est pas actif.";
        }
    }
}