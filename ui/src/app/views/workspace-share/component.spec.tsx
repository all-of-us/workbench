// Adds users correctly
// Does not allow for self-removal

import {shallow} from 'enzyme';
import * as React from 'react';

import {
  WorkspaceShareProps,
  WorkspaceShareState,
  WorkspaceShare
} from './component';

import {User, UserApi, UserRole, Workspace, WorkspaceAccessLevel} from 'generated/fetch';
import {registerApiClient} from "../../services/swagger-fetch-clients";
import {UserServiceStub} from "../../../testing/stubs/user-service-stub";
// import {registerApiClient, userApi} from "../../services/swagger-fetch-clients";
// import {UserServiceStub} from "../../../testing/stubs/user-service-stub";

describe('WorkspaceShareComponent', () => {
  let props: WorkspaceShareProps;

  const component = () => {
    return shallow<WorkspaceShare, WorkspaceShareProps, WorkspaceShareState>
    (<WorkspaceShare {...props}/>);
  };
  const harry = {givenName: 'Harry', familyName: 'Potter', email: 'harry.potter@hogwarts.edu'} as User;
  const harryRole = {...harry, role: WorkspaceAccessLevel.OWNER} as UserRole;
  const hermione = {givenName: 'Hermione', familyName: 'Granger', email: 'hermione.granger@hogwarts.edu'} as User;
  const hermioneRole = {...hermione, role: WorkspaceAccessLevel.WRITER} as UserRole;
  const ron = {givenName: 'Ronald', familyName: 'Weasley', email: 'ron.weasley@hogwarts.edu'} as User;
  const ronRole = {...ron, role: WorkspaceAccessLevel.READER} as UserRole;

  const tomRiddleDiary = {name: 'The Diary of Tom Marvolo Riddle', userRoles: [harryRole, hermioneRole, ronRole]} as Workspace;

  beforeEach(() => {
    props = {
      closeFunction: () => {},
      workspace: tomRiddleDiary,
      accessLevel: WorkspaceAccessLevel.OWNER,
      userEmail: 'harry.potter@test.com',
      sharing: true
    };
  });

  it('display correct ACL information', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
    expect(wrapper.find('[data-test-id="collab-user-name"]').length).toBe(3);
    expect(wrapper.find('[data-test-id="collab-user-email"]').length).toBe(3);
    const expectedNames = [harry, hermione, ron].map(u => u.givenName + ' ' + u.familyName);
    expect(wrapper.find('[data-test-id="collab-user-name"]').map(el => el.text())).toEqual(expectedNames);
    const expectedEmails = [harry, hermione, ron].map(u => u.email);
    expect(wrapper.find('[data-test-id="collab-user-email"]').map(el => el.text())).toEqual(expectedEmails);
  });

  it('removes user correctly', () => {
    const wrapper = component();
    wrapper.find('[data-test-id="remove-collab-ron.weasley@hogwarts.edu"]').simulate('click');
    const expectedNames = [harry, hermione].map(u => u.givenName + ' ' + u.familyName);
    expect(wrapper.find('[data-test-id="collab-user-name"]').map(el => el.text())).toEqual(expectedNames);
  });

  // TODO: 1/29/2019 US
  // Cannot decide between mocking or stubbing here. Will come back to this.
  // it('adds user correctly', () => {
  //   registerApiClient(UserApi, new UserServiceStub());
  //
  // });

});
