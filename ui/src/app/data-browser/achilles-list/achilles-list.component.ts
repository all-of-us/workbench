import { Component, OnInit } from '@angular/core';
import { AchillesService } from '../services/achilles.service';
import { Router } from '@angular/router';
import { AnalysisSection } from '../analysisClasses';

@Component({
  selector: 'app-achilles-list',
  templateUrl: './achilles-list.component.html',
  styleUrls: ['./achilles-list.component.css']
})
export class AchillesListComponent implements OnInit {
  dataList
  sections: AnalysisSection[];

  constructor(
    private achillesService: AchillesService,
    private router: Router
  ) { }

  ngOnInit() {
    this.achillesService.getSections('data-browser')
      .then(data => {
        this.sections = data;
        //
      });
  }
  toggleSection(id) {
    //this.sections[id].show = !this.sections[id].show;
  }
  showSection(id) {
    //  return this.sections[id].show;
  }
  // graphMe(index,aIndex){
  //   //
  //   var data = this.dataList[index].data[aIndex];
  //
  //
  // }
  navigation(id) {
    let link = ['/achilles-list', id];
    this.router.navigate(link);
  }

  go(args) {
    let q = {
      //  "concept_id": args.concept_id,
      //  "stratum": args.stratum
       "concept_id": 320128,
       "stratum":[1,2]
   }

    this.achillesService.getAllConceptResults(q)
      .subscribe(data => {
        //
      });
  }

}
