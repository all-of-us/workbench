import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-db-home',
  templateUrl: './db-home.component.html',
  styleUrls: ['../../styles/template.css', '../../styles/cards.css', './db-home.component.css']
})
export class DbHomeComponent implements OnInit {
  dbLogo = '/assets/db-images/Data_Browser_Logo.svg';
  dbDesc = `The Data Browser provides public access to various levels
  of the participant database. This data is anonymized, aggregated, and
  made available for all researchers, participants, and members of the
  general public to explore and learn about the cohort as a whole.`;
  constructor() { }

  ngOnInit() {
  }

}
