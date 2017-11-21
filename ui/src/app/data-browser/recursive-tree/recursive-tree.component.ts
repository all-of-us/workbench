import { Component, OnInit, Input, Output, OnChanges, EventEmitter } from '@angular/core';
import { TreeService } from '../services/tree.service';
import { ActivatedRoute, ParamMap, Router } from '@angular/router';

@Component({
  selector: "app-recursive-tree",
  templateUrl: './recursive-tree.component.html'


})
export class RecursiveTreeComponent {
  treeNodes
  routeId
  tree_nodes
  // Route domain map -- todo , these can be more than one eventually
  routeDomain = {
    'Lifestyle': 'Lifestyle',
    'Overall-Health': 'OverallHealth',
    'Person-Medical-History': 'PersonalMedicalHistory',
    'Family-Medical-History': 'FamilyHistory',

  }
  pageDomainId = null;

  @Input() domainID
  @Input() item: any;
  @Output() conceptEmit = new EventEmitter;
  constructor(private route: ActivatedRoute, private router: Router, private treeService: TreeService, ) {


    this.route.params.subscribe(params => {
      //get parameter from URL...  example:  http://localhost:4200/data-browser/drug  <--drug is params.id, which is defined in router.\
      // Set the domain id for the page if this route is dombain specific
      this.routeId = params.id;
      if (this.routeDomain[this.routeId]) {
        this.pageDomainId = this.routeDomain[this.routeId];
      }
      //
    })
  }

  ngOnInit() {

    //
  }
  ngOnChanges() {


  }
  passChildren(children) {
    // alert();
    //
    this.conceptEmit.emit(children)

  }
  passGrandChildren(children) {
    // alert();
    //
    this.conceptEmit.emit(children)

  }
}
