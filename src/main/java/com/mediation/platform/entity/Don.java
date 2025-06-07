package com.mediation.platform.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mediation.platform.enums.StatutDon;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "dons")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Don {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idDon;

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant doit être positif")
    @Column(nullable = false)
    private Double montant;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDate date;

    @Size(max = 500, message = "Le message ne peut dépasser 500 caractères")
    @Column(length = 500)
    private String message;

    @Column(nullable = false)
    private Boolean anonyme = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutDon statut = StatutDon.EN_ATTENTE;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dateCreation;

    // Relations - CORRECTION: Gestion des références circulaires
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_donateur", nullable = false)
    @JsonIgnoreProperties({"dons", "motDePasse", "adresse", "dateNaissance", "profession"})
    private Donateur donateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_projet", nullable = false)
    @JsonIgnoreProperties({"dons", "association.projets", "association.motDePasse"})
    private Projet projet;

    @OneToOne(mappedBy = "don", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"don"})
    private Transaction transaction;

    // Constructeurs
    public Don() {}

    public Don(Double montant, Donateur donateur, Projet projet) {
        this.montant = montant;
        this.donateur = donateur;
        this.projet = projet;
        this.date = LocalDate.now();
    }

    public Don(Double montant, Donateur donateur, Projet projet, String message) {
        this.montant = montant;
        this.donateur = donateur;
        this.projet = projet;
        this.message = message;
        this.date = LocalDate.now();
    }

    public Don(Double montant, Donateur donateur, Projet projet, String message, Boolean anonyme) {
        this.montant = montant;
        this.donateur = donateur;
        this.projet = projet;
        this.message = message;
        this.anonyme = anonyme;
        this.date = LocalDate.now();
    }

    // Getters et Setters
    public Long getIdDon() {
        return idDon;
    }

    public void setIdDon(Long idDon) {
        this.idDon = idDon;
    }

    public Double getMontant() {
        return montant;
    }

    public void setMontant(Double montant) {
        this.montant = montant;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getAnonyme() {
        return anonyme;
    }

    public void setAnonyme(Boolean anonyme) {
        this.anonyme = anonyme;
    }

    public StatutDon getStatut() {
        return statut;
    }

    public void setStatut(StatutDon statut) {
        this.statut = statut;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public Donateur getDonateur() {
        return donateur;
    }

    public void setDonateur(Donateur donateur) {
        this.donateur = donateur;
    }

    public Projet getProjet() {
        return projet;
    }

    public void setProjet(Projet projet) {
        this.projet = projet;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    // AJOUT: Méthodes pour exposer des informations dérivées sans les relations complètes
    public String getNomProjet() {
        return projet != null ? projet.getTitre() : null;
    }

    public String getNomAssociation() {
        return projet != null && projet.getAssociation() != null ?
                projet.getAssociation().getNomAssociation() : null;
    }

    public Long getIdProjet() {
        return projet != null ? projet.getIdProjet() : null;
    }

    public Long getIdDonateur() {
        return donateur != null ? donateur.getIdUtilisateur() : null;
    }

    public String getNomDonateurComplet() {
        return donateur != null ? donateur.getNomComplet() : null;
    }

    // Méthodes métier
    public void enregistrer() {
        this.statut = StatutDon.EN_ATTENTE;
        this.date = LocalDate.now();
    }

    public void confirmer() {
        if (this.statut == StatutDon.EN_ATTENTE) {
            this.statut = StatutDon.VALIDE;

            // Ajouter le don au projet pour mettre à jour le montant collecté
            if (this.projet != null) {
                this.projet.ajouterDon(this);
            }
        }
    }

    public void annuler() {
        if (this.statut == StatutDon.EN_ATTENTE) {
            this.statut = StatutDon.ANNULE;
        }
    }

    public void rembourser() {
        if (this.statut == StatutDon.VALIDE) {
            this.statut = StatutDon.REMBOURSE;

            // Retirer le montant du projet si nécessaire
            if (this.projet != null) {
                Double nouveauMontant = this.projet.getMontantCollecte() - this.montant;
                this.projet.setMontantCollecte(Math.max(0, nouveauMontant));
            }
        }
    }

    public boolean estConfirme() {
        return this.statut == StatutDon.VALIDE;
    }

    public boolean estEnAttente() {
        return this.statut == StatutDon.EN_ATTENTE;
    }

    public boolean estAnnule() {
        return this.statut == StatutDon.ANNULE;
    }

    public boolean estRembourse() {
        return this.statut == StatutDon.REMBOURSE;
    }

    public boolean estRefuse() {
        return this.statut == StatutDon.REFUSE;
    }

    public String getNomDonateurAffiche() {
        if (anonyme) {
            return "Donateur anonyme";
        } else if (donateur != null) {
            return donateur.getPrenom() + " " + donateur.getNom();
        } else {
            return "Donateur inconnu";
        }
    }

    public String getMessageAffiche() {
        if (message == null || message.trim().isEmpty()) {
            return "Aucun message";
        }
        return message;
    }

    public boolean peutEtreAnnule() {
        return this.statut == StatutDon.EN_ATTENTE;
    }

    public boolean peutEtreRembourse() {
        return this.statut == StatutDon.VALIDE;
    }

    // AJOUT: Méthodes utilitaires pour l'API
    public String getStatutAffichage() {
        switch (this.statut) {
            case EN_ATTENTE:
                return "En attente de validation";
            case VALIDE:
                return "Don validé";
            case REFUSE:
                return "Don refusé";
            case ANNULE:
                return "Don annulé";
            case REMBOURSE:
                return "Don remboursé";
            default:
                return "Statut inconnu";
        }
    }

    public String getCouleurStatut() {
        switch (this.statut) {
            case VALIDE:
                return "success";
            case REFUSE:
            case ANNULE:
                return "danger";
            case REMBOURSE:
                return "warning";
            case EN_ATTENTE:
                return "info";
            default:
                return "secondary";
        }
    }

    public boolean estRecent() {
        if (date == null) return false;
        return date.isAfter(LocalDate.now().minusDays(7));
    }

    public String getIconeStatut() {
        switch (this.statut) {
            case VALIDE:
                return "✅";
            case REFUSE:
                return "❌";
            case ANNULE:
                return "🚫";
            case REMBOURSE:
                return "💰";
            case EN_ATTENTE:
                return "⏳";
            default:
                return "❓";
        }
    }

    // AJOUT: Informations pour les notifications
    public String getDescriptionCourte() {
        return String.format("Don de %.2f DH pour '%s'",
                montant,
                projet != null ? projet.getTitre() : "projet inconnu");
    }

    public boolean necessiteValidation() {
        return this.statut == StatutDon.EN_ATTENTE;
    }

    // equals et hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Don don = (Don) o;
        return Objects.equals(idDon, don.idDon) &&
                Objects.equals(montant, don.montant) &&
                Objects.equals(donateur, don.donateur) &&
                Objects.equals(projet, don.projet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idDon, montant, donateur, projet);
    }

    // toString
    @Override
    public String toString() {
        return "Don{" +
                "idDon=" + idDon +
                ", montant=" + montant +
                ", date=" + date +
                ", statut=" + statut +
                ", anonyme=" + anonyme +
                ", donateur=" + (donateur != null ? donateur.getPrenom() + " " + donateur.getNom() : "null") +
                ", projet=" + (projet != null ? projet.getTitre() : "null") +
                '}';
    }
}