package com.mediation.platform.entity;


import com.mediation.platform.enums.RoleUtilisateur;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "administrateurs")
@PrimaryKeyJoinColumn(name = "id_utilisateur")
public class Administrateur extends Utilisateur {

    @Size(max = 100, message = "Le niveau d'accès ne peut dépasser 100 caractères")
    @Column(length = 100)
    private String niveauAcces;

    @Size(max = 100, message = "Le département ne peut dépasser 100 caractères")
    @Column(length = 100)
    private String departement;

    // Constructeurs
    public Administrateur() {
        super();
        this.setRole(RoleUtilisateur.ADMINISTRATEUR);
    }

    public Administrateur(String nom, String prenom, String email, String motDePasse) {
        super(nom, prenom, email, motDePasse, RoleUtilisateur.ADMINISTRATEUR);
    }

    public Administrateur(String nom, String prenom, String email, String motDePasse,
                          String niveauAcces, String departement) {
        super(nom, prenom, email, motDePasse, RoleUtilisateur.ADMINISTRATEUR);
        this.niveauAcces = niveauAcces;
        this.departement = departement;
    }

    // Getters et Setters
    public String getNiveauAcces() {
        return niveauAcces;
    }

    public void setNiveauAcces(String niveauAcces) {
        this.niveauAcces = niveauAcces;
    }

    public String getDepartement() {
        return departement;
    }

    public void setDepartement(String departement) {
        this.departement = departement;
    }

    // Méthodes métier
    public void validerAssociation(Association association) {
        if (association == null) {
            throw new IllegalArgumentException("L'association ne peut pas être null");
        }

        association.setStatutValidation(true);
        // Ici, on pourrait ajouter une logique de notification
    }

    public void rejeterAssociation(Association association, String motif) {
        if (association == null) {
            throw new IllegalArgumentException("L'association ne peut pas être null");
        }

        association.setStatutValidation(false);
        // Ici, on pourrait ajouter une logique de notification avec motif
    }

    public void gererUtilisateurs() {
        // Logique de gestion des utilisateurs
        // Cette méthode sera implémentée dans le service
    }

    public List<Transaction> superviserTransactions() {
        // Cette méthode sera implémentée dans le service
        // pour récupérer toutes les transactions
        return null; // Temporaire
    }

    public Map<String, Object> genererStatistiques() {
        Map<String, Object> statistiques = new HashMap<>();

        // Statistiques de base (à implémenter avec les services)
        statistiques.put("timestamp", java.time.LocalDateTime.now());
        statistiques.put("administrateur", this.getNom() + " " + this.getPrenom());

        // Les vraies statistiques seront calculées dans le service
        return statistiques;
    }

    public boolean peutValiderAssociation() {
        return "SUPER_ADMIN".equals(this.niveauAcces) ||
                "VALIDATEUR".equals(this.niveauAcces);
    }

    public boolean peutGererUtilisateurs() {
        return "SUPER_ADMIN".equals(this.niveauAcces);
    }

    public boolean peutSuperviserTransactions() {
        return "SUPER_ADMIN".equals(this.niveauAcces) ||
                "SUPERVISEUR".equals(this.niveauAcces);
    }

    // toString
    @Override
    public String toString() {
        return "Administrateur{" +
                "idUtilisateur=" + getIdUtilisateur() +
                ", nom='" + getNom() + '\'' +
                ", prenom='" + getPrenom() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", niveauAcces='" + niveauAcces + '\'' +
                ", departement='" + departement + '\'' +
                '}';
    }
}
