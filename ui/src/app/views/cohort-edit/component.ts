import {Component} from '@angular/core';
import {FormBuilder, FormControl, FormGroup, Validators} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';

import {Cohort, CohortsService, Workspace} from 'generated';

@Component({templateUrl: './component.html'})
export class CohortEditComponent {
  /* Properties */
  loading = false;
  form: FormGroup;

  /* Convenience property aliases */
  get name(): FormControl         { return this.form.get('name') as FormControl; }
  get description(): FormControl  { return this.form.get('description') as FormControl; }
  get cohort(): Cohort            { return this.route.snapshot.data.cohort; }
  get workspace(): Workspace      { return this.route.snapshot.data.workspace; }

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private fb: FormBuilder,
    private cohortService: CohortsService,
  ) {
    this.form = fb.group({
      name: [this.cohort.name, Validators.required],
      description: this.cohort.description || '',
    });
  }

  save(): void {
    this.loading = true;

    const newCohort = <Cohort>{
      ...this.cohort,
      name: this.name.value,
      description: this.description.value,
    };

    const call = this.cohortService.updateCohort(
      this.workspace.namespace,
      this.workspace.id,
      newCohort.id,
      newCohort
    );

    call.do(_ => this.loading = false)
      .subscribe(_ => this.backToWorkspace());
  }

  revert(): void {
    this.name.setValue(this.cohort.name);
    this.description.setValue(this.cohort.description);
  }

  backToWorkspace(): void {
    this.router.navigate(['workspace', this.workspace.namespace, this.workspace.id]);
  }

  get canSave(): boolean {
    const nameHasChanged = this.name.value !== this.cohort.name;
    const descHasChanged = this.description.value !== this.cohort.description;
    return this.form.valid && (nameHasChanged || descHasChanged);
  }
}
