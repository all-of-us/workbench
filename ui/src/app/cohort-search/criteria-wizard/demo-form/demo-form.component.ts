import {Component, OnInit} from '@angular/core';
import {FormControl, FormGroup} from '@angular/forms';

import {CohortBuilderService, Criteria} from 'generated';


const MAX_AGE = 120;
const MIN_AGE = 0;


@Component({
  selector: 'crit-demo-form',
  templateUrl: './demo-form.component.html',
  styleUrls: [
    './demo-form.component.css',
  ],
})
export class DemoFormComponent implements OnInit {
  demoForm = new FormGroup({
  });

  genders: Criteria[] = [];
  races: Criteria[] = [];

  constructor(
    private api: CohortBuilderService,
  ) {}

  ngOnInit() {}
  cancel() {}
  submit() {}
}
