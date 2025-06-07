package com.mediation.platform.controller;

import com.mediation.platform.dto.response.ApiResponse;
import com.mediation.platform.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/statistiques")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Statistiques", description = "API des statistiques de la plateforme")
public class StatistiquesController {

    @Autowired
    private AssociationService associationService;

    @Autowired
    private DonateurService donateurService;

    @Autowired
    private ProjetService projetService;

    @Autowired
    private DonService donService;

    @Autowired
    private UtilisateurService utilisateurService;

    /**
     * Statistiques publiques de la plateforme
     */
    @GetMapping("/publiques")
    @Operation(summary = "Statistiques publiques", description = "Statistiques générales visibles par tous")
    public ResponseEntity<?> getStatistiquesPubliques() {
        try {
            Map<String, Object> stats = new HashMap<>();

            // Statistiques de base
            stats.put("nombreAssociations", associationService.findValidatedAssociations().size());
            stats.put("nombreProjetsActifs", projetService.findActiveProjects().size());
            stats.put("montantTotalCollecte", donService.getTotalConfirmedDonations());
            stats.put("nombreDonateurs", donateurService.findDonatorsWithConfirmedDonations().size());
            stats.put("nombreDonsTotal", donService.findValidatedDons().size());

            // Associations par domaine
            Map<String, Long> associationsParDomaine = new HashMap<>();
            associationService.findValidatedAssociations().forEach(assoc -> {
                String domaine = assoc.getDomaineActivite() != null ? assoc.getDomaineActivite() : "Autre";
                associationsParDomaine.merge(domaine, 1L, Long::sum);
            });
            stats.put("associationsParDomaine", associationsParDomaine);

            // Projets terminés vs actifs
            stats.put("projetsTermines", projetService.findByStatut(com.mediation.platform.enums.StatutProjet.TERMINE).size());
            stats.put("tauxReussiteProjets", calculerTauxReussite());

            return ResponseEntity.ok(ApiResponse.success("Statistiques publiques", stats));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la récupération des statistiques", e.getMessage()));
        }
    }

    /**
     * Statistiques détaillées des associations
     */
    @GetMapping("/associations")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Statistiques associations", description = "Statistiques détaillées des associations")
    public ResponseEntity<?> getStatistiquesAssociations() {
        try {
            AssociationService.AssociationStats stats = associationService.getGeneralStats();
            return ResponseEntity.ok(ApiResponse.success("Statistiques associations", stats));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la récupération", e.getMessage()));
        }
    }

    /**
     * Statistiques détaillées des donateurs
     */
    @GetMapping("/donateurs")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Statistiques donateurs", description = "Statistiques détaillées des donateurs")
    public ResponseEntity<?> getStatistiquesDonateurs() {
        try {
            DonateurService.DonateurStats stats = donateurService.getGeneralStats();
            return ResponseEntity.ok(ApiResponse.success("Statistiques donateurs", stats));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la récupération", e.getMessage()));
        }
    }

    /**
     * Statistiques détaillées des projets
     */
    @GetMapping("/projets")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Statistiques projets", description = "Statistiques détaillées des projets")
    public ResponseEntity<?> getStatistiquesProjets() {
        try {
            ProjetService.ProjetStats stats = projetService.getGeneralStats();
            return ResponseEntity.ok(ApiResponse.success("Statistiques projets", stats));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la récupération", e.getMessage()));
        }
    }

    /**
     * Statistiques détaillées des dons
     */
    @GetMapping("/dons")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Statistiques dons", description = "Statistiques détaillées des dons")
    public ResponseEntity<?> getStatistiquesDons() {
        try {
            DonService.DonStats stats = donService.getGeneralStats();
            return ResponseEntity.ok(ApiResponse.success("Statistiques dons", stats));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la récupération", e.getMessage()));
        }
    }

    /**
     * Tendances de la plateforme
     */
    @GetMapping("/tendances")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Tendances plateforme", description = "Analyse des tendances de la plateforme")
    public ResponseEntity<?> getTendances() {
        try {
            Map<String, Object> tendances = new HashMap<>();

            tendances.put("projets", projetService.calculerTendances());
            tendances.put("dons", donService.calculerTendances());

            return ResponseEntity.ok(ApiResponse.success("Tendances de la plateforme", tendances));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la récupération des tendances", e.getMessage()));
        }
    }

