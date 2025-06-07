package com.mediation.platform.controller;

import com.mediation.platform.dto.response.ApiResponse;
import com.mediation.platform.entity.Association;
import com.mediation.platform.entity.Projet;
import com.mediation.platform.entity.Utilisateur;
import com.mediation.platform.service.AssociationService;
import com.mediation.platform.service.AuthenticationService;
import com.mediation.platform.service.ProjetService;
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
@RequestMapping("/associations")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Associations", description = "API de gestion des associations")
public class AssociationController {

    @Autowired
    private AssociationService associationService;

    @Autowired
    private ProjetService projetService;

    @Autowired
    private AuthenticationService authenticationService;

    /**
     * Lister toutes les associations validées (public)
     */
    @GetMapping
    @Operation(summary = "Liste des associations", description = "Récupère toutes les associations validées")
    public ResponseEntity<?> getAllAssociations() {
        try {
            List<Association> associations = associationService.findValidatedAssociations();
            return ResponseEntity.ok(ApiResponse.success("Associations récupérées avec succès", associations));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la récupération des associations", e.getMessage()));
        }
    }

    /**
     * Détails d'une association (public)
     */
    @GetMapping("/{id}")
    @Operation(summary = "Détails association", description = "Récupère les détails d'une association")
    public ResponseEntity<?> getAssociation(@PathVariable Long id) {
        try {
            Association association = associationService.findById(id);
            return ResponseEntity.ok(ApiResponse.success("Association récupérée avec succès", association));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Association non trouvée", e.getMessage()));
        }
    }

    /**
     * Dashboard de l'association
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ASSOCIATION')")
    @Operation(summary = "Dashboard association", description = "Récupère les données du tableau de bord")
    public ResponseEntity<?> getDashboard(@RequestHeader("Authorization") String token) {
        try {
            Utilisateur utilisateur = authenticationService.getCurrentUser(token);
            Association association = associationService.findById(utilisateur.getIdUtilisateur());

            Map<String, Object> dashboard = new HashMap<>();
            dashboard.put("association", association);
            dashboard.put("statutValidation", association.getStatutValidation());
            dashboard.put("nombreProjets", association.getNombreProjets());
            dashboard.put("nombreProjetsActifs", association.getNombreProjetsActifs());
            dashboard.put("nombreProjetsTermines", association.getNombreProjetsTermines());
            dashboard.put("montantTotalCollecte", association.getMontantTotalCollecte());

            List<Projet> projetsRecents = projetService.findByAssociation(association)
                    .stream().limit(5).toList();
            dashboard.put("projetsRecents", projetsRecents);

            return ResponseEntity.ok(ApiResponse.success("Dashboard récupéré avec succès", dashboard));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la récupération du dashboard", e.getMessage()));
        }
    }

    /**
     * Profil de l'association
     */
    @GetMapping("/profil")
    @PreAuthorize("hasRole('ASSOCIATION')")
    @Operation(summary = "Profil association", description = "Récupère le profil de l'association connectée")
    public ResponseEntity<?> getProfil(@RequestHeader("Authorization") String token) {
        try {
            Utilisateur utilisateur = authenticationService.getCurrentUser(token);
            Association association = associationService.findById(utilisateur.getIdUtilisateur());

            return ResponseEntity.ok(ApiResponse.success("Profil récupéré avec succès", association));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la récupération du profil", e.getMessage()));
        }
    }

    /**
     * Modifier le profil
     */
    @PutMapping("/profil")
    @PreAuthorize("hasRole('ASSOCIATION')")
    @Operation(summary = "Modifier profil", description = "Modifie le profil de l'association connectée")
    public ResponseEntity<?> updateProfil(
            @RequestBody Association associationData,
            @RequestHeader("Authorization") String token) {
        try {
            Utilisateur utilisateur = authenticationService.getCurrentUser(token);
            Association updatedAssociation = associationService.update(utilisateur.getIdUtilisateur(), associationData);

            return ResponseEntity.ok(ApiResponse.success("Profil mis à jour avec succès", updatedAssociation));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la mise à jour", e.getMessage()));
        }
    }

    /**
     * Projets de l'association
     */
    @GetMapping("/projets")
    @PreAuthorize("hasRole('ASSOCIATION')")
    @Operation(summary = "Projets association", description = "Récupère tous les projets de l'association")
    public ResponseEntity<?> getProjetsAssociation(@RequestHeader("Authorization") String token) {
        try {
            Utilisateur utilisateur = authenticationService.getCurrentUser(token);
            Association association = associationService.findById(utilisateur.getIdUtilisateur());
            List<Projet> projets = projetService.findByAssociation(association);

            return ResponseEntity.ok(ApiResponse.success("Projets récupérés avec succès", projets));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la récupération des projets", e.getMessage()));
        }
    }

    /**
     * Rechercher associations par nom
     */
    @GetMapping("/recherche")
    @Operation(summary = "Rechercher associations", description = "Recherche des associations par nom")
    public ResponseEntity<?> rechercherAssociations(@RequestParam String nom) {
        try {
            List<Association> associations = associationService.findByNomAssociation(nom);
            return ResponseEntity.ok(ApiResponse.success("Résultats de recherche", associations));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la recherche", e.getMessage()));
        }
    }

    /**
     * Associations par domaine d'activité
     */
    @GetMapping("/domaine/{domaine}")
    @Operation(summary = "Associations par domaine", description = "Récupère les associations d'un domaine spécifique")
    public ResponseEntity<?> getAssociationsByDomaine(@PathVariable String domaine) {
        try {
            List<Association> associations = associationService.findByDomaineActivite(domaine);
            return ResponseEntity.ok(ApiResponse.success("Associations du domaine", associations));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la récupération", e.getMessage()));
        }
    }

