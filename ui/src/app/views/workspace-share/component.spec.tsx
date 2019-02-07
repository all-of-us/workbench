import {mount} from 'enzyme';
import * as React from 'react';
import * as lolex from 'lolex';

import {WorkspaceShare, WorkspaceShareProps, WorkspaceShareState} from './component';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';

import {User, UserApi, UserRole, Workspace, WorkspaceAccessLevel} from 'generated/fetch';
import {registerApiClient} from "../../services/swagger-fetch-clients";
import {UserApiStub} from 'testing/stubs/user-api-stub'

describe('WorkspaceShareComponent', () => {
  let props: WorkspaceShareProps;

  const component = () => {
    return mount<WorkspaceShare, WorkspaceShareProps, WorkspaceShareState>
    (<WorkspaceShare {...props}/>);
  };
  const harry: User = {givenName: 'Harry', familyName: 'Potter', email: 'harry.potter@hogwarts.edu'};
  const harryRole: UserRole = {...harry, email: harry.email, role: WorkspaceAccessLevel.OWNER};
  const hermione: User = {givenName: 'Hermione', familyName: 'Granger', email: 'hermione.granger@hogwarts.edu'};
  const hermioneRole: UserRole = {...hermione, email: hermione.email, role: WorkspaceAccessLevel.WRITER};
  const ron: User = {givenName: 'Ronald', familyName: 'Weasley', email: 'ron.weasley@hogwarts.edu'};
  const ronRole: UserRole = {...ron, email: ron.email, role: WorkspaceAccessLevel.READER};
  const luna: User = {givenName: 'Luna', familyName: 'Lovegood', email: 'luna.lovegood@hogwarts.edu'};
  const lunaRole: UserRole = {...luna, email: luna.email, role: WorkspaceAccessLevel.NOACCESS};

  const tomRiddleDiary = {name: 'The Diary of Tom Marvolo Riddle', userRoles: [harryRole, hermioneRole, ronRole]} as Workspace;

  beforeEach(() => {
    registerApiClient(UserApi, new UserApiStub([harryRole, hermioneRole, ronRole, lunaRole]));
    props = {
      closeFunction: () => {},
      workspace: tomRiddleDiary,
      accessLevel: WorkspaceAccessLevel.OWNER,
      userEmail: 'harry.potter@hogwarts.edu',
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
    wrapper.find('[data-test-id="remove-collab-ron.weasley@hogwarts.edu"]').first().simulate('click');
    const expectedNames = [harry, hermione].map(u => u.givenName + ' ' + u.familyName);
    expect(wrapper.find('[data-test-id="collab-user-name"]').map(el => el.text())).toEqual(expectedNames);
  });

  it('adds user correctly', async() => {
    let clock = lolex.install({shouldAdvanceTime: true});
    const wrapper = component();
    wrapper.find('[data-test-id="search"]').simulate('change', {target: {value: 'luna'}});
    clock.tick(401);
    await waitOneTickAndUpdate(wrapper);
    wrapper.update();
    clock.uninstall();
    wrapper.find('[data-test-id="add-collab-luna.lovegood@hogwarts.edu"]').first().simulate('click');
    const expectedNames = [harry, hermione, ron, luna].map(u => u.givenName + ' ' + u.familyName);
    expect(wrapper.find('[data-test-id="collab-user-name"]').map(el => el.text())).toEqual(expectedNames);
  });

  it('does not allow self-removal', () => {
    const wrapper = component();
    const dataString = '[data-test-id="remove-collab-harry.potter@hogwarts.edu"]';
    expect(wrapper.find(dataString).length).toBe(0);
  })

});
