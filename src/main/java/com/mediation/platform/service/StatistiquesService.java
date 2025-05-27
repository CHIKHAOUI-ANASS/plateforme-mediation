package com.mediation.platform.service;

import com.mediation.platform.entity.Association;
import com.mediation.platform.entity.Donateur;
import com.mediation.platform.entity.Projet;
import com.mediation.platform.entity.Don;
import com.mediation.platform.entity.Transaction;
import com.mediation.platform.enums.RoleUtilisateur;
import com.mediation.platform.enums.StatutDon;
import com.mediation.platform.enums.StatutProjet;
import com.mediation.platform.enums.StatutTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatistiquesService {

    @Autowired
    private UtilisateurService utilisateurService;

    @Autowired
    private DonateurService donateurService;

    @Autowired
    private AssociationService associationService;

    @Autowired
    private ProjetService projetService;

    @Autowired
    private DonService donService;

    @Autowired
    private TransactionService transactionService;

    /**
     * Statistiques générales de la plateforme
     */
    public Map<String, Object> getStatistiquesGenerales() {
        Map<String, Object> stats = new HashMap<>();

        // Statistiques des utilisateurs
        stats.put("totalUtilisateurs", utilisateurService.findAll().size());
        stats.put("totalDonateurs", utilisateurService.countByRole(RoleUtilisateur.DONATEUR));
        stats.put("totalAssociations", utilisateurService.countByRole(RoleUtilisateur.ASSOCIATION));
        stats.put("totalAdministrateurs", utilisateurService.countByRole(RoleUtilisateur.ADMINISTRATEUR));

        // Statistiques des associations
        stats.put("associationsValidees", associationService.findValidatedAssociations().size());
        stats.put("associationsEnAttente", associationService.findPendingAssociations().size());
        stats.put("associationsAvecProjetsActifs", associationService.findWithActiveProjects().size());

        // Statistiques des projets
        stats.put("totalProjets", projetService.findAll().size());
        stats.put("projetsEnCours", projetService.findByStatut(StatutProjet.EN_COURS).size());
        stats.put("projetsTermines", projetService.findByStatut(StatutProjet.TERMINE).size());
        stats.put("projetsSuspendus", projetService.findByStatut(StatutProjet.SUSPENDU).size());
        stats.put("projetsAnnules", projetService.findByStatut(StatutProjet.ANNULE).size());

        // Statistiques des dons
        stats.put("totalDons", donService.findAll().size());
        stats.put("donsValides", donService.findByStatut(StatutDon.VALIDE).size());
        stats.put("donsEnAttente", donService.findByStatut(StatutDon.EN_ATTENTE).size());
        stats.put("donsRefuses", donService.findByStatut(StatutDon.REFUSE).size());
        stats.put("donsAnonymes", donService.findAnonymousDons().size());

        return stats;
    }

    /**
     * Statistiques financières
     */
    public Map<String, Object> getStatistiquesFinancieres() {
        Map<String, Object> stats = new HashMap<>();

        // Montants totaux
        Double totalDonsConfirmes = donService.getTotalConfirmedDonations();
        Double totalTransactionsReussies = transactionService.getTotalSuccessfulTransactions();
        Double totalFrais = transactionService.getTotalFees();
        Double tauxReussite = transactionService.getSuccessRate();

        stats.put("totalDonsConfirmes", totalDonsConfirmes != null ? totalDonsConfirmes : 0.0);
        stats.put("totalTransactionsReussies", totalTransactionsReussies != null ? totalTransactionsReussies : 0.0);
        stats.put("totalFrais", totalFrais != null ? totalFrais : 0.0);
        stats.put("tauxReussiteTransactions", tauxReussite != null ? tauxReussite : 0.0);

        // Nombre de donateurs uniques
        stats.put("donateursUniques", donService.getUniqueDonorsCount());

        // Montant moyen par don
        int nombreDons = donService.findByStatut(StatutDon.VALIDE).size();
        if (nombreDons > 0 && totalDonsConfirmes != null) {
            stats.put("montantMoyenParDon", totalDonsConfirmes / nombreDons);
        } else {
            stats.put("montantMoyenParDon", 0.0);
        }

        // Calcul du montant net (après frais)
        Double montantNet = (totalTransactionsReussies != null ? totalTransactionsReussies : 0.0) -
                (totalFrais != null ? totalFrais : 0.0);
        stats.put("montantNetCollecte", montantNet);

        return stats;
    }

    /**
     * Statistiques pour une période donnée
     */
    public Map<String, Object> getStatistiquesPeriode(LocalDate dateDebut, LocalDate dateFin) {
        Map<String, Object> stats = new HashMap<>();

        // Dons de la période
        List<Don> donsPeriode = donService.findByPeriod(dateDebut, dateFin);
        stats.put("nombreDonsPeriode", donsPeriode.size());

        Double montantPeriode = donsPeriode.stream()
                .filter(don -> don.getStatut() == StatutDon.VALIDE)
                .mapToDouble(Don::getMontant)
                .sum();
        stats.put("montantDonsPeriode", montantPeriode);

        // Transactions de la période
        LocalDateTime dateDebutTime = dateDebut.atStartOfDay();
        LocalDateTime dateFinTime = dateFin.plusDays(1).atStartOfDay();
        List<Transaction> transactionsPeriode = transactionService.findByPeriod(dateDebutTime, dateFinTime);
        stats.put("nombreTransactionsPeriode", transactionsPeriode.size());

        // Montant des transactions réussies de la période
        Double montantTransactionsPeriode = transactionsPeriode.stream()
                .filter(t -> t.getStatut() == StatutTransaction.REUSSIE)
                .mapToDouble(Transaction::getMontant)
                .sum();
        stats.put("montantTransactionsPeriode", montantTransactionsPeriode);

        // Associations validées récemment
        List<Association> associationsRecentes = associationService.findRecentlyValidated(dateDebutTime);
        stats.put("associationsValideesPeriode", associationsRecentes.size());

        // Évolution par rapport à la période précédente
        LocalDate dateDebutPrecedente = dateDebut.minusDays(dateFin.toEpochDay() - dateDebut.toEpochDay());
        LocalDate dateFinPrecedente = dateDebut.minusDays(1);
        Map<String, Object> periodePrecedente = getStatistiquesPeriode(dateDebutPrecedente, dateFinPrecedente);

        Double montantPrecedent = (Double) periodePrecedente.get("montantDonsPeriode");
        if (montantPrecedent != null && montantPrecedent > 0) {
            Double evolution = ((montantPeriode - montantPrecedent) / montantPrecedent) * 100;
            stats.put("evolutionMontant", evolution);
        } else {
            stats.put("evolutionMontant", 0.0);
        }

        return stats;
    }

    /**
     * Statistiques spécifiques à une association
     */
    public Map<String, Object> getStatistiquesAssociation(Long associationId) {
        Map<String, Object> stats = new HashMap<>();

        Association association = associationService.findById(associationId);

        // Projets de l'association
        List<Projet> projets = projetService.findByAssociation(association);
        stats.put("totalProjets", projets.size());

        long projetsEnCours = projets.stream()
                .filter(p -> p.getStatut() == StatutProjet.EN_COURS)
                .count();
        stats.put("projetsEnCours", projetsEnCours);

        long projetsTermines = projets.stream()
                .filter(p -> p.getStatut() == StatutProjet.TERMINE)
                .count();
        stats.put("projetsTermines", projetsTermines);

        // Montant total collecté
        Double montantTotal = projets.stream()
                .mapToDouble(Projet::getMontantCollecte)
                .sum();
        stats.put("montantTotalCollecte", montantTotal);

        // Montant demandé total
        Double montantDemande = projets.stream()
                .mapToDouble(Projet::getMontantDemande)
                .sum();
        stats.put("montantTotalDemande", montantDemande);

        // Taux de réussite financière
        if (montantDemande > 0) {
            stats.put("tauxReussite", (montantTotal / montantDemande) * 100);
        } else {
            stats.put("tauxReussite", 0.0);
        }

        // Nombre total de dons reçus
        int totalDons = projets.stream()
                .mapToInt(p -> p.getDons().size())
                .sum();
        stats.put("totalDonsRecus", totalDons);

        // Nombre de donateurs uniques
        long donateursUniques = projets.stream()
                .flatMap(p -> p.getDons().stream())
                .map(Don::getDonateur)
                .distinct()
                .count();
        stats.put("donateursUniques", donateursUniques);

        // Projet le plus performant
        Projet projetTopPerformant = projets.stream()
                .max((p1, p2) -> Double.compare(p1.getMontantCollecte(), p2.getMontantCollecte()))
                .orElse(null);

        if (projetTopPerformant != null) {
            Map<String, Object> topProjet = new HashMap<>();
            topProjet.put("titre", projetTopPerformant.getTitre());
            topProjet.put("montantCollecte", projetTopPerformant.getMontantCollecte());
            topProjet.put("progres", projetTopPerformant.calculerProgres());
            stats.put("projetTopPerformant", topProjet);
        }

        return stats;
    }

    /**
     * Statistiques spécifiques à un donateur
     */
    public Map<String, Object> getStatistiquesDonateur(Long donateurId) {
        Map<String, Object> stats = new HashMap<>();

        Donateur donateur = donateurService.findById(donateurId);

        // Dons du donateur
        List<Don> dons = donService.findByDonateur(donateur);
        stats.put("nombreDons", dons.size());

        Double montantTotal = dons.stream()
                .filter(don -> don.getStatut() == StatutDon.VALIDE)
                .mapToDouble(Don::getMontant)
                .sum();
        stats.put("montantTotalDonne", montantTotal);

        // Montant moyen par don
        if (dons.size() > 0) {
            stats.put("montantMoyenParDon", montantTotal / dons.size());
        } else {
            stats.put("montantMoyenParDon", 0.0);
        }

        // Premier et dernier don
        if (!dons.isEmpty()) {
            stats.put("premierDon", dons.get(dons.size() - 1).getDate());
            stats.put("dernierDon", dons.get(0).getDate());
        }

        // Dons anonymes
        long donsAnonymes = dons.stream()
                .filter(Don::getAnonyme)
                .count();
        stats.put("donsAnonymes", donsAnonymes);

        // Nombre de projets soutenus
        long projetsSoutenus = dons.stream()
                .map(Don::getProjet)
                .distinct()
                .count();
        stats.put("nombreProjetsSoutenus", projetsSoutenus);

        // Associations soutenues
        long associationsSoutenues = dons.stream()
                .map(don -> don.getProjet().getAssociation())
                .distinct()
                .count();
        stats.put("nombreAssociationsSoutenues", associationsSoutenues);

        // Don le plus important
        Don donMax = dons.stream()
                .filter(don -> don.getStatut() == StatutDon.VALIDE)
                .max((d1, d2) -> Double.compare(d1.getMontant(), d2.getMontant()))
                .orElse(null);

        if (donMax != null) {
            Map<String, Object> plusGrosDon = new HashMap<>();
            plusGrosDon.put("montant", donMax.getMontant());
            plusGrosDon.put("projet", donMax.getProjet().getTitre());
            plusGrosDon.put("date", donMax.getDate());
            stats.put("plusGrosDon", plusGrosDon);
        }

        return stats;
    }

    /**
     * Statistiques spécifiques à un projet
     */
    public Map<String, Object> getStatistiquesProjet(Long projetId) {
        Map<String, Object> stats = new HashMap<>();

        Projet projet = projetService.findById(projetId);

        // Informations de base
        stats.put("montantDemande", projet.getMontantDemande());
        stats.put("montantCollecte", projet.getMontantCollecte());
        stats.put("progres", projet.calculerProgres());
        stats.put("montantRestant", projet.getMontantRestant());

        // Dons pour le projet
        List<Don> dons = donService.findByProjet(projet);
        stats.put("nombreDons", dons.size());
        stats.put("nombreDonateurs", projet.getNombreDonateurs());

        // Montant moyen par don
        if (dons.size() > 0) {
            stats.put("montantMoyenParDon", projet.getMontantCollecte() / dons.size());
        } else {
            stats.put("montantMoyenParDon", 0.0);
        }

        // Dons avec message
        long donsAvecMessage = dons.stream()
                .filter(don -> don.getMessage() != null && !don.getMessage().trim().isEmpty())
                .count();
        stats.put("donsAvecMessage", donsAvecMessage);

        // Statut du projet
        stats.put("statut", projet.getStatut().toString());
        stats.put("estEnCours", projet.getStatut() == StatutProjet.EN_COURS);
        stats.put("estTermine", projet.estTermine());
        stats.put("estEnRetard", projet.estEnRetard());

        // Analyse temporelle
        if (projet.getDateDebut() != null && projet.getDateFin() != null) {
            long joursTotal = projet.getDateFin().toEpochDay() - projet.getDateDebut().toEpochDay();
            long joursEcoules = LocalDate.now().toEpochDay() - projet.getDateDebut().toEpochDay();

            if (joursTotal > 0) {
                stats.put("tempsEcoulePercent", Math.min(100.0, (joursEcoules * 100.0) / joursTotal));
            } else {
                stats.put("tempsEcoulePercent", 0.0);
            }
        }

        // Don le plus important pour ce projet
        Don donMax = dons.stream()
                .filter(don -> don.getStatut() == StatutDon.VALIDE)
                .max((d1, d2) -> Double.compare(d1.getMontant(), d2.getMontant()))
                .orElse(null);

        if (donMax != null) {
            Map<String, Object> plusGrosDon = new HashMap<>();
            plusGrosDon.put("montant", donMax.getMontant());
            plusGrosDon.put("donateur", donMax.getNomDonateurAffiche());
            plusGrosDon.put("date", donMax.getDate());
            stats.put("plusGrosDon", plusGrosDon);
        }

        // Répartition des dons par mois (12 derniers mois)
        Map<String, Double> repartitionMensuelle = new HashMap<>();
        LocalDate dateDebut = LocalDate.now().minusMonths(12);

        for (int i = 0; i < 12; i++) {
            LocalDate moisDebut = dateDebut.plusMonths(i);
            LocalDate moisFin = moisDebut.plusMonths(1).minusDays(1);

            Double montantMois = dons.stream()
                    .filter(don -> don.getStatut() == StatutDon.VALIDE)
                    .filter(don -> !don.getDate().isBefore(moisDebut) && !don.getDate().isAfter(moisFin))
                    .mapToDouble(Don::getMontant)
                    .sum();

            repartitionMensuelle.put(moisDebut.getMonth().name(), montantMois);
        }
        stats.put("repartitionMensuelle", repartitionMensuelle);

        return stats;
    }

    /**
     * Rapport d'activité global
     */
    public Map<String, Object> getRapportActivite() {
        Map<String, Object> rapport = new HashMap<>();

        LocalDate maintenant = LocalDate.now();
        LocalDate debutSemaine = maintenant.minusDays(7);
        LocalDate debutMois = maintenant.withDayOfMonth(1);
        LocalDateTime debutSemaineTime = debutSemaine.atStartOfDay();

        // Activité de la semaine
        rapport.put("activiteSemaine", getStatistiquesPeriode(debutSemaine, maintenant));

        // Activité du mois
        rapport.put("activiteMois", getStatistiquesPeriode(debutMois, maintenant));

        // Projets récents
        List<Projet> projetsRecents = projetService.findRecentProjects(debutSemaineTime);
        rapport.put("nouveauxProjets", projetsRecents.size());

        // Projets en retard
        List<Projet> projetsEnRetard = projetService.findOverdueProjects();
        rapport.put("projetsEnRetard", projetsEnRetard.size());

        // Projets proches de l'objectif (90%)
        List<Projet> projetsProches = projetService.findNearGoal(0.9);
        rapport.put("projetsProchesObjectif", projetsProches.size());

        // Top associations
        List<Association> topAssociations = associationService.findTopAssociations();
        rapport.put("topAssociations", topAssociations.size() > 5 ?
                topAssociations.subList(0, 5) : topAssociations);

        // Top donateurs
        List<Donateur> topDonateurs = donateurService.findTopDonators();
        rapport.put("topDonateurs", topDonateurs.size() > 5 ?
                topDonateurs.subList(0, 5) : topDonateurs);

        // Dons récents (7 derniers jours)
        List<Don> donsRecents = donService.findRecentDonations(debutSemaine);
        rapport.put("donsRecents", donsRecents.size());

        // Transactions récentes
        List<Transaction> transactionsRecentes = transactionService.findRecentTransactions(debutSemaineTime);
        rapport.put("transactionsRecentes", transactionsRecentes.size());

        // Gros dons (> 1000 DH)
        List<Don> grosDons = donService.findLargeDonations(1000.0);
        rapport.put("grosDonsSemaine", grosDons.stream()
                .filter(don -> !don.getDate().isBefore(debutSemaine))
                .count());

        return rapport;
    }

    /**
     * Tableau de bord complet pour les administrateurs
     */
    public Map<String, Object> getTableauDeBord() {
        Map<String, Object> dashboard = new HashMap<>();

        // Statistiques générales
        dashboard.put("statistiquesGenerales", getStatistiquesGenerales());

        // Statistiques financières
        dashboard.put("statistiquesFinancieres", getStatistiquesFinancieres());

        // Rapport d'activité
        dashboard.put("rapportActivite", getRapportActivite());

        // Alertes et notifications importantes
        Map<String, Object> alertes = new HashMap<>();
        alertes.put("projetsEnRetard", projetService.findOverdueProjects().size());
        alertes.put("associationsEnAttente", associationService.findPendingAssociations().size());
        alertes.put("transactionsEchouees", transactionService.findByStatut(StatutTransaction.ECHEC).size());
        alertes.put("donsEnAttente", donService.findByStatut(StatutDon.EN_ATTENTE).size());
        dashboard.put("alertes", alertes);

        // Tendances (évolution sur 30 jours)
        LocalDate maintenant = LocalDate.now();
        Map<String, Object> tendances = getStatistiquesPeriode(maintenant.minusDays(30), maintenant);
        dashboard.put("tendances30Jours", tendances);

        return dashboard;
    }

    /**
     * Statistiques pour un rapport PDF/Excel
     */
    public Map<String, Object> getStatistiquesRapport(LocalDate dateDebut, LocalDate dateFin) {
        Map<String, Object> rapport = new HashMap<>();

        // Période du rapport
        rapport.put("dateDebut", dateDebut);
        rapport.put("dateFin", dateFin);
        rapport.put("dateGeneration", LocalDateTime.now());

        // Statistiques générales
        rapport.put("statistiquesGenerales", getStatistiquesGenerales());

        // Statistiques de la période
        rapport.put("statistiquesPeriode", getStatistiquesPeriode(dateDebut, dateFin));

        // Top performers
        rapport.put("topAssociations", associationService.findTopAssociations().stream().limit(10).toList());
        rapport.put("topDonateurs", donateurService.findTopDonators().stream().limit(10).toList());
        rapport.put("topProjets", projetService.findTopProjects().stream().limit(10).toList());

        // Analyse détaillée des transactions
        List<Transaction> transactionsPeriode = transactionService.findByPeriod(
                dateDebut.atStartOfDay(),
                dateFin.plusDays(1).atStartOfDay()
        );

        Map<String, Object> analyseTrans = new HashMap<>();
        analyseTrans.put("totalTransactions", transactionsPeriode.size());
        analyseTrans.put("transactionsReussies",
                transactionsPeriode.stream().filter(t -> t.getStatut() == StatutTransaction.REUSSIE).count());
        analyseTrans.put("transactionsEchouees",
                transactionsPeriode.stream().filter(t -> t.getStatut() == StatutTransaction.ECHEC).count());

        rapport.put("analyseTransactions", analyseTrans);

        return rapport;
    }
}