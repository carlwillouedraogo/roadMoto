import { Component, signal } from '@angular/core';
import { ListeSortieComponent } from './liste-sortie-component/liste-sortie-component';

@Component({
  imports: [ListeSortieComponent],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {
  protected readonly title = signal('frontend');
}
