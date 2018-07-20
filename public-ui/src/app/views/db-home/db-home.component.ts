import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-db-home',
  templateUrl: './db-home.component.html',
  styleUrls: ['../../styles/template.css', '../../styles/cards.css', './db-home.component.css']
})
export class DbHomeComponent implements OnInit {
  pageImage = '/assets/db-images/woman-chair.png';
  dbLogo = '/assets/db-images/Data_Browser_Logo.svg';
  subTitle = 'Interested in browsing the public version of the All of Us Research Program data? ' +
      'Use the tools in the All of Us Data Browser application to explore!';

  constructor() { }

  ngOnInit() {
  }

}
