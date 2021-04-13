import * as React from 'react';
import {MenuItem, SnowmanButton} from './buttons';
import {PopupTrigger, TooltipTrigger} from './popups';

interface Action {
  icon: string;
  displayName: string;
  onClick: () => void;
  disabled: boolean;
  hoverText?: string;
}

const ResourceActionsMenu = (props: { actions: Action[] }) => {
  const {actions} = props;
  return <PopupTrigger
        data-test-id='resource-card-menu'
        side='bottom'
        closeOnClick
        content={
            <React.Fragment>
                {actions.map((action, i) => {
                  return (
                        <TooltipTrigger key={i} content={action.hoverText}>
                            <MenuItem
                                icon={action.icon}
                                onClick={() => action.onClick()}
                                disabled={action.disabled}>
                                {action.displayName}
                            </MenuItem>
                        </TooltipTrigger>);
                })}
            </React.Fragment>
        }
    >
        <SnowmanButton data-test-id='resource-menu'/>
    </PopupTrigger>;
};
export {
    Action,
    ResourceActionsMenu,
};
