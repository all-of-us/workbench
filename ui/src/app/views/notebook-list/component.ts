import {Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute} from '@angular/router';

import {convertToResources} from 'app/utils/resourceActions';

import {WorkspaceData} from 'app/resolvers/workspace';
import {BugReportComponent} from 'app/views/bug-report/component';
import {ConfirmDeleteModalComponent} from 'app/views/confirm-delete-modal/component';
import {NewNotebookModalComponent} from 'app/views/new-notebook-modal/component';
import {RenameModalComponent} from 'app/views/rename-modal/component';

import {
  Cluster,
  FileDetail,
  NotebookRename,
  PageVisit,
  ProfileService,
  RecentResource,
  Workspace,
  WorkspaceAccessLevel,
  WorkspacesService
} from 'generated';

@Component({
  styleUrls: ['../../styles/buttons.css',
    '../../styles/cards.css',
    './component.css'],
  templateUrl: './component.html',
})
export class NotebookListComponent implements OnInit, OnDestroy {

  private static PAGE_ID = 'notebook';
  notebooksLoading: boolean;
  notebookList: FileDetail[] = [];
  resourceList: RecentResource[] = [];
  workspace: Workspace;
  notebookError: boolean;
  wsNamespace: string;
  wsId: string;
  localizeNotebooksError: boolean;
  cluster: Cluster;
  notebookAuthListeners: EventListenerOrEventListenerObject[] = [];
  private accessLevel: WorkspaceAccessLevel;
  showTip: boolean;
  newPageVisit: PageVisit = { page: NotebookListComponent.PAGE_ID};
  firstVisit = true;
  notebookRenameConflictError = false;
  notebookRenameError = false;
  duplicateName = '';


  @ViewChild(BugReportComponent)
  bugReportComponent: BugReportComponent;
  @ViewChild(NewNotebookModalComponent)
  newNotebookModal: NewNotebookModalComponent;

  constructor(
    private route: ActivatedRoute,
    private profileService: ProfileService,
    private workspacesService: WorkspacesService,
  ) {
    const wsData: WorkspaceData = this.route.snapshot.data.workspace;
    this.workspace = wsData;
    this.accessLevel = wsData.accessLevel;
    this.showTip = false;
  }

  ngOnInit(): void {
    this.wsNamespace = this.route.snapshot.params['ns'];
    this.wsId = this.route.snapshot.params['wsid'];
    this.notebooksLoading = true;
    this.loadNotebookList();
    this.profileService.getMe().subscribe(
      profile => {
        if (profile.pageVisits) {
          this.firstVisit = !profile.pageVisits.some(v =>
            v.page === NotebookListComponent.PAGE_ID);
        }
      },
      error => {},
      () => {
        if (this.firstVisit) {
          this.showTip = true;
        }
        this.profileService.updatePageVisits(this.newPageVisit).subscribe();
      });
  }

  private loadNotebookList() {
    this.notebooksLoading = true;
    this.workspacesService.getNoteBookList(this.wsNamespace, this.wsId)
      .subscribe(
        fileList => {
          this.notebookList = fileList;
          this.resourceList = convertToResources(fileList);
          this.notebooksLoading = false;
        },
        error => {
          this.notebooksLoading = false;
          this.notebookError = true;
        });
  }

  ngOnDestroy(): void {
    this.notebookAuthListeners.forEach(f => window.removeEventListener('message', f));
  }

  newNotebook(): void {
    this.newNotebookModal.open();
  }

  updateList(rename?: NotebookRename): void {
    if (rename === undefined) {
      this.loadNotebookList();
    } else {
      const nb = this.resourceList.filter(resource => resource.notebook.name === rename.name)[0];
      const newNb = Object.assign({}, nb);
      newNb.notebook.name = rename.newName;
      this.resourceList.splice(this.resourceList.indexOf(nb), 1, newNb);
    }
  }

  duplicateNameError(dupName: string): void {
    this.duplicateName = dupName;
    this.notebookRenameConflictError = true;
  }

  invalidNameError(): void {
    this.notebookRenameError = true;
  }

  submitNotebooksLoadBugReport(): void {
    this.notebookError = false;
    this.bugReportComponent.reportBug();
    this.bugReportComponent.bugReport.shortDescription = 'Could not load notebooks';
  }

  dismissTip(): void {
    this.showTip = false;
  }

  submitNotebookLocalizeBugReport(): void {
    this.localizeNotebooksError = false;
    this.bugReportComponent.reportBug();
    this.bugReportComponent.bugReport.shortDescription = 'Could not localize notebook.';
  }
}
