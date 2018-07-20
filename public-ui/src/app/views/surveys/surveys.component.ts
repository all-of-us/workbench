import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ISubscription } from 'rxjs/Subscription';
import {DataBrowserService} from '../../../publicGenerated/api/dataBrowser.service';
@Component({
  selector: 'app-surveys',
  templateUrl: './surveys.component.html',
  styleUrls: ['../../styles/template.css', '../../styles/cards.css', './surveys.component.css']
})
export class SurveysComponent implements OnInit, OnDestroy {
  title = 'Browse Participant Surveys';
  subTitle = 'View full survey content for each survey questionnaire. Explore aggregate views of All of Us participant responses.';
  pageImage = '/assets/db-images/man-vest.png';
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
    this.subscription.unsubscribe();
  }
  public viewResults(r) {
    localStorage.setItem('dbDomain', JSON.stringify(r));
    localStorage.setItem('searchText', '');
    this.router.navigateByUrl('/survey/' + r.domainId.toLowerCase());
  }

}
