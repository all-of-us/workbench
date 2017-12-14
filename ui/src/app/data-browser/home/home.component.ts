import { Component } from '@angular/core';
import { AchillesService } from '../services/achilles.service';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent {
  homeData;
  infoData = {};
  anchor;
  pageTitle = 'AoU Data Browser Home';

  constructor( private achillesService: AchillesService) {
    this.homeData = this.achillesService.getDomains().subscribe(results => {
      this.homeData = results;
      console.log(this.homeData);
    });
  }

  // Recieve selected domain from tree browser menu
  receiveData(data) {
    this.infoData = data;
  }

  passAnchor(data) {
    this.anchor = data;
  }
}