    /**
     * Tableau de bord global
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Dashboard global", description = "Vue d'ensemble de toute la plateforme")
    public ResponseEntity<?> getDashboardGlobal() {
        try {
            Map<String, Object> dashboard = new HashMap<>();

            // Résumé général
            Map<String, Object> resume = new HashMap<>();
            resume.put("utilisateursTotal", utilisateurService.count());
            resume.put("associationsValidees", associationService.findValidatedAssociations().size());
            resume.put("projetsActifs", projetService.findActiveProjects().size());
            resume.put("montantTotalCollecte", donService.getTotalConfirmedDonations());
            resume.put("donsValides", donService.findValidatedDons().size());
            dashboard.put("resume", resume);

            // Statistiques détaillées
            dashboard.put("associations", associationService.getGeneralStats());
            dashboard.put("donateurs", donateurService.getGeneralStats());
            dashboard.put("projets", projetService.getGeneralStats());
            dashboard.put("dons", donService.getGeneralStats());

            // Tendances
            Map<String, Object> tendances = new HashMap<>();
            tendances.put("projets", projetService.calculerTendances());
            tendances.put("dons", donService.calculerTendances());
            dashboard.put("tendances", tendances);

            // Alertes et points d'attention
            Map<String, Object> alertes = new HashMap<>();
            alertes.put("associationsEnAttente", associationService.findPendingValidation().size());
            alertes.put("donsEnAttente", donService.findByStatut(com.mediation.platform.enums.StatutDon.EN_ATTENTE).size());
            alertes.put("projetsEnRetard", projetService.findOverdueProjects().size());
            dashboard.put("alertes", alertes);

            return ResponseEntity.ok(ApiResponse.success("Dashboard global", dashboard));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la génération du dashboard", e.getMessage()));
        }
    }

    /**
     * Rapport d'activité mensuel
     */
    @GetMapping("/rapport-mensuel")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Rapport mensuel", description = "Rapport d'activité du mois en cours")
    public ResponseEntity<?> getRapportMensuel() {
        try {
            Map<String, Object> rapport = new HashMap<>();

            // Période du rapport
            java.time.LocalDate maintenant = java.time.LocalDate.now();
            java.time.LocalDate debutMois = maintenant.withDayOfMonth(1);

            Map<String, Object> periode = new HashMap<>();
            periode.put("debut", debutMois);
            periode.put("fin", maintenant);
            periode.put("mois", maintenant.getMonth().toString());
            periode.put("annee", maintenant.getYear());
            rapport.put("periode", periode);

            // Activité du mois
            java.time.LocalDate debutMoisDate = debutMois;

            Map<String, Object> activiteMois = new HashMap<>();
            activiteMois.put("nouveauxDons", donService.findByPeriode(debutMoisDate, maintenant).size());
            activiteMois.put("nouveauxProjets", projetService.findRecentProjects(debutMois.atStartOfDay()).size());
            activiteMois.put("nouvellesAssociations", associationService.findRecentlyValidated(maintenant.getDayOfMonth()).size());
            activiteMois.put("montantCollecteCeMois", donService.findByPeriode(debutMoisDate, maintenant).stream()
                    .filter(don -> don.getStatut() == com.mediation.platform.enums.StatutDon.VALIDE)
                    .mapToDouble(don -> don.getMontant())
                    .sum());
            rapport.put("activiteMois", activiteMois);

            // Comparaison avec le mois précédent
            java.time.LocalDate debutMoisPrecedent = debutMois.minusMonths(1);
            java.time.LocalDate finMoisPrecedent = debutMois.minusDays(1);

            Map<String, Object> comparaisonMoisPrecedent = new HashMap<>();
            comparaisonMoisPrecedent.put("donsMoisPrecedent", donService.findByPeriode(debutMoisPrecedent, finMoisPrecedent).size());
            comparaisonMoisPrecedent.put("projetsActuelVsPrecedent", calculerEvolutionProjets(debutMois, debutMoisPrecedent));
            comparaisonMoisPrecedent.put("montantActuelVsPrecedent", calculerEvolutionMontant(debutMoisDate, maintenant, debutMoisPrecedent, finMoisPrecedent));
            rapport.put("comparaisonMoisPrecedent", comparaisonMoisPrecedent);

            return ResponseEntity.ok(ApiResponse.success("Rapport mensuel", rapport));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la génération du rapport", e.getMessage()));
        }
    }

    /**
     * Méthodes utilitaires privées
     */
    private double calculerTauxReussite() {
        try {
            long totalProjets = projetService.count();
            long projetsTermines = projetService.findByStatut(com.mediation.platform.enums.StatutProjet.TERMINE).size();

            if (totalProjets == 0) return 0.0;
            return (double) projetsTermines / totalProjets * 100;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double calculerEvolutionProjets(java.time.LocalDate debutMoisActuel, java.time.LocalDate debutMoisPrecedent) {
        try {
            int projetsActuels = projetService.findRecentProjects(debutMoisActuel.atStartOfDay()).size();
            int projetsPrecedents = projetService.findRecentProjects(debutMoisPrecedent.atStartOfDay()).stream()
                    .filter(p -> p.getDateCreation().isBefore(debutMoisActuel.atStartOfDay()))
                    .toList().size();

            if (projetsPrecedents == 0) return projetsActuels > 0 ? 100.0 : 0.0;
            return ((double) (projetsActuels - projetsPrecedents) / projetsPrecedents) * 100;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double calculerEvolutionMontant(java.time.LocalDate debutActuel, java.time.LocalDate finActuel,
                                            java.time.LocalDate debutPrecedent, java.time.LocalDate finPrecedent) {
        try {
            double montantActuel = donService.findByPeriode(debutActuel, finActuel).stream()
                    .filter(don -> don.getStatut() == com.mediation.platform.enums.StatutDon.VALIDE)
                    .mapToDouble(don -> don.getMontant())
                    .sum();

            double montantPrecedent = donService.findByPeriode(debutPrecedent, finPrecedent).stream()
                    .filter(don -> don.getStatut() == com.mediation.platform.enums.StatutDon.VALIDE)
                    .mapToDouble(don -> don.getMontant())
                    .sum();

            if (montantPrecedent == 0) return montantActuel > 0 ? 100.0 : 0.0;
            return ((montantActuel - montantPrecedent) / montantPrecedent) * 100;
        } catch (Exception e) {
            return 0.0;
        }
    }
}