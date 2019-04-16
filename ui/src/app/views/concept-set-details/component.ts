import {Component, OnDestroy, OnInit, ViewChild} from '@angular/core';

import {currentConceptSetStore, currentWorkspaceStore, navigate, urlParamsStore} from 'app/utils/navigation';
import {ConceptTableComponent} from 'app/views/concept-table/component';

import {
  Concept,
  ConceptSet,
  ConceptSetsService,
  WorkspaceAccessLevel,
} from 'generated';
import {ConceptSet as FetchConceptSet} from 'generated/fetch';

@Component({
  styleUrls: ['../../styles/buttons.css',
    '../../styles/cards.css',
    '../../styles/headers.css',
    '../../styles/inputs.css',
    '../../styles/errors.css',
    './component.css'],
  templateUrl: './component.html',
})
export class ConceptSetDetailsComponent implements OnInit, OnDestroy {
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
  selectedConcepts: Concept[];

  constructor(
    private conceptSetsService: ConceptSetsService,
  ) {
    this.receiveDelete = this.receiveDelete.bind(this);
    this.closeConfirmDelete = this.closeConfirmDelete.bind(this);
    this.removeFab = this.removeFab.bind(this);
  }

  ngOnInit() {
    const {ns, wsid, csid} = urlParamsStore.getValue();
    this.wsNamespace = ns;
    this.wsId = wsid;
    const {accessLevel} = currentWorkspaceStore.getValue();
    this.accessLevel = accessLevel;
    this.conceptSetsService.getConceptSet(ns, wsid, csid).subscribe(conceptSet => {
      currentConceptSetStore.next(conceptSet as unknown as FetchConceptSet);
      this.conceptSet = conceptSet;
      this.editName = conceptSet.name;
      this.editDescription = conceptSet.description;
    });
  }

  ngOnDestroy() {
    currentConceptSetStore.next(undefined);
  }

  validateEdits(): boolean {
    return !!this.editName;
  }

  removeFab(): void {
    this.removing = true;
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

  // TO-DO: This is a dummy function created because @Output in conceptTable is removed
  onSelectConcepts(concepts) {
    this.selectedConcepts = concepts;
  }

  get canEdit(): boolean {
    return this.accessLevel === WorkspaceAccessLevel.OWNER
        || this.accessLevel === WorkspaceAccessLevel.WRITER;
  }

  get selectedConceptsCount(): number {
    return !this.conceptTable ? 0 : this.conceptTable.selectedConcepts.length;
  }

  get disableRemoveFab(): boolean {
    return !this.canEdit || this.selectedConceptsCount === 0;
  }
}
