import * as React from 'react';
import { IconDefinition } from '@fortawesome/free-solid-svg-icons';

import { MenuItem, SnowmanButton } from './buttons';
import { PopupTrigger, TooltipTrigger } from './popups';

export interface Action {
  icon?: string;
  faIcon?: IconDefinition;
  displayName: string;
  onClick: () => void;
  disabled: boolean;
  hoverText?: string;
}

export const ResourceActionsMenu = (props: {
  actions: Action[];
  disabled?: boolean;
  menuButtonComponentOverride?: (props: { disabled: boolean }) => JSX.Element;
}) => {
  const { actions, disabled, menuButtonComponentOverride } = props;

  const menuButtonComponent: (props: { disabled: boolean }) => JSX.Element =
    menuButtonComponentOverride ?? SnowmanButton;

  return (
    <PopupTrigger
      data-test-id='resource-card-menu'
      side='bottom'
      closeOnClick
      content={
        !disabled && (
          <React.Fragment>
            {actions.map((action, i) => {
              const { hoverText, displayName } = action;
              return (
                <TooltipTrigger key={i} content={hoverText}>
                  <MenuItem {...action}>{displayName}</MenuItem>
                </TooltipTrigger>
              );
            })}
          </React.Fragment>
        )
      }
    >
      {menuButtonComponent({ ...{ disabled } })}
    </PopupTrigger>
  );
};
