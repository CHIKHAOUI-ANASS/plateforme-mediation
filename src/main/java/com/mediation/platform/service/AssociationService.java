package com.mediation.platform.service;

import com.mediation.platform.entity.Association;
import com.mediation.platform.repository.AssociationRepository;
import com.mediation.platform.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class AssociationService {

    @Autowired
    private AssociationRepository associationRepository;

    public List<Association> findAll() {
        return associationRepository.findAll();
    }

    public Association findById(Long id) {
        return associationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Association non trouvée avec l'ID: " + id));
    }

    public Association save(Association association) {
        association.setStatutValidation(false); // En attente de validation
        return associationRepository.save(association);
    }

    public Association update(Long id, Association association) {
        Association existingAssociation = findById(id);

        existingAssociation.setNom(association.getNom());
        existingAssociation.setNomAssociation(association.getNomAssociation());
        existingAssociation.setDescription(association.getDescription());
        existingAssociation.setDomaineActivite(association.getDomaineActivite());
        existingAssociation.setAdresse(association.getAdresse());
        existingAssociation.setTelephone(association.getTelephone());
        existingAssociation.setSiteWeb(association.getSiteWeb());

        return associationRepository.save(existingAssociation);
    }

    public void deleteById(Long id) {
        associationRepository.deleteById(id);
    }

    public List<Association> findByNom(String nom) {
        return associationRepository.findByNomAssociationContainingIgnoreCase(nom);
    }

    public List<Association> findByDomaine(String domaine) {
        return associationRepository.findByDomaineActiviteContainingIgnoreCase(domaine);
    }

    public List<Association> findByVille(String ville) {
        return associationRepository.findByAdresseContainingIgnoreCase(ville);
    }

    public List<Association> findValidatedAssociations() {
        return associationRepository.findByStatutValidationTrue();
    }

    public List<Association> findPendingAssociations() {
        return associationRepository.findByStatutValidationFalse();
    }

    public List<Association> findWithActiveProjects() {
        return associationRepository.findWithActiveProjects();
    }

    public List<Association> findTopAssociations() {
        return associationRepository.findTopAssociationsByDonations();
    }

    public List<Association> findRecentlyValidated(LocalDateTime dateDebut) {
        return associationRepository.findRecentlyValidated(dateDebut);
    }

    public Association validerAssociation(Long id) {
        Association association = findById(id);
        association.setStatutValidation(true);
        association.setDateValidation(LocalDateTime.now());
        return associationRepository.save(association);
    }

    public Association rejeterAssociation(Long id) {
        Association association = findById(id);
        association.setStatutValidation(false);
        return associationRepository.save(association);
    }
}
