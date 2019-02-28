import {Component, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute} from '@angular/router';

import {
  ConceptSet,
  RecentResource,
  Workspace,
  WorkspaceAccessLevel,
} from 'generated';

import {generateDomain} from 'app/utils/index';

import {CreateConceptSetModalComponent} from 'app/views/conceptset-create-modal/component';

import {WorkspaceData} from 'app/services/workspace-storage.service';

import {convertToResources, ResourceType} from 'app/utils/resourceActions';
import {ToolTipComponent} from 'app/views/tooltip/component';

import {conceptsApi, conceptSetsApi} from 'app/services/swagger-fetch-clients';
import {DomainInfo} from 'generated/fetch';

@Component({
  styleUrls: ['../../styles/buttons.css',
    '../../styles/cards.css',
    '../../styles/tooltip.css',
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
  isCreateModalOpen = false;
  conceptDomainList: Array<DomainInfo> = undefined;

  @ViewChild(ToolTipComponent)
  toolTip: ToolTipComponent;

  constructor(
    private route: ActivatedRoute,
  ) {
    const wsData: WorkspaceData = this.route.snapshot.data.workspace;
    this.workspace = wsData;
    this.accessLevel = wsData.accessLevel;
    this.loadConceptSets = this.loadConceptSets.bind(this);
    this.closeCreateModal = this.closeCreateModal.bind(this);
  }

  @ViewChild(CreateConceptSetModalComponent)
  conceptCreateModal: CreateConceptSetModalComponent;

  ngOnInit(): void {
    this.wsNamespace = this.route.snapshot.params['ns'];
    this.wsId = this.route.snapshot.params['wsid'];
    this.conceptSetsLoading = true;
    conceptsApi().getDomainInfo(this.wsNamespace, this.wsId).then((response) => {
      this.conceptDomainList = response.items;
    });
    this.loadConceptSets();
  }

  loadConceptSets() {
    this.conceptSetsLoading = true;
    conceptSetsApi().getConceptSetsInWorkspace(this.wsNamespace, this.wsId)
      .then(conceptSetListResponse => {
        this.conceptSetsList = conceptSetListResponse.items
            .map(s => ({...s, domain: generateDomain(s.domain)}));
        this.resourceList = convertToResources(this.conceptSetsList, this.wsNamespace,
          this.wsId, this.accessLevel, ResourceType.CONCEPT_SET);
        this.conceptSetsLoading = false;
      });
  }

  newConceptSet(): void {
    this.isCreateModalOpen = true;
  }

  closeCreateModal(): void {
    this.isCreateModalOpen = false;
  }

  get writePermission(): boolean {
    return this.accessLevel === WorkspaceAccessLevel.OWNER
      || this.accessLevel === WorkspaceAccessLevel.WRITER;
  }

  get actionsDisabled(): boolean {
    return !this.writePermission;
  }
}
