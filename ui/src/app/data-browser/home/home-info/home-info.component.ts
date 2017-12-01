import { Component, Input, OnChanges} from '@angular/core';
import { Router } from '@angular/router';


@Component({
  selector: 'app-home-info',
  templateUrl: './home-info.component.html',
  styleUrls: ['./home-info.component.css']
})
export class HomeInfoComponent implements OnChanges {
  @Input() infoData;
  @Input() homeData;
  @Input() anchor;
  highlight;

  constructor(private router: Router) {


  }

  ngOnChanges() {
      if (this.infoData) {
        window.location.hash = this.infoData.domain_display;
        this.highlight = this.infoData.domain_display;
      }
      if (this.anchor) {
        this.highlight = this.anchor;
        window.location.hash = this.anchor;
        this.anchor = undefined;
      }
  }

  routeTo(id) {
      let path = '/all?';
      if (id.domain_parent = 'domain') {
        path += 'domain_id=' + id.domain_id;
      } else {
        path += 'vocabulary_id=' + id.domain_id;
      }
      const link = ['all'];
      this.router.navigate(link);

  }
}
