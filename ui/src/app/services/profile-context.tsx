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

const ProfileContext = React.createContext({
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

  // For this particular store, we want to fetch whenever the first ProfileProvider first renders,
  // and then only when reload is explicitly called.
  // When converting Angular services, pay attention to how they are used and copy that behavior in
  // the store. For instance, the cdr version store and the server config store should only be
  // populated once per session.
  useEffect(() => {
    async function getProfileWrapper() {
      await getProfile();
    }

    if (reactProfileStore.get() !== undefined) {
      getProfileWrapper();
    }
  }, []);

  return <ProfileContext.Provider value={{
    profile: reactProfileStore.get().profile,
    reload: async() => await getProfile()
  }}>
    {children}
  </ProfileContext.Provider>;
};

// This HOC can be used to wrap class components that need ProfileContext injected.
// For function components, using useContext(ProfileContext) is preferred.
export const withProfileContext = Component =>
    props => (
        <ProfileContext.Consumer>
          {context => <Component profileContext={context} {...props}/>}
        </ProfileContext.Consumer>
    );
