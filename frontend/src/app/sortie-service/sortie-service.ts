import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Sortie } from '../sortie-data/sortie-data';
import { Page } from '../sortie-data/page';

@Injectable({
  providedIn: 'root',
})

export class SortieService {
  private http = inject(HttpClient);

  private readonly url = '/api/sorties';

  private sorties = signal<Page<Sortie>>({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 0 });
  private _chargement = signal(false);
  private _erreur = signal<string | null>(null);

  readonly allSorties = computed(() => this.sorties().content);
  readonly chargement = this._chargement.asReadonly();
  readonly erreur = this._erreur.asReadonly();

  chargerSorties() {
    this._chargement.set(true);
    this._erreur.set(null);

    this.http.get<Page<Sortie>>(this.url).subscribe({
      next: (page) => {
        this.sorties.set(page);
        this._chargement.set(false);
      },
      error: (err) => {
        this._erreur.set(err.message);
        this._chargement.set(false);
      }
    });
  }

}
