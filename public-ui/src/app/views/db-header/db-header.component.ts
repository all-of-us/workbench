import { Component, Input, OnInit } from '@angular/core';

@Component({
  selector: 'app-db-header',
  templateUrl: './db-header.component.html',
  styleUrls: ['./db-header.component.css']
})
export class DbHeaderComponent implements OnInit {
  @Input() noMenu = false;
  logo = '/assets/db-images/All_Of_Us_Logo.svg';
  dbLogo = '/assets/db-images/Data_Browser_Logo.svg';
  constructor() { }

  ngOnInit() {
  }

}
