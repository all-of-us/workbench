import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Params } from '@angular/router';

import 'rxjs/add/operator/switchMap';

import { User } from './user';
import { Repository } from './repository';
import { RepositoryService } from './repository.service';

import { vaadin } from './vaadin'

const vaadinRootElementId = 'cohort-builder-widget';
const vaadinConfig =
  {
    'theme': 'cohortbuilder',
    'versionInfo': {'vaadinVersion': '7.7.5'},
    'widgetset': 'org.vumc.ori.cohortbuilder.widgetsets.CohortBuilderWidgetSet',
    'comErrMsg': {
      'caption': 'Communication problem',
      'message': 'Take note of any unsaved data, and <u>click here</u> or press ESC to continue.',
      'url': null
    },
    'authErrMsg': {
      'caption': 'Authentication problem',
      'message': 'Take note of any unsaved data, and <u>click here</u> or press ESC to continue.',
      'url': null
    },
    'sessExpMsg': {
      'caption': 'Session Expired',
      'message': 'Take note of any unsaved data, and <u>click here</u> or press ESC to continue.',
      'url': null
    },
    'vaadinDir': 'http://35.185.116.214/pmi-cb/VAADIN/',
    'debug': false,
    'standalone': true,
    'heartbeatInterval': 300,
    'serviceUrl': 'http://35.185.116.214/pmi-cb/vaadinServlet/',
    'browserDetailsUrl': 'http://35.185.116.214/pmi-cb/'
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
