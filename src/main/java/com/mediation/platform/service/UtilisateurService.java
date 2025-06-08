package com.mediation.platform.service;

import com.mediation.platform.entity.Utilisateur;
import com.mediation.platform.enums.RoleUtilisateur;
import com.mediation.platform.enums.StatutUtilisateur;
import com.mediation.platform.exception.ResourceNotFoundException;
import com.mediation.platform.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UtilisateurService {

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    public Utilisateur findById(Long id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'ID: " + id));
    }

    public Optional<Utilisateur> findByEmail(String email) {
        return utilisateurRepository.findByEmail(email);
    }

    public List<Utilisateur> findAll() {
        return utilisateurRepository.findAll();
    }

    public List<Utilisateur> findByRole(RoleUtilisateur role) {
        return utilisateurRepository.findByRole(role);
    }

    public List<Utilisateur> findByStatut(StatutUtilisateur statut) {
        return utilisateurRepository.findByStatut(statut);
    }

    public List<Utilisateur> findActiveUsers() {
        return utilisateurRepository.findActiveUsers();
    }

    public List<Utilisateur> searchByName(String nom, String prenom) {
        return utilisateurRepository.findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCase(nom, prenom);
    }

    public Utilisateur save(Utilisateur utilisateur) {
        return utilisateurRepository.save(utilisateur);
    }

    public void deleteById(Long id) {
        Utilisateur utilisateur = findById(id);
        utilisateurRepository.delete(utilisateur);
    }

    public boolean existsById(Long id) {
        return utilisateurRepository.existsById(id);
    }

    public boolean existsByEmail(String email) {
        return utilisateurRepository.existsByEmail(email);
    }

    public long count() {
        return utilisateurRepository.count();
    }

    public long countByRole(RoleUtilisateur role) {
        return utilisateurRepository.countByRole(role);
    }

    public Utilisateur changerStatut(Long id, StatutUtilisateur nouveauStatut) {
        Utilisateur utilisateur = findById(id);
        utilisateur.setStatut(nouveauStatut);
        return utilisateurRepository.save(utilisateur);
    }

    public Utilisateur activerUtilisateur(Long id) {
        return changerStatut(id, StatutUtilisateur.ACTIF);
    }

    public Utilisateur desactiverUtilisateur(Long id) {
        return changerStatut(id, StatutUtilisateur.INACTIF);
    }

    public Utilisateur suspendreUtilisateur(Long id) {
        return changerStatut(id, StatutUtilisateur.SUSPENDU);
    }
}