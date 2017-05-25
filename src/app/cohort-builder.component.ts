// View which wraps the third-party, plain javascript cohort browser widget.

import { Component, OnInit }      from '@angular/core';
import { ActivatedRoute, Params } from '@angular/router';

import 'rxjs/add/operator/switchMap';

import { User }                   from './user';
import { Repository }             from './repository';
import { RepositoryService }      from './repository.service';

const vaadinRootElementId = 'cohort-builder-widget';
const vaadinConfig =
  {
    'theme': 'cohortbuilder',
    'versionInfo': {'vaadinVersion': '7.7.5'},
    'widgetset': 'org.vumc.ori.cohortbuilder.widgetsets.CohortBuilderWidgetSet',
    'comErrMsg': {
      'caption': 'Communication problem',
      'message': 'Take note of any unsaved data, and <u>click here</u> or press ESC to continue.',
    },
    'authErrMsg': {
      'caption': 'Authentication problem',
      'message': 'Take note of any unsaved data, and <u>click here</u> or press ESC to continue.',
    },
    'sessExpMsg': {
      'caption': 'Session Expired',
      'message': 'Take note of any unsaved data, and <u>click here</u> or press ESC to continue.',
    },
    'vaadinDir': 'https://35.185.116.214/pmi-cb/VAADIN/',
    'debug': false,
    'standalone': true,
    'heartbeatInterval': 300,
    'serviceUrl': 'https://35.185.116.214/pmi-cb/vaadinServlet/',
    'browserDetailsUrl': 'https://35.185.116.214/pmi-cb/'
  };

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
      .subscribe(repository => {
        this.repository = repository;
        vaadin.initApplication(vaadinRootElementId, vaadinConfig);
      });
  }
}
