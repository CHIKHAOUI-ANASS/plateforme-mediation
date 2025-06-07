package com.mediation.platform.service;

import com.mediation.platform.entity.Don;
import com.mediation.platform.entity.Donateur;
import com.mediation.platform.entity.Projet;
import com.mediation.platform.enums.StatutDon;
import com.mediation.platform.exception.BusinessException;
import com.mediation.platform.exception.ResourceNotFoundException;
import com.mediation.platform.repository.DonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class DonService {

    @Autowired
    private DonRepository donRepository;

    @Autowired
    private ProjetService projetService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailService emailService;

    /**
     * Trouver un don par ID
     */
    public Don findById(Long id) {
        return donRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Don non trouvé avec l'ID: " + id));
    }

    /**
     * Trouver tous les dons
     */
    public List<Don> findAll() {
        return donRepository.findAll();
    }

    /**
     * Sauvegarder un don
     */
    public Don save(Don don) {
        // Validations métier
        if (don.getMontant() <= 0) {
            throw new BusinessException("Le montant du don doit être positif");
        }

        if (don.getProjet() == null) {
            throw new BusinessException("Le projet est obligatoire");
        }

        if (!don.getProjet().peutRecevoirDons()) {
            throw new BusinessException("Ce projet ne peut plus recevoir de dons");
        }

        // Initialiser le statut
        if (don.getStatut() == null) {
            don.setStatut(StatutDon.EN_ATTENTE);
        }

        Don savedDon = donRepository.save(don);

        // Notifier l'association
        try {
            String nomDonateur = savedDon.getAnonyme() ? "Donateur anonyme" : savedDon.getDonateur().getNomComplet();

            notificationService.notifierDonRecu(
                    savedDon.getProjet().getAssociation(),
                    nomDonateur,
                    savedDon.getMontant(),
                    savedDon.getProjet().getTitre()
            );

            emailService.envoyerEmailDonRecu(
                    savedDon.getProjet().getAssociation(),
                    nomDonateur,
                    savedDon.getMontant(),
                    savedDon.getProjet().getTitre()
            );
        } catch (Exception e) {
            System.err.println("Erreur notification don reçu: " + e.getMessage());
        }

        return savedDon;
    }

    /**
     * Valider un don
     */
    public Don validerDon(Long donId) {
        Don don = findById(donId);

        if (don.getStatut() != StatutDon.EN_ATTENTE) {
            throw new BusinessException("Seuls les dons en attente peuvent être validés");
        }

        don.confirmer();
        Don savedDon = donRepository.save(don);

        // Mettre à jour le montant collecté du projet
        Projet projet = don.getProjet();
        projetService.updateMontantCollecte(projet.getIdProjet(), projet.getMontantCollecte() + don.getMontant());

        // Notifier le donateur
        try {
            notificationService.notifierDonValide(
                    savedDon.getDonateur(),
                    savedDon.getProjet().getTitre(),
                    savedDon.getMontant()
            );

            emailService.envoyerEmailConfirmationDon(
                    savedDon.getDonateur(),
                    savedDon.getMontant(),
                    savedDon.getProjet().getTitre(),
                    savedDon.getProjet().getAssociation().getNomAssociation()
            );
        } catch (Exception e) {
            System.err.println("Erreur notification don validé: " + e.getMessage());
        }

        return savedDon;
    }

    /**
     * Rejeter un don
     */
    public Don rejeterDon(Long donId) {
        Don don = findById(donId);

        if (don.getStatut() != StatutDon.EN_ATTENTE) {
            throw new BusinessException("Seuls les dons en attente peuvent être rejetés");
        }

        don.setStatut(StatutDon.REFUSE);
        Don savedDon = donRepository.save(don);

        // Notifier le donateur
        try {
            notificationService.notifierDonRefuse(
                    savedDon.getDonateur(),
                    savedDon.getProjet().getTitre(),
                    savedDon.getMontant(),
                    "Don rejeté par l'administrateur"
            );
        } catch (Exception e) {
            System.err.println("Erreur notification don rejeté: " + e.getMessage());
        }

        return savedDon;
    }

    /**
     * Supprimer un don
     */
    public void deleteById(Long id) {
        Don don = findById(id);

        if (don.getStatut() == StatutDon.VALIDE) {
            throw new BusinessException("Les dons validés ne peuvent pas être supprimés");
        }

        donRepository.delete(don);
    }

    /**
     * Dons d'un donateur
     */
    public List<Don> findByDonateur(Donateur donateur) {
        return donRepository.findByDonateurOrderByDateDesc(donateur);
    }

    /**
     * Dons pour un projet
     */
    public List<Don> findByProjet(Projet projet) {
        return donRepository.findByProjetOrderByDateDesc(projet);
    }

    /**
     * Dons par statut
     */
    public List<Don> findByStatut(StatutDon statut) {
        return donRepository.findByStatut(statut);
    }

    /**
     * Dons validés
     */
    public List<Don> findValidatedDons() {
        return donRepository.findByStatutOrderByDateDesc(StatutDon.VALIDE);
    }

    /**
     * Dons anonymes
     */
    public List<Don> findAnonymousDons() {
        return donRepository.findByAnonymeTrue();
    }

    /**
     * Dons avec message
     */
    public List<Don> findDonsWithMessage() {
        return donRepository.findDonsWithMessage();
    }

    /**
     * Dons par période
     */
    public List<Don> findByPeriode(LocalDate dateDebut, LocalDate dateFin) {
        return donRepository.findByDateBetweenOrderByDateDesc(dateDebut, dateFin);
    }

    /**
     * Dons récents (30 derniers jours)
     */
    public List<Don> findRecentDonations(LocalDate dateDebut) {
        return donRepository.findRecentDonations(dateDebut);
    }

    /**
     * Gros dons (montant supérieur à un seuil)
     */
    public List<Don> findLargeDonations(Double montantMin) {
        return donRepository.findLargeDonations(montantMin);
    }

    /**
     * Montant total des dons confirmés
     */
    public Double getTotalConfirmedDonations() {
        Double total = donRepository.getTotalConfirmedDonations();
        return total != null ? total : 0.0;
    }

    /**
     * Montant total pour un projet
     */
    public Double getTotalForProject(Projet projet) {
        Double total = donRepository.getTotalForProject(projet);
        return total != null ? total : 0.0;
    }

    /**
     * Nombre de donateurs uniques
     */
    public long getUniqueDonorsCount() {
        return donRepository.getUniqueDonorsCount();
    }

    /**
     * Rembourser un don
     */
    public Don rembourserDon(Long donId, String motif) {
        Don don = findById(donId);

        if (don.getStatut() != StatutDon.VALIDE) {
            throw new BusinessException("Seuls les dons validés peuvent être remboursés");
        }

        don.rembourser();
        Don savedDon = donRepository.save(don);

        // Mettre à jour le montant collecté du projet
        Projet projet = don.getProjet();
        double nouveauMontant = Math.max(0, projet.getMontantCollecte() - don.getMontant());
        projetService.updateMontantCollecte(projet.getIdProjet(), nouveauMontant);

        // Notifier le donateur
        try {
            notificationService.creerNotification(
                    "Don remboursé",
                    "Votre don de " + don.getMontant() + " DH a été remboursé. Motif: " +
                            (motif != null ? motif : "Non spécifié"),
                    com.mediation.platform.enums.TypeNotification.SYSTEME,
                    don.getDonateur(),
                    false
            );
        } catch (Exception e) {
            System.err.println("Erreur notification remboursement: " + e.getMessage());
        }

        return savedDon;
    }

    /**
     * Statistiques générales des dons
     */
    public DonStats getGeneralStats() {
        List<Don> allDons = findAll();

        DonStats stats = new DonStats();
        stats.setTotalDons(allDons.size());
        stats.setDonsValides((int) allDons.stream().filter(d -> d.getStatut() == StatutDon.VALIDE).count());
        stats.setDonsEnAttente((int) allDons.stream().filter(d -> d.getStatut() == StatutDon.EN_ATTENTE).count());
        stats.setDonsRefuses((int) allDons.stream().filter(d -> d.getStatut() == StatutDon.REFUSE).count());
        stats.setDonsAnonymes((int) allDons.stream().filter(Don::getAnonyme).count());

        stats.setMontantTotal(getTotalConfirmedDonations());
        stats.setDonateursUniques(getUniqueDonorsCount());

        if (stats.getDonsValides() > 0) {
            stats.setMontantMoyen(stats.getMontantTotal() / stats.getDonsValides());
        }

        // Don le plus important
        allDons.stream()
                .filter(d -> d.getStatut() == StatutDon.VALIDE)
                .mapToDouble(Don::getMontant)
                .max()
                .ifPresent(stats::setDonMaximum);

        return stats;
    }

    /**
     * Calculer les tendances des dons
     */
    public TendancesDons calculerTendances() {
        LocalDate maintenant = LocalDate.now();
        LocalDate debutMoisActuel = maintenant.withDayOfMonth(1);
        LocalDate finMoisActuel = maintenant;
        LocalDate debutMoisPrecedent = debutMoisActuel.minusMonths(1);
        LocalDate finMoisPrecedent = debutMoisActuel.minusDays(1);

        List<Don> donsMoisActuel = findByPeriode(debutMoisActuel, finMoisActuel);
        List<Don> donsMoisPrecedent = findByPeriode(debutMoisPrecedent, finMoisPrecedent);

        TendancesDons tendances = new TendancesDons();
        tendances.setDonsCeMois(donsMoisActuel.size());
        tendances.setDonsMoisPrecedent(donsMoisPrecedent.size());

        double montantCeMois = donsMoisActuel.stream()
                .filter(d -> d.getStatut() == StatutDon.VALIDE)
                .mapToDouble(Don::getMontant)
                .sum();

        double montantMoisPrecedent = donsMoisPrecedent.stream()
                .filter(d -> d.getStatut() == StatutDon.VALIDE)
                .mapToDouble(Don::getMontant)
                .sum();

        tendances.setMontantCeMois(montantCeMois);
        tendances.setMontantMoisPrecedent(montantMoisPrecedent);

        if (donsMoisPrecedent.size() > 0) {
            double evolutionNombre = ((double) (donsMoisActuel.size() - donsMoisPrecedent.size()) / donsMoisPrecedent.size()) * 100;
            tendances.setEvolutionNombrePourcentage(evolutionNombre);
        }

        if (montantMoisPrecedent > 0) {
            double evolutionMontant = ((montantCeMois - montantMoisPrecedent) / montantMoisPrecedent) * 100;
            tendances.setEvolutionMontantPourcentage(evolutionMontant);
        }

        return tendances;
    }

    /**
     * Vérifier si un don existe
     */
    public boolean existsById(Long id) {
        return donRepository.existsById(id);
    }

    /**
     * Compter le nombre total de dons
     */
    public long count() {
        return donRepository.count();
    }

    /**
     * Classes internes pour les statistiques
     */
    public static class DonStats {
        private int totalDons;
        private int donsValides;
        private int donsEnAttente;
        private int donsRefuses;
        private int donsAnonymes;
        private double montantTotal;
        private double montantMoyen;
        private double donMaximum;
        private long donateursUniques;

        // Getters et setters
        public int getTotalDons() { return totalDons; }
        public void setTotalDons(int totalDons) { this.totalDons = totalDons; }

        public int getDonsValides() { return donsValides; }
        public void setDonsValides(int donsValides) { this.donsValides = donsValides; }

        public int getDonsEnAttente() { return donsEnAttente; }
        public void setDonsEnAttente(int donsEnAttente) { this.donsEnAttente = donsEnAttente; }

        public int getDonsRefuses() { return donsRefuses; }
        public void setDonsRefuses(int donsRefuses) { this.donsRefuses = donsRefuses; }

        public int getDonsAnonymes() { return donsAnonymes; }
        public void setDonsAnonymes(int donsAnonymes) { this.donsAnonymes = donsAnonymes; }

        public double getMontantTotal() { return montantTotal; }
        public void setMontantTotal(double montantTotal) { this.montantTotal = montantTotal; }

        public double getMontantMoyen() { return montantMoyen; }
        public void setMontantMoyen(double montantMoyen) { this.montantMoyen = montantMoyen; }

        public double getDonMaximum() { return donMaximum; }
        public void setDonMaximum(double donMaximum) { this.donMaximum = donMaximum; }

        public long getDonateursUniques() { return donateursUniques; }
        public void setDonateursUniques(long donateursUniques) { this.donateursUniques = donateursUniques; }
    }

    public static class TendancesDons {
        private int donsCeMois;
        private int donsMoisPrecedent;
        private double montantCeMois;
        private double montantMoisPrecedent;
        private double evolutionNombrePourcentage;
        private double evolutionMontantPourcentage;

        // Getters et setters
        public int getDonsCeMois() { return donsCeMois; }
        public void setDonsCeMois(int donsCeMois) { this.donsCeMois = donsCeMois; }

        public int getDonsMoisPrecedent() { return donsMoisPrecedent; }
        public void setDonsMoisPrecedent(int donsMoisPrecedent) { this.donsMoisPrecedent = donsMoisPrecedent; }

        public double getMontantCeMois() { return montantCeMois; }
        public void setMontantCeMois(double montantCeMois) { this.montantCeMois = montantCeMois; }

        public double getMontantMoisPrecedent() { return montantMoisPrecedent; }
        public void setMontantMoisPrecedent(double montantMoisPrecedent) { this.montantMoisPrecedent = montantMoisPrecedent; }

        public double getEvolutionNombrePourcentage() { return evolutionNombrePourcentage; }
        public void setEvolutionNombrePourcentage(double evolutionNombrePourcentage) { this.evolutionNombrePourcentage = evolutionNombrePourcentage; }

        public double getEvolutionMontantPourcentage() { return evolutionMontantPourcentage; }
        public void setEvolutionMontantPourcentage(double evolutionMontantPourcentage) { this.evolutionMontantPourcentage = evolutionMontantPourcentage; }
    }
}