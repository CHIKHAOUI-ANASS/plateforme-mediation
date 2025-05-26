package com.mediation.platform.service;

import com.mediation.platform.entity.Don;
import com.mediation.platform.entity.Projet;
import com.mediation.platform.repository.DonRepository;
import com.mediation.platform.enums.StatutDon;
import com.mediation.platform.enums.StatutProjet;
import com.mediation.platform.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class DonService {

    @Autowired
    private DonRepository donRepository;

    @Autowired
    private ProjetService projetService;

    public List<Don> findAll() {
        return donRepository.findAll();
    }

    public Don findById(Long id) {
        return donRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Don non trouvé avec l'ID: " + id));
    }

    public Don save(Don don) {
        validateDon(don);
        don.setStatut(StatutDon.EN_ATTENTE);
        don.setDateDon(LocalDateTime.now());
        return donRepository.save(don);
    }

    public Don update(Long id, Don don) {
        Don existingDon = findById(id);
        existingDon.setMontant(don.getMontant());
        existingDon.setCommentaire(don.getCommentaire());
        return donRepository.save(existingDon);
    }

    public void deleteById(Long id) {
        Don don = findById(id);
        don.setStatut(StatutDon.ANNULE);
        donRepository.save(don);
    }

    public List<Don> findByDonateur(Long donateurId) {
        return donRepository.findByDonateurId(donateurId);
    }

    public List<Don> findByProjet(Long projetId) {
        return donRepository.findByProjetId(projetId);
    }

    public List<Don> findByStatut(StatutDon statut) {
        return donRepository.findByStatut(statut);
    }

    public BigDecimal getTotalDonsByProjet(Long projetId) {
        List<Don> dons = donRepository.findByProjetId(projetId);
        return dons.stream()
                .filter(don -> don.getStatut() == StatutDon.VALIDE)
                .map(Don::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalDonsByDonateur(Long donateurId) {
        List<Don> dons = donRepository.findByDonateurId(donateurId);
        return dons.stream()
                .filter(don -> don.getStatut() == StatutDon.VALIDE)
                .map(Don::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Don validerDon(Long donId) {
        Don don = findById(donId);
        don.setStatut(StatutDon.VALIDE);

        // Mettre à jour le montant collecté du projet
        Projet projet = don.getProjet();
        BigDecimal nouveauMontant = getTotalDonsByProjet(projet.getId());
        projetService.mettreAJourMontantCollecte(projet.getId(), nouveauMontant);

        return donRepository.save(don);
    }

    public Don rejeterDon(Long donId) {
        Don don = findById(donId);
        don.setStatut(StatutDon.REFUSE);
        return donRepository.save(don);
    }

    private void validateDon(Don don) {
        if (don.getMontant() == null || don.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant du don doit être supérieur à 0");
        }

        if (don.getDonateur() == null) {
            throw new IllegalArgumentException("Le donateur est obligatoire");
        }

        if (don.getProjet() == null) {
            throw new IllegalArgumentException("Le projet est obligatoire");
        }

        // Vérifier que le projet est actif
        Projet projet = projetService.findById(don.getProjet().getId());
        if (projet.getStatut() != StatutProjet.EN_COURS) {
            throw new IllegalArgumentException("Le projet n'est plus actif pour recevoir des dons");
        }
    }
}
