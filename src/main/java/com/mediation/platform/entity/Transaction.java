package com.mediation.platform.entity;



import com.mediation.platform.enums.StatutTransaction;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idTransaction;

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant doit être positif")
    @Column(nullable = false)
    private Double montant;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dateTransaction;

    @Size(max = 50, message = "Le mode de paiement ne peut dépasser 50 caractères")
    @Column(length = 50)
    private String modePayment;

    @Size(max = 255, message = "La référence externe ne peut dépasser 255 caractères")
    @Column(length = 255)
    private String referenceExterne;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutTransaction statut = StatutTransaction.EN_COURS;

    @Column(nullable = false)
    private Double frais = 0.0;

    @Size(max = 500, message = "Les détails ne peuvent dépasser 500 caractères")
    @Column(length = 500)
    private String details;

    @Size(max = 255, message = "Le message d'erreur ne peut dépasser 255 caractères")
    @Column(length = 255)
    private String messageErreur;

    // Relations
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_don", nullable = false)
    private Don don;

    // Constructeurs
    public Transaction() {}

    public Transaction(Double montant, Don don, String modePayment) {
        this.montant = montant;
        this.don = don;
        this.modePayment = modePayment;
        this.dateTransaction = LocalDateTime.now();
    }

    public Transaction(Double montant, Don don, String modePayment, String referenceExterne) {
        this.montant = montant;
        this.don = don;
        this.modePayment = modePayment;
        this.referenceExterne = referenceExterne;
        this.dateTransaction = LocalDateTime.now();
    }

    // Getters et Setters
    public Long getIdTransaction() {
        return idTransaction;
    }

    public void setIdTransaction(Long idTransaction) {
        this.idTransaction = idTransaction;
    }

    public Double getMontant() {
        return montant;
    }

    public void setMontant(Double montant) {
        this.montant = montant;
    }

    public LocalDateTime getDateTransaction() {
        return dateTransaction;
    }

    public void setDateTransaction(LocalDateTime dateTransaction) {
        this.dateTransaction = dateTransaction;
    }

    public String getModePayment() {
        return modePayment;
    }

    public void setModePayment(String modePayment) {
        this.modePayment = modePayment;
    }

    public String getReferenceExterne() {
        return referenceExterne;
    }

    public void setReferenceExterne(String referenceExterne) {
        this.referenceExterne = referenceExterne;
    }

    public StatutTransaction getStatut() {
        return statut;
    }

    public void setStatut(StatutTransaction statut) {
        this.statut = statut;
    }

    public Double getFrais() {
        return frais;
    }

    public void setFrais(Double frais) {
        this.frais = frais;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getMessageErreur() {
        return messageErreur;
    }

    public void setMessageErreur(String messageErreur) {
        this.messageErreur = messageErreur;
    }

    public Don getDon() {
        return don;
    }

    public void setDon(Don don) {
        this.don = don;
    }

    // Méthodes métier
    public boolean traiter() {
        if (this.statut == StatutTransaction.EN_COURS) {
            try {
                // Logique de traitement de la transaction
                // (intégration avec PayPal, etc.)

                this.statut = StatutTransaction.REUSSIE;
                this.details = "Transaction traitée avec succès";

                // Confirmer le don associé
                if (this.don != null) {
                    this.don.confirmer();
                }

                return true;
            } catch (Exception e) {
                this.statut = StatutTransaction.ECHOUEE;
                this.messageErreur = e.getMessage();
                return false;
            }
        }
        return false;
    }

    public void annuler() {
        if (this.statut == StatutTransaction.EN_COURS) {
            this.statut = StatutTransaction.ANNULEE;
            this.details = "Transaction annulée";

            // Annuler le don associé
            if (this.don != null) {
                this.don.annuler();
            }
        }
    }

    public void rembourser() {
        if (this.statut == StatutTransaction.REUSSIE) {
            // Logique de remboursement
            this.details = "Transaction remboursée";

            // Rembourser le don associé
            if (this.don != null) {
                this.don.rembourser();
            }
        }
    }

    public boolean estReussie() {
        return this.statut == StatutTransaction.REUSSIE;
    }

    public boolean estEnCours() {
        return this.statut == StatutTransaction.EN_COURS;
    }

    public boolean estEchouee() {
        return this.statut == StatutTransaction.ECHOUEE;
    }

    public boolean estAnnulee() {
        return this.statut == StatutTransaction.ANNULEE;
    }

    public Double getMontantNet() {
        return montant - frais;
    }

    public Double getPourcentageFrais() {
        if (montant == 0) return 0.0;
        return (frais / montant) * 100;
    }

    public void calculerFrais(Double tauxFrais) {
        if (tauxFrais != null && tauxFrais >= 0) {
            this.frais = montant * (tauxFrais / 100);
        }
    }

    public boolean peutEtreAnnulee() {
        return this.statut == StatutTransaction.EN_COURS;
    }

    public boolean peutEtreRemboursee() {
        return this.statut == StatutTransaction.REUSSIE;
    }

    public String getStatutAffiche() {
        switch (this.statut) {
            case EN_COURS:
                return "En cours de traitement";
            case REUSSIE:
                return "Transaction réussie";
            case ECHOUEE:
                return "Échec de la transaction";
            case ANNULEE:
                return "Transaction annulée";
            default:
                return "Statut inconnu";
        }
    }

    // equals et hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(idTransaction, that.idTransaction) &&
                Objects.equals(referenceExterne, that.referenceExterne);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idTransaction, referenceExterne);
    }

    // toString
    @Override
    public String toString() {
        return "Transaction{" +
                "idTransaction=" + idTransaction +
                ", montant=" + montant +
                ", dateTransaction=" + dateTransaction +
                ", modePayment='" + modePayment + '\'' +
                ", statut=" + statut +
                ", referenceExterne='" + referenceExterne + '\'' +
                ", frais=" + frais +
                '}';
    }
}
