import * as React from 'react';
import { IconDefinition } from '@fortawesome/free-solid-svg-icons';

import { KebabCircleButton, MenuItem, SnowmanButton } from './buttons';
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
  appsAnalysis?: boolean;
}) => {
  const { actions, disabled } = props;
  return (
    <PopupTrigger
      data-test-id='resource-card-menu'
      side='bottom'
      closeOnClick
      content={
        !disabled && (
          <React.Fragment>
            {actions.map((action, i) => {
              return (
                <TooltipTrigger key={i} content={action.hoverText}>
                  <MenuItem
                    icon={action.icon}
                    faIcon={action.faIcon}
                    onClick={() => action.onClick()}
                    disabled={action.disabled}
                  >
                    {action.displayName}
                  </MenuItem>
                </TooltipTrigger>
              );
            })}
          </React.Fragment>
        )
      }
    >
      {props.appsAnalysis ? (
        <KebabCircleButton data-test-id='resource-menu' disabled={disabled} />
      ) : (
        <SnowmanButton data-test-id='resource-menu' disabled={disabled} />
      )}
    </PopupTrigger>
  );
};
