package com.mediation.platform.service;

import com.mediation.platform.entity.Association;
import com.mediation.platform.entity.Projet;
import com.mediation.platform.enums.StatutProjet;
import com.mediation.platform.exception.BusinessException;
import com.mediation.platform.exception.ResourceNotFoundException;
import com.mediation.platform.repository.ProjetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ProjetService {

    @Autowired
    private ProjetRepository projetRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailService emailService;

    /**
     * Trouver un projet par ID
     */
    public Projet findById(Long id) {
        return projetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé avec l'ID: " + id));
    }

    /**
     * Trouver tous les projets
     */
    public List<Projet> findAll() {
        return projetRepository.findAll();
    }

    /**
     * Projets actifs uniquement
     */
    public List<Projet> findActiveProjects() {
        return projetRepository.findByStatutOrderByDateCreationDesc(StatutProjet.EN_COURS);
    }

    /**
     * Sauvegarder un projet
     */
    public Projet save(Projet projet) {
        // Valider que l'association est validée
        if (projet.getAssociation() != null && !projet.getAssociation().estValidee()) {
            throw new BusinessException("Seules les associations validées peuvent créer des projets");
        }

        // Valider les données du projet
        if (projet.getMontantDemande() <= 0) {
            throw new BusinessException("Le montant demandé doit être positif");
        }

        // Initialiser les valeurs par défaut
        if (projet.getMontantCollecte() == null) {
            projet.setMontantCollecte(0.0);
        }
        if (projet.getStatut() == null) {
            projet.setStatut(StatutProjet.EN_COURS);
        }
        if (projet.getDateDebut() == null) {
            projet.setDateDebut(LocalDate.now());
        }

        Projet savedProjet = projetRepository.save(projet);

        // Notifier les administrateurs du nouveau projet
        try {
            notificationService.notifierNouvelleAssociation(projet.getAssociation());
        } catch (Exception e) {
            System.err.println("Erreur notification nouveau projet: " + e.getMessage());
        }

        return savedProjet;
    }

    /**
     * Mettre à jour un projet
     */
    public Projet update(Long id, Projet projetData) {
        Projet projet = findById(id);

        // Vérifier que le projet peut être modifié
        if (projet.getStatut() == StatutProjet.TERMINE || projet.getStatut() == StatutProjet.ANNULE) {
            throw new BusinessException("Ce projet ne peut plus être modifié");
        }

        // Mettre à jour les champs modifiables
        if (projetData.getTitre() != null) {
            projet.setTitre(projetData.getTitre());
        }
        if (projetData.getDescription() != null) {
            projet.setDescription(projetData.getDescription());
        }
        if (projetData.getObjectif() != null) {
            projet.setObjectif(projetData.getObjectif());
        }
        if (projetData.getDateFin() != null) {
            projet.setDateFin(projetData.getDateFin());
        }
        if (projetData.getPriorite() != null) {
            projet.setPriorite(projetData.getPriorite());
        }
        if (projetData.getImages() != null) {
            projet.setImages(projetData.getImages());
        }

        return projetRepository.save(projet);
    }

    /**
     * Supprimer un projet (le marquer comme annulé)
     */
    public void deleteById(Long id) {
        Projet projet = findById(id);
        projet.setStatut(StatutProjet.ANNULE);
        projetRepository.save(projet);
    }

    /**
     * Projets par statut
     */
    public List<Projet> findByStatut(StatutProjet statut) {
        return projetRepository.findByStatut(statut);
    }

    /**
     * Projets d'une association
     */
    public List<Projet> findByAssociation(Association association) {
        return projetRepository.findByAssociation(association);
    }

    /**
     * Rechercher projets par mot-clé
     */
    public List<Projet> searchByKeyword(String keyword) {
        return projetRepository.searchByKeyword(keyword);
    }

    /**
     * Projets proches de l'objectif
     */
    public List<Projet> findNearGoal(double pourcentage) {
        return projetRepository.findNearGoal(pourcentage);
    }

    /**
     * Projets en retard
     */
    public List<Projet> findOverdueProjects() {
        return projetRepository.findOverdueProjects(LocalDate.now());
    }

    /**
     * Projets récents
     */
    public List<Projet> findRecentProjects(LocalDateTime dateDebut) {
        return projetRepository.findRecentProjects(dateDebut);
    }

    /**
     * Top projets par montant collecté
     */
    public List<Projet> findTopProjects() {
        return projetRepository.findTopProjectsByAmount();
    }

    /**
     * Marquer un projet comme terminé
     */
    public Projet marquerCommeTermine(Long id) {
        Projet projet = findById(id);

        projet.setStatut(StatutProjet.TERMINE);
        Projet savedProjet = projetRepository.save(projet);

        // Envoyer notifications
        try {
            emailService.envoyerEmailProjetTermine(
                    projet.getAssociation(),
                    projet.getTitre(),
                    projet.getMontantCollecte()
            );

            notificationService.creerNotification(
                    "Projet terminé",
                    "Félicitations ! Votre projet \"" + projet.getTitre() + "\" a été marqué comme terminé.",
                    com.mediation.platform.enums.TypeNotification.PROJET_COMPLETE,
                    projet.getAssociation(),
                    false
            );
        } catch (Exception e) {
            System.err.println("Erreur notification projet terminé: " + e.getMessage());
        }

        return savedProjet;
    }

    /**
     * Suspendre un projet
     */
    public Projet suspendreProjet(Long id, String motif) {
        Projet projet = findById(id);
        projet.setStatut(StatutProjet.SUSPENDU);

        Projet savedProjet = projetRepository.save(projet);

        // Notifier l'association
        try {
            notificationService.creerNotification(
                    "Projet suspendu",
                    "Votre projet \"" + projet.getTitre() + "\" a été suspendu. Motif: " +
                            (motif != null ? motif : "Non spécifié"),
                    com.mediation.platform.enums.TypeNotification.SECURITE,
                    projet.getAssociation(),
                    true
            );
        } catch (Exception e) {
            System.err.println("Erreur notification suspension projet: " + e.getMessage());
        }

        return savedProjet;
    }

    /**
     * Réactiver un projet suspendu
     */
    public Projet reactiverProjet(Long id) {
        Projet projet = findById(id);

        if (projet.getStatut() != StatutProjet.SUSPENDU) {
            throw new BusinessException("Seuls les projets suspendus peuvent être réactivés");
        }

        projet.setStatut(StatutProjet.EN_COURS);
        Projet savedProjet = projetRepository.save(projet);

        // Notifier l'association
        try {
            notificationService.creerNotification(
                    "Projet réactivé",
                    "Votre projet \"" + projet.getTitre() + "\" a été réactivé avec succès.",
                    com.mediation.platform.enums.TypeNotification.MISE_A_JOUR_PROFIL,
                    projet.getAssociation(),
                    false
            );
        } catch (Exception e) {
            System.err.println("Erreur notification réactivation projet: " + e.getMessage());
        }

        return savedProjet;
    }

    /**
     * Vérifier et mettre à jour les projets expirés
     */
    @Transactional
    public void verifierProjetsExpires() {
        List<Projet> projetsEnRetard = findOverdueProjects();

        for (Projet projet : projetsEnRetard) {
            if (projet.getStatut() == StatutProjet.EN_COURS) {
                projet.setStatut(StatutProjet.ANNULE);
                projetRepository.save(projet);

                // Notifier l'association
                try {
                    notificationService.creerNotification(
                            "Projet expiré",
                            "Votre projet \"" + projet.getTitre() + "\" a expiré.",
                            com.mediation.platform.enums.TypeNotification.PROJET_EXPIRE,
                            projet.getAssociation(),
                            true
                    );
                } catch (Exception e) {
                    System.err.println("Erreur notification projet expiré: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Envoyer rappels d'échéance
     */
    @Transactional
    public void envoyerRappelsEcheance() {
        LocalDate dateLimit = LocalDate.now().plusDays(7); // Projets qui expirent dans 7 jours

        List<Projet> projetsProchesExpiration = projetRepository.findAll().stream()
                .filter(p -> p.getStatut() == StatutProjet.EN_COURS)
                .filter(p -> p.getDateFin() != null)
                .filter(p -> p.getDateFin().isBefore(dateLimit) || p.getDateFin().isEqual(dateLimit))
                .toList();

        for (Projet projet : projetsProchesExpiration) {
            Long joursRestants = projet.getJoursRestants();
            double progres = projet.calculerProgres();

            try {
                emailService.envoyerRappelEcheance(
                        projet.getAssociation(),
                        projet.getTitre(),
                        joursRestants != null ? joursRestants.intValue() : 0,
                        progres
                );

                notificationService.creerNotification(
                        "Rappel d'échéance",
                        "Votre projet \"" + projet.getTitre() + "\" expire dans " + joursRestants + " jour(s).",
                        com.mediation.platform.enums.TypeNotification.RAPPEL_ECHEANCE,
                        projet.getAssociation(),
                        joursRestants != null && joursRestants <= 3
                );
            } catch (Exception e) {
                System.err.println("Erreur envoi rappel échéance: " + e.getMessage());
            }
        }
    }

    /**
     * Statistiques générales des projets
     */
    public ProjetStats getGeneralStats() {
        List<Projet> allProjets = findAll();

        ProjetStats stats = new ProjetStats();
        stats.setTotalProjets(allProjets.size());
        stats.setProjetsActifs((int) allProjets.stream().filter(Projet::estActif).count());
        stats.setProjetsTermines((int) allProjets.stream().filter(Projet::estTermine).count());
        stats.setProjetsEnRetard((int) allProjets.stream().filter(Projet::estEnRetard).count());

        double montantTotalDemande = allProjets.stream()
                .mapToDouble(Projet::getMontantDemande)
                .sum();
        stats.setMontantTotalDemande(montantTotalDemande);

        double montantTotalCollecte = allProjets.stream()
                .mapToDouble(Projet::getMontantCollecte)
                .sum();
        stats.setMontantTotalCollecte(montantTotalCollecte);

        if (montantTotalDemande > 0) {
            stats.setTauxReussiteGlobal((montantTotalCollecte / montantTotalDemande) * 100);
        }

        return stats;
    }

    /**
     * Mettre à jour le montant collecté d'un projet
     */
    public Projet updateMontantCollecte(Long projetId, Double nouveauMontant) {
        Projet projet = findById(projetId);
        projet.setMontantCollecte(nouveauMontant);

        // Vérifier si l'objectif est atteint
        if (projet.getMontantCollecte() >= projet.getMontantDemande()) {
            projet.setStatut(StatutProjet.TERMINE);

            // Notifier l'association
            try {
                notificationService.creerNotification(
                        "Objectif atteint !",
                        "Félicitations ! Votre projet \"" + projet.getTitre() + "\" a atteint son objectif.",
                        com.mediation.platform.enums.TypeNotification.PROJET_COMPLETE,
                        projet.getAssociation(),
                        false
                );
            } catch (Exception e) {
                System.err.println("Erreur notification objectif atteint: " + e.getMessage());
            }
        }

        return projetRepository.save(projet);
    }

    /**
     * Vérifier si un projet existe
     */
    public boolean existsById(Long id) {
        return projetRepository.existsById(id);
    }

    /**
     * Compter le nombre total de projets
     */
    public long count() {
        return projetRepository.count();
    }

    /**
     * Projets par priorité
     */
    public List<Projet> findByPriorite(String priorite) {
        return projetRepository.findByPrioriteOrderByDateCreationDesc(priorite);
    }

    /**
     * Calculer les tendances des projets
     */
    public TendancesProjets calculerTendances() {
        LocalDateTime maintenant = LocalDateTime.now();
        LocalDateTime debutMoisActuel = maintenant.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime debutMoisPrecedent = debutMoisActuel.minusMonths(1);

        List<Projet> projetsMoisActuel = projetRepository.findRecentProjects(debutMoisActuel);
        List<Projet> projetsMoisPrecedent = projetRepository.findRecentProjects(debutMoisPrecedent).stream()
                .filter(p -> p.getDateCreation().isBefore(debutMoisActuel))
                .toList();

        TendancesProjets tendances = new TendancesProjets();
        tendances.setProjetsCreeCeMois(projetsMoisActuel.size());
        tendances.setProjetsCreeMoisPrecedent(projetsMoisPrecedent.size());

        if (projetsMoisPrecedent.size() > 0) {
            double evolution = ((double) (projetsMoisActuel.size() - projetsMoisPrecedent.size()) / projetsMoisPrecedent.size()) * 100;
            tendances.setEvolutionPourcentage(evolution);
        }

        return tendances;
    }

    /**
     * Classe interne pour les statistiques
     */
    public static class ProjetStats {
        private int totalProjets;
        private int projetsActifs;
        private int projetsTermines;
        private int projetsEnRetard;
        private double montantTotalDemande;
        private double montantTotalCollecte;
        private double tauxReussiteGlobal;

        // Getters et setters
        public int getTotalProjets() { return totalProjets; }
        public void setTotalProjets(int totalProjets) { this.totalProjets = totalProjets; }

        public int getProjetsActifs() { return projetsActifs; }
        public void setProjetsActifs(int projetsActifs) { this.projetsActifs = projetsActifs; }

        public int getProjetsTermines() { return projetsTermines; }
        public void setProjetsTermines(int projetsTermines) { this.projetsTermines = projetsTermines; }

        public int getProjetsEnRetard() { return projetsEnRetard; }
        public void setProjetsEnRetard(int projetsEnRetard) { this.projetsEnRetard = projetsEnRetard; }

        public double getMontantTotalDemande() { return montantTotalDemande; }
        public void setMontantTotalDemande(double montantTotalDemande) { this.montantTotalDemande = montantTotalDemande; }

        public double getMontantTotalCollecte() { return montantTotalCollecte; }
        public void setMontantTotalCollecte(double montantTotalCollecte) { this.montantTotalCollecte = montantTotalCollecte; }

        public double getTauxReussiteGlobal() { return tauxReussiteGlobal; }
        public void setTauxReussiteGlobal(double tauxReussiteGlobal) { this.tauxReussiteGlobal = tauxReussiteGlobal; }
    }

    /**
     * Classe interne pour les tendances
     */
    public static class TendancesProjets {
        private int projetsCreeCeMois;
        private int projetsCreeMoisPrecedent;
        private double evolutionPourcentage;

        // Getters et setters
        public int getProjetsCreeCeMois() { return projetsCreeCeMois; }
        public void setProjetsCreeCeMois(int projetsCreeCeMois) { this.projetsCreeCeMois = projetsCreeCeMois; }

        public int getProjetsCreeMoisPrecedent() { return projetsCreeMoisPrecedent; }
        public void setProjetsCreeMoisPrecedent(int projetsCreeMoisPrecedent) { this.projetsCreeMoisPrecedent = projetsCreeMoisPrecedent; }

        public double getEvolutionPourcentage() { return evolutionPourcentage; }
        public void setEvolutionPourcentage(double evolutionPourcentage) { this.evolutionPourcentage = evolutionPourcentage; }
    }
}