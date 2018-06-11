import { Component, OnInit, OnDestroy } from '@angular/core';
import {DataBrowserService} from '../../../publicGenerated/api/dataBrowser.service';
import { Router } from '@angular/router';
import { ISubscription } from 'rxjs/Subscription';
@Component({
  selector: 'app-surveys',
  templateUrl: './surveys.component.html',
  styleUrls: ['../../styles/template.css', '../../styles/cards.css', './surveys.component.css']
})
export class SurveysComponent implements OnInit {
  title = 'Browse Participant Surveys';
  subTitle = 'Conduct a simple keyword search to quickly identify survey questions ' +
    'related to your area of interest.';
  pageImage = '/assets/images/create-account-male-standing.png';
  surveys = [];
  private subscription: ISubscription;

  constructor(
    private api: DataBrowserService,
    private router: Router
  ) { }

  ngOnInit() {
    this.subscription = this.api.getSurveyList().subscribe(
      result => {
        this.surveys = result.items.filter(item => item.conceptId );
      });
  }
  ngOnDestroy() {
    console.log("unsubscribing surveys list view");
    this.subscription.unsubscribe();
  }
  public viewResults(r) {
    localStorage.setItem("dbDomain", JSON.stringify(r));
    localStorage.setItem("searchText", '');
    this.router.navigateByUrl('/survey/' + r.domainId.toLowerCase());
  }

}
