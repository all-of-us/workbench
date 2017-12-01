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
    this.homeData = achillesService.getDomains().subscribe(results => {
      this.homeData = results;
    });
  }

  receiveData(data) {
    // this.infoData = data;
    const title = data.domain_display;
    const id = data.domain_id;
    const desc = data.domain_desc;
    const parent = data.domain_parent;
    const route = data.domain_route;
    this.infoData = { domain_id: id, domain_display: title,
        domain_desc: desc, domain_parent: parent, domain_route: route };
  }

  passAnchor(data) {
    this.anchor = data;
  }
}
