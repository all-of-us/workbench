import {Component, Input, OnChanges, OnInit, SimpleChanges} from '@angular/core';
import {fromJS, List} from 'immutable';

@Component({
  selector: 'app-search-bar',
  templateUrl: './search-bar.component.html',
  styleUrls: ['./search-bar.component.css']
})
export class SearchBarComponent implements OnChanges, OnInit {
  @Input() tree;
  filteredTree =  List();
  expandedTree: any;
  searchTerm = '';
  constructor() { }

  ngOnInit() {
    console.log(this.tree);
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes.tree.currentValue.size) {

    }
    console.log(changes.tree.currentValue.toJS());
  }

  searchTree() {
    this.expandedTree = this.tree.toJS();
    const filtered = this.filterTree(this.tree.toJS(), []);
    this.filteredTree = fromJS(this.mergeExpanded(filtered, this.expandedTree));

    console.log(this.filteredTree.toJS());
  }

  filterTree(tree: Array<any>, path: Array<number>) {
    const filtered = tree.map((item, i) => {
      path.push(i);
      if (item.name.toLowerCase().includes(this.searchTerm.toLowerCase())) {
        const start = item.name.toLowerCase().indexOf(this.searchTerm.toLowerCase());
        const end = start + this.searchTerm.length;
        item.name = item.name.slice(0, start) + '<b>'
          + item.name.slice(start, end) + '</b>'
          + item.name.slice(end);
        if (path.length) {
          this.setExpanded(path, 0);
        } else {
          this.expandedTree[i].expanded = true;
        }
      }
      if (item.children.length) {
        item.children = this.filterTree(item.children, path);
      }
      path.pop();
      return item;
    });
    return filtered;
  }

  setExpanded(path: Array<number>, start: number) {
    let obj = this.expandedTree[path[0]];
    if (start > 0) {
      for (let x = 1; x <= start; x++) {
        obj = obj.children[path[x]];
      }
    }
    if (obj.children.length) {
      obj.expanded = true;
    }
    if (path[start + 1]) {
      this.setExpanded(path, start + 1);
    }
  }

  mergeExpanded(filtered: Array<any>, expanded: Array<any>) {
    expanded.forEach((item, i) => {
      if (item.expanded) {
        filtered[i].expanded = true;
      }
      if (filtered[i].children.length) {
        filtered[i].children = this.mergeExpanded(filtered[i].children, item.children);
      }
    });
    return filtered;
  }
}
