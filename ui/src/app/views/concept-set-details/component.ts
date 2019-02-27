import {Component, OnInit, ViewChild} from '@angular/core';

import {currentConceptSetStore, currentWorkspaceStore, navigate, urlParamsStore} from 'app/utils/navigation';
import {ConceptTableComponent} from 'app/views/concept-table/component';

import {
  ConceptSet,
  ConceptSetsService,
  WorkspaceAccessLevel,
} from 'generated';

@Component({
  styleUrls: ['../../styles/buttons.css',
    '../../styles/cards.css',
    '../../styles/headers.css',
    '../../styles/inputs.css',
    '../../styles/errors.css',
    './component.css'],
  templateUrl: './component.html',
})
export class ConceptSetDetailsComponent implements OnInit {
  @ViewChild(ConceptTableComponent) conceptTable;

  wsNamespace: string;
  wsId: string;
  accessLevel: WorkspaceAccessLevel;
  conceptSet: ConceptSet;

  editing = false;
  editSubmitting = false;
  editName: string;
  editDescription: string;

  removing = false;
  removeSubmitting = false;
  confirmDeleting = false;

  constructor(
    private conceptSetsService: ConceptSetsService,
  ) {
    this.receiveDelete = this.receiveDelete.bind(this);
    this.closeConfirmDelete = this.closeConfirmDelete.bind(this);
  }

  ngOnInit() {
    const {ns, wsid} = urlParamsStore.getValue();
    this.wsNamespace = ns;
    this.wsId = wsid;
    const {accessLevel} = currentWorkspaceStore.getValue();
    this.accessLevel = accessLevel;
    this.conceptSet = currentConceptSetStore.getValue() as unknown as ConceptSet;
    this.editName = this.conceptSet.name;
    this.editDescription = this.conceptSet.description;
  }

  validateEdits(): boolean {
    return !!this.editName;
  }

  submitEdits() {
    if (!this.validateEdits() || this.editSubmitting) {
      return;
    }
    this.editSubmitting = true;
    this.conceptSetsService.updateConceptSet(this.wsNamespace, this.wsId, this.conceptSet.id, {
      ...this.conceptSet,
      name: this.editName,
      description: this.editDescription
    }).subscribe((updated) => {
      this.conceptSet = updated;
      this.editSubmitting = false;
      this.editing = false;
    }, () => {
      // TODO(calbach): Handle errors.
      this.editSubmitting = false;
    });
  }

  closeConfirmDelete(): void {
    this.confirmDeleting = false;
  }

  receiveDelete() {
    this.conceptSetsService.deleteConceptSet(this.wsNamespace, this.wsId, this.conceptSet.id)
      .subscribe(() => {
        navigate(['workspaces', this.wsNamespace, this.wsId, 'concepts', 'sets']);
        this.closeConfirmDelete();
      });
  }

  removeConcepts() {
    this.removeSubmitting = true;
    this.conceptSetsService.updateConceptSetConcepts(
      this.wsNamespace, this.wsId, this.conceptSet.id, {
        etag: this.conceptSet.etag,
        removedIds: this.conceptTable.selectedConcepts.map(c => c.conceptId)
      }).subscribe((cs) => {
        this.conceptSet = cs;
        this.removing = false;
        this.removeSubmitting = false;
      }, () => {
        // TODO(calbach): Handle errors.
        this.removeSubmitting = false;
      });
  }

  get canEdit(): boolean {
    return this.accessLevel === WorkspaceAccessLevel.OWNER
        || this.accessLevel === WorkspaceAccessLevel.WRITER;
  }

  get selectedConceptsCount(): number {
    if (!this.conceptTable) {
      return 0;
    }
    return this.conceptTable.selectedConcepts.length;
  }

  get disableRemoveFab(): boolean {
    return !this.canEdit || this.selectedConceptsCount === 0;
  }
}
