package com.mediation.platform.enums;

public enum TypeNotification {
    DON_RECU("Don reçu"),
    DON_VALIDE("Don validé"),
    DON_REFUSE("Don refusé"),
    PROJET_COMPLETE("Projet complété"),
    PROJET_EXPIRE("Projet expiré"),
    NOUVEAU_PROJET("Nouveau projet"),
    VALIDATION_ASSOCIATION("Validation association"),
    REFUS_ASSOCIATION("Refus association"),
    RAPPEL_ECHEANCE("Rappel échéance"),
    MISE_A_JOUR_PROFIL("Mise à jour profil"),
    SECURITE("Sécurité"),
    SYSTEME("Système");




    private final String libelle;

    TypeNotification(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
