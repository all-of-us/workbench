import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-db-header',
  templateUrl: './db-header.component.html',
  styleUrls: ['./db-header.component.css']
})
export class DbHeaderComponent implements OnInit {
  logo = "/assets/images/all-of-us-logo.png";
  constructor() { }

  ngOnInit() {
  }

}
