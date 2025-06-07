package com.mediation.platform.controller;

import com.mediation.platform.dto.response.ApiResponse;
import com.mediation.platform.entity.Don;
import com.mediation.platform.entity.Donateur;
import com.mediation.platform.entity.Projet;
import com.mediation.platform.entity.Utilisateur;
import com.mediation.platform.enums.StatutDon;
import com.mediation.platform.service.AuthenticationService;
import com.mediation.platform.service.DonService;
import com.mediation.platform.service.ProjetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dons")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Dons", description = "API de gestion des dons")
public class DonController {

    @Autowired
    private DonService donService;

    @Autowired
    private ProjetService projetService;

    @Autowired
    private AuthenticationService authenticationService;

    /**
     * Créer un nouveau don
     */
    @PostMapping
    @PreAuthorize("hasRole('DONATEUR')")
    @Operation(summary = "Créer un don", description = "Effectuer un don pour un projet")
    public ResponseEntity<?> creerDon(
            @Valid @RequestBody CreateDonRequest donRequest,
            @RequestHeader("Authorization") String token) {
        try {
            Utilisateur utilisateur = authenticationService.getCurrentUser(token);

            if (!(utilisateur instanceof Donateur)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Seuls les donateurs peuvent effectuer des dons"));
            }

            Donateur donateur = (Donateur) utilisateur;
            Projet projet = projetService.findById(donRequest.getIdProjet());

            // Vérifier que le projet peut recevoir des dons
            if (!projet.peutRecevoirDons()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Ce projet ne peut plus recevoir de dons"));
            }

            // Créer le don
            Don don = new Don();
            don.setMontant(donRequest.getMontant());
            don.setMessage(donRequest.getMessage());
            don.setAnonyme(donRequest.getAnonyme() != null ? donRequest.getAnonyme() : false);
            don.setDonateur(donateur);
            don.setProjet(projet);

            Don savedDon = donService.save(don);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Don créé avec succès", savedDon));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la création du don", e.getMessage()));
        }
    }

    /**
     * Lister tous les dons (admin seulement)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Liste des dons", description = "Récupère tous les dons (admin seulement)")
    public ResponseEntity<?> getAllDons() {
        try {
            List<Don> dons = donService.findAll();
            return ResponseEntity.ok(ApiResponse.success("Dons récupérés avec succès", dons));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la récupération des dons", e.getMessage()));
        }
    }

    /**
     * Détails d'un don
     */
    @GetMapping("/{id}")
    @Operation(summary = "Détails d'un don", description = "Récupère les détails d'un don spécifique")
    public ResponseEntity<?> getDon(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        try {
            Utilisateur utilisateur = authenticationService.getCurrentUser(token);
            Don don = donService.findById(id);

            // Vérifier les permissions
            boolean estProprietaire = don.getDonateur().getIdUtilisateur().equals(utilisateur.getIdUtilisateur());
            boolean estAssociation = don.getProjet().getAssociation().getIdUtilisateur().equals(utilisateur.getIdUtilisateur());
            boolean estAdmin = "ADMINISTRATEUR".equals(utilisateur.getRole().name());

            if (!estProprietaire && !estAssociation && !estAdmin) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Accès refusé"));
            }

            return ResponseEntity.ok(ApiResponse.success("Don récupéré avec succès", don));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Don non trouvé", e.getMessage()));
        }
    }

    /**
     * Dons par projet
     */
    @GetMapping("/projet/{projetId}")
    @Operation(summary = "Dons par projet", description = "Récupère tous les dons d'un projet")
    public ResponseEntity<?> getDonsByProjet(@PathVariable Long projetId) {
        try {
            Projet projet = projetService.findById(projetId);
            List<Don> dons = donService.findByProjet(projet);
            return ResponseEntity.ok(ApiResponse.success("Dons du projet récupérés", dons));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la récupération", e.getMessage()));
        }
    }

    /**
     * Dons par statut (admin seulement)
     */
    @GetMapping("/statut/{statut}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Dons par statut", description = "Récupère les dons selon leur statut")
    public ResponseEntity<?> getDonsByStatut(@PathVariable String statut) {
        try {
            StatutDon statutDon = StatutDon.valueOf(statut.toUpperCase());
            List<Don> dons = donService.findByStatut(statutDon);
            return ResponseEntity.ok(ApiResponse.success("Dons récupérés avec succès", dons));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Statut invalide", "Statuts valides: EN_ATTENTE, VALIDE, REFUSE, ANNULE, REMBOURSE"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la récupération", e.getMessage()));
        }
    }

    /**
     * Valider un don (admin seulement)
     */
    @PostMapping("/{id}/valider")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Valider un don", description = "Valide un don en attente")
    public ResponseEntity<?> validerDon(@PathVariable Long id) {
        try {
            Don don = donService.validerDon(id);
            return ResponseEntity.ok(ApiResponse.success("Don validé avec succès", don));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la validation", e.getMessage()));
        }
    }

    /**
     * Rejeter un don (admin seulement)
     */
    @PostMapping("/{id}/rejeter")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Rejeter un don", description = "Rejette un don en attente")
    public ResponseEntity<?> rejeterDon(@PathVariable Long id) {
        try {
            Don don = donService.rejeterDon(id);
            return ResponseEntity.ok(ApiResponse.success("Don rejeté", don));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors du rejet", e.getMessage()));
        }
    }

    /**
     * Annuler un don (donateur seulement, si en attente)
     */
    @PostMapping("/{id}/annuler")
    @PreAuthorize("hasRole('DONATEUR')")
    @Operation(summary = "Annuler un don", description = "Annule un don en attente")
    public ResponseEntity<?> annulerDon(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        try {
            Utilisateur utilisateur = authenticationService.getCurrentUser(token);
            Don don = donService.findById(id);

            // Vérifier que l'utilisateur est le propriétaire du don
            if (!don.getDonateur().getIdUtilisateur().equals(utilisateur.getIdUtilisateur())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Vous ne pouvez annuler que vos propres dons"));
            }

            // Vérifier que le don peut être annulé
            if (!don.peutEtreAnnule()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Ce don ne peut plus être annulé"));
            }

            donService.deleteById(id);
            return ResponseEntity.ok(ApiResponse.success("Don annulé avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de l'annulation", e.getMessage()));
        }
    }

    /**
     * Dons récents
     */
    @GetMapping("/recents")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Dons récents", description = "Dons des 7 derniers jours")
    public ResponseEntity<?> getDonsRecents() {
        try {
            java.time.LocalDate dateDebut = java.time.LocalDate.now().minusDays(7);
            List<Don> dons = donService.findRecentDonations(dateDebut);
            return ResponseEntity.ok(ApiResponse.success("Dons récents", dons));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la récupération", e.getMessage()));
        }
    }

    /**
     * Gros dons
     */
    @GetMapping("/gros-dons")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Gros dons", description = "Dons supérieurs à un montant donné")
    public ResponseEntity<?> getGrosDons(@RequestParam(defaultValue = "1000") Double montantMin) {
        try {
            List<Don> dons = donService.findLargeDonations(montantMin);
            return ResponseEntity.ok(ApiResponse.success("Gros dons récupérés", dons));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la récupération", e.getMessage()));
        }
    }

    /**
     * Statistiques des dons
     */
    @GetMapping("/statistiques")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Statistiques dons", description = "Statistiques globales des dons")
    public ResponseEntity<?> getStatistiquesDons() {
        try {
            java.util.Map<String, Object> stats = new java.util.HashMap<>();
            stats.put("totalDons", donService.findAll().size());
            stats.put("montantTotal", donService.getTotalConfirmedDonations());
            stats.put("donateursUniques", donService.getUniqueDonorsCount());
            stats.put("donsValides", donService.findValidatedDons().size());
            stats.put("donsEnAttente", donService.findByStatut(StatutDon.EN_ATTENTE).size());
            stats.put("donsAnonymes", donService.findAnonymousDons().size());

            return ResponseEntity.ok(ApiResponse.success("Statistiques des dons", stats));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la récupération des statistiques", e.getMessage()));
        }
    }

    /**
     * DTO pour la création d'un don
     */
    public static class CreateDonRequest {
        private Double montant;
        private String message;
        private Boolean anonyme;
        private Long idProjet;

        // Getters et setters
        public Double getMontant() { return montant; }
        public void setMontant(Double montant) { this.montant = montant; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public Boolean getAnonyme() { return anonyme; }
        public void setAnonyme(Boolean anonyme) { this.anonyme = anonyme; }

        public Long getIdProjet() { return idProjet; }
        public void setIdProjet(Long idProjet) { this.idProjet = idProjet; }
    }
}