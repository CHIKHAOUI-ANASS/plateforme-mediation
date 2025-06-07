package com.mediation.platform.service;

import com.mediation.platform.entity.Notification;
import com.mediation.platform.entity.Utilisateur;
import com.mediation.platform.enums.RoleUtilisateur;
import com.mediation.platform.enums.TypeNotification;
import com.mediation.platform.repository.NotificationRepository;
import com.mediation.platform.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    /**
     * Créer une notification
     */
    public Notification creerNotification(String titre, String message, TypeNotification type,
                                          Utilisateur utilisateur, Boolean urgent) {
        Notification notification = new Notification(titre, message, type, utilisateur, urgent);
        return notificationRepository.save(notification);
    }

    /**
     * Notifier nouvelle association aux admins
     */
    public void notifierNouvelleAssociation(Utilisateur association) {
        List<Utilisateur> admins = utilisateurRepository.findByRole(RoleUtilisateur.ADMINISTRATEUR);

        for (Utilisateur admin : admins) {
            Notification notification = Notification.creerNotificationNouveauProjet(
                    admin,
                    "Nouvelle association",
                    association.getNomComplet()
            );
            notificationRepository.save(notification);
        }
    }

    /**
     * Créer notification de validation
     */
    public void creerNotificationValidation(Utilisateur utilisateur) {
        Notification notification = Notification.creerNotificationValidationAssociation(
                utilisateur,
                utilisateur.getNomComplet()
        );
        notificationRepository.save(notification);
    }

    /**
     * Créer notification de refus
     */
    public void creerNotificationRefus(Utilisateur utilisateur, String motif) {
        Notification notification = Notification.creerNotificationRefusAssociation(
                utilisateur,
                utilisateur.getNomComplet(),
                motif
        );
        notificationRepository.save(notification);
    }

    /**
     * Notifier don reçu
     */
    public void notifierDonRecu(Utilisateur association, String nomDonateur, Double montant, String nomProjet) {
        Notification notification = Notification.creerNotificationDonRecu(
                association,
                nomDonateur,
                montant,
                nomProjet
        );
        notificationRepository.save(notification);
    }

    /**
     * Notifier don validé
     */
    public void notifierDonValide(Utilisateur donateur, String nomProjet, Double montant) {
        Notification notification = Notification.creerNotificationDonValide(
                donateur,
                nomProjet,
                montant
        );
        notificationRepository.save(notification);
    }

    /**
     * Notifier don refusé
     */
    public void notifierDonRefuse(Utilisateur donateur, String nomProjet, Double montant, String motif) {
        Notification notification = Notification.creerNotificationDonRefuse(
                donateur,
                nomProjet,
                montant,
                motif
        );
        notificationRepository.save(notification);
    }

    /**
     * Récupérer notifications d'un utilisateur
     */
    public List<Notification> getNotificationsUtilisateur(Utilisateur utilisateur) {
        return notificationRepository.findByUtilisateurOrderByDateEnvoiDesc(utilisateur);
    }

    /**
     * Récupérer notifications non lues
     */
    public List<Notification> getNotificationsNonLues(Utilisateur utilisateur) {
        return notificationRepository.findByUtilisateurAndLuFalseOrderByDateEnvoiDesc(utilisateur);
    }

    /**
     * Compter notifications non lues
     */
    public long compterNotificationsNonLues(Utilisateur utilisateur) {
        return notificationRepository.countUnreadByUser(utilisateur);
    }

    /**
     * Marquer notification comme lue
     */
    public void marquerCommeLue(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification non trouvée"));
        notification.marquerCommeLue();
        notificationRepository.save(notification);
    }

    /**
     * Marquer toutes les notifications comme lues
     */
    public void marquerToutesCommeLues(Utilisateur utilisateur) {
        notificationRepository.markAllAsReadForUser(utilisateur);
    }

    /**
     * Supprimer une notification
     */
    public void supprimerNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }
}