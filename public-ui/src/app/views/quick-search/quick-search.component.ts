import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import {DataBrowserService} from '../../../publicGenerated/api/dataBrowser.service';
import {DbDomain} from '../../../publicGenerated/model/dbDomain';
import {DbDomainListResponse} from '../../../publicGenerated/model/dbDomainListResponse';



@Component({
  selector: 'app-quick-search',
  templateUrl: './quick-search.component.html',
  styleUrls: ['../../styles/template.css', '../../styles/cards.css', './quick-search.component.css']
})
export class QuickSearchComponent implements OnInit {
  pageImage = '/assets/db-images/man-standing.png';
  title = 'Quick Guided Search';
  subTitle = 'Answer the following questions for a quick and easy feasibility assessment and to learn about ' +
      'the data that applies to your research topic of interest.';

  // List of domain filters. We will add a checked to these so type any
  ehrDomains = [];
  surveysChecked = false;
  searchResults = [];
  domainResults = [];
  surveyResults = [];
  searchText = '';
  prevSearchText = '';
  totalParticipants;
  domains = [];
  loading = false;


  constructor(private api: DataBrowserService,
              private router: Router) {
  }

  ngOnInit() {
    this.api.getDomainFilters().subscribe(
      result => {
        this.ehrDomains = result.items;
        /* Add checked = false to our domain list for ui */
        for (const d of this.ehrDomains) {
          d.checked = false;
        }
      });

    this.api.getParticipantCount().subscribe(result => this.totalParticipants = result.countValue);

    // Initialize results to all totals
    this.api.getDomainTotals().subscribe(data => {
      this.domains = data.items;
      this.searchResults = this.domains;
      this.domainResults = this.domains.filter(d => d.dbType === 'domain_filter');
      this.surveyResults = this.domains.filter(d => d.dbType === 'survey');

    });
  }

  // Subscribe too many here works for demo.
  public searchDomains() {
    // If they clear the search text reset
    const minSearchLength = 3;
    if (this.searchText.length < minSearchLength) {
      this.domainResults = this.domains.filter(d => d.dbType === 'domain_filter');
      this.surveyResults = this.domains.filter(d => d.dbType === 'survey');
    }

    this.prevSearchText = this.searchText;
    localStorage.setItem('searchText', this.searchText);

    if (this.searchText.length >= minSearchLength) {
      this.loading = true;
      this.api.getDomainSearchResults(this.searchText).subscribe(data => {
        this.searchResults = data.items;
        this.domainResults = data.items.filter(d => d.dbType === 'domain_filter');
        this.surveyResults = data.items.filter(s => s.dbType === 'survey');
        this.loading = false;
      });
    }
  }

  public viewResults(r) {
    localStorage.setItem('dbDomain', JSON.stringify(r));
    localStorage.setItem('searchText', this.searchText);

    if (r.dbType === 'survey') {
      this.router.navigateByUrl('/survey/' + r.domainId.toLowerCase());
    } else {
      this.router.navigateByUrl('/ehr/' + r.domainId.toLowerCase());
    }
  }

}
