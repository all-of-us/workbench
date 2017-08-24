import { Component, OnInit, Input, OnDestroy, ViewEncapsulation } from '@angular/core';
import { SearchService, BroadcastService } from '../service';
import { Criteria, SearchParameter } from '../model';
import 'rxjs/add/operator/takeWhile';

@Component({
  selector: 'app-criteria-tree',
  templateUrl: 'criteria-tree.component.html',
  styleUrls: ['criteria-tree.component.css'],
  encapsulation: ViewEncapsulation.None
})
export class CriteriaTreeComponent implements OnInit, OnDestroy {

  @Input()
  criteriaType: string;
  nodes: Criteria[] = [];
  public options = {
    useVirtualScroll: true,
    nodeHeight: 22
  };
  private alive = true;

  constructor(private searchService: SearchService, private broadcastService: BroadcastService) {}

  ngOnInit(): void {
    this.searchService.getParentNodes(this.criteriaType)
      .takeWhile(() => this.alive)
      .subscribe(nodes => {
        this.nodes = nodes;
      });
    // this.nodes = [
    //   new Criteria(1, 'name1', 'type1', 'code1', 10, false, false, true, 'domainId1', []),
    //   new Criteria(2, 'name2', 'type2', 'code2', 10, false, false, true, 'domainId2', []),
    //   new Criteria(3, 'name3', 'type3', 'code3', 10, false, false, true, 'domainId3', []),
    //   new Criteria(4, 'name4', 'type4', 'code4', 10, false, false, true, 'domainId4', []),
    //   new Criteria(5, 'name5', 'type5', 'code5', 10, false, false, true, 'domainId5', []),
    //   new Criteria(1, 'name1', 'type1', 'code1', 10, false, false, true, 'domainId1', []),
    //   new Criteria(2, 'name2', 'type2', 'code2', 10, false, false, true, 'domainId2', []),
    //   new Criteria(3, 'name3', 'type3', 'code3', 10, false, false, true, 'domainId3', []),
    //   new Criteria(4, 'name4', 'type4', 'code4', 10, false, false, true, 'domainId4', []),
    //   new Criteria(5, 'name5', 'type5', 'code5', 10, false, false, true, 'domainId5', []),
    //   new Criteria(1, 'name1', 'type1', 'code1', 10, false, false, true, 'domainId1', []),
    //   new Criteria(2, 'name2', 'type2', 'code2', 10, false, false, true, 'domainId2', []),
    //   new Criteria(3, 'name3', 'type3', 'code3', 10, false, false, true, 'domainId3', []),
    //   new Criteria(4, 'name4', 'type4', 'code4', 10, false, false, true, 'domainId4', []),
    //   new Criteria(5, 'name5', 'type5', 'code5', 10, false, false, true, 'domainId5', []),
    //   new Criteria(1, 'name1', 'type1', 'code1', 10, false, false, true, 'domainId1', []),
    //   new Criteria(2, 'name2', 'type2', 'code2', 10, false, false, true, 'domainId2', []),
    //   new Criteria(3, 'name3', 'type3', 'code3', 10, false, false, true, 'domainId3', []),
    //   new Criteria(4, 'name4', 'type4', 'code4', 10, false, false, true, 'domainId4', []),
    //   new Criteria(5, 'name5', 'type5', 'code5', 10, false, false, true, 'domainId5', []),
    //   new Criteria(1, 'name1', 'type1', 'code1', 10, false, false, true, 'domainId1', []),
    //   new Criteria(2, 'name2', 'type2', 'code2', 10, false, false, true, 'domainId2', []),
    //   new Criteria(3, 'name3', 'type3', 'code3', 10, false, false, true, 'domainId3', []),
    //   new Criteria(4, 'name4', 'type4', 'code4', 10, false, false, true, 'domainId4', []),
    //   new Criteria(5, 'name5', 'type5', 'code5', 10, false, false, true, 'domainId5', []),
    //   new Criteria(1, 'name1', 'type1', 'code1', 10, false, false, true, 'domainId1', []),
    //   new Criteria(2, 'name2', 'type2', 'code2', 10, false, false, true, 'domainId2', []),
    //   new Criteria(3, 'name3', 'type3', 'code3', 10, false, false, true, 'domainId3', []),
    //   new Criteria(4, 'name4', 'type4', 'code4', 10, false, false, true, 'domainId4', []),
    //   new Criteria(5, 'name5', 'type5', 'code5', 10, false, false, true, 'domainId5', []),
    //   new Criteria(1, 'name1', 'type1', 'code1', 10, false, false, true, 'domainId1', []),
    //   new Criteria(2, 'name2', 'type2', 'code2', 10, false, false, true, 'domainId2', []),
    //   new Criteria(3, 'name3', 'type3', 'code3', 10, false, false, true, 'domainId3', []),
    //   new Criteria(4, 'name4', 'type4', 'code4', 10, false, false, true, 'domainId4', []),
    //   new Criteria(5, 'name5', 'type5', 'code5', 10, false, false, true, 'domainId5', [])
    // ];
  }

  // clearFilter(treeModel: TreeModel, filter: any) {
  //   filter.value = null;
  //   treeModel.clearFilter();
  //   treeModel.collapseAll();
  // }
  //
  // filterNodes(treeModel: TreeModel, filterValue: string) {
  //   if (filterValue.length > 2) {
  //     treeModel.filterNodes(filterValue);
  //   }
  // }

  public selectCriteria(node: any): void {
    node.data.values.push(new SearchParameter(node.data.code, node.data.domainId));
    this.broadcastService.selectCriteria(node.data);
  }

  public event(event: any) {
    if (event.eventName === 'toggleExpanded' && event.isExpanded) {
      if (!event.treeModel.getNodeById(event.node.id).data.children) {
        this.searchService.getChildNodes(event.node.data)
          .takeWhile(() => this.alive)
          .subscribe(nodes => {
            event.treeModel.getNodeById(event.node.id).data.children = nodes;
            event.treeModel.update();
          });
      }
    }
  }

  public toggleTree(node: any) {
    if (node.isCollapsed) {
      node.expand();
    } else if (node.isExpanded) {
      node.collapse();
    }
  }

  ngOnDestroy() {
    this.alive = false;
  }

}


