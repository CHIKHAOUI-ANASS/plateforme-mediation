package com.mediation.platform.service;

import com.mediation.platform.entity.Utilisateur;
import com.mediation.platform.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class AuthenticationService {

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Optional<Utilisateur> authenticate(String email, String motDePasse) {
        Optional<Utilisateur> utilisateur = utilisateurRepository.findByEmail(email);

        if (utilisateur.isPresent() && passwordEncoder.matches(motDePasse, utilisateur.get().getMotDePasse())) {
            // Mettre à jour la dernière connexion
            Utilisateur user = utilisateur.get();
            user.setDerniereConnexion(LocalDateTime.now());
            utilisateurRepository.save(user);
            return utilisateur;
        }

        return Optional.empty();
    }

    public boolean changerMotDePasse(Long utilisateurId, String ancienMotDePasse, String nouveauMotDePasse) {
        Optional<Utilisateur> utilisateur = utilisateurRepository.findById(utilisateurId);

        if (utilisateur.isPresent() && passwordEncoder.matches(ancienMotDePasse, utilisateur.get().getMotDePasse())) {
            Utilisateur user = utilisateur.get();
            user.setMotDePasse(passwordEncoder.encode(nouveauMotDePasse));
            user.setDateModification(LocalDateTime.now());
            utilisateurRepository.save(user);
            return true;
        }

        return false;
    }

    public void resetMotDePasse(String email, String nouveauMotDePasse) {
        Optional<Utilisateur> utilisateur = utilisateurRepository.findByEmail(email);

        if (utilisateur.isPresent()) {
            Utilisateur user = utilisateur.get();
            user.setMotDePasse(passwordEncoder.encode(nouveauMotDePasse));
            user.setDateModification(LocalDateTime.now());
            utilisateurRepository.save(user);
        }
    }
}
