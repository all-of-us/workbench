import {Component, EventEmitter, Input, Output} from '@angular/core';
import {FormBuilder, FormControl, FormGroup, Validators} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';

import {
  Cohort,
  CohortsService,
  ConceptSet,
  ConceptSetsService,
  RecentResource,
  Workspace
} from 'generated';

import {ResourceType} from 'app/utils/resourceActions';

@Component({
  selector: 'app-edit-modal',
  styleUrls: ['./component.css',
    '../../styles/buttons.css',
    '../../styles/inputs.css'],
  templateUrl: './component.html'
})
export class EditModalComponent {
  /* Properties */
  loading = false;
  form: FormGroup;
  @Input() resource: RecentResource;
  @Input() cohort: any;
  @Output() updateFinished = new EventEmitter<any>();
  editing = false;
  rType: string;
  rName: string;
  rDescription: string;

  /* Convenience property aliases */
  get name(): FormControl         { return this.form.get('name') as FormControl; }
  get description(): FormControl  { return this.form.get('description') as FormControl; }
  get workspace(): Workspace      { return this.route.snapshot.data.workspace; }

  constructor(
    private route: ActivatedRoute,
    private fb: FormBuilder,
    private cohortService: CohortsService,
    private conceptSetService: ConceptSetsService,
  ) {
    this.form = fb.group({
      name: ['', Validators.required],
      description: '',
    });
  }

  open(): void {
    this.editing = true;
    this.loading = false;
    this.rType = this.resource.cohort ? 'Cohort' : 'Concept Set';
    this.rName = this.resource.cohort ? this.resource.cohort.name : this.resource.conceptSet.name;
    this.rDescription = this.resource.cohort ?
      this.resource.cohort.description : this.resource.conceptSet.description;
    this.form.setValue({ name: this.rName, description: this.rDescription });
    if (this.resource.cohort) {
      this.form.setValue({name: this.resource.cohort.name,
        description: this.resource.cohort.description});
    } else if (this.resource.conceptSet) {
      this.form.setValue({name: this.resource.conceptSet.name,
        description: this.resource.conceptSet.description});
    }
    this.form.setValue({ name: this.rName, description: this.rDescription });
  }

  close(): void {
    this.editing = false;
  }

  save(): void {
    this.loading = true;

    if (this.resource.cohort) {
      const newCohort = <Cohort>{
        ...this.resource.cohort,
        name: this.name.value,
        description: this.description.value,
      };

      this.cohortService.updateCohort(
        this.workspace.namespace,
        this.workspace.id,
        newCohort.id,
        newCohort
      ).do(_ => this.loading = false)
        .subscribe(_ => this.updateFinished.emit());

    } else if (this.resource.conceptSet) {
      const newConceptSet = <ConceptSet>{
        ...this.resource.conceptSet,
        name: this.name.value,
        description: this.description.value
      };

      this.conceptSetService.updateConceptSet(
        this.workspace.namespace,
        this.workspace.id,
        newConceptSet.id,
        newConceptSet
      ).do(_ => this.loading = false)
        .subscribe(_ => this.updateFinished.emit());
    }
  }

  get canSave(): boolean {
    if (this.editing) {
      const nameHasChanged = this.name.value !== this.resource.cohort.name ||
        this.name.value !== this.resource.conceptSet.name;
      const descHasChanged = this.description.value !== this.resource.cohort.description ||
        this.description.value !== this.resource.conceptSet.description;
      return this.form.valid && (nameHasChanged || descHasChanged) && !this.loading;
    } else {
      return false;
    }
  }
}
