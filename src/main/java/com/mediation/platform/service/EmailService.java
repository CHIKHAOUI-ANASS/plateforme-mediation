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

    @Value("${app.email.from}")
    private String fromEmail;

    public void envoyerEmailBienvenue(String destinataire, String nom) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(destinataire);
        message.setSubject("Bienvenue sur la plateforme de médiation");
        message.setText("Bonjour " + nom + ",\n\n" +
                "Bienvenue sur notre plateforme de médiation entre donateurs et associations caritatives.\n" +
                "Votre compte a été créé avec succès.\n\n" +
                "Cordialement,\nL'équipe de la plateforme");

        mailSender.send(message);
    }

    public void envoyerEmailValidationAssociation(String destinataire, String nomAssociation) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(destinataire);
        message.setSubject("Validation de votre association");
        message.setText("Bonjour,\n\n" +
                "Votre association '" + nomAssociation + "' a été validée avec succès.\n" +
                "Vous pouvez maintenant créer des projets et recevoir des dons.\n\n" +
                "Cordialement,\nL'équipe de la plateforme");

        mailSender.send(message);
    }

    public void envoyerEmailDonRecu(String destinataire, String nomDonateur, String montant, String nomProjet) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(destinataire);
        message.setSubject("Nouveau don reçu");
        message.setText("Bonjour,\n\n" +
                "Vous avez reçu un nouveau don de " + montant + " DH de " + nomDonateur +
                " pour votre projet '" + nomProjet + "'.\n\n" +
                "Cordialement,\nL'équipe de la plateforme");

        mailSender.send(message);
    }

    public void envoyerEmailDonValide(String destinataire, String nomProjet, String montant) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(destinataire);
        message.setSubject("Don validé");
        message.setText("Bonjour,\n\n" +
                "Votre don de " + montant + " DH pour le projet '" + nomProjet + "' a été validé.\n" +
                "Merci pour votre générosité !\n\n" +
                "Cordialement,\nL'équipe de la plateforme");

        mailSender.send(message);
    }

    public void envoyerEmailProjetComplete(String destinataire, String nomProjet) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(destinataire);
        message.setSubject("Projet complété");
        message.setText("Félicitations !\n\n" +
                "Votre projet '" + nomProjet + "' a atteint son objectif financier.\n" +
                "Merci à tous les donateurs qui ont contribué à ce succès.\n\n" +
                "Cordialement,\nL'équipe de la plateforme");

        mailSender.send(message);
    }

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

        mailSender.send(message);
    }
}
