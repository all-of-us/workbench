import {Component, OnDestroy, OnInit} from '@angular/core';

import {
  UserMetricsService,
  RecentResource
} from 'generated';

@Component({
  // styleUrls: ['./component.css'],
  templateUrl: './component.html',
})

export class RecentWorkComponent implements OnInit, OnDestroy {
  resourceList: RecentResource[];

  constructor(
    private userMetricsService: UserMetricsService
  ){}

  ngOnInit(): void {
    this.userMetricsService.getUserRecentResources().subscribe((resources) => {
      this.resourceList = resources;
    });
  }

  ngOnDestroy() {

  }
}