import {NgRedux} from '@angular-redux/store';
import {Component, OnInit} from '@angular/core';
import {Subscription} from 'rxjs/Subscription';
import {CohortSearchState, groupList, itemList} from '../redux/store';

@Component({
  selector: 'app-inactive-list',
  templateUrl: './inactive-list.component.html',
  styleUrls: ['./inactive-list.component.css']
})
export class InactiveListComponent implements OnInit {

  subscription: Subscription;
  groups: any;

  constructor(private ngRedux: NgRedux<CohortSearchState>) { }

  ngOnInit() {
    this.subscription = this.ngRedux.select(groupList('includes'))
      .subscribe(groups => {
        console.log(groups.toJS());
        this.groups = groups
          .filter(group => group.get('status') === 'deleted')
          .map(group => {
            return group.set('items', itemList(group.get('id'))(this.ngRedux.getState()));
          });
      });
  }

}
