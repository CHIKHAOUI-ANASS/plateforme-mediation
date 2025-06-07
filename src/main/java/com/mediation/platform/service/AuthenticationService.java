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
import com.mediation.platform.repository.UtilisateurRepository;
import com.mediation.platform.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class AuthenticationService {

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthMapper authMapper;

    @Autowired
    private EmailService emailService;

    /**
     * Connexion utilisateur
     */
    public LoginResponse login(LoginRequest loginRequest) {
        try {
            // Vérifier si l'utilisateur existe
            Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findByEmail(loginRequest.getEmail());
            if (utilisateurOpt.isEmpty()) {
                throw new AuthenticationException("Email ou mot de passe incorrect");
            }

            Utilisateur utilisateur = utilisateurOpt.get();

            // Vérifier le statut de l'utilisateur
            if (!utilisateur.estActif()) {
                throw new AuthenticationException("Compte non activé ou suspendu");
            }

            // Authentifier l'utilisateur
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getMotDePasse()
                    )
            );

            // Générer les tokens JWT
            String accessToken = jwtUtil.generateToken(
                    utilisateur.getEmail(),
                    utilisateur.getRole().name(),
                    utilisateur.getIdUtilisateur()
            );

            String refreshToken = jwtUtil.generateRefreshToken(utilisateur.getEmail());

            // Mettre à jour la dernière connexion
            utilisateur.setDerniereConnexion(LocalDateTime.now());
            utilisateurRepository.save(utilisateur);

            // Créer la réponse
            LoginResponse response = authMapper.toLoginResponse(utilisateur);
            response.setToken(accessToken);
            // Note: Le refreshToken pourrait être ajouté plus tard

            return response;

        } catch (Exception e) {
            throw new AuthenticationException("Email ou mot de passe incorrect");
        }
    }

    /**
     * Inscription donateur
     */
    public Utilisateur registerDonateur(RegisterDonateurRequest request) {
        // Vérifier si l'email existe déjà
        if (utilisateurRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Un compte avec cet email existe déjà");
        }

        // Créer le donateur
        Donateur donateur = authMapper.toDonateurEntity(request);

        // Encoder le mot de passe
        donateur.setMotDePasse(passwordEncoder.encode(request.getMotDePasse()));

        // Définir le statut initial
        donateur.setStatut(StatutUtilisateur.ACTIF); // Les donateurs sont activés directement

        // Sauvegarder
        Donateur savedDonateur = utilisateurRepository.save(donateur);

        // Envoyer email de bienvenue
        try {
            emailService.envoyerEmailBienvenue(
                    savedDonateur.getEmail(),
                    savedDonateur.getNomComplet()
            );
        } catch (Exception e) {
            // Ne pas faire échouer l'inscription si l'email échoue
            System.err.println("Erreur envoi email de bienvenue: " + e.getMessage());
        }

        return savedDonateur;
    }

    /**
     * Inscription association
     */
    public Utilisateur registerAssociation(RegisterAssociationRequest request) {
        // Vérifier si l'email existe déjà
        if (utilisateurRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Un compte avec cet email existe déjà");
        }

        // Créer l'association
        Association association = authMapper.toAssociationEntity(request);

        // Encoder le mot de passe
        association.setMotDePasse(passwordEncoder.encode(request.getMotDePasse()));

        // Définir le statut initial (en attente de validation)
        association.setStatut(StatutUtilisateur.EN_ATTENTE);
        association.setStatutValidation(false);

        // Sauvegarder
        Association savedAssociation = utilisateurRepository.save(association);

        // Envoyer email de confirmation
        try {
            emailService.envoyerEmailBienvenue(
                    savedAssociation.getEmail(),
                    savedAssociation.getNomComplet()
            );
        } catch (Exception e) {
            System.err.println("Erreur envoi email de bienvenue: " + e.getMessage());
        }

        return savedAssociation;
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
    public boolean changerMotDePasse(Long utilisateurId, String ancienMotDePasse, String nouveauMotDePasse) {
        Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findById(utilisateurId);

        if (utilisateurOpt.isEmpty()) {
            return false;
        }

        Utilisateur utilisateur = utilisateurOpt.get();

        // Vérifier l'ancien mot de passe
        if (!passwordEncoder.matches(ancienMotDePasse, utilisateur.getMotDePasse())) {
            return false;
        }

        // Changer le mot de passe
        utilisateur.setMotDePasse(passwordEncoder.encode(nouveauMotDePasse));
        utilisateur.setDateModification(LocalDateTime.now());
        utilisateurRepository.save(utilisateur);

        return true;
    }

    /**
     * Réinitialiser le mot de passe
     */
    public void resetMotDePasse(String email) {
        Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findByEmail(email);

        if (utilisateurOpt.isEmpty()) {
            // Ne pas révéler si l'email existe ou non pour des raisons de sécurité
            return;
        }

        Utilisateur utilisateur = utilisateurOpt.get();

        // Générer un nouveau mot de passe temporaire
        String nouveauMotDePasse = genererMotDePasseTemporaire();

        // Mettre à jour le mot de passe
        utilisateur.setMotDePasse(passwordEncoder.encode(nouveauMotDePasse));
        utilisateur.setDateModification(LocalDateTime.now());
        utilisateurRepository.save(utilisateur);

        // Envoyer le nouveau mot de passe par email
        try {
            emailService.envoyerEmailResetMotDePasse(
                    utilisateur.getEmail(),
                    nouveauMotDePasse
            );
        } catch (Exception e) {
            throw new BusinessException("Erreur lors de l'envoi de l'email de réinitialisation");
        }
    }

    /**
     * Rafraîchir le token JWT
     */
    public LoginResponse refreshToken(String refreshToken) {
        try {
            if (!jwtUtil.validateToken(refreshToken)) {
                throw new AuthenticationException("Token de rafraîchissement invalide");
            }

            String email = jwtUtil.extractUsername(refreshToken);
            Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findByEmail(email);

            if (utilisateurOpt.isEmpty()) {
                throw new AuthenticationException("Utilisateur non trouvé");
            }

            Utilisateur utilisateur = utilisateurOpt.get();

            if (!utilisateur.estActif()) {
                throw new AuthenticationException("Compte non actif");
            }

            // Générer un nouveau token d'accès
            String newAccessToken = jwtUtil.generateToken(
                    utilisateur.getEmail(),
                    utilisateur.getRole().name(),
                    utilisateur.getIdUtilisateur()
            );

            // Créer la réponse
            LoginResponse response = authMapper.toLoginResponse(utilisateur);
            response.setToken(newAccessToken);

            return response;

        } catch (Exception e) {
            throw new AuthenticationException("Erreur lors du rafraîchissement du token");
        }
    }

    /**
     * Déconnexion (invalider le token côté client)
     */
    public void logout(String token) {
        // Pour l'instant, la déconnexion se fait côté client
        // Dans une implémentation plus avancée, on pourrait maintenir une blacklist des tokens
        // ou utiliser Redis pour stocker les tokens valides
    }

    /**
     * Obtenir l'utilisateur connecté à partir du token
     */
    public Utilisateur getCurrentUser(String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (!jwtUtil.validateToken(token)) {
            throw new AuthenticationException("Token invalide");
        }

        String email = jwtUtil.extractUsername(token);
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationException("Utilisateur non trouvé"));
    }

    /**
     * Générer un mot de passe temporaire
     */
    private String genererMotDePasseTemporaire() {
        String caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder motDePasse = new StringBuilder();

        for (int i = 0; i < 8; i++) {
            int index = (int) (Math.random() * caracteres.length());
            motDePasse.append(caracteres.charAt(index));
        }

        return motDePasse.toString();
    }

    /**
     * Valider un utilisateur (pour les associations)
     */
    public void validerUtilisateur(Long utilisateurId) {
        Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findById(utilisateurId);

        if (utilisateurOpt.isEmpty()) {
            throw new BusinessException("Utilisateur non trouvé");
        }

        Utilisateur utilisateur = utilisateurOpt.get();
        utilisateur.setStatut(StatutUtilisateur.ACTIF);

        if (utilisateur instanceof Association) {
            Association association = (Association) utilisateur;
            association.setStatutValidation(true);
            association.setDateValidation(LocalDateTime.now());

            // Envoyer email de validation
            try {
                emailService.envoyerEmailValidationAssociation(
                        association.getEmail(),
                        association.getNomAssociation()
                );
            } catch (Exception e) {
                System.err.println("Erreur envoi email de validation: " + e.getMessage());
            }
        }

        utilisateurRepository.save(utilisateur);
    }

    /**
     * Rejeter un utilisateur (pour les associations)
     */
    public void rejeterUtilisateur(Long utilisateurId, String motif) {
        Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findById(utilisateurId);

        if (utilisateurOpt.isEmpty()) {
            throw new BusinessException("Utilisateur non trouvé");
        }

        Utilisateur utilisateur = utilisateurOpt.get();
        utilisateur.setStatut(StatutUtilisateur.REFUSE);

        if (utilisateur instanceof Association) {
            Association association = (Association) utilisateur;
            association.setStatutValidation(false);

            // Envoyer email de refus
            try {
                emailService.envoyerEmailRefusAssociation(
                        association.getEmail(),
                        association.getNomAssociation(),
                        motif
                );
            } catch (Exception e) {
                System.err.println("Erreur envoi email de refus: " + e.getMessage());
            }
        }

        utilisateurRepository.save(utilisateur);
    }
}