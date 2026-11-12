package com.projet.projet_RoadMoto.entity;

import com.projet.projet_RoadMoto.domain.Niveau;
import com.projet.projet_RoadMoto.domain.Role;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
public class Utilisateur {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(nullable = false, unique = true)
    private String pseudo;
    @Column(nullable = false, unique = true)
    private String email;
    private String motDePasseHash;
    private String motoModele;
    @Enumerated(EnumType.STRING)
    private Niveau niveau;
    @Enumerated(EnumType.STRING)
    private Role role;
    @CreationTimestamp
    private OffsetDateTime creeLe;

    // Égalité sur l'identifiant uniquement : pas de boucle entre associations bidirectionnelles,
    // pas de chargement lazy déclenché, et instanceof accepte les proxies Hibernate.
    // Une entité pas encore persistée n'est égale qu'à elle-même.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Utilisateur other)) return false;
        return id != 0 && id == other.getId();
    }

    // Constant pour rester stable quand l'id est attribué à la persistance (HashSet, etc.)
    @Override
    public int hashCode() {
        return Utilisateur.class.hashCode();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPseudo() {
        return pseudo;
    }

    public void setPseudo(String pseudo) {
        this.pseudo = pseudo;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMotDePasseHash() {
        return motDePasseHash;
    }

    public void setMotDePasseHash(String motDePasseHash) {
        this.motDePasseHash = motDePasseHash;
    }

    public String getMotoModele() {
        return motoModele;
    }

    public void setMotoModele(String motoModele) {
        this.motoModele = motoModele;
    }

    public Niveau getNiveau() {
        return niveau;
    }

    public void setNiveau(Niveau niveau) {
        this.niveau = niveau;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public OffsetDateTime getCreeLe() {
        return creeLe;
    }

    public void setCreeLe(OffsetDateTime creeLe) {
        this.creeLe = creeLe;
    }
}
