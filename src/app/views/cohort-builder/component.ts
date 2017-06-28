// View which wraps the third-party, plain javascript cohort browser widget.

import 'rxjs/add/operator/switchMap';

import {Component, Inject, OnInit} from '@angular/core';
import {ActivatedRoute, Params} from '@angular/router';

import {Repository} from 'app/models/repository';
import {RepositoryService} from 'app/services/repository.service';
import {User} from 'app/models/user';
import {VAADIN_CLIENT} from 'app/vaadin-client';

const vaadinRootElementId = 'cohort-builder-widget';
const vaadinConfig = {
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

@Component({templateUrl: './component.html'})
export class CohortBuilderComponent implements OnInit {
  vaadinJsUrl =
      'https://35.185.116.214/pmi-cb/VAADIN/vaadinBootstrap.js?v=7.7.5';

  repository: Repository;

  constructor(
      private route: ActivatedRoute,
      private repositoryService: RepositoryService,
      @Inject(VAADIN_CLIENT) private vaadinInst: any
  ) {}

  ngOnInit(): void {
    this.route.params
        .switchMap(
            (params: Params) => this.repositoryService.get(+params['id']))
        .subscribe(repository => {
          this.repository = repository;
          if (this.vaadinInst) {
            this.vaadinInst.initApplication(vaadinRootElementId, vaadinConfig);
          } else {
            console.error('Vaadin failed to load.');
          }
        });
  }
}
