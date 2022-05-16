import * as React from 'react';

import { AccessModule, Profile } from 'generated/fetch';

import { FlexColumn } from 'app/components/flex';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';

import { styles } from './data-access-requirements';
import { MaybeModule } from './maybe-module';

interface InitialCardProps {
  profile: Profile;
  modules: AccessModule[];
  activeModule: AccessModule;
  clickableModules: AccessModule[];
  spinnerProps: WithSpinnerOverlayProps;
  children?: string | React.ReactNode;
}

export const ModulesForInitialRegistration = (props: InitialCardProps) => {
  const {
    profile,
    modules,
    activeModule,
    clickableModules,
    spinnerProps,
    children,
  } = props;

  return (
    <FlexColumn style={styles.modulesContainer}>
      {modules.map((moduleName, index) => (
        <MaybeModule
          {...{ moduleName, profile, spinnerProps }}
          key={moduleName}
          active={activeModule === moduleName}
          clickable={clickableModules.includes(moduleName)}
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
