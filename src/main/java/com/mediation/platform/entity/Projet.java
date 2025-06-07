package com.mediation.platform.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mediation.platform.enums.StatutProjet;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "projets")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Projet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idProjet;

    @NotBlank(message = "Le titre est obligatoire")
    @Size(max = 200, message = "Le titre ne peut dépasser 200 caractères")
    @Column(nullable = false, length = 200)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Size(max = 500, message = "L'objectif ne peut dépasser 500 caractères")
    @Column(length = 500)
    private String objectif;

    @NotNull(message = "Le montant demandé est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant demandé doit être positif")
    @Column(nullable = false)
    private Double montantDemande;

    @Column(nullable = false)
    private Double montantCollecte = 0.0;

    private LocalDate dateDebut;

    private LocalDate dateFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutProjet statut = StatutProjet.EN_COURS;

    @Size(max = 50, message = "La priorité ne peut dépasser 50 caractères")
    @Column(length = 50)
    private String priorite;

    @Size(max = 500, message = "Les images ne peuvent dépasser 500 caractères")
    @Column(length = 500)
    private String images;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    private LocalDateTime dateModification;

    // Relations - CORRECTION: Gestion des références circulaires
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_association", nullable = false)
    @JsonIgnoreProperties({"projets", "motDePasse"}) // Ignore les champs qui peuvent créer des boucles
    private Association association;

    @OneToMany(mappedBy = "projet", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore // Pour éviter les boucles infinies dans la sérialisation JSON
    private List<Don> dons = new ArrayList<>();

    // Constructeurs
    public Projet() {}

    public Projet(String titre, String description, Double montantDemande, Association association) {
        this.titre = titre;
        this.description = description;
        this.montantDemande = montantDemande;
        this.association = association;
        this.dateDebut = LocalDate.now();
    }

    public Projet(String titre, String description, String objectif, Double montantDemande,
                  Association association, LocalDate dateDebut, LocalDate dateFin) {
        this.titre = titre;
        this.description = description;
        this.objectif = objectif;
        this.montantDemande = montantDemande;
        this.association = association;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
    }

    // Getters et Setters
    public Long getIdProjet() {
        return idProjet;
    }

    public void setIdProjet(Long idProjet) {
        this.idProjet = idProjet;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getObjectif() {
        return objectif;
    }

    public void setObjectif(String objectif) {
        this.objectif = objectif;
    }

    public Double getMontantDemande() {
        return montantDemande;
    }

    public void setMontantDemande(Double montantDemande) {
        this.montantDemande = montantDemande;
    }

    public Double getMontantCollecte() {
        return montantCollecte;
    }

    public void setMontantCollecte(Double montantCollecte) {
        this.montantCollecte = montantCollecte;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }

    public StatutProjet getStatut() {
        return statut;
    }

    public void setStatut(StatutProjet statut) {
        this.statut = statut;
    }

    public String getPriorite() {
        return priorite;
    }

    public void setPriorite(String priorite) {
        this.priorite = priorite;
    }

    public String getImages() {
        return images;
    }

    public void setImages(String images) {
        this.images = images;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public LocalDateTime getDateModification() {
        return dateModification;
    }

    public void setDateModification(LocalDateTime dateModification) {
        this.dateModification = dateModification;
    }

    public Association getAssociation() {
        return association;
    }

    public void setAssociation(Association association) {
        this.association = association;
    }

    public List<Don> getDons() {
        return dons;
    }

    public void setDons(List<Don> dons) {
        this.dons = dons;
    }

    // AJOUT: Méthodes pour exposer des informations dérivées sans les relations complètes
    public String getNomAssociation() {
        return association != null ? association.getNomAssociation() : null;
    }

    public String getDomaineActivite() {
        return association != null ? association.getDomaineActivite() : null;
    }

    public Long getIdAssociation() {
        return association != null ? association.getIdUtilisateur() : null;
    }

    public int getNombreDons() {
        return dons != null ? dons.size() : 0;
    }

    public int getNombreDonsValides() {
        if (dons == null) return 0;
        return (int) dons.stream()
                .filter(don -> "VALIDE".equals(don.getStatut().name()))
                .count();
    }

    // Méthodes métier
    public void ajouterProjet() {
        this.statut = StatutProjet.EN_COURS;
        this.dateDebut = LocalDate.now();
    }

    public void modifierProjet(String nouveauTitre, String nouvelleDescription, String nouvelObjectif) {
        if (nouveauTitre != null && !nouveauTitre.trim().isEmpty()) {
            this.titre = nouveauTitre;
        }
        if (nouvelleDescription != null) {
            this.description = nouvelleDescription;
        }
        if (nouvelObjectif != null) {
            this.objectif = nouvelObjectif;
        }
    }

    public Double calculerProgres() {
        if (montantDemande == null || montantDemande == 0) {
            return 0.0;
        }
        return Math.round((montantCollecte / montantDemande) * 100 * 100.0) / 100.0; // Arrondi à 2 décimales
    }

    public void ajouterDon(Don don) {
        if (don != null && this.statut == StatutProjet.EN_COURS) {
            this.dons.add(don);
            this.montantCollecte += don.getMontant();

            // Vérifier si l'objectif est atteint
            if (this.montantCollecte >= this.montantDemande) {
                this.statut = StatutProjet.TERMINE;
            }
        }
    }

    public boolean estActif() {
        return this.statut == StatutProjet.EN_COURS;
    }

    public boolean estTermine() {
        return this.statut == StatutProjet.TERMINE ||
                (this.montantCollecte >= this.montantDemande);
    }

    public boolean estEnRetard() {
        return this.dateFin != null &&
                LocalDate.now().isAfter(this.dateFin) &&
                !estTermine();
    }

    public Double getMontantRestant() {
        return Math.max(0, montantDemande - montantCollecte);
    }

    public int getNombreDonateurs() {
        return (int) dons.stream()
                .map(Don::getDonateur)
                .distinct()
                .count();
    }

    // AJOUT: Méthodes utilitaires pour l'API
    public boolean peutRecevoirDons() {
        return this.estActif() &&
                (this.dateFin == null || !LocalDate.now().isAfter(this.dateFin)) &&
                this.association != null && this.association.estValidee();
    }

    public String getStatutAffichage() {
        if (estTermine()) {
            return "Objectif atteint";
        } else if (estEnRetard()) {
            return "En retard";
        } else if (estActif()) {
            return "En cours";
        } else {
            return statut.getLibelle();
        }
    }

    public String getCouleurStatut() {
        if (estTermine()) {
            return "success";
        } else if (estEnRetard()) {
            return "danger";
        } else if (estActif()) {
            return "primary";
        } else {
            return "secondary";
        }
    }

    // AJOUT: Calculs de temps
    public Long getJoursRestants() {
        if (dateFin == null) return null;
        LocalDate maintenant = LocalDate.now();
        if (maintenant.isAfter(dateFin)) return 0L;
        return dateFin.toEpochDay() - maintenant.toEpochDay();
    }

    public Double getPourcentageTempsEcoule() {
        if (dateDebut == null || dateFin == null) return null;

        LocalDate maintenant = LocalDate.now();
        long totalJours = dateFin.toEpochDay() - dateDebut.toEpochDay();
        long joursEcoules = maintenant.toEpochDay() - dateDebut.toEpochDay();

        if (totalJours <= 0) return 100.0;

        double pourcentage = (joursEcoules * 100.0) / totalJours;
        return Math.min(100.0, Math.max(0.0, pourcentage));
    }

    // equals et hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Projet projet = (Projet) o;
        return Objects.equals(idProjet, projet.idProjet) &&
                Objects.equals(titre, projet.titre);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idProjet, titre);
    }

    // toString
    @Override
    public String toString() {
        return "Projet{" +
                "idProjet=" + idProjet +
                ", titre='" + titre + '\'' +
                ", montantDemande=" + montantDemande +
                ", montantCollecte=" + montantCollecte +
                ", statut=" + statut +
                ", association=" + (association != null ? association.getNomAssociation() : "null") +
                ", progres=" + calculerProgres() + "%" +
                '}';
    }
}