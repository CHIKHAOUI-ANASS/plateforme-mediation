package com.mediation.platform.controller;

import com.mediation.platform.dto.response.ApiResponse;
import com.mediation.platform.entity.Association;
import com.mediation.platform.entity.Projet;
import com.mediation.platform.entity.Utilisateur;
import com.mediation.platform.enums.StatutProjet;
import com.mediation.platform.service.AuthenticationService;
import com.mediation.platform.service.ProjetService;

import java.util.HashMap;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/projets")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Projets", description = "API de gestion des projets")
public class ProjetController {

    @Autowired
    private ProjetService projetService;

    @Autowired
    private AuthenticationService authenticationService;

    /**
     * Lister tous les projets (public)
     */
    @GetMapping
    @Operation(summary = "Liste des projets", description = "Récupère tous les projets actifs")
    public ResponseEntity<ApiResponse<List<Projet>>> getAllProjets() {
        try {
            List<Projet> projets = projetService.findActiveProjects();
            return ResponseEntity.ok(ApiResponse.success("Projets récupérés avec succès", projets));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la récupération des projets", e.getMessage()));
        }
    }

    /**
     * Détails d'un projet (public)
     */
    @GetMapping("/{id}")
    @Operation(summary = "Détails projet", description = "Récupère les détails d'un projet")
    public ResponseEntity<ApiResponse<Projet>> getProjet(@PathVariable Long id) {
        try {
            Projet projet = projetService.findById(id);
            return ResponseEntity.ok(ApiResponse.success("Projet récupéré avec succès", projet));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Projet non trouvé", e.getMessage()));
        }
    }

