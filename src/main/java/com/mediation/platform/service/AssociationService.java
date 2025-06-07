package com.mediation.platform.service;

import com.mediation.platform.entity.Association;
import com.mediation.platform.exception.ResourceNotFoundException;
import com.mediation.platform.repository.AssociationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class AssociationService {

    @Autowired
    private AssociationRepository associationRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailService emailService;

    /**
     * Trouver une association par ID
     */
    public Association findById(Long id) {
        return associationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Association non trouvée avec l'ID: " + id));
    }

    /**
     * Trouver toutes les associations
     */
    public List<Association> findAll() {
        return associationRepository.findAll();
    }

    /**
     * Associations validées uniquement
     */
    public List<Association> findValidatedAssociations() {
        return associationRepository.findByStatutValidationTrue();
    }

    /**
     * Associations en attente de validation
     */
    public List<Association> findPendingValidation() {
        return associationRepository.findByStatutValidationFalse();
    }

    /**
     * Sauvegarder une association
     */
    public Association save(Association association) {
        return associationRepository.save(association);
    }

    /**
     * Mettre à jour une association
     */
    public Association update(Long id, Association associationData) {
        Association association = findById(id);

        // Mettre à jour les champs modifiables
        if (associationData.getNom() != null) {
            association.setNom(associationData.getNom());
        }
        if (associationData.getPrenom() != null) {
            association.setPrenom(associationData.getPrenom());
        }
        if (associationData.getTelephone() != null) {
            association.setTelephone(associationData.getTelephone());
        }
        if (associationData.getNomAssociation() != null) {
            association.setNomAssociation(associationData.getNomAssociation());
        }
        if (associationData.getAdresse() != null) {
            association.setAdresse(associationData.getAdresse());
        }
        if (associationData.getSiteWeb() != null) {
            association.setSiteWeb(associationData.getSiteWeb());
        }
        if (associationData.getDescription() != null) {
            association.setDescription(associationData.getDescription());
        }
        if (associationData.getDomaineActivite() != null) {
            association.setDomaineActivite(associationData.getDomaineActivite());
        }

        return associationRepository.save(association);
    }

    /**
     * Supprimer une association
     */
    public void deleteById(Long id) {
        Association association = findById(id);
        associationRepository.delete(association);
    }

    /**
     * Valider une association
     */
    public Association validerAssociation(Long id) {
        Association association = findById(id);

        association.setStatutValidation(true);
        association.setDateValidation(LocalDateTime.now());
        association.setStatut(com.mediation.platform.enums.StatutUtilisateur.ACTIF);

        Association savedAssociation = associationRepository.save(association);

        // Envoyer notifications
        try {
            emailService.envoyerEmailValidation(savedAssociation);
            notificationService.creerNotificationValidation(savedAssociation);
        } catch (Exception e) {
            System.err.println("Erreur envoi notification validation: " + e.getMessage());
        }

        return savedAssociation;
    }

    /**
     * Rejeter une association
     */
    public Association rejeterAssociation(Long id, String motif) {
        Association association = findById(id);

        association.setStatutValidation(false);
        association.setStatut(com.mediation.platform.enums.StatutUtilisateur.REFUSE);

        Association savedAssociation = associationRepository.save(association);

        // Envoyer notifications
        try {
            emailService.envoyerEmailRefus(savedAssociation, motif);
            notificationService.creerNotificationRefus(savedAssociation, motif);
        } catch (Exception e) {
            System.err.println("Erreur envoi notification refus: " + e.getMessage());
        }

        return savedAssociation;
    }

    /**
     * Rechercher associations par nom
     */
    public List<Association> findByNomAssociation(String nom) {
        return associationRepository.findByNomAssociationContainingIgnoreCase(nom);
    }

    /**
     * Rechercher associations par domaine d'activité
     */
    public List<Association> findByDomaineActivite(String domaine) {
        return associationRepository.findByDomaineActiviteContainingIgnoreCase(domaine);
    }

    /**
     * Rechercher associations par ville
     */
    public List<Association> findByVille(String ville) {
        return associationRepository.findByAdresseContainingIgnoreCase(ville);
    }

    /**
     * Associations récemment validées
     */
    public List<Association> findRecentlyValidated(int nombreJours) {
        LocalDateTime dateDebut = LocalDateTime.now().minusDays(nombreJours);
        return associationRepository.findRecentlyValidated(dateDebut);
    }

    /**
     * Associations avec des projets actifs
     */
    public List<Association> findWithActiveProjects() {
        return associationRepository.findWithActiveProjects();
    }

    /**
     * Top associations par montant collecté
     */
    public List<Association> findTopAssociationsByDonations() {
        return associationRepository.findTopAssociationsByDonations();
    }

    /**
     * Statistiques générales des associations
     */
    public AssociationStats getGeneralStats() {
        List<Association> allAssociations = findAll();
        List<Association> validatedAssociations = findValidatedAssociations();
        List<Association> pendingAssociations = findPendingValidation();

        AssociationStats stats = new AssociationStats();
        stats.setTotalAssociations(allAssociations.size());
        stats.setAssociationsValidees(validatedAssociations.size());
        stats.setAssociationsEnAttente(pendingAssociations.size());
        stats.setAssociationsAvecProjetsActifs(findWithActiveProjects().size());

        double montantTotalCollecte = validatedAssociations.stream()
                .mapToDouble(Association::getMontantTotalCollecte)
                .sum();
        stats.setMontantTotalCollecte(montantTotalCollecte);

        int totalProjets = validatedAssociations.stream()
                .mapToInt(Association::getNombreProjets)
                .sum();
        stats.setTotalProjets(totalProjets);

        return stats;
    }

    /**
     * Vérifier si une association existe
     */
    public boolean existsById(Long id) {
        return associationRepository.existsById(id);
    }

    /**
     * Compter le nombre total d'associations
     */
    public long count() {
        return associationRepository.count();
    }

    /**
     * Suspendre une association
     */
    public Association suspendreAssociation(Long id, String motif) {
        Association association = findById(id);
        association.setStatut(com.mediation.platform.enums.StatutUtilisateur.SUSPENDU);

        Association savedAssociation = associationRepository.save(association);

        // Envoyer notification
        try {
            notificationService.creerNotification(
                    "Compte suspendu",
                    "Votre compte a été suspendu. Motif: " + (motif != null ? motif : "Non spécifié"),
                    com.mediation.platform.enums.TypeNotification.SECURITE,
                    savedAssociation,
                    true
            );
        } catch (Exception e) {
            System.err.println("Erreur envoi notification suspension: " + e.getMessage());
        }

        return savedAssociation;
    }

    /**
     * Réactiver une association
     */
    public Association reactiverAssociation(Long id) {
        Association association = findById(id);
        association.setStatut(com.mediation.platform.enums.StatutUtilisateur.ACTIF);

        Association savedAssociation = associationRepository.save(association);

        // Envoyer notification
        try {
            notificationService.creerNotification(
                    "Compte réactivé",
                    "Votre compte a été réactivé avec succès.",
                    com.mediation.platform.enums.TypeNotification.MISE_A_JOUR_PROFIL,
                    savedAssociation,
                    false
            );
        } catch (Exception e) {
            System.err.println("Erreur envoi notification réactivation: " + e.getMessage());
        }

        return savedAssociation;
    }

    /**
     * Classe interne pour les statistiques
     */
    public static class AssociationStats {
        private int totalAssociations;
        private int associationsValidees;
        private int associationsEnAttente;
        private int associationsAvecProjetsActifs;
        private double montantTotalCollecte;
        private int totalProjets;

        // Getters et setters
        public int getTotalAssociations() { return totalAssociations; }
        public void setTotalAssociations(int totalAssociations) { this.totalAssociations = totalAssociations; }

        public int getAssociationsValidees() { return associationsValidees; }
        public void setAssociationsValidees(int associationsValidees) { this.associationsValidees = associationsValidees; }

        public int getAssociationsEnAttente() { return associationsEnAttente; }
        public void setAssociationsEnAttente(int associationsEnAttente) { this.associationsEnAttente = associationsEnAttente; }

        public int getAssociationsAvecProjetsActifs() { return associationsAvecProjetsActifs; }
        public void setAssociationsAvecProjetsActifs(int associationsAvecProjetsActifs) { this.associationsAvecProjetsActifs = associationsAvecProjetsActifs; }

        public double getMontantTotalCollecte() { return montantTotalCollecte; }
        public void setMontantTotalCollecte(double montantTotalCollecte) { this.montantTotalCollecte = montantTotalCollecte; }

        public int getTotalProjets() { return totalProjets; }
        public void setTotalProjets(int totalProjets) { this.totalProjets = totalProjets; }
    }
}