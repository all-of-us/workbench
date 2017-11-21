import { Component, OnInit, Input } from '@angular/core';
import { AchillesService } from '../services/achilles.service';
import { Router, ActivatedRoute, ParamMap } from '@angular/router';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit {
  homeData
  infoData = {}
  routeId
  anchor
  pageTitle = "AoU Data Browser Home"

  constructor(private router: Router, private achillesService: AchillesService, private route: ActivatedRoute) {



    /* achillesService.getDomains()
      .subscribe(results => {
        this.homeData = results;
      }) // end of getDomains.subscibe
     */
  }
  ngOnInit() {
  }

  reciveData(data) {
    // this.infoData = data;

    let title = data.domain_display;
    let id = data.domain_id;
    let desc = data.domain_desc;
    let parent = data.domain_parent;
    let route = data.domain_route;
    this.infoData = { domain_id: id, domain_display: title, domain_desc: desc, domain_parent: parent,domain_route:route }
  }

passAnchor(data) {
  this.anchor = data
}
}
