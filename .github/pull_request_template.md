## Objectif
Compléter le cycle de vie d'une sortie et d'une participation : consultation du
détail, désistement d'un participant, annulation et suppression par l'organisateur.

## Contenu
- `GET /api/sorties/{id}` — un brouillon n'est visible que de son organisateur
- `DELETE /api/sorties/{id}/participation` — désistement, avec retour de `COMPLETE`
  à `PUBLIEE` lorsqu'une place se libère
- `POST /api/sorties/{id}/annuler` — réservé à l'organisateur
- `DELETE /api/sorties/{id}` — suppression limitée aux brouillons
- Tests des règles de visibilité et des nouvelles transitions

## Notes
**Visibilité des brouillons** : un brouillon inaccessible renvoie `404` et non `403`,
pour ne pas confirmer l'existence de la ressource à un appelant non autorisé.

**Désistement** : la participation passe en `DESISTE` plutôt que d'être supprimée,
afin de conserver l'historique. Une réinscription réactive la ligne existante au lieu
d'en créer une seconde, ce que la contrainte d'unicité interdirait.

**Suppression ou annulation** : seul un brouillon peut être supprimé — personne ne
l'a vu, aucun participant n'est engagé. Une sortie publiée s'annule : elle reste en
base avec le statut `ANNULEE` et ses participants conservent le statut `INSCRIT`,
puisqu'aucun d'eux ne s'est désisté. Chaque entité porte ainsi son propre fait.

## À suivre
`GET /api/me/sorties` reste à faire : c'est le seul écran où un participant pourra
constater qu'une sortie a été annulée, la liste publique ne retournant que les
sorties publiées.