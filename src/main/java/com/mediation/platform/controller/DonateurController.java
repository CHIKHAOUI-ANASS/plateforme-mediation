package com.mediation.platform.controller;

import com.mediation.platform.dto.response.ApiResponse;
import com.mediation.platform.entity.Don;
import com.mediation.platform.entity.Donateur;
import com.mediation.platform.entity.Utilisateur;
import com.mediation.platform.service.AuthenticationService;
import com.mediation.platform.service.DonateurService;
import com.mediation.platform.service.DonService;

import java.util.HashMap;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/donateur")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Donateur", description = "API pour les donateurs")
public class DonateurController {

    @Autowired
    private DonateurService donateurService;

    @Autowired
    private DonService donService;

    @Autowired
    private AuthenticationService authenticationService;

    /**
     * Dashboard du donateur
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('DONATEUR')")
    @Operation(summary = "Dashboard donateur", description = "Récupère les statistiques du donateur connecté")
    public ResponseEntity<?> getDashboard(@RequestHeader("Authorization") String token) {
        try {
            Utilisateur utilisateur = authenticationService.getCurrentUser(token);
            Donateur donateur = donateurService.findById(utilisateur.getIdUtilisateur());

            // Créer les statistiques du dashboard
            Map<String, Object> dashboard = new HashMap<>();
            dashboard.put("donateur", donateur);
            dashboard.put("nombreDons", donateur.getDons() != null ? donateur.getDons().size() : 0);
            dashboard.put("montantTotal", donateur.getMontantTotalDons());
            dashboard.put("niveauDonateur", donateur.getNiveauDonateur());
            dashboard.put("projetsSOUTENUS", donateur.getNombreProjetsSoutenus());
            dashboard.put("dernierDon", donateur.getDernierDon());

            return ResponseEntity.ok(ApiResponse.success("Dashboard récupéré avec succès", dashboard));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la récupération du dashboard", e.getMessage()));
        }
    }

    /**
     * Profil du donateur
     */
    @GetMapping("/profil")
    @PreAuthorize("hasRole('DONATEUR')")
    @Operation(summary = "Profil donateur", description = "Récupère le profil du donateur connecté")
    public ResponseEntity<?> getProfil(@RequestHeader("Authorization") String token) {
        try {
            Utilisateur utilisateur = authenticationService.getCurrentUser(token);
            Donateur donateur = donateurService.findById(utilisateur.getIdUtilisateur());

            return ResponseEntity.ok(ApiResponse.success("Profil récupéré avec succès", donateur));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la récupération du profil", e.getMessage()));
        }
    }

    /**
     * Modifier le profil
     */
    @PutMapping("/profil")
    @PreAuthorize("hasRole('DONATEUR')")
    @Operation(summary = "Modifier profil", description = "Modifie le profil du donateur connecté")
    public ResponseEntity<?> updateProfil(
            @RequestBody Donateur donateurData,
            @RequestHeader("Authorization") String token) {
        try {
            Utilisateur utilisateur = authenticationService.getCurrentUser(token);
            Donateur updatedDonateur = donateurService.update(utilisateur.getIdUtilisateur(), donateurData);

            return ResponseEntity.ok(ApiResponse.success("Profil mis à jour avec succès", updatedDonateur));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la mise à jour", e.getMessage()));
        }
    }

    /**
     * Historique des dons
     */
    @GetMapping("/dons")
    @PreAuthorize("hasRole('DONATEUR')")
    @Operation(summary = "Historique des dons", description = "Récupère l'historique des dons du donateur")
    public ResponseEntity<?> getHistoriqueDons(@RequestHeader("Authorization") String token) {
        try {
            Utilisateur utilisateur = authenticationService.getCurrentUser(token);
            Donateur donateur = donateurService.findById(utilisateur.getIdUtilisateur());
            List<Don> dons = donService.findByDonateur(donateur);

            return ResponseEntity.ok(ApiResponse.success("Historique récupéré avec succès", dons));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la récupération de l'historique", e.getMessage()));
        }
    }

    /**
     * Statistiques personnelles
     */
    @GetMapping("/statistiques")
    @PreAuthorize("hasRole('DONATEUR')")
    @Operation(summary = "Statistiques donateur", description = "Récupère les statistiques détaillées du donateur")
    public ResponseEntity<?> getStatistiques(@RequestHeader("Authorization") String token) {
        try {
            Utilisateur utilisateur = authenticationService.getCurrentUser(token);
            Donateur donateur = donateurService.findById(utilisateur.getIdUtilisateur());

            Map<String, Object> stats = new HashMap<>();
            stats.put("montantTotal", donateur.getMontantTotalDons());
            stats.put("nombreDons", donateur.getDons() != null ? donateur.getDons().size() : 0);
            stats.put("montantMoyen", donateur.getMontantMoyenParDon());
            stats.put("projetsSOUTENUS", donateur.getNombreProjetsSoutenus());
            stats.put("associationsSoutenues", donateur.getNombreAssociationsSoutenues());
            stats.put("niveau", donateur.getNiveauDonateur());
            stats.put("estActif", donateur.estDonateurActif());
            stats.put("estRegulier", donateur.estDonateurRegulier());
            stats.put("plusGrosDon", donateur.getDonLePlusImportant());
            stats.put("premierDon", donateur.getDatePremierDon());
            stats.put("dernierDon", donateur.getDateDernierDon());

            return ResponseEntity.ok(ApiResponse.success("Statistiques récupérées avec succès", stats));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la récupération des statistiques", e.getMessage()));
        }
    }

    /**
     * Rapport personnel
     */
    @GetMapping("/rapport")
    @PreAuthorize("hasRole('DONATEUR')")
    @Operation(summary = "Rapport personnel", description = "Génère un rapport personnalisé pour le donateur")
    public ResponseEntity<?> genererRapport(@RequestHeader("Authorization") String token) {
        try {
            Utilisateur utilisateur = authenticationService.getCurrentUser(token);
            Donateur donateur = donateurService.findById(utilisateur.getIdUtilisateur());

            String rapport = donateur.genererRapportPersonnel();

            return ResponseEntity.ok(ApiResponse.success("Rapport généré avec succès", rapport));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la génération du rapport", e.getMessage()));
        }
    }
}