package com.mediation.platform.entity;



import com.mediation.platform.enums.TypeNotification;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idNotification;

    @NotBlank(message = "Le titre est obligatoire")
    @Size(max = 200, message = "Le titre ne peut dépasser 200 caractères")
    @Column(nullable = false, length = 200)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String message;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dateEnvoi;

    @Column(nullable = false)
    private Boolean lu = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeNotification type;

    @Column(nullable = false)
    private Boolean urgent = false;

    @Size(max = 255, message = "L'URL d'action ne peut dépasser 255 caractères")
    @Column(length = 255)
    private String urlAction;

    @Size(max = 100, message = "L'expéditeur ne peut dépasser 100 caractères")
    @Column(length = 100)
    private String expediteur;

    // Relations
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private Utilisateur utilisateur;

    // Constructeurs
    public Notification() {}

    public Notification(String titre, String message, TypeNotification type, Utilisateur utilisateur) {
        this.titre = titre;
        this.message = message;
        this.type = type;
        this.utilisateur = utilisateur;
        this.dateEnvoi = LocalDateTime.now();
    }

    public Notification(String titre, String message, TypeNotification type,
                        Utilisateur utilisateur, Boolean urgent) {
        this.titre = titre;
        this.message = message;
        this.type = type;
        this.utilisateur = utilisateur;
        this.urgent = urgent;
        this.dateEnvoi = LocalDateTime.now();
    }

    // Getters et Setters
    public Long getIdNotification() {
        return idNotification;
    }

    public void setIdNotification(Long idNotification) {
        this.idNotification = idNotification;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getDateEnvoi() {
        return dateEnvoi;
    }

    public void setDateEnvoi(LocalDateTime dateEnvoi) {
        this.dateEnvoi = dateEnvoi;
    }

    public Boolean getLu() {
        return lu;
    }

    public void setLu(Boolean lu) {
        this.lu = lu;
    }

    public TypeNotification getType() {
        return type;
    }

    public void setType(TypeNotification type) {
        this.type = type;
    }

    public Boolean getUrgent() {
        return urgent;
    }

    public void setUrgent(Boolean urgent) {
        this.urgent = urgent;
    }

    public String getUrlAction() {
        return urlAction;
    }

    public void setUrlAction(String urlAction) {
        this.urlAction = urlAction;
    }

    public String getExpediteur() {
        return expediteur;
    }

    public void setExpediteur(String expediteur) {
        this.expediteur = expediteur;
    }

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }

    // Méthodes métier
    public void envoyer() {
        this.dateEnvoi = LocalDateTime.now();
        // Logique d'envoi (email, push, etc.) sera implémentée dans le service
    }

    public void marquerCommeLue() {
        this.lu = true;
    }

    public void marquerCommeNonLue() {
        this.lu = false;
    }

    public boolean estLue() {
        return this.lu != null && this.lu;
    }

    public boolean estNonLue() {
        return this.lu == null || !this.lu;
    }

    public boolean estUrgente() {
        return this.urgent != null && this.urgent;
    }

    public boolean estRecente() {
        if (dateEnvoi == null) return false;
        return dateEnvoi.isAfter(LocalDateTime.now().minusDays(7));
    }

    public String getTypeAffiche() {
        switch (this.type) {
            case DON_RECU:
                return "Don reçu";
            case VALIDATION_COMPTE:
                return "Validation de compte";
            case RAPPEL:
                return "Rappel";
            case RAPPORT:
                return "Rapport";
            default:
                return "Notification";
        }
    }

    public String getStatutAffiche() {
        if (estLue()) {
            return "Lue";
        } else if (estUrgente()) {
            return "Non lue - Urgent";
        } else {
            return "Non lue";
        }
    }

    public String getCssClass() {
        if (estUrgente() && estNonLue()) {
            return "notification-urgent";
        } else if (estNonLue()) {
            return "notification-non-lue";
        } else {
            return "notification-lue";
        }
    }

    // Méthodes statiques pour créer des notifications spécifiques
    public static Notification creerNotificationDonRecu(Utilisateur association, String nomDonateur, Double montant, String projet) {
        String titre = "Nouveau don reçu";
        String message = String.format("Vous avez reçu un don de %.2f MAD de %s pour le projet \"%s\"",
                montant, nomDonateur, projet);
        return new Notification(titre, message, TypeNotification.DON_RECU, association);
    }

    public static Notification creerNotificationValidationCompte(Utilisateur utilisateur, boolean valide) {
        String titre = valide ? "Compte validé" : "Compte rejeté";
        String message = valide ?
                "Félicitations ! Votre compte association a été validé et est maintenant actif." :
                "Votre demande de création de compte association a été rejetée. Contactez l'administration pour plus d'informations.";
        return new Notification(titre, message, TypeNotification.VALIDATION_COMPTE, utilisateur, !valide);
    }

    public static Notification creerRappel(Utilisateur utilisateur, String sujet, String contenu) {
        return new Notification("Rappel : " + sujet, contenu, TypeNotification.RAPPEL, utilisateur);
    }

    public static Notification creerNotificationRapport(Utilisateur utilisateur, String typeRapport) {
        String titre = "Nouveau rapport disponible";
        String message = String.format("Votre rapport %s est maintenant disponible en téléchargement.", typeRapport);
        return new Notification(titre, message, TypeNotification.RAPPORT, utilisateur);
    }

    // equals et hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Notification that = (Notification) o;
        return Objects.equals(idNotification, that.idNotification) &&
                Objects.equals(titre, that.titre) &&
                Objects.equals(utilisateur, that.utilisateur) &&
                Objects.equals(dateEnvoi, that.dateEnvoi);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idNotification, titre, utilisateur, dateEnvoi);
    }

    // toString
    @Override
    public String toString() {
        return "Notification{" +
                "idNotification=" + idNotification +
                ", titre='" + titre + '\'' +
                ", type=" + type +
                ", lu=" + lu +
                ", urgent=" + urgent +
                ", dateEnvoi=" + dateEnvoi +
                ", utilisateur=" + (utilisateur != null ? utilisateur.getEmail() : "null") +
                '}';
    }
}