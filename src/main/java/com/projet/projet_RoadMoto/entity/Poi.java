package com.projet.projet_RoadMoto.entity;

import com.projet.projet_RoadMoto.domain.Source;
import com.projet.projet_RoadMoto.domain.TypePoi;
import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;

import java.util.Objects;


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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Poi poi = (Poi) o;
        return Objects.equals(id, poi.id) && Objects.equals(nom, poi.nom) && Objects.equals(localisation, poi.localisation) && Objects.equals(altitudeMetres, poi.altitudeMetres) && type == poi.type && source == poi.source && Objects.equals(osmId, poi.osmId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nom, localisation, altitudeMetres, type, source, osmId);
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
