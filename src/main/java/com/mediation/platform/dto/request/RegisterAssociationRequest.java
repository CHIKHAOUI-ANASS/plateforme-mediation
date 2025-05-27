package com.mediation.platform.dto.request;

import jakarta.validation.constraints.*;

public class RegisterAssociationRequest {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100, message = "Le nom ne peut dépasser 100 caractères")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(max = 100, message = "Le prénom ne peut dépasser 100 caractères")
    private String prenom;

    @Email(message = "Format email invalide")
    @NotBlank(message = "L'email est obligatoire")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 6, message = "Le mot de passe doit avoir au moins 6 caractères")
    private String motDePasse;

    @Size(max = 20, message = "Le téléphone ne peut dépasser 20 caractères")
    private String telephone;

    @NotBlank(message = "Le nom de l'association est obligatoire")
    @Size(max = 200, message = "Le nom de l'association ne peut dépasser 200 caractères")
    private String nomAssociation;

    @Size(max = 500, message = "L'adresse ne peut dépasser 500 caractères")
    private String adresse;

    @Size(max = 255, message = "L'URL du site web ne peut dépasser 255 caractères")
    private String siteWeb;

    private String description;

    @Size(max = 100, message = "Le domaine d'activité ne peut dépasser 100 caractères")
    private String domaineActivite;

    @Size(max = 500, message = "Les documents légaux ne peuvent dépasser 500 caractères")
    private String documentsLegaux;

    // Constructeurs
    public RegisterAssociationRequest() {}

    // Getters et Setters
    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMotDePasse() {
        return motDePasse;
    }

    public void setMotDePasse(String motDePasse) {
        this.motDePasse = motDePasse;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getNomAssociation() {
        return nomAssociation;
    }

    public void setNomAssociation(String nomAssociation) {
        this.nomAssociation = nomAssociation;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getSiteWeb() {
        return siteWeb;
    }

    public void setSiteWeb(String siteWeb) {
        this.siteWeb = siteWeb;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDomaineActivite() {
        return domaineActivite;
    }

    public void setDomaineActivite(String domaineActivite) {
        this.domaineActivite = domaineActivite;
    }

    public String getDocumentsLegaux() {
        return documentsLegaux;
    }

    public void setDocumentsLegaux(String documentsLegaux) {
        this.documentsLegaux = documentsLegaux;
    }
}