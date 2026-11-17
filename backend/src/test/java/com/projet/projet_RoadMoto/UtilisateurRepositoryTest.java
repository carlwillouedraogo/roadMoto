package com.projet.projet_RoadMoto;

import com.projet.projet_RoadMoto.domain.Niveau;
import com.projet.projet_RoadMoto.domain.Role;
import com.projet.projet_RoadMoto.entity.Utilisateur;
import com.projet.projet_RoadMoto.repository.UtilisateurRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import org.testcontainers.junit.jupiter.Testcontainers;


import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(TestcontainersConfig.class)
public class UtilisateurRepositoryTest {

    @Autowired
    private UtilisateurRepository repository;

    /**
        Test pour vérifier si l'utilisateur est bien enregistré
     */
    @Test
    void enregistreUtilisateur(){
        Utilisateur u = new Utilisateur();
        u.setEmail("mirror@example.com");
        u.setPseudo("mirror");
        u.setMotDePasseHash("hash");
        u.setNiveau(Niveau.TOURING);
        u.setRole(Role.USER);

        Utilisateur sauve = repository.save(u);
        Assertions.assertNotNull(sauve);
        Assertions.assertNotNull(sauve.getCreeLe());
        Assertions.assertTrue(repository.existsByEmail("mirror@example.com"));
        Assertions.assertNotNull(sauve.getId());
    }

    /**
     * Test pour vérifier si l'email existant existe déjà
     */

    @Test
    void refuseEmailDoublon(){
        repository.saveAndFlush(creerUtilisateur("doublon@example.com"));

        Utilisateur second = creerUtilisateur("doublon@example.com");
        second.setPseudo("autre");

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(second));
    }

    /**
     * Méthode permettant de créér un utilisateur
     * @param mail
     * @return Utilisateur
     */

    private Utilisateur creerUtilisateur(String mail) {
        Utilisateur u = new Utilisateur();
        u.setEmail(mail);
        u.setPseudo("doublon");
        u.setMotDePasseHash("doublon");
        u.setNiveau(Niveau.TOURING);
        u.setRole(Role.USER);
        return u;
    }
}
