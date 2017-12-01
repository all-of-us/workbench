import { Component, Input} from '@angular/core';
import { Router } from '@angular/router';
import { AchillesService } from '../../services/achilles.service';

@Component({
  selector: 'app-home-info',
  templateUrl: './home-info.component.html',
  styleUrls: ['./home-info.component.css']
})
export class HomeInfoComponent{
  @Input() infoData;
  @Input() homeData;
  @Input() anchor;
  vocabularies = [];
  phoneData;
  highlight;

  constructor(private router: Router,
    private achillesService: AchillesService) {

    /*achillesService.getDomains()
      .subscribe(results => {
        for (let b of results) {
          this.achillesService.VocabShow(b)
            .subscribe(data => {
              b.domain_vocab = data;
            })
        }
        this.phoneData = results

      })//end of subscibe
      */
  }

  ngOnChanges() {
      /*
    this.achillesService.VocabShow(this.infoData)
      .subscribe(results => {
        this.vocabularies = results;
      });*/

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
      }
      else {
        path += 'vocabulary_id=' + id.domain_id;
      }
      const link = ['all'];
      this.router.navigate(link);

  }
}
