import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
@Component({
  selector: 'app-survey-view',
  templateUrl: './survey-view.component.html',
  styleUrls: ['./survey-view.component.css']
})
export class SurveyViewComponent implements OnInit {

  domainId: string;
  title = 'Hello Survey';
  subTitle = 'Domain Desc here ';
  constructor(private route: ActivatedRoute) {
    this.route.params.subscribe(params => {
      this.domainId = params.id;
    });
  }

  ngOnInit() {
  }

}
