import {Component, CUSTOM_ELEMENTS_SCHEMA} from '@angular/core';
import {ComponentFixture, fakeAsync, TestBed} from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {By} from '@angular/platform-browser';
import {RouterTestingModule} from '@angular/router/testing';

import {ClarityModule} from '@clr/angular';

import {CohortsServiceStub} from 'testing/stubs/cohort-service-stub';
import {SignInServiceStub} from 'testing/stubs/sign-in-service-stub';
import {WorkspacesServiceStub} from 'testing/stubs/workspace-service-stub';
import {simulateClick, updateAndTick} from 'testing/test-helpers';

import {SignInService} from 'app/services/sign-in.service';
import {CohortsService} from 'generated/api/cohorts.service';
import {ConceptSetsService} from 'generated/api/conceptSets.service';
import {WorkspacesService} from 'generated/api/workspaces.service';

import {ResourceCardComponent, ResourceCardMenuComponent} from './component';

import {ConfirmDeleteModalComponent} from 'app/views/confirm-delete-modal/component';
import {EditModalComponent} from 'app/views/edit-modal/component';
import {RenameModalComponent} from 'app/views/rename-modal/component';

@Component({
  selector: 'app-resource-card-wrapper',
  template: '<app-resource-card [resourceCard]="resourceCard"></app-resource-card>'
})

class ResourceCardWrapperComponent {
  resourceCard = {
    workspaceId: 1,
    workspaceNamespace: 'defaultNamespace',
    workspaceFirecloudName: 'defaultFirecloudName',
    permission: 'Owner',
    cohort: new CohortsServiceStub().cohorts[0],
    modifiedTime: Date.now().toString()
  };
}

describe('ResourceCardComponent', () => {
  let component: ResourceCardComponent;
  let fixture:  ComponentFixture<ResourceCardWrapperComponent>;
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        FormsModule,
        ReactiveFormsModule,
        RouterTestingModule,
        ClarityModule.forRoot()
      ],
      declarations: [
        ResourceCardWrapperComponent,
        ResourceCardComponent,
        ResourceCardMenuComponent,
        ConfirmDeleteModalComponent,
        RenameModalComponent,
        EditModalComponent,
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
      providers: [
        {provide: CohortsService, useValue: new CohortsServiceStub()},
        {provide: ConceptSetsService },
        {provide: SignInService, useValue: new SignInServiceStub()},
        {provide: WorkspacesService, useValue: new WorkspacesServiceStub()},
      ]
    }).compileComponents()
      .then(() => {
      fixture = TestBed.createComponent(ResourceCardWrapperComponent);
      component = fixture.debugElement.children[0].componentInstance;

      fixture.detectChanges();
    });
  }));

  it('should render', fakeAsync(() => {
    expect(component).toBeTruthy();
  }));

  // it should render cohort if resource is cohort
  it('should render cohort if resource is cohort', fakeAsync(() => {
    expect(fixture.debugElement.query(By.css('.cohort-card'))).toBeTruthy();
    expect(fixture.debugElement.query(By.css('#cohort-decoration'))).toBeTruthy();
  }));

  // it should render notebook if resource is notebook
  it('should render notebook if resource is notebook', fakeAsync(() => {
    setNotebookResource(fixture, component);
    updateAndTick(fixture);
    expect(fixture.debugElement.query(By.css('.notebook-card'))).toBeTruthy();
  }));
});

function setNotebookResource(fixture, component) {
  component.resourceCard = {
    workspaceId: 1,
    workspaceNamespace: 'defaultNamespace',
    workspaceFirecloudName: 'defaultFirecloudName',
    permission: 'Owner',
    modifiedTime: Date.now().toString(),
    notebook: {
      'name': 'mockFile.ipynb',
      'path': 'gs://bucket/notebooks/mockFile.ipynb',
      'lastModifiedTime': 100
    },
  };
  // call OnInit here because that's where ResourceCard reads the resourceType
  component.ngOnInit();
  updateAndTick(fixture);
}
