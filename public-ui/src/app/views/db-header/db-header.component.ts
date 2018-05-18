import { Component, OnInit, Input } from '@angular/core';

@Component({
  selector: 'app-db-header',
  templateUrl: './db-header.component.html',
  styleUrls: ['./db-header.component.css']
})
export class DbHeaderComponent implements OnInit {
  @Input() noMenu: boolean = false;
  logo = "/assets/db-images/All_Of_Us_Logo.svg";
  dbLogo = '/assets/db-images/Data_Browser_Logo.svg';
  constructor() { }

  ngOnInit() {
  }

}
