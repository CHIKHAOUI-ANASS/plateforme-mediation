package com.mediation.platform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.email.from:noreply@mediation-platform.com}")
    private String fromEmail;

    /**
     * Envoyer un email de bienvenue
     */
    public void envoyerEmailBienvenue(String destinataire, String nom) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(destinataire);
        message.setSubject("Bienvenue sur la plateforme de médiation");
        message.setText("Bonjour " + nom + ",\n\n" +
                "Bienvenue sur notre plateforme de médiation entre donateurs et associations caritatives.\n" +
                "Votre compte a été créé avec succès.\n\n" +
                "Cordialement,\nL'équipe de la plateforme");

        try {
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email de bienvenue: " + e.getMessage());
        }
    }

    /**
     * Envoyer un email de validation d'association
     */
    public void envoyerEmailValidationAssociation(String destinataire, String nomAssociation) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(destinataire);
        message.setSubject("Validation de votre association");
        message.setText("Bonjour,\n\n" +
                "Votre association '" + nomAssociation + "' a été validée avec succès.\n" +
                "Vous pouvez maintenant créer des projets et recevoir des dons.\n\n" +
                "Cordialement,\nL'équipe de la plateforme");

        try {
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email de validation: " + e.getMessage());
        }
    }

    /**
     * Envoyer un email pour un don reçu
     */
    public void envoyerEmailDonRecu(String destinataire, String nomDonateur, String montant, String nomProjet) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(destinataire);
        message.setSubject("Nouveau don reçu");
        message.setText("Bonjour,\n\n" +
                "Vous avez reçu un nouveau don de " + montant + " DH de " + nomDonateur +
                " pour votre projet '" + nomProjet + "'.\n\n" +
                "Cordialement,\nL'équipe de la plateforme");

        try {
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email de don reçu: " + e.getMessage());
        }
    }

    /**
     * Envoyer un email pour un don validé
     */
    public void envoyerEmailDonValide(String destinataire, String nomProjet, String montant) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(destinataire);
        message.setSubject("Don validé");
        message.setText("Bonjour,\n\n" +
                "Votre don de " + montant + " DH pour le projet '" + nomProjet + "' a été validé.\n" +
                "Merci pour votre générosité !\n\n" +
                "Cordialement,\nL'équipe de la plateforme");

        try {
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email de don validé: " + e.getMessage());
        }
    }

    /**
     * Envoyer un email pour un projet complété
     */
    public void envoyerEmailProjetComplete(String destinataire, String nomProjet) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(destinataire);
        message.setSubject("Projet complété");
        message.setText("Félicitations !\n\n" +
                "Votre projet '" + nomProjet + "' a atteint son objectif financier.\n" +
                "Merci à tous les donateurs qui ont contribué à ce succès.\n\n" +
                "Cordialement,\nL'équipe de la plateforme");

        try {
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email de projet complété: " + e.getMessage());
        }
    }

    /**
     * Envoyer un email de réinitialisation de mot de passe
     */
    public void envoyerEmailResetMotDePasse(String destinataire, String nouveauMotDePasse) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(destinataire);
        message.setSubject("Réinitialisation de mot de passe");
        message.setText("Bonjour,\n\n" +
                "Votre mot de passe a été réinitialisé.\n" +
                "Votre nouveau mot de passe temporaire est : " + nouveauMotDePasse + "\n" +
                "Veuillez le changer dès votre prochaine connexion.\n\n" +
                "Cordialement,\nL'équipe de la plateforme");

        try {
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email de reset: " + e.getMessage());
        }
    }

    /**
     * Envoyer un email pour une notification urgente
     * (Méthode ajoutée pour le NotificationService)
     */
    public void envoyerEmailNotificationUrgente(String destinataire, String nom, String titre, String message) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(fromEmail);
        mailMessage.setTo(destinataire);
        mailMessage.setSubject("🚨 URGENT - " + titre);
        mailMessage.setText("Bonjour " + nom + ",\n\n" +
                "Vous avez reçu une notification urgente :\n\n" +
                "📋 " + titre + "\n" +
                "💬 " + (message != null ? message : "Aucun détail supplémentaire") + "\n\n" +
                "⚠️ Cette notification nécessite votre attention immédiate.\n" +
                "Veuillez vous connecter à la plateforme pour plus de détails.\n\n" +
                "Cordialement,\nL'équipe de la plateforme");

        try {
            mailSender.send(mailMessage);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email urgent: " + e.getMessage());
        }
    }

    /**
     * Envoyer un email de refus d'association
     */
    public void envoyerEmailRefusAssociation(String destinataire, String nomAssociation, String motif) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(destinataire);
        message.setSubject("Demande d'association refusée");
        message.setText("Bonjour,\n\n" +
                "Nous regrettons de vous informer que votre demande d'association '" + nomAssociation +
                "' a été refusée.\n\n" +
                "Motif : " + (motif != null ? motif : "Non spécifié") + "\n\n" +
                "Vous pouvez contacter notre équipe pour obtenir plus d'informations ou " +
                "soumettre une nouvelle demande après avoir corrigé les points mentionnés.\n\n" +
                "Cordialement,\nL'équipe de la plateforme");

        try {
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email de refus: " + e.getMessage());
        }
    }

    /**
     * Envoyer un email de rappel d'échéance de projet
     */
    public void envoyerEmailRappelEcheance(String destinataire, String nomProjet, int joursRestants, double progres) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(destinataire);
        message.setSubject("⏰ Rappel : Échéance proche pour votre projet");
        message.setText("Bonjour,\n\n" +
                "Nous vous rappelons que votre projet '" + nomProjet +
                "' arrive à échéance dans " + joursRestants + " jour(s).\n\n" +
                "📊 Progression actuelle : " + String.format("%.1f", progres) + "%\n\n" +
                "N'hésitez pas à promouvoir votre projet pour atteindre votre objectif !\n\n" +
                "Cordialement,\nL'équipe de la plateforme");

        try {
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email de rappel: " + e.getMessage());
        }
    }

    /**
     * Envoyer un email de rapport mensuel/hebdomadaire
     */
    public void envoyerEmailRapport(String destinataire, String nom, String typeRapport, String contenuRapport) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(destinataire);
        message.setSubject("📊 Votre rapport " + typeRapport + " est disponible");
        message.setText("Bonjour " + nom + ",\n\n" +
                "Votre rapport " + typeRapport + " est maintenant disponible.\n\n" +
                "📈 Résumé :\n" + contenuRapport + "\n\n" +
                "Connectez-vous à votre espace pour consulter le rapport détaillé.\n\n" +
                "Cordialement,\nL'équipe de la plateforme");

        try {
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email de rapport: " + e.getMessage());
        }
    }

    /**
     * Envoyer un email de maintenance programmée
     */
    public void envoyerEmailMaintenance(String destinataire, String nom, String dateDebut, String dureeEstimee, String details) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(destinataire);
        message.setSubject("🔧 Maintenance programmée de la plateforme");
        message.setText("Bonjour " + nom + ",\n\n" +
                "Nous vous informons qu'une maintenance de notre plateforme est programmée :\n\n" +
                "📅 Date et heure : " + dateDebut + "\n" +
                "⏱️ Durée estimée : " + dureeEstimee + "\n" +
                "🔧 Détails : " + details + "\n\n" +
                "Durant cette période, la plateforme pourra être temporairement inaccessible.\n" +
                "Nous nous excusons pour la gêne occasionnée.\n\n" +
                "Cordialement,\nL'équipe de la plateforme");

        try {
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email de maintenance: " + e.getMessage());
        }
    }

    /**
     * Envoyer un email personnalisé (méthode générique)
     */
    public void envoyerEmailPersonnalise(String destinataire, String sujet, String contenu) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(destinataire);
        message.setSubject(sujet);
        message.setText(contenu);

        try {
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email personnalisé: " + e.getMessage());
        }
    }

    /**
     * Envoyer un email à plusieurs destinataires
     */
    public void envoyerEmailGroupe(String[] destinataires, String sujet, String contenu) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(destinataires);
        message.setSubject(sujet);
        message.setText(contenu);

        try {
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email de groupe: " + e.getMessage());
        }
    }

    /**
     * Vérifier la configuration email
     */
    public boolean verifierConfiguration() {
        try {
            SimpleMailMessage testMessage = new SimpleMailMessage();
            testMessage.setFrom(fromEmail);
            testMessage.setTo(fromEmail); // Envoie à soi-même pour test
            testMessage.setSubject("Test de configuration email");
            testMessage.setText("Ceci est un test de configuration email.");

            mailSender.send(testMessage);
            return true;
        } catch (Exception e) {
            System.err.println("Erreur de configuration email: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtenir les informations de configuration
     */
    public String getFromEmail() {
        return fromEmail;
    }

    /**
     * Envoyer un email avec accusé de réception (si supporté)
     */
    public void envoyerEmailAvecAccuse(String destinataire, String sujet, String contenu) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(destinataire);
        message.setSubject(sujet);
        message.setText(contenu + "\n\n---\nCet email a été envoyé automatiquement par la plateforme de médiation.");

        try {
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email avec accusé: " + e.getMessage());
        }
    }
}