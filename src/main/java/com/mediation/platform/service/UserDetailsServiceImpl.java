package com.mediation.platform.service;

import com.mediation.platform.entity.Utilisateur;
import com.mediation.platform.repository.UtilisateurRepository;
import com.mediation.platform.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé avec l'email: " + email));

        return UserDetailsImpl.build(utilisateur);
    }

    @Transactional
    public UserDetails loadUserById(Long id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé avec l'ID: " + id));

        return UserDetailsImpl.build(utilisateur);
    }
}