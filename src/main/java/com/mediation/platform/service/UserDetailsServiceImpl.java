package com.mediation.platform.service;

import com.mediation.platform.entity.Utilisateur;
import com.mediation.platform.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé avec l'email: " + email));

        return User.builder()
                .username(utilisateur.getEmail())
                .password(utilisateur.getMotDePasse())
                .authorities(getAuthorities(utilisateur))
                .accountExpired(false)
                .accountLocked(!utilisateur.estActif())
                .credentialsExpired(false)
                .disabled(!utilisateur.estActif())
                .build();
    }

    private Collection<? extends GrantedAuthority> getAuthorities(Utilisateur utilisateur) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Ajouter le rôle avec le préfixe ROLE_
        authorities.add(new SimpleGrantedAuthority("ROLE_" + utilisateur.getRole().name()));

        return authorities;
    }
}