package com.projet.projet_RoadMoto.entity;

import com.projet.projet_RoadMoto.domain.StatutParticipation;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_participation_sortie_utilisateur",
        columnNames = {"sortie_id", "utilisateur_id"}
))
public class Participation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sortie_id", nullable = false)
    private Sortie sortie;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutParticipation statut;
    @CreationTimestamp
    private OffsetDateTime inscritLe;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Participation that = (Participation) o;
        return Objects.equals(id, that.id) && Objects.equals(sortie, that.sortie) && Objects.equals(utilisateur, that.utilisateur) && statut == that.statut && Objects.equals(inscritLe, that.inscritLe);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, sortie, utilisateur, statut, inscritLe);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Sortie getSortie() {
        return sortie;
    }

    public void setSortie(Sortie sortie) {
        this.sortie = sortie;
    }

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }

    public StatutParticipation getStatut() {
        return statut;
    }

    public void setStatut(StatutParticipation statut) {
        this.statut = statut;
    }

    public OffsetDateTime getInscritLe() {
        return inscritLe;
    }

    public void setInscritLe(OffsetDateTime inscritLe) {
        this.inscritLe = inscritLe;
    }
}
