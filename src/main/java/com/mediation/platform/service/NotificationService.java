package com.mediation.platform.service;

import com.mediation.platform.entity.Notification;
import com.mediation.platform.entity.Utilisateur;
import com.mediation.platform.entity.Don;
import com.mediation.platform.entity.Projet;
import com.mediation.platform.entity.Association;
import com.mediation.platform.repository.NotificationRepository;
import com.mediation.platform.enums.TypeNotification;
import com.mediation.platform.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EmailService emailService;

    /**
     * Récupérer toutes les notifications
     */
    public List<Notification> findAll() {
        return notificationRepository.findAll();
    }

    /**
     * Récupérer une notification par ID
     */
    public Notification findById(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification non trouvée avec l'ID: " + id));
    }

    /**
     * Créer une nouvelle notification
     */
    public Notification creerNotification(Utilisateur utilisateur, String titre, String message, TypeNotification type) {
        return creerNotification(utilisateur, titre, message, type, false);
    }

    /**
     * Créer une nouvelle notification avec niveau d'urgence
     */
    public Notification creerNotification(Utilisateur utilisateur, String titre, String message,
                                          TypeNotification type, boolean urgent) {
        if (utilisateur == null) {
            throw new IllegalArgumentException("L'utilisateur est obligatoire pour créer une notification");
        }

        if (titre == null || titre.trim().isEmpty()) {
            throw new IllegalArgumentException("Le titre de la notification est obligatoire");
        }

        Notification notification = new Notification();
        notification.setUtilisateur(utilisateur);
        notification.setTitre(titre);
        notification.setMessage(message != null ? message : "");
        notification.setType(type);
        notification.setLu(false);
        notification.setUrgent(urgent);
        // dateEnvoi sera géré par @CreationTimestamp dans l'entité

        Notification savedNotification = notificationRepository.save(notification);

        // Envoyer email si notification urgente
        if (urgent) {
            try {
                emailService.envoyerEmailNotificationUrgente(
                        utilisateur.getEmail(),
                        utilisateur.getPrenom() + " " + utilisateur.getNom(),
                        titre,
                        message
                );
            } catch (Exception e) {
                // Log l'erreur mais ne fait pas échouer la création de notification
                System.err.println("Erreur lors de l'envoi de l'email urgent: " + e.getMessage());
            }
        }

        return savedNotification;
    }

    /**
     * Créer une notification avec URL d'action et expéditeur
     */
    public Notification creerNotificationComplete(Utilisateur utilisateur, String titre, String message,
                                                  TypeNotification type, boolean urgent,
                                                  String urlAction, String expediteur) {
        Notification notification = creerNotification(utilisateur, titre, message, type, urgent);
        notification.setUrlAction(urlAction);
        notification.setExpediteur(expediteur);
        return notificationRepository.save(notification);
    }

    /**
     * Récupérer les notifications d'un utilisateur
     */
    public List<Notification> findByUtilisateur(Utilisateur utilisateur) {
        if (utilisateur == null) {
            throw new IllegalArgumentException("L'utilisateur ne peut pas être null");
        }
        return notificationRepository.findByUtilisateurOrderByDateEnvoiDesc(utilisateur);
    }

    /**
     * Récupérer les notifications non lues d'un utilisateur
     */
    public List<Notification> findNotificationsNonLues(Utilisateur utilisateur) {
        if (utilisateur == null) {
            throw new IllegalArgumentException("L'utilisateur ne peut pas être null");
        }
        return notificationRepository.findByUtilisateurAndLuFalseOrderByDateEnvoiDesc(utilisateur);
    }

    /**
     * Récupérer les notifications par type
     */
    public List<Notification> findByType(TypeNotification type) {
        return notificationRepository.findByType(type);
    }

    /**
     * Récupérer les notifications urgentes non lues
     */
    public List<Notification> findUrgentNotifications() {
        return notificationRepository.findByUrgentTrueAndLuFalseOrderByDateEnvoiDesc();
    }

    /**
     * Récupérer les notifications récentes
     */
    public List<Notification> findRecentNotifications(LocalDateTime dateDebut) {
        return notificationRepository.findRecentNotifications(dateDebut);
    }

    /**
     * Compter les notifications non lues d'un utilisateur
     */
    public long countUnreadByUser(Utilisateur utilisateur) {
        if (utilisateur == null) {
            return 0;
        }
        return notificationRepository.countUnreadByUser(utilisateur);
    }

    /**
     * Marquer une notification comme lue
     */
    public Notification marquerCommeLue(Long notificationId) {
        Notification notification = findById(notificationId);
        notification.marquerCommeLue(); // Utilise la méthode de l'entité
        return notificationRepository.save(notification);
    }

    /**
     * Marquer une notification comme non lue
     */
    public Notification marquerCommeNonLue(Long notificationId) {
        Notification notification = findById(notificationId);
        notification.marquerCommeNonLue(); // Utilise la méthode de l'entité
        return notificationRepository.save(notification);
    }

    /**
     * Marquer toutes les notifications d'un utilisateur comme lues
     */
    public void marquerToutesCommeLues(Utilisateur utilisateur) {
        if (utilisateur == null) {
            throw new IllegalArgumentException("L'utilisateur ne peut pas être null");
        }
        notificationRepository.markAllAsReadForUser(utilisateur);
    }

    /**
     * Supprimer une notification
     */
    public void deleteById(Long id) {
        if (!notificationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Notification non trouvée avec l'ID: " + id);
        }
        notificationRepository.deleteById(id);
    }

    /**
     * Supprimer toutes les notifications lues d'un utilisateur
     */
    public void supprimerNotificationsLues(Utilisateur utilisateur) {
        if (utilisateur == null) {
            throw new IllegalArgumentException("L'utilisateur ne peut pas être null");
        }

        List<Notification> notificationsLues = notificationRepository.findByUtilisateurOrderByDateEnvoiDesc(utilisateur)
                .stream()
                .filter(Notification::estLue)
                .toList();

        notificationRepository.deleteAll(notificationsLues);
    }

    /**
     * Nettoyer les anciennes notifications (plus de X jours)
     */
    public void nettoyerAnciennesNotifications(int joursAnciennete) {
        LocalDateTime dateLimite = LocalDateTime.now().minusDays(joursAnciennete);
        List<Notification> anciennesNotifications = notificationRepository.findAll()
                .stream()
                .filter(n -> n.getDateEnvoi().isBefore(dateLimite) && n.estLue())
                .toList();

        notificationRepository.deleteAll(anciennesNotifications);
    }

    // ========== MÉTHODES UTILITAIRES POUR CRÉER DES NOTIFICATIONS SPÉCIFIQUES ==========

    /**
     * Notifier qu'un nouveau don a été reçu (utilise la méthode statique de l'entité)
     */
    public void notifierNouveauDon(Association association, Don don) {
        String nomDonateur = don.getAnonyme() ? "Donateur anonyme" :
                don.getDonateur().getPrenom() + " " + don.getDonateur().getNom();

        // Utilise la méthode statique de l'entité
        Notification notification = Notification.creerNotificationDonRecu(
                association, nomDonateur, don.getMontant(), don.getProjet().getTitre()
        );

        notificationRepository.save(notification);

        // Envoyer aussi un email
        try {
            emailService.envoyerEmailDonRecu(
                    association.getEmail(),
                    nomDonateur,
                    don.getMontant().toString(),
                    don.getProjet().getTitre()
            );
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email de don reçu: " + e.getMessage());
        }
    }

    /**
     * Notifier la validation/refus d'un compte (utilise la méthode statique de l'entité)
     */
    public void notifierValidationCompte(Utilisateur utilisateur, boolean valide) {
        Notification notification = Notification.creerNotificationValidationCompte(utilisateur, valide);
        notificationRepository.save(notification);

        // Envoyer email selon le statut
        try {
            if (valide && utilisateur instanceof Association) {
                emailService.envoyerEmailValidationAssociation(
                        utilisateur.getEmail(),
                        ((Association) utilisateur).getNomAssociation()
                );
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email de validation: " + e.getMessage());
        }
    }

    /**
     * Créer un rappel (utilise la méthode statique de l'entité)
     */
    public void creerRappel(Utilisateur utilisateur, String sujet, String contenu) {
        Notification notification = Notification.creerRappel(utilisateur, sujet, contenu);
        notificationRepository.save(notification);
    }

    /**
     * Notifier qu'un rapport est disponible (utilise la méthode statique de l'entité)
     */
    public void notifierRapportDisponible(Utilisateur utilisateur, String typeRapport) {
        Notification notification = Notification.creerNotificationRapport(utilisateur, typeRapport);
        notificationRepository.save(notification);
    }

    /**
     * Notifier qu'un don a été validé
     */
    public void notifierDonValide(Don don) {
        String titre = "Don validé";
        String message = String.format("Votre don de %.2f DH pour le projet '%s' a été validé avec succès. Merci pour votre générosité !",
                don.getMontant(), don.getProjet().getTitre());

        creerNotification(don.getDonateur(), titre, message, TypeNotification.DON_VALIDE);

        // Envoyer aussi un email
        try {
            emailService.envoyerEmailDonValide(
                    don.getDonateur().getEmail(),
                    don.getProjet().getTitre(),
                    don.getMontant().toString()
            );
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email de don validé: " + e.getMessage());
        }
    }

    /**
     * Notifier qu'un don a été refusé
     */
    public void notifierDonRefuse(Don don, String motif) {
        String titre = "Don refusé";
        String message = String.format("Votre don de %.2f DH pour le projet '%s' a été refusé. Motif: %s",
                don.getMontant(), don.getProjet().getTitre(), motif != null ? motif : "Non spécifié");

        creerNotification(don.getDonateur(), titre, message, TypeNotification.DON_REFUSE, true);
    }

    /**
     * Notifier qu'un projet a atteint son objectif
     */
    public void notifierProjetComplete(Projet projet) {
        String titre = "Projet complété";
        String message = String.format("Félicitations ! Votre projet '%s' a atteint son objectif de %.2f DH. " +
                        "Montant total collecté: %.2f DH",
                projet.getTitre(), projet.getMontantDemande(), projet.getMontantCollecte());

        creerNotification(projet.getAssociation(), titre, message, TypeNotification.PROJET_COMPLETE);

        // Notifier aussi tous les donateurs du projet
        projet.getDons().forEach(don -> {
            if (don.getStatut().name().equals("VALIDE")) {
                String titreDonatateur = "Projet complété";
                String messageDonatateur = String.format("Le projet '%s' que vous avez soutenu a atteint son objectif ! " +
                                "Merci pour votre contribution de %.2f DH.",
                        projet.getTitre(), don.getMontant());

                creerNotification(don.getDonateur(), titreDonatateur, messageDonatateur, TypeNotification.PROJET_COMPLETE);
            }
        });

        // Envoyer email
        try {
            emailService.envoyerEmailProjetComplete(
                    projet.getAssociation().getEmail(),
                    projet.getTitre()
            );
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email de projet complété: " + e.getMessage());
        }
    }

    /**
     * Notifier qu'un projet est proche de son échéance
     */
    public void notifierProjetProcheEcheance(Projet projet, int joursRestants) {
        String titre = "Échéance proche";
        String message = String.format("Attention ! Votre projet '%s' arrive à échéance dans %d jour(s). " +
                        "Montant collecté: %.2f DH sur %.2f DH demandés (%.1f%%).",
                projet.getTitre(), joursRestants, projet.getMontantCollecte(),
                projet.getMontantDemande(), projet.calculerProgres());

        creerNotification(projet.getAssociation(), titre, message, TypeNotification.RAPPEL_ECHEANCE, true);
    }

    /**
     * Notifier qu'un projet a expiré
     */
    public void notifierProjetExpire(Projet projet) {
        String titre = "Projet expiré";
        String message = String.format("Votre projet '%s' a atteint sa date d'échéance. " +
                        "Montant final collecté: %.2f DH sur %.2f DH demandés (%.1f%%).",
                projet.getTitre(), projet.getMontantCollecte(),
                projet.getMontantDemande(), projet.calculerProgres());

        creerNotification(projet.getAssociation(), titre, message, TypeNotification.PROJET_EXPIRE, true);
    }

    /**
     * Notifier la validation d'une association
     */
    public void notifierValidationAssociation(Association association) {
        String titre = "Association validée";
        String message = String.format("Félicitations ! Votre association '%s' a été validée avec succès. " +
                        "Vous pouvez maintenant créer des projets et recevoir des dons.",
                association.getNomAssociation());

        creerNotification(association, titre, message, TypeNotification.VALIDATION_ASSOCIATION);

        // Envoyer email
        try {
            emailService.envoyerEmailValidationAssociation(
                    association.getEmail(),
                    association.getNomAssociation()
            );
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email de validation: " + e.getMessage());
        }
    }

    /**
     * Notifier le refus d'une association
     */
    public void notifierRefusAssociation(Association association, String motif) {
        String titre = "Association refusée";
        String message = String.format("Votre demande d'association '%s' a été refusée. " +
                        "Motif: %s. Vous pouvez contacter l'administration pour plus d'informations.",
                association.getNomAssociation(), motif != null ? motif : "Non spécifié");

        creerNotification(association, titre, message, TypeNotification.REFUS_ASSOCIATION, true);
    }

    /**
     * Notifier un nouveau projet créé (aux administrateurs)
     */
    public void notifierNouveauProjet(Projet projet, List<Utilisateur> administrateurs) {
        String titre = "Nouveau projet créé";
        String message = String.format("Un nouveau projet '%s' a été créé par l'association '%s'. " +
                        "Objectif: %.2f DH. Échéance: %s",
                projet.getTitre(), projet.getAssociation().getNomAssociation(),
                projet.getMontantDemande(),
                projet.getDateFin() != null ? projet.getDateFin().toString() : "Non définie");

        administrateurs.forEach(admin ->
                creerNotification(admin, titre, message, TypeNotification.NOUVEAU_PROJET)
        );
    }

    /**
     * Notifier une mise à jour de profil
     */
    public void notifierMiseAJourProfil(Utilisateur utilisateur) {
        String titre = "Profil mis à jour";
        String message = "Votre profil a été mis à jour avec succès.";

        creerNotification(utilisateur, titre, message, TypeNotification.MISE_A_JOUR_PROFIL);
    }

    /**
     * Notifier un problème de sécurité
     */
    public void notifierProblemeSecurite(Utilisateur utilisateur, String typeProbleme) {
        String titre = "Alerte de sécurité";
        String message = String.format("Nous avons détecté une activité suspecte sur votre compte: %s. " +
                        "Si ce n'est pas vous, veuillez changer votre mot de passe immédiatement.",
                typeProbleme);

        creerNotification(utilisateur, titre, message, TypeNotification.SECURITE, true);
    }

    /**
     * Notifier une maintenance système
     */
    public void notifierMaintenanceSysteme(List<Utilisateur> utilisateurs, String message, LocalDateTime dateDebutMaintenance) {
        String titre = "Maintenance programmée";
        String messageComplet = String.format("Maintenance du système programmée le %s. %s",
                dateDebutMaintenance.toString(), message);

        utilisateurs.forEach(utilisateur ->
                creerNotification(utilisateur, titre, messageComplet, TypeNotification.SYSTEME)
        );
    }

    // ========== MÉTHODES D'ANALYSE ET STATISTIQUES ==========

    /**
     * Obtenir les statistiques des notifications
     */
    public Map<String, Object> getStatistiquesNotifications() {
        Map<String, Object> stats = new HashMap<>();

        // Nombre total de notifications
        stats.put("totalNotifications", notificationRepository.count());

        // Notifications non lues
        long nonLues = notificationRepository.findAll().stream()
                .filter(Notification::estNonLue)
                .count();
        stats.put("notificationsNonLues", nonLues);

        // Notifications urgentes
        stats.put("notificationsUrgentes", findUrgentNotifications().size());

        // Notifications récentes (7 derniers jours)
        LocalDateTime dateDebut = LocalDateTime.now().minusDays(7);
        long recentes = findRecentNotifications(dateDebut).stream()
                .filter(Notification::estRecente)
                .count();
        stats.put("notificationsRecentes", recentes);

        return stats;
    }

    /**
     * Obtenir les notifications pour un tableau de bord
     */
    public Map<String, Object> getNotificationsDashboard(Utilisateur utilisateur) {
        Map<String, Object> dashboard = new HashMap<>();

        if (utilisateur != null) {
            // Notifications de l'utilisateur
            List<Notification> notifications = findByUtilisateur(utilisateur);
            dashboard.put("mesNotifications", notifications.stream().limit(10).toList());

            // Nombre non lues
            dashboard.put("nombreNonLues", countUnreadByUser(utilisateur));

            // Notifications urgentes
            List<Notification> urgentes = notifications.stream()
                    .filter(n -> n.estUrgente() && n.estNonLue())
                    .limit(5)
                    .toList();
            dashboard.put("notificationsUrgentes", urgentes);
        }

        // Notifications système récentes
        LocalDateTime dateDebut = LocalDateTime.now().minusHours(24);
        List<Notification> systeme = findRecentNotifications(dateDebut).stream()
                .filter(n -> n.getType() == TypeNotification.SYSTEME)
                .limit(3)
                .toList();
        dashboard.put("notificationsSysteme", systeme);

        return dashboard;
    }

    // ========== MÉTHODES SUPPLÉMENTAIRES ADAPTÉES À L'ENTITÉ ==========

    /**
     * Récupérer les notifications récentes d'un utilisateur
     */
    public List<Notification> findNotificationsRecentes(Utilisateur utilisateur) {
        return findByUtilisateur(utilisateur).stream()
                .filter(Notification::estRecente)
                .toList();
    }

    /**
     * Récupérer les notifications par CSS class (pour l'affichage)
     */
    public Map<String, List<Notification>> groupByStatus(Utilisateur utilisateur) {
        List<Notification> notifications = findByUtilisateur(utilisateur);
        Map<String, List<Notification>> grouped = new HashMap<>();

        grouped.put("urgent", notifications.stream()
                .filter(n -> n.estUrgente() && n.estNonLue())
                .toList());

        grouped.put("nonLue", notifications.stream()
                .filter(n -> n.estNonLue() && !n.estUrgente())
                .toList());

        grouped.put("lue", notifications.stream()
                .filter(Notification::estLue)
                .toList());

        return grouped;
    }

    /**
     * Obtenir le résumé des notifications pour un utilisateur
     */
    public Map<String, Object> getResumeNotifications(Utilisateur utilisateur) {
        Map<String, Object> resume = new HashMap<>();

        List<Notification> notifications = findByUtilisateur(utilisateur);

        resume.put("total", notifications.size());
        resume.put("nonLues", notifications.stream().filter(Notification::estNonLue).count());
        resume.put("urgentes", notifications.stream().filter(n -> n.estUrgente() && n.estNonLue()).count());
        resume.put("recentes", notifications.stream().filter(Notification::estRecente).count());

        // Dernière notification
        if (!notifications.isEmpty()) {
            resume.put("derniere", notifications.get(0));
        }

        return resume;
    }

    /**
     * Mettre à jour une notification existante
     */
    public Notification updateNotification(Long id, String titre, String message, String urlAction) {
        Notification notification = findById(id);

        if (titre != null && !titre.trim().isEmpty()) {
            notification.setTitre(titre);
        }

        if (message != null) {
            notification.setMessage(message);
        }

        if (urlAction != null) {
            notification.setUrlAction(urlAction);
        }

        return notificationRepository.save(notification);
    }

    /**
     * Dupliquer une notification pour plusieurs utilisateurs
     */
    public List<Notification> diffuserNotification(List<Utilisateur> utilisateurs, String titre,
                                                   String message, TypeNotification type, boolean urgent) {
        List<Notification> notifications = new ArrayList<>();

        for (Utilisateur utilisateur : utilisateurs) {
            Notification notification = creerNotification(utilisateur, titre, message, type, urgent);
            notifications.add(notification);
        }

        return notifications;
    }

    /**
     * Marquer comme lues toutes les notifications d'un type pour un utilisateur
     */
    public void marquerCommeLuesParType(Utilisateur utilisateur, TypeNotification type) {
        List<Notification> notifications = findByUtilisateur(utilisateur).stream()
                .filter(n -> n.getType() == type && n.estNonLue())
                .toList();

        notifications.forEach(Notification::marquerCommeLue);
        notificationRepository.saveAll(notifications);
    }

    /**
     * Ajouter une méthode d'envoi d'email pour notifications urgentes
     */
    private void envoyerEmailNotificationUrgente(String destinataire, String nom, String titre, String message) {
        try {
            // Utilise une méthode spécialisée de EmailService
            if (emailService != null) {
                // Cette méthode doit être ajoutée à EmailService
                SimpleMailMessage mailMessage = new SimpleMailMessage();
                mailMessage.setTo(destinataire);
                mailMessage.setSubject("🚨 URGENT - " + titre);
                mailMessage.setText("Bonjour " + nom + ",\n\n" +
                        "Vous avez reçu une notification urgente :\n\n" +
                        titre + "\n" +
                        message + "\n\n" +
                        "Veuillez vous connecter à la plateforme pour plus de détails.\n\n" +
                        "Cordialement,\nL'équipe de la plateforme");

                // Cette ligne nécessite une méthode dans EmailService
                // emailService.envoyerEmail(mailMessage);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email urgent: " + e.getMessage());
        }
    }
}