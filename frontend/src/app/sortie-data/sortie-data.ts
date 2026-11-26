export interface Sortie {
  id: number;
  titre: string;
  description: string | null;
  dateDepart: Date;
  nomLieuDepart: string;
  latitude: number;
  longitude: number;
  niveau: 'TOURING' | 'SPORT' | 'SPORTIF' | 'DECOUVERTE';
  statut: 'PUBLIEE' | 'ANNULEE' | 'COMPLETE' | 'BROUILLON' | 'TERMINEE';
  placesMax: number;
  placesRestantes: number;
  organisateur: {
    id: number;
    pseudo: string;
  };
  creeLe: Date;
}
