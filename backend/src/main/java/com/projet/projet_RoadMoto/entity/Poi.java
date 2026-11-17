package com.projet.projet_RoadMoto.entity;

import com.projet.projet_RoadMoto.domain.Source;
import com.projet.projet_RoadMoto.domain.TypePoi;
import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;



@Entity
public class Poi {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String nom;
    @Column(columnDefinition = "geography(Point,4326)", nullable = false)
    private Point localisation;
    private Integer altitudeMetres;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypePoi type;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Source source;
    @Column(unique = true)
    private Long osmId;

    // Égalité sur l'identifiant uniquement : pas de boucle entre associations bidirectionnelles,
    // pas de chargement lazy déclenché, et instanceof accepte les proxies Hibernate.
    // Une entité pas encore persistée n'est égale qu'à elle-même.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Poi other)) return false;
        return id != null && id.equals(other.getId());
    }

    // Constant pour rester stable quand l'id est attribué à la persistance (HashSet, etc.)
    @Override
    public int hashCode() {
        return Poi.class.hashCode();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public Point getLocalisation() {
        return localisation;
    }

    public void setLocalisation(Point localisation) {
        this.localisation = localisation;
    }

    public Integer getAltitudeMetres() {
        return altitudeMetres;
    }

    public void setAltitudeMetres(Integer altitudeMetres) {
        this.altitudeMetres = altitudeMetres;
    }

    public TypePoi getType() {
        return type;
    }

    public void setType(TypePoi type) {
        this.type = type;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public Long getOsmId() {
        return osmId;
    }

    public void setOsmId(Long osmId) {
        this.osmId = osmId;
    }
}
