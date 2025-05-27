package com.mediation.platform.dto.mapper;

import com.mediation.platform.dto.request.RegisterAssociationRequest;
import com.mediation.platform.dto.request.RegisterDonateurRequest;
import com.mediation.platform.dto.response.LoginResponse;
import com.mediation.platform.entity.Association;
import com.mediation.platform.entity.Donateur;
import com.mediation.platform.entity.Utilisateur;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {

    /**
     * Convertir Utilisateur vers LoginResponse
     */
    public LoginResponse toLoginResponse(Utilisateur utilisateur) {
        if (utilisateur == null) {
            return new LoginResponse(false, "Utilisateur non trouvé");
        }

        LoginResponse response = new LoginResponse();
        response.setSuccess(true);
        response.setMessage("Connexion réussie");
        response.setIdUtilisateur(utilisateur.getIdUtilisateur());
        response.setNom(utilisateur.getNom());
        response.setPrenom(utilisateur.getPrenom());
        response.setEmail(utilisateur.getEmail());
        response.setRole(utilisateur.getRole());
        response.setStatut(utilisateur.getStatut());
        // Note: Le token JWT sera ajouté plus tard

        return response;
    }

    /**
     * Convertir RegisterDonateurRequest vers Donateur
     */
    public Donateur toDonateurEntity(RegisterDonateurRequest request) {
        if (request == null) {
            return null;
        }

        Donateur donateur = new Donateur();
        donateur.setNom(request.getNom());
        donateur.setPrenom(request.getPrenom());
        donateur.setEmail(request.getEmail());
        donateur.setMotDePasse(request.getMotDePasse()); // Sera encodé dans le service
        donateur.setTelephone(request.getTelephone());
        donateur.setAdresse(request.getAdresse());
        donateur.setDateNaissance(request.getDateNaissance());
        donateur.setProfession(request.getProfession());

        return donateur;
    }

    /**
     * Convertir RegisterAssociationRequest vers Association
     */
    public Association toAssociationEntity(RegisterAssociationRequest request) {
        if (request == null) {
            return null;
        }

        Association association = new Association();
        association.setNom(request.getNom());
        association.setPrenom(request.getPrenom());
        association.setEmail(request.getEmail());
        association.setMotDePasse(request.getMotDePasse()); // Sera encodé dans le service
        association.setTelephone(request.getTelephone());
        association.setNomAssociation(request.getNomAssociation());
        association.setAdresse(request.getAdresse());
        association.setSiteWeb(request.getSiteWeb());
        association.setDescription(request.getDescription());
        association.setDomaineActivite(request.getDomaineActivite());
        association.setDocumentsLegaux(request.getDocumentsLegaux());

        return association;
    }
}
