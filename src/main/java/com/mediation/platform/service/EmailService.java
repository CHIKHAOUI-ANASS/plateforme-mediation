package com.mediation.platform.service;

import com.mediation.platform.entity.Utilisateur;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender emailSender;

    @Value("${app.mail.from:noreply@mediation-platform.com}")
    private String fromEmail;

    @Value("${app.name:Plateforme de Médiation}")
    private String appName;

    /**
     * Envoyer email de bienvenue aux donateurs
     */
    public void envoyerEmailBienvenue(Utilisateur utilisateur) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(utilisateur.getEmail());
        message.setFrom(fromEmail);
        message.setSubject("Bienvenue sur " + appName + " !");

        String texte = String.format(
                "Bonjour %s %s,\n\n" +
                        "Bienvenue sur %s !\n\n" +
                        "Votre compte donateur a été créé avec succès. Vous pouvez maintenant :\n" +
                        "- Explorer les associations et leurs projets\n" +
                        "- Effectuer des dons en toute sécurité\n" +
                        "- Suivre l'impact de vos contributions\n\n" +
                        "Merci de faire partie de notre communauté solidaire.\n\n" +
                        "Cordialement,\n" +
                        "L'équipe %s",
                utilisateur.getPrenom(),
                utilisateur.getNom(),
                appName,
                appName
        );

        message.setText(texte);
        emailSender.send(message);
    }

    /**
     * Envoyer email de confirmation d'inscription (associations)
     */
    public void envoyerEmailConfirmationInscription(Utilisateur utilisateur) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(utilisateur.getEmail());
        message.setFrom(fromEmail);
        message.setSubject("Demande d'inscription reçue - " + appName);

        String texte = String.format(
                "Bonjour %s %s,\n\n" +
                        "Nous avons bien reçu votre demande d'inscription sur %s.\n\n" +
                        "Votre dossier est actuellement en cours d'examen par notre équipe. " +
                        "Vous recevrez un email de confirmation une fois la validation effectuée.\n\n" +
                        "Ce processus peut prendre 2-3 jours ouvrables.\n\n" +
                        "Merci pour votre patience.\n\n" +
                        "Cordialement,\n" +
                        "L'équipe %s",
                utilisateur.getPrenom(),
                utilisateur.getNom(),
                appName,
                appName
        );

        message.setText(texte);
        emailSender.send(message);
    }

    /**
     * Envoyer email de validation de compte
     */
    public void envoyerEmailValidation(Utilisateur utilisateur) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(utilisateur.getEmail());
        message.setFrom(fromEmail);
        message.setSubject("Votre compte a été validé - " + appName);

        String texte = String.format(
                "Bonjour %s %s,\n\n" +
                        "Excellente nouvelle ! Votre compte a été validé avec succès.\n\n" +
                        "Vous pouvez maintenant vous connecter et accéder à toutes les fonctionnalités de %s.\n\n" +
                        "Connectez-vous dès maintenant pour commencer à créer vos projets et recevoir des dons.\n\n" +
                        "Bienvenue dans notre communauté !\n\n" +
                        "Cordialement,\n" +
                        "L'équipe %s",
                utilisateur.getPrenom(),
                utilisateur.getNom(),
                appName,
                appName
        );

        message.setText(texte);
        emailSender.send(message);
    }

    /**
     * Envoyer email de refus de compte
     */
    public void envoyerEmailRefus(Utilisateur utilisateur, String motif) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(utilisateur.getEmail());
        message.setFrom(fromEmail);
        message.setSubject("Votre demande d'inscription - " + appName);

        String texte = String.format(
                "Bonjour %s %s,\n\n" +
                        "Nous vous remercions pour votre intérêt pour %s.\n\n" +
                        "Après examen de votre dossier, nous ne pouvons malheureusement pas valider votre inscription.\n\n" +
                        "Motif : %s\n\n" +
                        "Si vous pensez qu'il s'agit d'une erreur, n'hésitez pas à nous contacter.\n\n" +
                        "Cordialement,\n" +
                        "L'équipe %s",
                utilisateur.getPrenom(),
                utilisateur.getNom(),
                appName,
                motif != null ? motif : "Non spécifié",
                appName
        );

        message.setText(texte);
        emailSender.send(message);
    }

    /**
     * Envoyer nouveau mot de passe
     */
    public void envoyerNouveauMotDePasse(Utilisateur utilisateur, String nouveauMotDePasse) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(utilisateur.getEmail());
        message.setFrom(fromEmail);
        message.setSubject("Réinitialisation de votre mot de passe - " + appName);

        String texte = String.format(
                "Bonjour %s %s,\n\n" +
                        "Votre mot de passe a été réinitialisé avec succès.\n\n" +
                        "Votre nouveau mot de passe temporaire est : %s\n\n" +
                        "IMPORTANT : Nous vous recommandons fortement de changer ce mot de passe " +
                        "dès votre prochaine connexion pour des raisons de sécurité.\n\n" +
                        "Si vous n'avez pas demandé cette réinitialisation, contactez-nous immédiatement.\n\n" +
                        "Cordialement,\n" +
                        "L'équipe %s",
                utilisateur.getPrenom(),
                utilisateur.getNom(),
                nouveauMotDePasse,
                appName
        );

        message.setText(texte);
        emailSender.send(message);
    }

    /**
     * Envoyer notification de don reçu (association)
     */
    public void envoyerEmailDonRecu(Utilisateur association, String nomDonateur, Double montant, String nomProjet) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(association.getEmail());
        message.setFrom(fromEmail);
        message.setSubject("Nouveau don reçu - " + appName);

        String texte = String.format(
                "Bonjour %s %s,\n\n" +
                        "Excellente nouvelle ! Vous avez reçu un nouveau don.\n\n" +
                        "Détails du don :\n" +
                        "- Montant : %.2f DH\n" +
                        "- Donateur : %s\n" +
                        "- Projet : %s\n\n" +
                        "Merci de continuer à faire la différence !\n\n" +
                        "Cordialement,\n" +
                        "L'équipe %s",
                association.getPrenom(),
                association.getNom(),
                montant,
                nomDonateur,
                nomProjet,
                appName
        );

        message.setText(texte);
        emailSender.send(message);
    }

    /**
     * Envoyer confirmation de don (donateur)
     */
    public void envoyerEmailConfirmationDon(Utilisateur donateur, Double montant, String nomProjet, String nomAssociation) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(donateur.getEmail());
        message.setFrom(fromEmail);
        message.setSubject("Confirmation de votre don - " + appName);

        String texte = String.format(
                "Bonjour %s %s,\n\n" +
                        "Merci pour votre générosité !\n\n" +
                        "Votre don a été effectué avec succès :\n" +
                        "- Montant : %.2f DH\n" +
                        "- Projet : %s\n" +
                        "- Association : %s\n\n" +
                        "Vous pouvez suivre l'impact de votre contribution dans votre espace personnel.\n\n" +
                        "Merci de faire partie du changement !\n\n" +
                        "Cordialement,\n" +
                        "L'équipe %s",
                donateur.getPrenom(),
                donateur.getNom(),
                montant,
                nomProjet,
                nomAssociation,
                appName
        );

        message.setText(texte);
        emailSender.send(message);
    }

    /**
     * Envoyer rappel d'échéance de projet
     */
    public void envoyerRappelEcheance(Utilisateur association, String nomProjet, int joursRestants, double progres) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(association.getEmail());
        message.setFrom(fromEmail);
        message.setSubject("Rappel : Échéance de projet approche - " + appName);

        String texte = String.format(
                "Bonjour %s %s,\n\n" +
                        "Votre projet \"%s\" arrive bientôt à échéance.\n\n" +
                        "Informations :\n" +
                        "- Jours restants : %d\n" +
                        "- Progression actuelle : %.1f%%\n\n" +
                        "N'hésitez pas à promouvoir votre projet pour atteindre votre objectif !\n\n" +
                        "Cordialement,\n" +
                        "L'équipe %s",
                association.getPrenom(),
                association.getNom(),
                nomProjet,
                joursRestants,
                progres,
                appName
        );

        message.setText(texte);
        emailSender.send(message);
    }

    /**
     * Méthode générique pour envoyer un email
     */
    public void envoyerEmail(String destinataire, String sujet, String contenu) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(destinataire);
        message.setFrom(fromEmail);
        message.setSubject(sujet);
        message.setText(contenu);
        emailSender.send(message);
    }

    /**
     * Envoyer email de notification de projet terminé
     */
    public void envoyerEmailProjetTermine(Utilisateur association, String nomProjet, Double montantCollecte) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(association.getEmail());
        message.setFrom(fromEmail);
        message.setSubject("Félicitations ! Projet terminé - " + appName);

        String texte = String.format(
                "Bonjour %s %s,\n\n" +
                        "Félicitations ! Votre projet \"%s\" a atteint son objectif !\n\n" +
                        "Montant total collecté : %.2f DH\n\n" +
                        "Merci d'avoir fait confiance à notre plateforme pour réaliser ce beau projet.\n" +
                        "N'hésitez pas à partager l'impact de ce projet avec vos donateurs.\n\n" +
                        "Encore félicitations !\n\n" +
                        "Cordialement,\n" +
                        "L'équipe %s",
                association.getPrenom(),
                association.getNom(),
                nomProjet,
                montantCollecte,
                appName
        );

        message.setText(texte);
        emailSender.send(message);
    }

    /**
     * Envoyer email de rapport mensuel
     */
    public void envoyerRapportMensuel(Utilisateur utilisateur, String rapport) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(utilisateur.getEmail());
        message.setFrom(fromEmail);
        message.setSubject("Votre rapport mensuel - " + appName);

        String texte = String.format(
                "Bonjour %s %s,\n\n" +
                        "Voici votre rapport d'activité du mois :\n\n" +
                        "%s\n\n" +
                        "Merci de votre engagement sur notre plateforme !\n\n" +
                        "Cordialement,\n" +
                        "L'équipe %s",
                utilisateur.getPrenom(),
                utilisateur.getNom(),
                rapport,
                appName
        );

        message.setText(texte);
        emailSender.send(message);
    }
}