import {Component, OnInit} from "@angular/core";
import {ConceptSetsService} from "../../../generated/api/conceptSets.service";
import {Workspace} from "../../../generated/model/workspace";
import {WorkspaceAccessLevel} from "../../../generated/model/workspaceAccessLevel";
import {WorkspaceData} from "../../services/workspace-storage.service";
import {ActivatedRoute} from "@angular/router";
import {ConceptSet} from "../../../generated/model/conceptSet";
import {RecentResource} from "../../../generated/model/recentResource";
import {convertToResources, ResourceType} from "../../utils/resourceActions";

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
  conceptSetsLoading: boolean = false;
  conceptSetsList: ConceptSet[];
  resourceList: RecentResource[];
  duplicateName: string;
  nameConflictError: boolean = false;

  constructor(
    private route: ActivatedRoute,
    private conceptSetsService: ConceptSetsService
  ) {
    const wsData: WorkspaceData = this.route.snapshot.data.workspace;
    this.workspace = wsData;
    this.accessLevel = wsData.accessLevel
  }

  ngOnInit(): void {
      this.wsNamespace = this.route.snapshot.params['ns'];
      this.wsId = this.route.snapshot.params['wsid'];
      this.conceptSetsLoading = true;
      this.loadConceptSets();
  }

  private loadConceptSets() {
    this.conceptSetsService.getConceptSetsInWorkspace(this.wsNamespace, this.wsId)
      .subscribe(conceptSetListResponse => {
        this.conceptSetsList = conceptSetListResponse.items;
        this.resourceList = convertToResources(this.conceptSetsList, this.wsNamespace,
          this.wsId, this.accessLevel, ResourceType.CONCEPT_SET);
        this.conceptSetsLoading = false;
      });
  }

  newConceptSet(): void {
    // Need new concept set modal implemented
    // this.newConceptSetModal.open();
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
