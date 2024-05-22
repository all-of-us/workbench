import * as React from 'react';
import * as fp from 'lodash/fp';

import {
  User,
  UserApi,
  UserRole,
  WorkspaceAccessLevel,
  WorkspacesApi,
} from 'generated/fetch';

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
  waitForNoSpinner,
} from 'testing/react-test-helpers';
import { UserApiStub } from 'testing/stubs/user-api-stub';
import { WorkspacesApiStub } from 'testing/stubs/workspaces-api-stub';

import { WorkspaceShare, WorkspaceShareProps } from './workspace-share';

describe('WorkspaceShare', () => {
  let props: WorkspaceShareProps;
  let user;

  const component = () => {
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

  const getUserRoleDropdownLabel = (desiredUser: User) => {
    return `Role selector for ${desiredUser.email}`;
  };

  const getAddCollaboratorLabel = (desiredUser: User) => {
    return `Button to add ${desiredUser.email} as a collaborator`;
  };
  const getRemoveCollaboratorLabel = (desiredUser: User) => {
    return `Button to remove ${desiredUser.email} as a collaborator`;
  };

  const addUser = async (userToAdd: User) => {
    const searchBar = screen.getByTestId('search');
    await user.clear(searchBar);
    await user.click(searchBar);
    await user.paste(userToAdd.givenName);
    await screen.findByTestId('drop-down');
    await user.click(screen.getByLabelText(getAddCollaboratorLabel(userToAdd)));
  };

  const removeUser = async (userToRemove: User) => {
    await user.click(
      screen.getByLabelText(getRemoveCollaboratorLabel(userToRemove))
    );
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
    component();

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
    component();
    await waitForNoSpinner();

    expect(
      getSelectComponentValue(
        screen.getByLabelText(getUserRoleDropdownLabel(harry))
      )
    ).toEqual(fp.capitalize(WorkspaceAccessLevel[harryRole.role]));

    expect(
      getSelectComponentValue(
        screen.getByLabelText(getUserRoleDropdownLabel(hermione))
      )
    ).toEqual(fp.capitalize(WorkspaceAccessLevel[hermioneRole.role]));

    expect(
      getSelectComponentValue(
        screen.getByLabelText(getUserRoleDropdownLabel(ron))
      )
    ).toEqual(fp.capitalize(WorkspaceAccessLevel[ronRole.role]));

    expect(
      screen.queryByLabelText(getUserRoleDropdownLabel(luna))
    ).not.toBeInTheDocument();
  });

  it('removes user correctly', async () => {
    component();
    await waitForNoSpinner();

    await removeUser(ron);

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
    component();
    await waitForNoSpinner();
    await addUser(luna);
    const expectedNames = fp
      .sortBy('familyName', [harry, hermione, ron, luna])
      .map((u) => u.givenName + ' ' + u.familyName);
    expect(
      (await screen.findAllByTestId('collab-user-name')).map(
        (el) => el.textContent
      )
    ).toEqual(expectedNames);
  });

  it('does not allow self-removal', async () => {
    component();
    await waitForNoSpinner();
    expect(
      screen.queryByLabelText(getRemoveCollaboratorLabel(harry))
    ).not.toBeInTheDocument();
  });

  it('does not allow self role change', async () => {
    component();
    await waitForNoSpinner();

    expect(
      screen.getByLabelText(getUserRoleDropdownLabel(harry))
    ).toBeDisabled();
  });

  it('saves acl correctly after changes made', async () => {
    component();
    await waitForNoSpinner();

    // Luna: add, remove, add again
    await addUser(luna);
    await removeUser(luna);
    await addUser(luna);

    // Ron: remove, add, remove again
    await removeUser(ron);
    await addUser(ron);
    await removeUser(ron);

    // change hermione's access to owner
    const hermoineRoleDropdown = screen.getByLabelText(
      getUserRoleDropdownLabel(hermione)
    );
    await user.click(hermoineRoleDropdown);
    await user.click(
      screen.getByLabelText(`Select Owner role for ${hermione.email}`)
    );

    const spy = jest.spyOn(workspacesApi(), 'shareWorkspacePatch');
    await user.click(
      screen.getByRole('button', {
        name: /save/i,
      })
    );
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
