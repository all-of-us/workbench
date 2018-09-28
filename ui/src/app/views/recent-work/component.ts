import {Component, Input, OnInit} from '@angular/core';

import {
  CohortsService,
  RecentResource,
  UserMetricsService,
  Workspace,
  WorkspacesService,
} from 'generated';

import {convertToResources, ResourceType} from 'app/utils/resourceActions';
import {WorkspaceData} from "../../services/workspace-storage.service";
import {WorkspaceAccessLevel} from "../../../generated/model/workspaceAccessLevel";
import {Observable} from "rxjs/Observable";
import {ActivatedRoute} from "@angular/router";
import {FileDetail} from "../../../generated/model/fileDetail";
import {CohortListResponse} from "../../../generated/model/cohortListResponse";

@Component({
  selector: 'app-recent-work',
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})

export class RecentWorkComponent implements OnInit {
  resourceList: RecentResource[];
  fullList: RecentResource[] = [];
  @Input('workspace')
  workspace: Workspace;
  accessLevel: WorkspaceAccessLevel;
  startIndex = 0;
  cssClass: string;
  resourcesLoading = false;
  constructor(
    private userMetricsService: UserMetricsService,
    private workspacesService: WorkspacesService,
    private cohortsService: CohortsService,
    private route: ActivatedRoute,
  ) {}
  index: Number;
  size = 3;

  ngOnInit(): void {
    this.resourcesLoading = true;
    this.updateList();
  }

  updateList(): void {
    if (this.workspace) {
      this.cssClass = 'for-list';
      const wsData: WorkspaceData = this.route.snapshot.data.workspace;
      this.accessLevel = wsData.accessLevel;
      let notebookCall = this.workspacesService.getNoteBookList(this.workspace.namespace, this.workspace.id);
      let cohortCall = this.cohortsService.getCohortsInWorkspace(this.workspace.namespace, this.workspace.id);
      Observable.forkJoin(notebookCall, cohortCall).subscribe(([notebooks, cohorts]) =>
      {
        let notebookResources = convertToResources(notebooks, this.workspace.namespace,
          this.workspace.id, this.accessLevel, ResourceType.NOTEBOOK);
        let cohortResources = convertToResources(cohorts.items, this.workspace.namespace,
          this.workspace.id, this.accessLevel, ResourceType.COHORT);
        this.fullList = notebookResources.concat(cohortResources);
        this.fullList.sort((leftSide, rightSide): number => {
          if (leftSide.modifiedTime < rightSide.modifiedTime) return 1;
          if (leftSide.modifiedTime > rightSide.modifiedTime) return -1;
          return 0;
        });
        this.resourceList = this.fullList.slice(this.startIndex, this.startIndex + this.size);
        this.resourcesLoading = false;
      });
    } else {
      this.userMetricsService.getUserRecentResources().subscribe((resources) => {
        this.fullList = resources;
        this.resourceList = this.fullList.slice(this.startIndex, this.startIndex + this.size);
        this.resourcesLoading = false;
      });
    }
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
