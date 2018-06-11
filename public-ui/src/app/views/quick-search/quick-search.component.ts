import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import {DataBrowserService} from '../../../publicGenerated/api/dataBrowser.service';
import {DbDomain} from '../../../publicGenerated/model/dbDomain';
import {DbDomainListResponse} from '../../../publicGenerated/model/dbDomainListResponse';
import { FormControl } from '@angular/forms';

import 'rxjs/add/operator/debounceTime';
import 'rxjs/add/operator/distinctUntilChanged';
import 'rxjs/add/operator/switchMap';

@Component({
  selector: 'app-quick-search',
  templateUrl: './quick-search.component.html',
  styleUrls: ['../../styles/template.css', '../../styles/cards.css', './quick-search.component.css']
})
export class QuickSearchComponent implements OnInit {
  pageImage = '/assets/db-images/man-standing.png';
  title = 'Quick Guided Search';
  subTitle = 'Enter keywords to search EHR data and survey modules. Or click on an EHR domain or survey.';

  // List of domain filters. We will add a checked to these so type any


  searchResults = [];
  domainResults = [];
  surveyResults = [];
  searchText: FormControl = new FormControl();
  prevSearchText = '';
  totalParticipants;
  domains = [];
  loading = false;


  constructor(private api: DataBrowserService,
              private router: Router) {
  }

  ngOnInit() {

    this.api.getParticipantCount().subscribe(result => this.totalParticipants = result.countValue);

    // Initialize results to all totals
    this.api.getDomainTotals().subscribe(data => {
      this.domains = data.items;
      this.searchResults = this.domains;
      this.domainResults = this.domains.filter(d => d.dbType === 'domain_filter');
      this.surveyResults = this.domains.filter(d => d.dbType === 'survey');

    });
    this.searchText.valueChanges
      .debounceTime(200)
      .distinctUntilChanged()
      .switchMap((query) => this.searchDomains(query))
      .subscribe(data => {
        this.searchResults = data.items;
        this.domainResults = data.items.filter(d => d.dbType === 'domain_filter');
        this.surveyResults = data.items.filter(s => s.dbType === 'survey');
        this.loading = false;
      });


  }

  public searchDomains(query) {


    this.prevSearchText = query;
    localStorage.setItem('searchText', query);
    return this.api.getDomainSearchResults(query);

   /* if (this.searchText.length >= minSearchLength) {
      this.loading = true;
      this.api.getDomainSearchResults(this.searchText).subscribe(data => {
        this.searchResults = data.items;
        this.domainResults = data.items.filter(d => d.dbType === 'domain_filter');
        this.surveyResults = data.items.filter(s => s.dbType === 'survey');
        this.loading = false;
      });
    }*/
  }

  public viewResults(r) {
    localStorage.setItem('dbDomain', JSON.stringify(r));
    localStorage.setItem('searchText', this.prevSearchText);

    if (r.dbType === 'survey') {
      this.router.navigateByUrl('/survey/' + r.domainId.toLowerCase());
    } else {
      this.router.navigateByUrl('/ehr/' + r.domainId.toLowerCase());
    }
  }

}
