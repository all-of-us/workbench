import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import {Concept} from '../ConceptClasses';
import { TreeService } from '../services/tree.service';
@Component({
  selector: 'app-lazy-tree',
  templateUrl: './lazy-tree.component.html'
})
export class LazyTreeComponent implements OnInit {
  routeId;
  tree_nodes: Concept[] = [];
  loading: boolean;
  // Route domain map -- todo , these can be more than one eventually
  routeDomain = {
    'Lifestyle': 'Lifestyle',
    'Overall-Health': 'OverallHealth',
    'Person-Medical-History': 'PersonalMedicalHistory',
    'Family-Medical-History': 'FamilyHistory',
  };
  pageDomainId = null;

  @Input() vocabularyId;
  @Input() parent: Concept;
  @Output() conceptEmit = new EventEmitter;

  // Function to return true if we can expand this
  canExpand(item: Concept) {
    if (item.is_group) {
      return true;
    }
    if (!item.is_group && !item.is_selectable) {
      return true;
    }
    return false;
  }

  // Function to return true if we can click this
  canClick(item: Concept) {
    if (item.is_selectable) {
      return true;
    }
    if (item.is_group) {
      return true;
    }
    return false;
  }

  constructor(private route: ActivatedRoute,
              private treeService: TreeService, ) {
    this.route.params.subscribe(params => {
      // get parameter from URL...
        // example:  http://localhost:4200/data-browser/drug
        // <--drug is params.id, which is defined in router.\
      // Set the domain id for the page if this route is dombain specific
      this.routeId = params.id;
      if (this.routeDomain[this.routeId]) {
        this.pageDomainId = this.routeDomain[this.routeId];
      }
    });
  }

  ngOnInit() {
    // Load the tree
    this.loading = true;
    // Don't hit server for children if we already have them
    if (this.parent.children && this.parent.children.length) {
        this.tree_nodes = this.parent.children;
        this.loading = false;
    } else {
      this.treeService.getTreeNodes(this.vocabularyId, this.parent.id).subscribe(results => {
        this.tree_nodes = results;
        this.parent.children = results;
        this.loading = false;
      });
    }
  }



  clickItem(item) {
    // Currently passing children up from tree too
    // If have children,  then emit
    if (item.children && item.children.length) {
      this.conceptEmit.emit(item);
    } else {
      this.treeService.getTreeNodes(this.vocabularyId, item.id).subscribe(results => {
        item.children = results;
        this.loading = false;
        this.conceptEmit.emit(item);
      });
    }
  }
  receiveItem(item) {

    this.conceptEmit.emit(item);
  }
  passGrandChildren(children) {
    this.conceptEmit.emit(children);
  }
}
