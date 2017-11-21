import { Component, OnInit,Output,EventEmitter, Input } from '@angular/core';
import { Router,ActivatedRoute, ParamMap }            from '@angular/router';
import { AnalysisSection} from '../analysisClasses';
import { AchillesService} from '../services/achilles.service';

@Component({
  selector: 'app-data-browser-header',
  templateUrl: './data-browser-header.component.html',
  styleUrls: ['./data-browser-header.component.css']
})
export class DataBrowserHeaderComponent implements OnInit {
  sections: AnalysisSection[];
  activeroute
  surveyMenu
  routeId:string;
  @Output() selected = new EventEmitter<AnalysisSection>();
  @Input() pageTitle

  constructor(
    private router: Router,
    private achillesService:AchillesService,
    private route: ActivatedRoute) {
      this.route.params.subscribe(params => {
        this.routeId = params.id;
      })
      // Init menu sections
      this.sections = [];
      /*this.achillesService.getSections('data-browser')
        .then(data => {
          this.sections = data;

        });*/

  }
  ngOnInit() {
    // Get Sections menu for page -- either survey menu or ehr menu for Example

    // Get route param , first one
    // pass it to get sections so it can get the correct section menu



  }
  routeTo(id){
    let link = ['/', id];
    this.router.navigate(link);


  }
  routeToSurvey(id){
    let link = ['/survey', id];
    this.router.navigate(link);

  }
  onSelect(section:AnalysisSection){
    this.selected.emit(section);
  }

  active(item){
    if (item) {
      if (item.innerText.toLowerCase() == this.routeId) {
        return true
      }else if (item.innerText == this.routeId){
        return true
      }else{
        return false
      }
    }
  }

  headerselect(name) {
    let input = "/data-browser/" + name
    if ("/data-browser/conditions" == this.router.routerState.snapshot.url) {
      //make boolean = true
      this.activeroute = true;
      //apply this to only one of the section links in header...
    }
  }

  selectedIdx = 0;

  selectItem(index):void {
      this.selectedIdx = index;
  }
  emit(elm){

      this.selected.emit(elm.innerText);
  }

}
