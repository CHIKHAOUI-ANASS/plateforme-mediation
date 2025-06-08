// 2. DonateurRepository.java
package com.mediation.platform.repository;

import com.mediation.platform.entity.Donateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DonateurRepository extends JpaRepository<Donateur, Long> {

    // Recherche par profession
    List<Donateur> findByProfessionContainingIgnoreCase(String profession);

    // Recherche par âge (calculé à partir de la date de naissance)
    @Query("SELECT d FROM Donateur d WHERE YEAR(CURRENT_DATE) - YEAR(d.dateNaissance) BETWEEN :ageMin AND :ageMax")
    List<Donateur> findByAgeBetween(@Param("ageMin") int ageMin, @Param("ageMax") int ageMax);

    // Donateurs qui ont fait des dons confirmés
    @Query("SELECT DISTINCT d FROM Donateur d JOIN d.dons don WHERE don.statut = 'VALIDE'")
    List<Donateur> findDonatorsWithConfirmedDonations();

    // Top donateurs par montant total
    @Query("SELECT d FROM Donateur d JOIN d.dons don WHERE don.statut = 'VALIDE' " +
            "GROUP BY d ORDER BY SUM(don.montant) DESC")
    List<Donateur> findTopDonators();

    // Recherche par ville/région
    List<Donateur> findByAdresseContainingIgnoreCase(String ville);
}