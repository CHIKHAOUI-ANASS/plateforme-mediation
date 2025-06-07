package com.mediation.platform.controller;

import com.mediation.platform.dto.response.ApiResponse;
import com.mediation.platform.entity.Utilisateur;
import com.mediation.platform.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasRole('ADMINISTRATEUR')")
@Tag(name = "Administration", description = "API d'administration de la plateforme")
public class AdminController {

    @Autowired
    private UtilisateurService utilisateurService;

    @Autowired
    private AssociationService associationService;

    @Autowired
    private DonateurService donateurService;

    @Autowired
    private ProjetService projetService;

    @Autowired
    private DonService donService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuthenticationService authenticationService;

    /**
     * Dashboard administrateur - Vue d'ensemble
     */
    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard admin", description = "Vue d'ensemble de la plateforme")
    public ResponseEntity<?> getDashboard() {
        try {
            Map<String, Object> dashboard = new HashMap<>();

            // Statistiques générales
            Map<String, Object> utilisateurs = new HashMap<>();
            utilisateurs.put("total", utilisateurService.count());
            utilisateurs.put("donateurs", donateurService.count());
            utilisateurs.put("associations", associationService.count());
            utilisateurs.put("associationsEnAttente", associationService.findPendingValidation().size());
            dashboard.put("utilisateurs", utilisateurs);

            Map<String, Object> projets = new HashMap<>();
            projets.put("total", projetService.count());
            projets.put("actifs", projetService.findActiveProjects().size());
            projets.put("termines", projetService.findByStatut(com.mediation.platform.enums.StatutProjet.TERMINE).size());
            projets.put("enRetard", projetService.findOverdueProjects().size());
            dashboard.put("projets", projets);

            Map<String, Object> dons = new HashMap<>();
            dons.put("total", donService.count());
            dons.put("valides", donService.findValidatedDons().size());
            dons.put("enAttente", donService.findByStatut(com.mediation.platform.enums.StatutDon.EN_ATTENTE).size());
            dons.put("montantTotal", donService.getTotalConfirmedDonations());
            dashboard.put("dons", dons);

            // Statistiques détaillées
            dashboard.put("statsAssociations", associationService.getGeneralStats());
            dashboard.put("statsDonateurs", donateurService.getGeneralStats());
            dashboard.put("statsProjets", projetService.getGeneralStats());
            dashboard.put("statsDons", donService.getGeneralStats());

            // Tendances
            dashboard.put("tendancesProjets", projetService.calculerTendances());
            dashboard.put("tendancesDons", donService.calculerTendances());

            return ResponseEntity.ok(ApiResponse.success("Dashboard admin récupéré", dashboard));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la récupération du dashboard", e.getMessage()));
        }
    }

    /**
     * Gestion des utilisateurs - Vue d'ensemble
     */
    @GetMapping("/utilisateurs")
    @Operation(summary = "Gestion utilisateurs", description = "Liste et statistiques des utilisateurs")
    public ResponseEntity<?> getUtilisateurs() {
        try {
            Map<String, Object> data = new HashMap<>();

            data.put("donateurs", donateurService.findAll());
            data.put("associations", associationService.findAll());
            data.put("associationsEnAttente", associationService.findPendingValidation());

            Map<String, Object> statistiques = new HashMap<>();
            statistiques.put("totalUtilisateurs", utilisateurService.count());
            statistiques.put("donateurs", donateurService.count());
            statistiques.put("associations", associationService.count());
            statistiques.put("associationsValidees", associationService.findValidatedAssociations().size());
            statistiques.put("associationsEnAttente", associationService.findPendingValidation().size());
            data.put("statistiques", statistiques);

            return ResponseEntity.ok(ApiResponse.success("Utilisateurs récupérés", data));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la récupération", e.getMessage()));
        }
    }

    /**
     * Gestion des projets - Vue d'ensemble
     */
    @GetMapping("/projets")
    @Operation(summary = "Gestion projets", description = "Vue d'ensemble des projets")
    public ResponseEntity<?> getProjets() {
        try {
            Map<String, Object> data = new HashMap<>();

            data.put("projetsActifs", projetService.findActiveProjects());
            data.put("projetsTermines", projetService.findByStatut(com.mediation.platform.enums.StatutProjet.TERMINE));
            data.put("projetsEnRetard", projetService.findOverdueProjects());
            data.put("projetsRecents", projetService.findRecentProjects(java.time.LocalDateTime.now().minusDays(30)));
            data.put("topProjets", projetService.findTopProjects());
            data.put("statistiques", projetService.getGeneralStats());

            return ResponseEntity.ok(ApiResponse.success("Projets récupérés", data));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la récupération", e.getMessage()));
        }
    }

