import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs/Rx';
import {DataBrowserService} from '../../../publicGenerated/api/dataBrowser.service';
import {AchillesResult} from '../../../publicGenerated/model/achillesResult';
import {Analysis} from '../../../publicGenerated/model/analysis';
import {DbDomain} from '../../../publicGenerated/model/dbDomain';
import {DbDomainListResponse} from '../../../publicGenerated/model/dbDomainListResponse';
import {QuestionConcept} from '../../../publicGenerated/model/questionConcept';
import {QuestionConceptListResponse} from '../../../publicGenerated/model/questionConceptListResponse';
import {ChartComponent} from '../../data-browser/chart/chart.component';

@Component({
  selector: 'app-ehr-view',
  templateUrl: './ehr-view.component.html',
  styleUrls: ['./ehr-view.component.css']
})
export class EhrViewComponent implements OnInit {
  domainId: string;
  title ;
  subTitle;
  dbDomain;
  searchText;

  constructor(private route: ActivatedRoute, private api: DataBrowserService) {
    this.route.params.subscribe(params => {
      this.domainId = params.id;
    });
  }

  ngOnInit() {
    // Get search result from localStorage
      this.searchText = localStorage.getItem('searchText');
      this.dbDomain = localStorage.getItem('dbDomain');
      this.subTitle = "Keyword: " + this.searchText;
      this.title = "View Full Results: " + this.dbDomain.domainDisplay;

  }

}
