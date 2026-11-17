CREATE TABLE utilisateur (
         id               BIGSERIAL PRIMARY KEY,
         email            VARCHAR(255) NOT NULL UNIQUE,
         pseudo           VARCHAR(50)  NOT NULL UNIQUE,
         mot_de_passe_hash VARCHAR(255) NOT NULL,
         moto_modele      VARCHAR(100),
         niveau           VARCHAR(20)  NOT NULL,
         role             VARCHAR(20)  NOT NULL DEFAULT 'USER',
         cree_le          TIMESTAMPTZ  NOT NULL DEFAULT now()
);
