import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { ActivatedRoute, ParamMap, Router }            from '@angular/router';
import { AnalysisSection} from '../AnalysisClasses';
import { AchillesService} from '../services/achilles.service';

@Component({
  selector: 'app-data-browser-header',
  templateUrl: './data-browser-header.component.html',
  styleUrls: ['./data-browser-header.component.css']
})
export class DataBrowserHeaderComponent implements OnInit {
  sections: AnalysisSection[];
  activeroute;
  selectedIdx = 0;
  routeId: string;
  @Output() selected = new EventEmitter<AnalysisSection>();
  @Input() pageTitle;

  constructor(
    private router: Router,
    private achillesService: AchillesService,
    private route: ActivatedRoute) {
      this.route.params.subscribe(params => {
        this.routeId = params.id;
      });
      // Init menu sections
      this.sections = [];

  }
  ngOnInit() {
    // Get Sections menu for page -- either survey menu or ehr menu for Example

    // Get route param , first one
    // pass it to get sections so it can get the correct section menu



  }
  routeTo(id) {
    const link = ['/', id];
    this.router.navigate(link);


  }
  routeToSurvey(id) {
    const link = ['/survey', id];
    this.router.navigate(link);

  }
  onSelect(section: AnalysisSection) {
    this.selected.emit(section);
  }

  active(item) {
    if (item) {
      if (item.innerText.toLowerCase() === this.routeId) {
        return true;
      } else if (item.innerText === this.routeId) {
        return true;
      } else {
        return false;
      }
    }
  }

  headerselect(name) {
    const input = '/data-browser/' + name;
    if ('/data-browser/conditions' == this.router.routerState.snapshot.url) {
      this.activeroute = true;
    }
  }

  selectItem(index): void {
      this.selectedIdx = index;
  }

  emit(elm){
      this.selected.emit(elm.innerText);
  }

}
