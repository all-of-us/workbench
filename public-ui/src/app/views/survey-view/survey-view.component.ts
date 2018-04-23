import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import {DataBrowserService} from '../../../publicGenerated/api/dataBrowser.service';
import {DbDomain} from '../../../publicGenerated/model/dbDomain';
import {DbDomainListResponse} from '../../../publicGenerated/model/dbDomainListResponse';
@Component({
  selector: 'app-survey-view',
  templateUrl: './survey-view.component.html',
  styleUrls: ['../../styles/template.css', '../../styles/cards.css', './survey-view.component.css']
})
export class SurveyViewComponent implements OnInit {

  domainId: string;
  title = 'Hello Survey';
  subTitle = 'Domain Desc here ';
  surveys = [];
  survey = {};
  constructor(private route: ActivatedRoute, private api: DataBrowserService) {
    this.route.params.subscribe(params => {
      this.domainId = params.id;
    });
  }


  ngOnInit() {
    this.api.getSurveyList().subscribe(
      result => {
        this.surveys = result.items;
        console.log("Survey view: " ,  this.surveys);
        for (const item of this.surveys) {
          if (item.domainId.toLowerCase() === this.domainId.toLowerCase()) {
            this.survey = item;
          }
        }
      }
    );
  }


}
