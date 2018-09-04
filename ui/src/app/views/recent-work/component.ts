import {Component, OnDestroy, OnInit} from '@angular/core';

import {
  RecentResource,
  UserMetricsService
} from 'generated';

@Component({
  selector: 'app-recent-work',
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})

export class RecentWorkComponent implements OnInit {
  resourceList: RecentResource[];
  fullList: RecentResource[];
  constructor(
    private userMetricsService: UserMetricsService
  ){}

  ngOnInit(): void {
    this.userMetricsService.getUserRecentResources().subscribe((resources) => {
      this.fullList = resources;
      // this should actually be first 3 elements of full List
      this.resourceList = this.fullList.slice(0,3);
    });
  }

  moveDownList(): void {
      this.fullList.push(this.fullList.shift());
      console.log(this.fullList);
      //again should actually be first 3 elements of fullList
      this.resourceList = ["testing2", "testing3", "testing1"];
  }

  moveUpList(): void {
      this.fullList.unshift(this.fullList.pop());
      console.log(this.fullList);
      this.resourceList = ["testing1", "testing2", "testing3"];
  }

}