    /**
     * Créer un projet (associations uniquement)
     */
    @PostMapping
    @PreAuthorize("hasRole('ASSOCIATION')")
    @Operation(summary = "Créer projet", description = "Créer un nouveau projet (associations seulement)")
    public ResponseEntity<ApiResponse<Projet>> creerProjet(
            @Valid @RequestBody Projet projetData,
            @RequestHeader("Authorization") String token) {
        try {
            Utilisateur utilisateur = authenticationService.getCurrentUser(token);

            if (!(utilisateur instanceof Association)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Seules les associations peuvent créer des projets"));
            }

            Association association = (Association) utilisateur;

            // Vérifier que l'association est validée
            if (!association.estValidee()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Votre association doit être validée pour créer des projets"));
            }

            // Associer le projet à l'association
            projetData.setAssociation(association);

            Projet nouveauProjet = projetService.save(projetData);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Projet créé avec succès", nouveauProjet));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la création du projet", e.getMessage()));
        }
    }

    /**
     * Modifier un projet (propriétaire uniquement)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ASSOCIATION')")
    @Operation(summary = "Modifier projet", description = "Modifie un projet existant")
    public ResponseEntity<ApiResponse<Projet>> modifierProjet(
            @PathVariable Long id,
            @Valid @RequestBody Projet projetData,
            @RequestHeader("Authorization") String token) {
        try {
            Utilisateur utilisateur = authenticationService.getCurrentUser(token);
            Projet projetExistant = projetService.findById(id);

            // Vérifier que l'utilisateur est le propriétaire du projet
            if (!projetExistant.getAssociation().getIdUtilisateur().equals(utilisateur.getIdUtilisateur())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Vous n'êtes pas autorisé à modifier ce projet"));
            }

            Projet projetModifie = projetService.update(id, projetData);

            return ResponseEntity.ok(ApiResponse.success("Projet modifié avec succès", projetModifie));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la modification", e.getMessage()));
        }
    }

    /**
     * Supprimer un projet (propriétaire uniquement)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ASSOCIATION')")
    @Operation(summary = "Supprimer projet", description = "Supprime un projet (le marque comme annulé)")
    public ResponseEntity<ApiResponse<String>> supprimerProjet(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        try {
            Utilisateur utilisateur = authenticationService.getCurrentUser(token);
            Projet projet = projetService.findById(id);

            // Vérifier que l'utilisateur est le propriétaire du projet
            if (!projet.getAssociation().getIdUtilisateur().equals(utilisateur.getIdUtilisateur())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Vous n'êtes pas autorisé à supprimer ce projet"));
            }

            projetService.deleteById(id);

            return ResponseEntity.ok(ApiResponse.success("Projet supprimé avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la suppression", e.getMessage()));
        }
    }

    /**
     * Rechercher des projets
     */
    @GetMapping("/recherche")
    @Operation(summary = "Rechercher projets", description = "Recherche des projets par mot-clé")
    public ResponseEntity<ApiResponse<List<Projet>>> rechercherProjets(@RequestParam String keyword) {
        try {
            List<Projet> projets = projetService.searchByKeyword(keyword);
            return ResponseEntity.ok(ApiResponse.success("Résultats de recherche", projets));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la recherche", e.getMessage()));
        }
    }

    /**
     * Projets par statut
     */
    @GetMapping("/statut/{statut}")
    @Operation(summary = "Projets par statut", description = "Récupère les projets selon leur statut")
    public ResponseEntity<ApiResponse<List<Projet>>> getProjetsByStatut(@PathVariable String statut) {
        try {
            StatutProjet statutProjet = StatutProjet.valueOf(statut.toUpperCase());
            List<Projet> projets = projetService.findByStatut(statutProjet);
            return ResponseEntity.ok(ApiResponse.success("Projets récupérés avec succès", projets));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Statut invalide", "Statuts valides: EN_COURS, TERMINE, ANNULE, SUSPENDU"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la récupération", e.getMessage()));
        }
    }

    /**
     * Projets proches de l'objectif
     */
    @GetMapping("/proche-objectif")
    @Operation(summary = "Projets proches objectif", description = "Projets ayant atteint au moins 90% de leur objectif")
    public ResponseEntity<ApiResponse<List<Projet>>> getProjetsProchesObjectif() {
        try {
            List<Projet> projets = projetService.findNearGoal(0.9);
            return ResponseEntity.ok(ApiResponse.success("Projets proches de l'objectif", projets));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la récupération", e.getMessage()));
        }
    }

    /**
     * Projets en retard
     */
    @GetMapping("/en-retard")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Projets en retard", description = "Projets ayant dépassé leur date limite")
    public ResponseEntity<ApiResponse<List<Projet>>> getProjetsEnRetard() {
        try {
            List<Projet> projets = projetService.findOverdueProjects();
            return ResponseEntity.ok(ApiResponse.success("Projets en retard", projets));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la récupération", e.getMessage()));
        }
    }

    /**
     * Projets récents
     */
    @GetMapping("/recents")
    @Operation(summary = "Projets récents", description = "Projets créés dans les 30 derniers jours")
    public ResponseEntity<ApiResponse<List<Projet>>> getProjetsRecents() {
        try {
            LocalDateTime dateDebut = LocalDateTime.now().minusDays(30);
            List<Projet> projets = projetService.findRecentProjects(dateDebut);
            return ResponseEntity.ok(ApiResponse.success("Projets récents", projets));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la récupération", e.getMessage()));
        }
    }

    /**
     * Top projets (par montant collecté)
     */
    @GetMapping("/top")
    @Operation(summary = "Top projets", description = "Projets classés par montant collecté")
    public ResponseEntity<ApiResponse<List<Projet>>> getTopProjets() {
        try {
            List<Projet> projets = projetService.findTopProjects();
            return ResponseEntity.ok(ApiResponse.success("Top projets", projets));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la récupération", e.getMessage()));
        }
    }

    /**
     * Statistiques d'un projet
     */
    @GetMapping("/{id}/statistiques")
    @Operation(summary = "Statistiques projet", description = "Statistiques détaillées d'un projet")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistiquesProjet(@PathVariable Long id) {
        try {
            Projet projet = projetService.findById(id);

            Map<String, Object> stats = new HashMap<>();
            stats.put("montantDemande", projet.getMontantDemande());
            stats.put("montantCollecte", projet.getMontantCollecte());
            stats.put("progres", projet.calculerProgres());
            stats.put("montantRestant", projet.getMontantRestant());
            stats.put("nombreDons", projet.getDons() != null ? projet.getDons().size() : 0);
            stats.put("nombreDonateurs", projet.getNombreDonateurs());
            stats.put("statut", projet.getStatut());
            stats.put("estTermine", projet.estTermine());
            stats.put("estEnRetard", projet.estEnRetard());
            stats.put("joursRestants", projet.getJoursRestants());
            stats.put("peutRecevoirDons", projet.peutRecevoirDons());

            return ResponseEntity.ok(ApiResponse.success("Statistiques du projet", stats));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la récupération des statistiques", e.getMessage()));
        }
    }

    /**
     * Marquer un projet comme terminé (propriétaire ou admin)
     */
    @PostMapping("/{id}/terminer")
    @PreAuthorize("hasRole('ASSOCIATION') or hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Terminer projet", description = "Marque un projet comme terminé")
    public ResponseEntity<ApiResponse<String>> terminerProjet(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        try {
            Utilisateur utilisateur = authenticationService.getCurrentUser(token);
            Projet projet = projetService.findById(id);

            // Vérifier les permissions
            boolean estProprietaire = projet.getAssociation().getIdUtilisateur().equals(utilisateur.getIdUtilisateur());
            boolean estAdmin = "ADMINISTRATEUR".equals(utilisateur.getRole().name());

            if (!estProprietaire && !estAdmin) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Vous n'êtes pas autorisé à terminer ce projet"));
            }

            projetService.marquerCommeTermine(id);

            return ResponseEntity.ok(ApiResponse.success("Projet marqué comme terminé"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la finalisation", e.getMessage()));
        }
    }
}