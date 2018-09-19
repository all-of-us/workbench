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
  index: Number;
  size = 3;
  ngOnInit(): void {
    this.updateList();
  }

  updateList(): void {
    this.userMetricsService.getUserRecentResources().subscribe((resources) => {
      this.fullList = resources;
      this.resourceList = this.fullList.slice(this.startIndex, this.startIndex + this.size);
    });
  }

  scrollLeft(): void {
    this.startIndex = Math.max(this.startIndex - 1, 0);
    this.resourceList = this.fullList.slice(this.startIndex, this.startIndex + this.size);
  }

  scrollRight(): void {
    this.startIndex = Math.min(this.startIndex + 1, this.fullList.length) ;
    this.resourceList = this.fullList.slice(this.startIndex, this.startIndex + this.size);
  }

  rightScrollVisible(): boolean {
    return (this.fullList.length > 3) && (this.fullList.length > this.startIndex + this.size);
  }

  leftScrollVisible(): boolean {
    return (this.fullList.length > this.size) && (this.startIndex > 0);
  }

  elementVisible(): boolean {
    return this.fullList.length > 0;
  }

  // Exposed for testing
  setUserMetricsService(svc: UserMetricsService) {
    this.userMetricsService = svc;
  }
}
