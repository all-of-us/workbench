import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-db-home',
  templateUrl: './db-home.component.html',
  styleUrls: ['../../styles/template.css', '../../styles/cards.css', './db-home.component.css']
})
export class DbHomeComponent implements OnInit {
  pageImage = '/assets/db-images/woman-chair.png';
  dbLogo = '/assets/db-images/Data_Browser_Logo.svg';
  subTitle = 'Interested in taking a look at the data before signing up to access ' +
    'the full data set? ' +
    'Use the tools in the AoU Data Browser platform to explore the data available!';

  constructor() { }

  ngOnInit() {
  }

}
