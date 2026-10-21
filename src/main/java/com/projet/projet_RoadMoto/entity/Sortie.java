package com.projet.projet_RoadMoto.entity;

import com.projet.projet_RoadMoto.domain.Niveau;
import com.projet.projet_RoadMoto.domain.Statut;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.locationtech.jts.geom.Point;

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
public class Sortie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String titre;
    private String description;
    private OffsetDateTime dateDepart;
    private String nomLieuDepart;
    @Column(columnDefinition = "geography(Point,4326)")
    private Point lieuDepart;
    private Integer distanceKm;
    private Integer dureeEstimeeMin;
    @Enumerated(EnumType.STRING)
    private Niveau niveau;
    private int placesMax;
    @Enumerated(EnumType.STRING)
    private Statut statut;
    @Version
    private int version;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organisateur_id", nullable = false)
    private Utilisateur organisateur;
    @CreationTimestamp
    private OffsetDateTime creeLe;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Sortie sortie = (Sortie) o;
        return distanceKm == sortie.distanceKm && dureeEstimeeMin == sortie.dureeEstimeeMin && placesMax == sortie.placesMax && version == sortie.version && Objects.equals(id, sortie.id) && Objects.equals(titre, sortie.titre) && Objects.equals(description, sortie.description) && Objects.equals(dateDepart, sortie.dateDepart) && Objects.equals(nomLieuDepart, sortie.nomLieuDepart) && Objects.equals(lieuDepart, sortie.lieuDepart) && niveau == sortie.niveau && statut == sortie.statut && Objects.equals(organisateur, sortie.organisateur) && Objects.equals(creeLe, sortie.creeLe);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, titre, description, dateDepart, nomLieuDepart, lieuDepart, distanceKm, dureeEstimeeMin, niveau, placesMax, statut, version, organisateur, creeLe);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public OffsetDateTime getDateDepart() {
        return dateDepart;
    }

    public void setDateDepart(OffsetDateTime dateDepart) {
        this.dateDepart = dateDepart;
    }

    public String getNomLieuDepart() {
        return nomLieuDepart;
    }

    public void setNomLieuDepart(String nomLieuDepart) {
        this.nomLieuDepart = nomLieuDepart;
    }

    public Point getLieuDepart() {
        return lieuDepart;
    }

    public void setLieuDepart(Point lieuDepart) {
        this.lieuDepart = lieuDepart;
    }

    public Integer getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Integer distanceKm) {
        this.distanceKm = distanceKm;
    }

    public Integer getDureeEstimeeMin() {
        return dureeEstimeeMin;
    }

    public void setDureeEstimeeMin(Integer dureeEstimeeMin) {
        this.dureeEstimeeMin = dureeEstimeeMin;
    }

    public Niveau getNiveau() {
        return niveau;
    }

    public void setNiveau(Niveau niveau) {
        this.niveau = niveau;
    }

    public int getPlacesMax() {
        return placesMax;
    }

    public void setPlacesMax(int placesMax) {
        this.placesMax = placesMax;
    }

    public Statut getStatut() {
        return statut;
    }

    public void setStatut(Statut statut) {
        this.statut = statut;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Utilisateur getOrganisateur() {
        return organisateur;
    }

    public void setOrganisateur(Utilisateur organisateur) {
        this.organisateur = organisateur;
    }

    public OffsetDateTime getCreeLe() {
        return creeLe;
    }

    public void setCreeLe(OffsetDateTime creeLe) {
        this.creeLe = creeLe;
    }
}
