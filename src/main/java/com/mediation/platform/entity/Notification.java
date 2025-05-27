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
            case DON_VALIDE:
                return "Don validé";
            case DON_REFUSE:
                return "Don refusé";
            case PROJET_COMPLETE:
                return "Projet complété";
            case PROJET_EXPIRE:
                return "Projet expiré";
            case NOUVEAU_PROJET:
                return "Nouveau projet";
            case VALIDATION_ASSOCIATION:
                return "Validation association";
            case REFUS_ASSOCIATION:
                return "Refus association";
            case RAPPEL_ECHEANCE:
                return "Rappel échéance";
            case MISE_A_JOUR_PROFIL:
                return "Mise à jour profil";
            case SECURITE:
                return "Sécurité";
            case SYSTEME:
                return "Système";
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

    // Méthodes statiques pour créer des notifications spécifiques (corrigées selon l'énumération)
    public static Notification creerNotificationDonRecu(Utilisateur association, String nomDonateur, Double montant, String projet) {
        String titre = "Nouveau don reçu";
        String message = String.format("Vous avez reçu un don de %.2f DH de %s pour le projet \"%s\"",
                montant, nomDonateur, projet);
        return new Notification(titre, message, TypeNotification.DON_RECU, association);
    }

    public static Notification creerNotificationDonValide(Utilisateur donateur, String nomProjet, Double montant) {
        String titre = "Don validé";
        String message = String.format("Votre don de %.2f DH pour le projet \"%s\" a été validé avec succès.",
                montant, nomProjet);
        return new Notification(titre, message, TypeNotification.DON_VALIDE, donateur);
    }

    public static Notification creerNotificationDonRefuse(Utilisateur donateur, String nomProjet, Double montant, String motif) {
        String titre = "Don refusé";
        String message = String.format("Votre don de %.2f DH pour le projet \"%s\" a été refusé. Motif: %s",
                montant, nomProjet, motif != null ? motif : "Non spécifié");
        return new Notification(titre, message, TypeNotification.DON_REFUSE, donateur, true);
    }

    public static Notification creerNotificationProjetComplete(Utilisateur association, String nomProjet, Double montantCollecte) {
        String titre = "Projet complété";
        String message = String.format("Félicitations ! Votre projet \"%s\" a atteint son objectif avec %.2f DH collectés.",
                nomProjet, montantCollecte);
        return new Notification(titre, message, TypeNotification.PROJET_COMPLETE, association);
    }

    public static Notification creerNotificationProjetExpire(Utilisateur association, String nomProjet, Double montantCollecte, Double montantDemande) {
        String titre = "Projet expiré";
        String message = String.format("Votre projet \"%s\" a expiré. Montant collecté: %.2f DH sur %.2f DH demandés.",
                nomProjet, montantCollecte, montantDemande);
        return new Notification(titre, message, TypeNotification.PROJET_EXPIRE, association, true);
    }

    public static Notification creerNotificationNouveauProjet(Utilisateur administrateur, String nomProjet, String nomAssociation) {
        String titre = "Nouveau projet créé";
        String message = String.format("Un nouveau projet \"%s\" a été créé par l'association \"%s\".",
                nomProjet, nomAssociation);
        return new Notification(titre, message, TypeNotification.NOUVEAU_PROJET, administrateur);
    }

    public static Notification creerNotificationValidationAssociation(Utilisateur association, String nomAssociation) {
        String titre = "Association validée";
        String message = String.format("Votre association \"%s\" a été validée avec succès. Vous pouvez maintenant créer des projets.",
                nomAssociation);
        return new Notification(titre, message, TypeNotification.VALIDATION_ASSOCIATION, association);
    }

    public static Notification creerNotificationRefusAssociation(Utilisateur association, String nomAssociation, String motif) {
        String titre = "Association refusée";
        String message = String.format("Votre demande d'association \"%s\" a été refusée. Motif: %s",
                nomAssociation, motif != null ? motif : "Non spécifié");
        return new Notification(titre, message, TypeNotification.REFUS_ASSOCIATION, association, true);
    }

    public static Notification creerNotificationRappelEcheance(Utilisateur association, String nomProjet, int joursRestants, Double progres) {
        String titre = "Rappel d'échéance";
        String message = String.format("Votre projet \"%s\" expire dans %d jour(s). Progression: %.1f%%",
                nomProjet, joursRestants, progres);
        return new Notification(titre, message, TypeNotification.RAPPEL_ECHEANCE, association, joursRestants <= 3);
    }

    public static Notification creerNotificationMiseAJourProfil(Utilisateur utilisateur) {
        String titre = "Profil mis à jour";
        String message = "Votre profil a été mis à jour avec succès.";
        return new Notification(titre, message, TypeNotification.MISE_A_JOUR_PROFIL, utilisateur);
    }

    public static Notification creerNotificationSecurite(Utilisateur utilisateur, String typeEvenement) {
        String titre = "Alerte de sécurité";
        String message = String.format("Activité de sécurité détectée: %s. Si ce n'est pas vous, changez votre mot de passe.",
                typeEvenement);
        return new Notification(titre, message, TypeNotification.SECURITE, utilisateur, true);
    }

    public static Notification creerNotificationSysteme(Utilisateur utilisateur, String message) {
        String titre = "Notification système";
        return new Notification(titre, message, TypeNotification.SYSTEME, utilisateur);
    }

    public static Notification creerRappel(Utilisateur utilisateur, String sujet, String contenu) {
        return new Notification("Rappel : " + sujet, contenu, TypeNotification.RAPPEL_ECHEANCE, utilisateur);
    }

    public static Notification creerNotificationRapport(Utilisateur utilisateur, String typeRapport) {
        String titre = "Nouveau rapport disponible";
        String message = String.format("Votre rapport %s est maintenant disponible en téléchargement.", typeRapport);
        return new Notification(titre, message, TypeNotification.SYSTEME, utilisateur);
    }

    public static Notification creerNotificationValidationCompte(Utilisateur utilisateur, boolean valide) {
        Notification notification = new Notification();
        notification.utilisateur = utilisateur;
        notification.lu = valide;
        notification.message = valide
                ? "Votre compte a été validé."
                : "Votre compte a été refusé.";
        return notification;
    }

    // Méthodes utiles pour l'affichage
    public String getIcone() {
        switch (this.type) {
            case DON_RECU:
                return "💰";
            case DON_VALIDE:
                return "✅";
            case DON_REFUSE:
                return "❌";
            case PROJET_COMPLETE:
                return "🎉";
            case PROJET_EXPIRE:
                return "⏰";
            case NOUVEAU_PROJET:
                return "📋";
            case VALIDATION_ASSOCIATION:
                return "✅";
            case REFUS_ASSOCIATION:
                return "❌";
            case RAPPEL_ECHEANCE:
                return "⏰";
            case MISE_A_JOUR_PROFIL:
                return "👤";
            case SECURITE:
                return "🔒";
            case SYSTEME:
                return "⚙️";
            default:
                return "🔔";
        }
    }

    public String getCouleur() {
        switch (this.type) {
            case DON_RECU:
            case DON_VALIDE:
            case PROJET_COMPLETE:
            case VALIDATION_ASSOCIATION:
                return "success";
            case DON_REFUSE:
            case PROJET_EXPIRE:
            case REFUS_ASSOCIATION:
            case SECURITE:
                return "danger";
            case RAPPEL_ECHEANCE:
                return "warning";
            case NOUVEAU_PROJET:
            case MISE_A_JOUR_PROFIL:
                return "info";
            case SYSTEME:
                return "secondary";
            default:
                return "primary";
        }
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