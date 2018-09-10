import {Component, OnInit} from '@angular/core';

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
  fullList: RecentResource[] = [];
  startIndex = 0;
  constructor(
    private userMetricsService: UserMetricsService
  ) {}


  ngOnInit(): void {
    this.updateList();
  }

  updateList(): void {
    this.userMetricsService.getUserRecentResources().subscribe((resources) => {
      this.fullList = resources;
      this.resourceList = this.fullList.slice(this.startIndex, this.startIndex + 3);
    });
  }

  moveDownList(): void {
    this.startIndex = this.startIndex - 1;
    this.resourceList = this.fullList.slice(this.startIndex, this.startIndex + 3);
  }

  moveUpList(): void {
    this.startIndex = this.startIndex + 1;
    this.resourceList = this.fullList.slice(this.startIndex, this.startIndex + 3);
  }

  rightScrollVisible(): boolean {
    return (this.fullList.length > 3) && (this.fullList.length > this.startIndex + 3);
  }

  leftScrollVisible(): boolean {
    return (this.fullList.length > 3) && (this.startIndex > 0);
  }

  elementVisible(): boolean {
    return this.fullList.length > 0;
  }

}
