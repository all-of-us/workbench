import {Component, ElementRef, HostListener, Input, OnInit, ViewChild} from '@angular/core';


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
  resourcesLoading: boolean;
  fullList: RecentResource[] = [];
  @Input('headerText')
  headerText = 'Your Recent Work';
  startIndex = 0;
  constructor(
    private userMetricsService: UserMetricsService
  ) {}
  index: Number;
  size = 3;
  @ViewChild('recentWork')
  eMainFrame: ElementRef;
  width: number;

  ngOnInit(): void {
    this.resourcesLoading = true;
    this.updateList();
  }

  @HostListener('window:resize')
  onResize() {
    const width = this.eMainFrame.nativeElement.offsetWidth;
    if ((this.resourcesLoading === false) && (width)) {
      this.size = Math.floor((width - 100) / 200) || 1;
      this.updateList();
    }
  }

  updateList(): void {
    this.userMetricsService.getUserRecentResources().subscribe((resources) => {
      this.fullList = resources;
      this.resourceList = this.fullList.slice(this.startIndex, this.startIndex + this.size);
      this.resourcesLoading = false;
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
