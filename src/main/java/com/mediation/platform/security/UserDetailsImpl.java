package com.mediation.platform.security;

import com.mediation.platform.entity.Utilisateur;
import com.mediation.platform.enums.StatutUtilisateur;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class UserDetailsImpl implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final String role;
    private final StatutUtilisateur statut;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserDetailsImpl(Long id, String email, String password, String role,
                           StatutUtilisateur statut, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.role = role;
        this.statut = statut;
        this.authorities = authorities;
    }

    public static UserDetailsImpl build(Utilisateur utilisateur) {
        // Convertir le rôle en autorité Spring Security
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + utilisateur.getRole().name());

        return new UserDetailsImpl(
                utilisateur.getIdUtilisateur(),
                utilisateur.getEmail(),
                utilisateur.getMotDePasse(),
                utilisateur.getRole().name(),
                utilisateur.getStatut(),
                Collections.singletonList(authority)
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return statut != StatutUtilisateur.SUSPENDU;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return statut == StatutUtilisateur.ACTIF;
    }

    // Getters supplémentaires
    public Long getId() {
        return id;
    }

    public String getRole() {
        return role;
    }

    public StatutUtilisateur getStatut() {
        return statut;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserDetailsImpl that = (UserDetailsImpl) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
