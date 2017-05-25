import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Params } from '@angular/router';

import 'rxjs/add/operator/switchMap';

import { User } from './user';
import { Repository } from './repository';
import { RepositoryService } from './repository.service';

@Component({
  templateUrl: './cohort-builder.component.html'
})
export class CohortBuilderComponent implements OnInit {
  repository: Repository;

  constructor(
    private route: ActivatedRoute,
    private repositoryService: RepositoryService
  ) { }

  ngOnInit(): void {
    this.route.params
      .switchMap((params: Params) => this.repositoryService.get(+params['id']))
      .subscribe(repository => this.repository = repository);
  }
}