    /**
     * Associations par ville
     */
    @GetMapping("/ville/{ville}")
    @Operation(summary = "Associations par ville", description = "Récupère les associations d'une ville")
    public ResponseEntity<?> getAssociationsByVille(@PathVariable String ville) {
        try {
            List<Association> associations = associationService.findByVille(ville);
            return ResponseEntity.ok(ApiResponse.success("Associations de la ville", associations));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la récupération", e.getMessage()));
        }
    }

    /**
     * Top associations par montant collecté
     */
    @GetMapping("/top")
    @Operation(summary = "Top associations", description = "Associations classées par montant collecté")
    public ResponseEntity<?> getTopAssociations() {
        try {
            List<Association> associations = associationService.findTopAssociationsByDonations();
            return ResponseEntity.ok(ApiResponse.success("Top associations", associations));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la récupération", e.getMessage()));
        }
    }

    /**
     * Associations avec projets actifs
     */
    @GetMapping("/projets-actifs")
    @Operation(summary = "Associations actives", description = "Associations ayant des projets en cours")
    public ResponseEntity<?> getAssociationsWithActiveProjects() {
        try {
            List<Association> associations = associationService.findWithActiveProjects();
            return ResponseEntity.ok(ApiResponse.success("Associations avec projets actifs", associations));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la récupération", e.getMessage()));
        }
    }

    /**
     * Associations récemment validées
     */
    @GetMapping("/recentes")
    @Operation(summary = "Associations récentes", description = "Associations validées récemment")
    public ResponseEntity<?> getRecentAssociations(
            @RequestParam(defaultValue = "30") int jours) {
        try {
            List<Association> associations = associationService.findRecentlyValidated(jours);
            return ResponseEntity.ok(ApiResponse.success("Associations récentes", associations));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la récupération", e.getMessage()));
        }
    }

    /**
     * Statistiques des associations (admin)
     */
    @GetMapping("/statistiques")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Statistiques associations", description = "Statistiques globales des associations")
    public ResponseEntity<?> getStatistiquesAssociations() {
        try {
            AssociationService.AssociationStats stats = associationService.getGeneralStats();
            return ResponseEntity.ok(ApiResponse.success("Statistiques des associations", stats));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la récupération des statistiques", e.getMessage()));
        }
    }

    // ========== ENDPOINTS ADMIN ==========

    /**
     * Toutes les associations (admin)
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Toutes les associations", description = "Liste complète des associations (admin)")
    public ResponseEntity<?> getAllAssociationsAdmin() {
        try {
            List<Association> associations = associationService.findAll();
            return ResponseEntity.ok(ApiResponse.success("Toutes les associations récupérées", associations));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la récupération", e.getMessage()));
        }
    }

    /**
     * Associations en attente de validation (admin)
     */
    @GetMapping("/admin/en-attente")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Associations en attente", description = "Associations en attente de validation")
    public ResponseEntity<?> getAssociationsEnAttente() {
        try {
            List<Association> associations = associationService.findPendingValidation();
            return ResponseEntity.ok(ApiResponse.success("Associations en attente", associations));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la récupération", e.getMessage()));
        }
    }

    /**
     * Valider une association (admin)
     */
    @PostMapping("/admin/{id}/valider")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Valider association", description = "Valide une association en attente")
    public ResponseEntity<?> validerAssociation(@PathVariable Long id) {
        try {
            Association association = associationService.validerAssociation(id);
            return ResponseEntity.ok(ApiResponse.success("Association validée avec succès", association));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la validation", e.getMessage()));
        }
    }

    /**
     * Rejeter une association (admin)
     */
    @PostMapping("/admin/{id}/rejeter")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Rejeter association", description = "Rejette une association en attente")
    public ResponseEntity<?> rejeterAssociation(
            @PathVariable Long id,
            @RequestParam(required = false) String motif) {
        try {
            Association association = associationService.rejeterAssociation(id, motif);
            return ResponseEntity.ok(ApiResponse.success("Association rejetée", association));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors du rejet", e.getMessage()));
        }
    }

    /**
     * Suspendre une association (admin)
     */
    @PostMapping("/admin/{id}/suspendre")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Suspendre association", description = "Suspend une association")
    public ResponseEntity<?> suspendreAssociation(
            @PathVariable Long id,
            @RequestParam(required = false) String motif) {
        try {
            Association association = associationService.suspendreAssociation(id, motif);
            return ResponseEntity.ok(ApiResponse.success("Association suspendue", association));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la suspension", e.getMessage()));
        }
    }

    /**
     * Réactiver une association (admin)
     */
    @PostMapping("/admin/{id}/reactiver")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Réactiver association", description = "Réactive une association suspendue")
    public ResponseEntity<?> reactiverAssociation(@PathVariable Long id) {
        try {
            Association association = associationService.reactiverAssociation(id);
            return ResponseEntity.ok(ApiResponse.success("Association réactivée", association));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la réactivation", e.getMessage()));
        }
    }

    /**
     * Supprimer une association (admin)
     */
    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Supprimer association", description = "Supprime définitivement une association")
    public ResponseEntity<?> supprimerAssociation(@PathVariable Long id) {
        try {
            associationService.deleteById(id);
            return ResponseEntity.ok(ApiResponse.success("Association supprimée avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la suppression", e.getMessage()));
        }
    }
}