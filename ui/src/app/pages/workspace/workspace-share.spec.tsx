import * as React from 'react';
import Select from 'react-select';
import * as fp from 'lodash/fp';
import { mount, ReactWrapper } from 'enzyme';

import {
  User,
  UserApi,
  UserRole,
  WorkspaceAccessLevel,
  WorkspacesApi,
} from 'generated/fetch';

import FakeTimers from '@sinonjs/fake-timers';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {
  registerApiClient,
  workspacesApi,
} from 'app/services/swagger-fetch-clients';
import { profileStore, serverConfigStore } from 'app/utils/stores';
import { WorkspaceData } from 'app/utils/workspace-data';

import defaultServerConfig from 'testing/default-server-config';
import {
  getSelectComponentValue,
  waitOneTickAndUpdate,
} from 'testing/react-test-helpers';
import { UserApiStub } from 'testing/stubs/user-api-stub';
import { WorkspacesApiStub } from 'testing/stubs/workspaces-api-stub';

import { WorkspaceShare, WorkspaceShareProps } from './workspace-share';

describe('WorkspaceShare', () => {
  let props: WorkspaceShareProps;
  let user;

  const component = () => {
    return mount(<WorkspaceShare {...props} />);
  };
  const componentAlt = () => {
    return render(<WorkspaceShare {...props} />);
  };
  const harry: User = {
    givenName: 'Harry',
    familyName: 'Potter',
    email: 'harry.potter@hogwarts.edu',
  };
  const harryRole: UserRole = {
    ...harry,
    email: harry.email,
    role: WorkspaceAccessLevel.OWNER,
  };
  const hermione: User = {
    givenName: 'Hermione',
    familyName: 'Granger',
    email: 'hermione.granger@hogwarts.edu',
  };
  const hermioneRole: UserRole = {
    ...hermione,
    email: hermione.email,
    role: WorkspaceAccessLevel.WRITER,
  };
  const ron: User = {
    givenName: 'Ronald',
    familyName: 'Weasley',
    email: 'ron.weasley@hogwarts.edu',
  };
  const ronRole: UserRole = {
    ...ron,
    email: ron.email,
    role: WorkspaceAccessLevel.READER,
  };
  const luna: User = {
    givenName: 'Luna',
    familyName: 'Lovegood',
    email: 'luna.lovegood@hogwarts.edu',
  };
  const lunaRole: UserRole = {
    ...luna,
    email: luna.email,
    role: WorkspaceAccessLevel.NO_ACCESS,
  };

  const tomRiddleDiary = {
    namespace: 'Horcrux',
    name: 'The Diary of Tom Marvolo Riddle',
    etag: '1',
    id: 'The Diary of Tom Marvolo Riddle',
    accessLevel: WorkspaceAccessLevel.OWNER,
  } as WorkspaceData;
  const tomRiddleDiaryUserRoles = [harryRole, hermioneRole, ronRole];

  const getSelectString = (user: UserRole) => {
    return '.' + user.email.replace(/[@\.]/g, '') + '-user-role__single-value';
  };

  const getSelectStringAlt = (user: UserRole) => {
    return `Role selector for ${user.email}`;
  };

  const getAddCollaboratorLabel = (user: User) => {
    return `Button to add ${user.email} as a collaborator`;
  };
  const getRemoveCollaboratorLabel = (user: User) => {
    return `Button to remove ${user.email} as a collaborator`;
  };

  const searchForUser = async (wrapper: ReactWrapper, clock, value: string) => {
    wrapper
      .find('[data-test-id="search"]')
      .simulate('change', { target: { value } });
    clock.tick(401);
    await waitOneTickAndUpdate(wrapper);
    wrapper.update();
  };

  const searchForUserAlt = async (value: string) => {
    const searchBar = screen.getByTestId('search');
    await user.clear(searchBar);
    await user.click(searchBar);
    await user.paste(value);
    await screen.findByTestId('drop-down');
  };

  beforeEach(() => {
    registerApiClient(
      UserApi,
      new UserApiStub([harryRole, hermioneRole, ronRole, lunaRole])
    );
    registerApiClient(
      WorkspacesApi,
      new WorkspacesApiStub([tomRiddleDiary], tomRiddleDiaryUserRoles)
    );
    serverConfigStore.set({ config: defaultServerConfig });

    props = {
      workspace: tomRiddleDiary,
      onClose: () => {},
    };

    profileStore.set({
      profile: {
        username: 'harry.potter@hogwarts.edu',
        generalDiscoverySources: [],
        partnerDiscoverySources: [],
      },
      load: jest.fn(),
      reload: jest.fn(),
      updateCache: jest.fn(),
    });
    user = userEvent.setup();
  });

  it('display correct users', async () => {
    componentAlt();

    let collabUserNames;
    await waitFor(() => {
      collabUserNames = screen.getAllByTestId('collab-user-name');
      expect(collabUserNames.length).toBe(3);
    });
    const collabUserEmails = screen.getAllByTestId('collab-user-email');
    expect(collabUserEmails.length).toBe(3);
    const expectedUsers = fp.sortBy('familyName', [harry, hermione, ron]);
    expect(collabUserNames.map((el) => el.textContent)).toEqual(
      expectedUsers.map((u) => u.givenName + ' ' + u.familyName)
    );
    expect(collabUserEmails.map((el) => el.textContent)).toEqual(
      expectedUsers.map((u) => u.email)
    );
  });

  it('displays correct role info', async () => {
    componentAlt();
    await waitFor(() =>
      expect(screen.queryByLabelText('Please Wait')).not.toBeInTheDocument()
    );

    expect(
      getSelectComponentValue(
        screen.getByLabelText(getSelectStringAlt(harryRole))
      )
    ).toEqual(fp.capitalize(WorkspaceAccessLevel[harryRole.role]));

    expect(
      getSelectComponentValue(
        screen.getByLabelText(getSelectStringAlt(hermioneRole))
      )
    ).toEqual(fp.capitalize(WorkspaceAccessLevel[hermioneRole.role]));

    expect(
      getSelectComponentValue(
        screen.getByLabelText(getSelectStringAlt(ronRole))
      )
    ).toEqual(fp.capitalize(WorkspaceAccessLevel[ronRole.role]));

    expect(
      screen.queryByLabelText(getSelectStringAlt(lunaRole))
    ).not.toBeInTheDocument();
  });

  it('removes user correctly', async () => {
    componentAlt();
    await waitFor(() =>
      expect(screen.queryByLabelText('Please Wait')).not.toBeInTheDocument()
    );

    const removeButton = screen.getByLabelText(getRemoveCollaboratorLabel(ron));

    await user.click(removeButton);

    const expectedNames = fp
      .sortBy('familyName', [harry, hermione])
      .map((u) => u.givenName + ' ' + u.familyName);
    expect(
      (await screen.findAllByTestId('collab-user-name')).map(
        (el) => el.textContent
      )
    ).toEqual(expectedNames);
  });

  it('adds user correctly', async () => {
    componentAlt();
    await searchForUserAlt('luna');
    await user.click(screen.getByLabelText(getAddCollaboratorLabel(luna)));
    const expectedNames = fp
      .sortBy('familyName', [harry, hermione, ron, luna])
      .map((u) => u.givenName + ' ' + u.familyName);
    expect(
      (await screen.findAllByTestId('collab-user-name')).map(
        (el) => el.textContent
      )
    ).toEqual(expectedNames);
  });

  it('does not allow self-removal', () => {
    const wrapper = component();
    const dataString =
      '[data-test-id="remove-collab-harry.potter@hogwarts.edu"]';
    expect(wrapper.find(dataString).length).toBe(0);
  });

  it('does not allow self role change', async () => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);

    const roleSelectProps = wrapper
      .find('[data-test-id="harry.potter@hogwarts.edu-user-role"]')
      .first()
      .props() as { isDisabled: boolean };
    expect(roleSelectProps.isDisabled).toBe(true);
  });

  it('saves acl correctly after changes made', async () => {
    const clock = FakeTimers.install({ shouldAdvanceTime: true });
    const wrapper = component();
    await searchForUser(wrapper, clock, 'luna');

    // Luna: add, remove, add again
    wrapper
      .find('[data-test-id="add-collab-luna.lovegood@hogwarts.edu"]')
      .first()
      .simulate('click');
    wrapper
      .find('[data-test-id="remove-collab-luna.lovegood@hogwarts.edu"]')
      .first()
      .simulate('click');
    await searchForUser(wrapper, clock, 'luna');
    wrapper
      .find('[data-test-id="add-collab-luna.lovegood@hogwarts.edu"]')
      .first()
      .simulate('click');

    // Ron: remove, add, remove again
    wrapper
      .find('[data-test-id="remove-collab-ron.weasley@hogwarts.edu"]')
      .first()
      .simulate('click');
    await searchForUser(wrapper, clock, 'ron');
    wrapper
      .find('[data-test-id="add-collab-ron.weasley@hogwarts.edu"]')
      .first()
      .simulate('click');
    wrapper
      .find('[data-test-id="remove-collab-ron.weasley@hogwarts.edu"]')
      .first()
      .simulate('click');

    clock.uninstall();

    // change hermione's access to owner
    const selectComponent = wrapper
      .find('[data-test-id="hermione.granger@hogwarts.edu-user-role"]')
      .find(Select);
    selectComponent.instance().setState({ menuIsOpen: true });
    wrapper.update();
    wrapper
      .find('.hermionegrangerhogwartsedu-user-role__option')
      .findWhere(
        (n) =>
          n.text() ===
          fp.capitalize(WorkspaceAccessLevel[WorkspaceAccessLevel.OWNER])
      )
      .first()
      .simulate('click');
    wrapper.update();

    const spy = jest.spyOn(workspacesApi(), 'shareWorkspacePatch');
    wrapper.find('[data-test-id="save"]').first().simulate('click');
    expect(spy).toHaveBeenCalledWith(
      tomRiddleDiary.namespace,
      tomRiddleDiary.name,
      {
        workspaceEtag: tomRiddleDiary.etag,
        items: [
          { ...lunaRole, role: WorkspaceAccessLevel.READER },
          { ...ronRole, role: WorkspaceAccessLevel.NO_ACCESS },
          { ...hermioneRole, role: WorkspaceAccessLevel.OWNER },
        ],
      }
    );
  });
});
