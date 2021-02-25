import * as React from 'react';
import {useEffect} from 'react';

import {Profile} from 'generated/fetch';

import {profileApi} from 'app/services/swagger-fetch-clients';
import {apiCallWithConflictRetries} from 'app/utils/retry';
import {useStore} from 'app/utils/stores';
import {atom} from 'app/utils/subscribable';

interface ProfileStore {
  profile?: Profile;
}

const reactProfileStore = atom<ProfileStore>({});

export const ProfileContext = React.createContext({
  profile: {},
  reload: () => {}
});

export const ProfileProvider = ({children}) => {
  useStore(reactProfileStore);

  const getProfile = async() => {
    const profile = await apiCallWithConflictRetries(() => profileApi().getMe());
    reactProfileStore.set({...reactProfileStore.get(), profile: profile});
    return profile;
  };

  useEffect(() => {
    async function getProfileWrapper() {
      await getProfile();
    }

    getProfileWrapper();
  }, []);

  return <ProfileContext.Provider value={{
    profile: reactProfileStore.get().profile,
    reload: async() => await getProfile()
  }}>
    {children}
  </ProfileContext.Provider>;
};

export const withProfileContext = Component =>
    props => (
        <ProfileContext.Consumer>
          {context => <Component profileContext={context} {...props}/>}
        </ProfileContext.Consumer>
    );
