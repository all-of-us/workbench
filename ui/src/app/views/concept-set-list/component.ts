import {Component, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute} from '@angular/router';

import {
ConceptSet,
ConceptSetsService,
RecentResource,
Workspace,
WorkspaceAccessLevel,
} from 'generated';

import {CreateConceptSetModalComponent} from 'app/views/conceptset-create-modal/component';

import {WorkspaceData} from 'app/services/workspace-storage.service';

import {convertToResources, ResourceType} from 'app/utils/resourceActions';

@Component({
  styleUrls: ['../../styles/buttons.css',
    '../../styles/cards.css',
    './component.css'],
  templateUrl: './component.html',
})
export class ConceptSetListComponent implements OnInit {
  workspace: Workspace;
  accessLevel: WorkspaceAccessLevel;
  wsNamespace: string;
  wsId: string;
  conceptSetsLoading = false;
  conceptSetsList: ConceptSet[];
  resourceList: RecentResource[];
  duplicateName: string;
  nameConflictError = false;

  constructor(
    private route: ActivatedRoute,
    private conceptSetsService: ConceptSetsService
  ) {
    const wsData: WorkspaceData = this.route.snapshot.data.workspace;
    this.workspace = wsData;
    this.accessLevel = wsData.accessLevel;
  }

  @ViewChild(CreateConceptSetModalComponent)
  conceptCreateModal: CreateConceptSetModalComponent;

  ngOnInit(): void {
      this.wsNamespace = this.route.snapshot.params['ns'];
      this.wsId = this.route.snapshot.params['wsid'];
      this.conceptSetsLoading = true;
      this.loadConceptSets();
  }

  loadConceptSets() {
    this.conceptSetsService.getConceptSetsInWorkspace(this.wsNamespace, this.wsId)
      .subscribe(conceptSetListResponse => {
        this.conceptSetsList = conceptSetListResponse.items;
        this.resourceList = convertToResources(this.conceptSetsList, this.wsNamespace,
          this.wsId, this.accessLevel, ResourceType.CONCEPT_SET);
        this.conceptSetsLoading = false;
      });
  }

  newConceptSet(): void {
    this.conceptCreateModal.open();
  }

  duplicateNameError(dupName: string) {
    this.duplicateName = dupName;
    this.nameConflictError = true;
  }

  get writePermission(): boolean {
    return this.accessLevel === WorkspaceAccessLevel.OWNER
      || this.accessLevel === WorkspaceAccessLevel.WRITER;
  }

  get actionsDisabled(): boolean {
    return !this.writePermission;
  }
}
