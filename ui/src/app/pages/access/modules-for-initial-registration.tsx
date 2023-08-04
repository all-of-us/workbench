import * as React from 'react';

import { AccessModule, Profile } from 'generated/fetch';

import { FlexColumn } from 'app/components/flex';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';

import { styles } from './data-access-requirements';
import { MaybeModule } from './maybe-module';

interface InitialCardProps {
  profile: Profile;
  modules: AccessModule[];
  focusedModule: AccessModule;
  activeModules: AccessModule[];
  spinnerProps: WithSpinnerOverlayProps;
  children?: string | React.ReactNode;
}

export const ModulesForInitialRegistration = (props: InitialCardProps) => {
  const {
    profile,
    modules,
    focusedModule,
    activeModules,
    spinnerProps,
    children,
  } = props;

  return (
    <FlexColumn style={styles.modulesContainer}>
      {modules.map((moduleName, index) => (
        <MaybeModule
          {...{ moduleName, profile, spinnerProps }}
          key={moduleName}
          focused={focusedModule === moduleName}
          active={activeModules.includes(moduleName)}
          style={{
            marginTop:
              index > 0 || moduleName === AccessModule.CTCOMPLIANCETRAINING
                ? '1.9em'
                : '0em',
          }}
        />
      ))}
      {children}
    </FlexColumn>
  );
};
