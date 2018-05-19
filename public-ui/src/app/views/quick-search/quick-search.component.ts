import { Component, OnInit } from '@angular/core';
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
  searchText;


  constructor(private api: DataBrowserService) {
  }

  ngOnInit() {
      this.api.getDomainFilters().subscribe(
          result => {
              this.ehrDomains = result.items;
              /* Add checked = false to our domain list for ui */
              for (const d of this.ehrDomains ) {
                  d.checked = false;
              }
              console.log(this.ehrDomains); }
      );
  }

  public searchDomains() {
      console.log("Search called ", this.surveysChecked);
      console.log(this.ehrDomains.filter(d => d.checked ));
      this.api.getDomainSearchResults(this.searchText).subscribe(data => this.searchResults = data.items);
  }

}
