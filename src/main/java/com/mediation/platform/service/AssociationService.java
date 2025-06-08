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

    // ========== MÉTHODES EXISTANTES ==========
    public Association findById(Long id) {
        return associationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Association non trouvée avec l'ID: " + id));
    }

    public List<Association> findAll() {
        return associationRepository.findAll();
    }

    public List<Association> findValidatedAssociations() {
        return associationRepository.findByStatutValidationTrue();
    }

    public List<Association> findPendingValidation() {
        return associationRepository.findByStatutValidationFalse();
    }

    public Association save(Association association) {
        return associationRepository.save(association);
    }

    public Association update(Long id, Association associationData) {
        Association association = findById(id);

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

    public void deleteById(Long id) {
        Association association = findById(id);
        associationRepository.delete(association);
    }

    public Association validerAssociation(Long id) {
        Association association = findById(id);

        association.setStatutValidation(true);
        association.setDateValidation(LocalDateTime.now());
        association.setStatut(com.mediation.platform.enums.StatutUtilisateur.ACTIF);

        Association savedAssociation = associationRepository.save(association);

        try {
            emailService.envoyerEmailValidation(savedAssociation);
            notificationService.creerNotificationValidation(savedAssociation);
        } catch (Exception e) {
            System.err.println("Erreur envoi notification validation: " + e.getMessage());
        }

        return savedAssociation;
    }

    public Association rejeterAssociation(Long id, String motif) {
        Association association = findById(id);

        association.setStatutValidation(false);
        association.setStatut(com.mediation.platform.enums.StatutUtilisateur.REFUSE);

        Association savedAssociation = associationRepository.save(association);

        try {
            emailService.envoyerEmailRefus(savedAssociation, motif);
            notificationService.creerNotificationRefus(savedAssociation, motif);
        } catch (Exception e) {
            System.err.println("Erreur envoi notification refus: " + e.getMessage());
        }

        return savedAssociation;
    }

    public List<Association> findByNomAssociation(String nom) {
        return associationRepository.findByNomAssociationContainingIgnoreCase(nom);
    }

    public List<Association> findByDomaineActivite(String domaine) {
        return associationRepository.findByDomaineActiviteContainingIgnoreCase(domaine);
    }

    public List<Association> findByVille(String ville) {
        return associationRepository.findByAdresseContainingIgnoreCase(ville);
    }

    public List<Association> findRecentlyValidated(int nombreJours) {
        LocalDateTime dateDebut = LocalDateTime.now().minusDays(nombreJours);
        return associationRepository.findRecentlyValidated(dateDebut);
    }

    public List<Association> findWithActiveProjects() {
        return associationRepository.findWithActiveProjects();
    }

    public List<Association> findTopAssociationsByDonations() {
        return associationRepository.findTopAssociationsByDonations();
    }

    // ========== NOUVELLES MÉTHODES AJOUTÉES ==========

    /**
     * Associations récemment validées avec LocalDateTime
     */
    public List<Association> findRecentlyValidated(LocalDateTime dateDebut) {
        return associationRepository.findRecentlyValidated(dateDebut);
    }

    /**
     * Associations en attente de validation (alias pour compatibilité)
     */
    public List<Association> findPendingAssociations() {
        return findPendingValidation();
    }

    /**
     * Top associations (alias pour compatibilité)
     */
    public List<Association> findTopAssociations() {
        return findTopAssociationsByDonations();
    }

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

    public boolean existsById(Long id) {
        return associationRepository.existsById(id);
    }

    public long count() {
        return associationRepository.count();
    }

    public Association suspendreAssociation(Long id, String motif) {
        Association association = findById(id);
        association.setStatut(com.mediation.platform.enums.StatutUtilisateur.SUSPENDU);

        Association savedAssociation = associationRepository.save(association);

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

    public Association reactiverAssociation(Long id) {
        Association association = findById(id);
        association.setStatut(com.mediation.platform.enums.StatutUtilisateur.ACTIF);

        Association savedAssociation = associationRepository.save(association);

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