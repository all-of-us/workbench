import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-surveys',
  templateUrl: './surveys.component.html',
  styleUrls: ['../../styles/page.css', './surveys.component.css']
})
export class SurveysComponent implements OnInit {
  title = 'Browse Survey Instruments';
  subTitle = 'Conduct a simple keyword search to quickly identify survey questions ' +
    'related to your area of interest.';
  pageImage = '/assets/images/create-account-male-standing.png';
  constructor() { }

  ngOnInit() {

  }

}
