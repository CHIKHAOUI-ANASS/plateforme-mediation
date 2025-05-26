package com.mediation.platform.repository;

import com.mediation.platform.entity.Administrateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdministrateurRepository extends JpaRepository<Administrateur, Long> {

    // Recherche par niveau d'accès
    List<Administrateur> findByNiveauAcces(String niveauAcces);

    // Recherche par département
    List<Administrateur> findByDepartementContainingIgnoreCase(String departement);

    // Super administrateurs
    @Query("SELECT a FROM Administrateur a WHERE a.niveauAcces = 'SUPER_ADMIN'")
    List<Administrateur> findSuperAdmins();

    // Administrateurs actifs
    @Query("SELECT a FROM Administrateur a WHERE a.statut = 'ACTIF'")
    List<Administrateur> findActiveAdmins();
}