package com.mediation.platform.service;

import com.mediation.platform.dto.mapper.AuthMapper;
import com.mediation.platform.dto.request.LoginRequest;
import com.mediation.platform.dto.request.RegisterAssociationRequest;
import com.mediation.platform.dto.request.RegisterDonateurRequest;
import com.mediation.platform.dto.response.LoginResponse;
import com.mediation.platform.entity.Association;
import com.mediation.platform.entity.Donateur;
import com.mediation.platform.entity.Utilisateur;
import com.mediation.platform.enums.StatutUtilisateur;
import com.mediation.platform.exception.AuthenticationException;
import com.mediation.platform.exception.BusinessException;
import com.mediation.platform.exception.ResourceNotFoundException;
import com.mediation.platform.repository.UtilisateurRepository;
import com.mediation.platform.security.JwtUtil;
import com.mediation.platform.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AuthenticationService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthMapper authMapper;

    @Autowired
    private EmailService emailService;

    @Autowired
    private NotificationService notificationService;

    /**
     * Authentification d'un utilisateur
     */
    public LoginResponse login(LoginRequest loginRequest) {
        try {
            // Vérifier que l'utilisateur existe
            Utilisateur utilisateur = utilisateurRepository.findByEmail(loginRequest.getEmail())
                    .orElseThrow(() -> new AuthenticationException("Email ou mot de passe incorrect"));

            // Vérifier le statut du compte
            if (utilisateur.getStatut() == StatutUtilisateur.SUSPENDU) {
                throw new AuthenticationException("Votre compte a été suspendu");
            }
            if (utilisateur.getStatut() == StatutUtilisateur.REFUSE) {
                throw new AuthenticationException("Votre compte a été refusé");
            }
            if (utilisateur.getStatut() == StatutUtilisateur.EN_ATTENTE) {
                throw new AuthenticationException("Votre compte est en attente de validation");
            }

            // Authentifier avec Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getMotDePasse()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            // Générer les tokens JWT
            String accessToken = jwtUtil.generateToken(
                    userDetails.getUsername(),
                    userDetails.getRole(),
                    userDetails.getId()
            );
            String refreshToken = jwtUtil.generateRefreshToken(userDetails.getUsername());

            // Mettre à jour la dernière connexion
            utilisateur.marquerConnexion();
            utilisateurRepository.save(utilisateur);

            // Créer la réponse
            LoginResponse loginResponse = authMapper.toLoginResponse(utilisateur);
            loginResponse.setToken(accessToken);
            loginResponse.setDerniereConnexion(utilisateur.getDerniereConnexion());

            return loginResponse;

        } catch (Exception e) {
            throw new AuthenticationException("Échec de l'authentification: " + e.getMessage());
        }
    }

    /**
     * Inscription d'un donateur
     */
    public Utilisateur registerDonateur(RegisterDonateurRequest request) {
        // Vérifier si l'email existe déjà
        if (emailExists(request.getEmail())) {
            throw new BusinessException("Cet email est déjà utilisé");
        }

        // Créer le donateur
        Donateur donateur = authMapper.toDonateurEntity(request);
        donateur.setMotDePasse(passwordEncoder.encode(request.getMotDePasse()));
        donateur.setStatut(StatutUtilisateur.ACTIF); // Les donateurs sont activés directement

        // Sauvegarder
        Donateur savedDonateur = utilisateurRepository.save(donateur);

        // Envoyer email de bienvenue
        try {
            emailService.envoyerEmailBienvenue(savedDonateur);
        } catch (Exception e) {
            // Log l'erreur mais ne pas faire échouer l'inscription
            System.err.println("Erreur envoi email: " + e.getMessage());
        }

        return savedDonateur;
    }

    /**
     * Inscription d'une association
     */
    public Utilisateur registerAssociation(RegisterAssociationRequest request) {
        // Vérifier si l'email existe déjà
        if (emailExists(request.getEmail())) {
            throw new BusinessException("Cet email est déjà utilisé");
        }

        // Créer l'association
        Association association = authMapper.toAssociationEntity(request);
        association.setMotDePasse(passwordEncoder.encode(request.getMotDePasse()));
        association.setStatut(StatutUtilisateur.EN_ATTENTE); // En attente de validation
        association.setStatutValidation(false);

        // Sauvegarder
        Association savedAssociation = utilisateurRepository.save(association);

        // Envoyer email de confirmation
        try {
            emailService.envoyerEmailConfirmationInscription(savedAssociation);
        } catch (Exception e) {
            System.err.println("Erreur envoi email: " + e.getMessage());
        }

        // Notifier les administrateurs
        try {
            notificationService.notifierNouvelleAssociation(savedAssociation);
        } catch (Exception e) {
            System.err.println("Erreur notification admin: " + e.getMessage());
        }

        return savedAssociation;
    }

    /**
     * Rafraîchir le token
     */
    public LoginResponse refreshToken(String refreshToken) {
        try {
            if (!jwtUtil.validateToken(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
                throw new AuthenticationException("Token de rafraîchissement invalide");
            }

            String username = jwtUtil.extractUsername(refreshToken);
            Utilisateur utilisateur = utilisateurRepository.findByEmail(username)
                    .orElseThrow(() -> new AuthenticationException("Utilisateur non trouvé"));

            // Générer un nouveau token d'accès
            String newAccessToken = jwtUtil.generateToken(
                    utilisateur.getEmail(),
                    utilisateur.getRole().name(),
                    utilisateur.getIdUtilisateur()
            );

            LoginResponse loginResponse = authMapper.toLoginResponse(utilisateur);
            loginResponse.setToken(newAccessToken);

            return loginResponse;

        } catch (Exception e) {
            throw new AuthenticationException("Erreur lors du rafraîchissement du token");
        }
    }

    /**
     * Déconnexion
     */
    public void logout(String token) {
        // Dans une implémentation complète, on pourrait ajouter le token à une blacklist
        // Pour l'instant, on se contente de vider le contexte de sécurité
        SecurityContextHolder.clearContext();
    }

    /**
     * Obtenir l'utilisateur connecté
     */
    public Utilisateur getCurrentUser(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AuthenticationException("Token d'authentification manquant");
        }

        String token = authHeader.substring(7);
        String username = jwtUtil.extractUsername(token);

        return utilisateurRepository.findByEmail(username)
                .orElseThrow(() -> new AuthenticationException("Utilisateur non trouvé"));
    }

    /**
     * Vérifier si un email existe
     */
    public boolean emailExists(String email) {
        return utilisateurRepository.existsByEmail(email);
    }

    /**
     * Changer le mot de passe
     */
    public boolean changerMotDePasse(Long userId, String ancienMotDePasse, String nouveauMotDePasse) {
        Utilisateur utilisateur = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        // Vérifier l'ancien mot de passe
        if (!passwordEncoder.matches(ancienMotDePasse, utilisateur.getMotDePasse())) {
            return false;
        }

        // Changer le mot de passe
        utilisateur.setMotDePasse(passwordEncoder.encode(nouveauMotDePasse));
        utilisateurRepository.save(utilisateur);

        return true;
    }

    /**
     * Réinitialiser le mot de passe
     */
    public void resetMotDePasse(String email) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun compte associé à cet email"));

        // Générer un nouveau mot de passe temporaire
        String nouveauMotDePasse = genererMotDePasseTemporaire();
        utilisateur.setMotDePasse(passwordEncoder.encode(nouveauMotDePasse));
        utilisateurRepository.save(utilisateur);

        // Envoyer par email
        try {
            emailService.envoyerNouveauMotDePasse(utilisateur, nouveauMotDePasse);
        } catch (Exception e) {
            throw new BusinessException("Erreur lors de l'envoi de l'email");
        }
    }

    /**
     * Valider un utilisateur (admin)
     */
    public void validerUtilisateur(Long userId) {
        Utilisateur utilisateur = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        utilisateur.setStatut(StatutUtilisateur.ACTIF);

        if (utilisateur instanceof Association) {
            Association association = (Association) utilisateur;
            association.setStatutValidation(true);
            association.setDateValidation(LocalDateTime.now());
        }

        utilisateurRepository.save(utilisateur);

        // Envoyer notification
        try {
            emailService.envoyerEmailValidation(utilisateur);
            notificationService.creerNotificationValidation(utilisateur);
        } catch (Exception e) {
            System.err.println("Erreur notification validation: " + e.getMessage());
        }
    }

    /**
     * Rejeter un utilisateur (admin)
     */
    public void rejeterUtilisateur(Long userId, String motif) {
        Utilisateur utilisateur = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        utilisateur.setStatut(StatutUtilisateur.REFUSE);
        utilisateurRepository.save(utilisateur);

        // Envoyer notification
        try {
            emailService.envoyerEmailRefus(utilisateur, motif);
            notificationService.creerNotificationRefus(utilisateur, motif);
        } catch (Exception e) {
            System.err.println("Erreur notification refus: " + e.getMessage());
        }
    }

    /**
     * Générer un mot de passe temporaire
     */
    private String genererMotDePasseTemporaire() {
        return UUID.randomUUID().toString().substring(0, 12);
    }
}