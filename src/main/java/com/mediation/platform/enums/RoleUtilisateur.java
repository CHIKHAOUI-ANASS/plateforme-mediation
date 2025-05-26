package com.mediation.platform.enums;

public enum RoleUtilisateur {
    DONATEUR("Donateur"),
    ASSOCIATION("Association"),
    ADMINISTRATEUR("Administrateur");

    private final String libelle;

    RoleUtilisateur(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
