package com.projet.projet_RoadMoto.entity;

import com.projet.projet_RoadMoto.domain.Source;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.locationtech.jts.geom.LineString;


import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Itineraire {
    @Id
    private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "sortie_id")
    private Sortie sortie;
    @Column(name = "trace", columnDefinition = "geography(LineString,4326)", nullable = false)
    private LineString trace;
    @Column(name = "distance_m", nullable = false)
    private Integer distanceM;
    @Column(name = "duree_s", nullable = false)
    private Integer dureeS;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Source source;
    @OneToMany(mappedBy = "itineraire", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordre")
    private List<Etape> etapes = new ArrayList<>();
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime calculeLe;

    // Égalité sur l'identifiant uniquement : pas de boucle entre associations bidirectionnelles,
    // pas de chargement lazy déclenché, et instanceof accepte les proxies Hibernate.
    // Une entité pas encore persistée n'est égale qu'à elle-même.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Itineraire other)) return false;
        return id != null && id.equals(other.getId());
    }

    // Constant pour rester stable quand l'id est attribué à la persistance (HashSet, etc.)
    @Override
    public int hashCode() {
        return Itineraire.class.hashCode();
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

    public Integer getDistanceM() {
        return distanceM;
    }

    public void setDistanceM(Integer distanceM) {
        this.distanceM = distanceM;
    }

    public LineString getTrace() {
        return trace;
    }

    public void setTrace(LineString trace) {
        this.trace = trace;
    }

    public Integer getDureeS() {
        return dureeS;
    }

    public void setDureeS(Integer dureeS) {
        this.dureeS = dureeS;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public OffsetDateTime getCalculeLe() {
        return calculeLe;
    }

    public void setCalculeLe(OffsetDateTime calculeLe) {
        this.calculeLe = calculeLe;
    }

    public List<Etape> getEtapes() {
        return etapes;
    }

    public void ajouterEtape(Etape etape){
        etapes.add(etape);
        etape.setItineraire(this);
        etape.setOrdre(etapes.size() - 1);
    }
}
