import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AnalysisSection} from '../AnalysisClasses';

@Component({
  selector: 'app-data-browser-header',
  templateUrl: './data-browser-header.component.html',
  styleUrls: ['./data-browser-header.component.css']
})
export class DataBrowserHeaderComponent {
  sections: AnalysisSection[];
  routeId: string;
  @Output() selected = new EventEmitter<AnalysisSection>();
  @Input() pageTitle;

  constructor(
    private route: ActivatedRoute) {
      this.route.params.subscribe(params => {
        this.routeId = params.id;
      });
      // Init menu sections
      this.sections = [];
  }

  /* todo when we need to
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

  selectItem(index): void {
      this.selectedIdx = index;
  }

  emit(elm) {
      this.selected.emit(elm.innerText);
  } */

}
