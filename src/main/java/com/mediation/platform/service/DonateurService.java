package com.mediation.platform.service;

import com.mediation.platform.entity.Donateur;
import com.mediation.platform.repository.DonateurRepository;
import com.mediation.platform.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DonateurService {

    @Autowired
    private DonateurRepository donateurRepository;

    public List<Donateur> findAll() {
        return donateurRepository.findAll();
    }

    public Donateur findById(Long id) {
        return donateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Donateur non trouvé avec l'ID: " + id));
    }

    public Donateur save(Donateur donateur) {
        return donateurRepository.save(donateur);
    }

    public Donateur update(Long id, Donateur donateur) {
        Donateur existingDonateur = findById(id);

        existingDonateur.setNom(donateur.getNom());
        existingDonateur.setPrenom(donateur.getPrenom());
        existingDonateur.setTelephone(donateur.getTelephone());
        existingDonateur.setAdresse(donateur.getAdresse());
        existingDonateur.setProfession(donateur.getProfession());
        existingDonateur.setDateNaissance(donateur.getDateNaissance());

        return donateurRepository.save(existingDonateur);
    }

    public void deleteById(Long id) {
        donateurRepository.deleteById(id);
    }

    public List<Donateur> findByProfession(String profession) {
        return donateurRepository.findByProfessionContainingIgnoreCase(profession);
    }

    public List<Donateur> findByAge(int ageMin, int ageMax) {
        return donateurRepository.findByAgeBetween(ageMin, ageMax);
    }

    public List<Donateur> findByVille(String ville) {
        return donateurRepository.findByAdresseContainingIgnoreCase(ville);
    }

    public List<Donateur> findDonatorsWithConfirmedDonations() {
        return donateurRepository.findDonatorsWithConfirmedDonations();
    }

    public List<Donateur> findTopDonators() {
        return donateurRepository.findTopDonators();
    }

    public Double getTotalDonsByDonateur(Long donateurId) {
        Donateur donateur = findById(donateurId);
        return donateur.getMontantTotalDons();
    }
}