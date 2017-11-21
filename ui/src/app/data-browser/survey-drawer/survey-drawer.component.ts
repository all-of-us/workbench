import { Component, OnInit, Input } from '@angular/core';

@Component({
  selector: 'app-survey-drawer',
  templateUrl: './survey-drawer.component.html',
  styleUrls: ['./survey-drawer.component.css']
})
export class SurveyDrawerComponent implements OnInit {
  @Input() concept
  @Input() analyses
  constructor() { }

  ngOnInit() {
  }

}
