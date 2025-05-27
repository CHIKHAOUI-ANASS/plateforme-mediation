package com.mediation.platform.service;

import com.mediation.platform.entity.Utilisateur;
import com.mediation.platform.enums.RoleUtilisateur;
import com.mediation.platform.enums.StatutUtilisateur;
import com.mediation.platform.exception.ResourceNotFoundException;
import com.mediation.platform.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UtilisateurService {

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<Utilisateur> findAll() {
        return utilisateurRepository.findAll();
    }

    public Utilisateur findById(Long id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'ID: " + id));
    }

    public Optional<Utilisateur> findByEmail(String email) {
        return utilisateurRepository.findByEmail(email);
    }

    public Utilisateur save(Utilisateur utilisateur) {
        validateUtilisateur(utilisateur);

        // Encoder le mot de passe
        if (utilisateur.getMotDePasse() != null) {
            utilisateur.setMotDePasse(passwordEncoder.encode(utilisateur.getMotDePasse()));
        }

        return utilisateurRepository.save(utilisateur);
    }

    public Utilisateur update(Long id, Utilisateur utilisateur) {
        Utilisateur existingUser = findById(id);

        existingUser.setNom(utilisateur.getNom());
        existingUser.setPrenom(utilisateur.getPrenom());
        existingUser.setTelephone(utilisateur.getTelephone());

        return utilisateurRepository.save(existingUser);
    }

    public void deleteById(Long id) {
        Utilisateur utilisateur = findById(id);
        utilisateur.setStatut(StatutUtilisateur.INACTIF);
        utilisateurRepository.save(utilisateur);
    }

    public boolean existsByEmail(String email) {
        return utilisateurRepository.existsByEmail(email);
    }

    public List<Utilisateur> findByStatut(StatutUtilisateur statut) {
        return utilisateurRepository.findByStatut(statut);
    }

    public List<Utilisateur> findByRole(RoleUtilisateur role) {
        return utilisateurRepository.findByRole(role);
    }

    public List<Utilisateur> findActiveUsers() {
        return utilisateurRepository.findActiveUsers();
    }

    public long countByRole(RoleUtilisateur role) {
        return utilisateurRepository.countByRole(role);
    }

    public List<Utilisateur> searchByName(String nom, String prenom) {
        return utilisateurRepository.findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCase(nom, prenom);
    }

    private void validateUtilisateur(Utilisateur utilisateur) {
        if (utilisateur.getEmail() == null || utilisateur.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("L'email est obligatoire");
        }

        if (existsByEmail(utilisateur.getEmail())) {
            throw new IllegalArgumentException("Un utilisateur avec cet email existe déjà");
        }

        if (utilisateur.getMotDePasse() == null || utilisateur.getMotDePasse().length() < 6) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins 6 caractères");
        }
    }
}
