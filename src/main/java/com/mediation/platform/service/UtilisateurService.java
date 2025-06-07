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

    /**
     * Trouver un utilisateur par ID
     */
    public Utilisateur findById(Long id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'ID: " + id));
    }

    /**
     * Trouver un utilisateur par email
     */
    public Optional<Utilisateur> findByEmail(String email) {
        return utilisateurRepository.findByEmail(email);
    }

    /**
     * Trouver tous les utilisateurs
     */
    public List<Utilisateur> findAll() {
        return utilisateurRepository.findAll();
    }

    /**
     * Utilisateurs par rôle
     */
    public List<Utilisateur> findByRole(RoleUtilisateur role) {
        return utilisateurRepository.findByRole(role);
    }

    /**
     * Utilisateurs par statut
     */
    public List<Utilisateur> findByStatut(StatutUtilisateur statut) {
        return utilisateurRepository.findByStatut(statut);
    }

    /**
     * Utilisateurs actifs
     */
    public List<Utilisateur> findActiveUsers() {
        return utilisateurRepository.findActiveUsers();
    }

    /**
     * Rechercher utilisateurs par nom/prénom
     */
    public List<Utilisateur> searchByName(String nom, String prenom) {
        return utilisateurRepository.findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCase(nom, prenom);
    }

    /**
     * Sauvegarder un utilisateur
     */
    public Utilisateur save(Utilisateur utilisateur) {
        return utilisateurRepository.save(utilisateur);
    }

    /**
     * Supprimer un utilisateur
     */
    public void deleteById(Long id) {
        Utilisateur utilisateur = findById(id);
        utilisateurRepository.delete(utilisateur);
    }

    /**
     * Vérifier si un utilisateur existe
     */
    public boolean existsById(Long id) {
        return utilisateurRepository.existsById(id);
    }

    /**
     * Vérifier si un email existe
     */
    public boolean existsByEmail(String email) {
        return utilisateurRepository.existsByEmail(email);
    }

    /**
     * Compter le nombre total d'utilisateurs
     */
    public long count() {
        return utilisateurRepository.count();
    }

    /**
     * Compter par rôle
     */
    public long countByRole(RoleUtilisateur role) {
        return utilisateurRepository.countByRole(role);
    }

    /**
     * Changer le statut d'un utilisateur
     */
    public Utilisateur changerStatut(Long id, StatutUtilisateur nouveauStatut) {
        Utilisateur utilisateur = findById(id);
        utilisateur.setStatut(nouveauStatut);
        return utilisateurRepository.save(utilisateur);
    }

    /**
     * Activer un utilisateur
     */
    public Utilisateur activerUtilisateur(Long id) {
        return changerStatut(id, StatutUtilisateur.ACTIF);
    }

    /**
     * Désactiver un utilisateur
     */
    public Utilisateur desactiverUtilisateur(Long id) {
        return changerStatut(id, StatutUtilisateur.INACTIF);
    }

    /**
     * Suspendre un utilisateur
     */
    public Utilisateur suspendreUtilisateur(Long id) {
        return changerStatut(id, StatutUtilisateur.SUSPENDU);
    }
}