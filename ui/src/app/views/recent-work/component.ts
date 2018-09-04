import {Component, OnDestroy, OnInit} from '@angular/core';

import {
  UserMetricsService,
  RecentResource
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
  ) {}

  ngOnInit(): void {
    this.userMetricsService.getUserRecentResources().subscribe((resources) => {
      this.fullList = resources;
      this.resourceList = this.fullList.slice(0, 3);
    });
  }

  moveDownList(): void {
    this.fullList.unshift(this.fullList.pop());
    this.resourceList = this.fullList.slice(0, 3);
  }

  moveUpList(): void {
      this.fullList.push(this.fullList.shift());
      this.resourceList = this.fullList.slice(0, 3);
  }

}