    /**
     * Gestion des dons - Vue d'ensemble
     */
    @GetMapping("/dons")
    @Operation(summary = "Gestion dons", description = "Vue d'ensemble des dons")
    public ResponseEntity<?> getDons() {
        try {
            Map<String, Object> data = new HashMap<>();

            data.put("donsEnAttente", donService.findByStatut(com.mediation.platform.enums.StatutDon.EN_ATTENTE));
            data.put("donsValides", donService.findValidatedDons().stream().limit(100).toList());
            data.put("donsRecents", donService.findRecentDonations(java.time.LocalDate.now().minusDays(7)));
            data.put("grosDons", donService.findLargeDonations(1000.0));
            data.put("statistiques", donService.getGeneralStats());

            return ResponseEntity.ok(ApiResponse.success("Dons récupérés", data));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la récupération", e.getMessage()));
        }
    }

    /**
     * Notifications système
     */
    @GetMapping("/notifications")
    @Operation(summary = "Notifications admin", description = "Notifications pour les administrateurs")
    public ResponseEntity<?> getNotifications(@RequestHeader("Authorization") String token) {
        try {
            Utilisateur admin = authenticationService.getCurrentUser(token);

            Map<String, Object> data = new HashMap<>();
            data.put("notifications", notificationService.getNotificationsUtilisateur(admin));
            data.put("nonLues", notificationService.getNotificationsNonLues(admin));
            data.put("nombreNonLues", notificationService.compterNotificationsNonLues(admin));

            return ResponseEntity.ok(ApiResponse.success("Notifications récupérées", data));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la récupération", e.getMessage()));
        }
    }

    /**
     * Rapports et statistiques avancés
     */
    @GetMapping("/rapports")
    @Operation(summary = "Rapports admin", description = "Rapports et statistiques détaillés")
    public ResponseEntity<?> getRapports() {
        try {
            Map<String, Object> rapports = new HashMap<>();

            // Statistiques détaillées par période
            Map<String, Object> performanceGlobale = new HashMap<>();
            performanceGlobale.put("montantTotalCollecte", donService.getTotalConfirmedDonations());
            performanceGlobale.put("nombreDonsTotal", donService.count());
            performanceGlobale.put("nombreProjetsTotaux", projetService.count());
            performanceGlobale.put("tauxReussiteProjets", calculerTauxReussiteProjets());
            performanceGlobale.put("donMoyenParProjet", calculerDonMoyenParProjet());
            rapports.put("performanceGlobale", performanceGlobale);

            // Top performers
            Map<String, Object> topPerformers = new HashMap<>();
            topPerformers.put("topAssociations", associationService.findTopAssociationsByDonations().stream().limit(10).toList());
            topPerformers.put("topDonateurs", donateurService.findTopDonators().stream().limit(10).toList());
            topPerformers.put("topProjets", projetService.findTopProjects().stream().limit(10).toList());
            rapports.put("topPerformers", topPerformers);

            // Analyses de tendances
            Map<String, Object> tendances = new HashMap<>();
            tendances.put("projets", projetService.calculerTendances());
            tendances.put("dons", donService.calculerTendances());
            rapports.put("tendances", tendances);

            return ResponseEntity.ok(ApiResponse.success("Rapports générés", rapports));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la génération des rapports", e.getMessage()));
        }
    }

    /**
     * Actions de maintenance système
     */
    @PostMapping("/maintenance/verifier-projets-expires")
    @Operation(summary = "Vérifier projets expirés", description = "Lance la vérification des projets expirés")
    public ResponseEntity<?> verifierProjetsExpires() {
        try {
            projetService.verifierProjetsExpires();
            return ResponseEntity.ok(ApiResponse.success("Vérification des projets expirés effectuée"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la vérification", e.getMessage()));
        }
    }

    /**
     * Envoyer rappels d'échéance
     */
    @PostMapping("/maintenance/rappels-echeance")
    @Operation(summary = "Envoyer rappels", description = "Envoie les rappels d'échéance aux associations")
    public ResponseEntity<?> envoyerRappelsEcheance() {
        try {
            projetService.envoyerRappelsEcheance();
            return ResponseEntity.ok(ApiResponse.success("Rappels d'échéance envoyés"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de l'envoi des rappels", e.getMessage()));
        }
    }

    /**
     * Marquer toutes les notifications comme lues
     */
    @PostMapping("/notifications/marquer-lues")
    @Operation(summary = "Marquer notifications lues", description = "Marque toutes les notifications comme lues")
    public ResponseEntity<?> marquerNotificationsLues(@RequestHeader("Authorization") String token) {
        try {
            Utilisateur admin = authenticationService.getCurrentUser(token);
            notificationService.marquerToutesCommeLues(admin);
            return ResponseEntity.ok(ApiResponse.success("Notifications marquées comme lues"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la mise à jour", e.getMessage()));
        }
    }

    /**
     * Méthodes utilitaires privées
     */
    private double calculerTauxReussiteProjets() {
        try {
            long totalProjets = projetService.count();
            long projetsTermines = projetService.findByStatut(com.mediation.platform.enums.StatutProjet.TERMINE).size();

            if (totalProjets == 0) return 0.0;
            return (double) projetsTermines / totalProjets * 100;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double calculerDonMoyenParProjet() {
        try {
            double montantTotal = donService.getTotalConfirmedDonations();
            long nombreProjets = projetService.count();

            if (nombreProjets == 0) return 0.0;
            return montantTotal / nombreProjets;
        } catch (Exception e) {
            return 0.0;
        }
    }
}