import * as fp from 'lodash/fp';
import * as React from 'react';

import {Clickable, MenuItem} from 'app/components/buttons';
import {ResourceCardBase} from 'app/components/card';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {SnowmanIcon} from 'app/components/icons';
import {PopupTrigger, TooltipTrigger} from 'app/components/popups';
import colors from 'app/styles/colors';
import {formatWorkspaceResourceDisplayDate, reactStyles} from 'app/utils';
import {navigateAndPreventDefaultIfNoKeysPressed} from 'app/utils/navigation';
import {
  getDescription,
  getDisplayName,
  getResourceUrl,
  getTypeString,
  isCohort,
  isCohortReview,
  isConceptSet,
  isDataSet,
  isNotebook,
} from 'app/utils/resources';
import {WorkspaceResource} from 'generated/fetch';

const styles = reactStyles({
  card: {
    marginTop: '1rem',
    justifyContent: 'space-between',
    marginRight: '1rem',
    padding: '0.75rem 0.75rem 0rem 0.75rem',
    boxShadow: '0 0 0 0'
  },
  cardName: {
    fontSize: '18px', fontWeight: 500, lineHeight: '22px', color: colors.accent,
    cursor: 'pointer', wordBreak: 'break-all', textOverflow: 'ellipsis',
    overflow: 'hidden', display: '-webkit-box', WebkitLineClamp: 3,
    WebkitBoxOrient: 'vertical', textDecoration: 'none'
  },
  cardDescription: {
    textOverflow: 'ellipsis', overflow: 'hidden', display: '-webkit-box',
    WebkitLineClamp: 4, WebkitBoxOrient: 'vertical'
  },
  lastModified: {
    color: colors.primary,
    fontSize: '11px',
    display: 'inline-block',
    lineHeight: '14px',
    fontWeight: 300,
    marginBottom: '0.2rem'
  },
  resourceType: {
    height: '22px',
    width: 'max-content',
    paddingLeft: '10px',
    paddingRight: '10px',
    borderRadius: '2px',
    display: 'flex',
    justifyContent: 'center',
    color: colors.white,
    fontFamily: 'Montserrat, sans-serif',
    fontSize: '12px',
    fontWeight: 500
  },
  cardFooter: {
    display: 'flex',
    flexDirection: 'column'
  }
});

interface Action {
  icon: string;
  displayName: string;
  onClick: () => void;
  disabled: boolean;
  hoverText?: string;
}

const ResourceActionsMenu = (props: {actions: Action[]}) => {
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
    <Clickable data-test-id='resource-menu'>
      <SnowmanIcon disabled={false}/>
    </Clickable>
  </PopupTrigger>;
};

const StyledResourceType = (props: {resource: WorkspaceResource}) => {
  const {resource} = props;

  function getColor(): string {
    return fp.cond([
      [isCohort, () => colors.resourceCardHighlights.cohort],
      [isCohortReview, () => colors.resourceCardHighlights.cohortReview],
      [isConceptSet, () => colors.resourceCardHighlights.conceptSet],
      [isDataSet, () => colors.resourceCardHighlights.dataSet],
      [isNotebook, () => colors.resourceCardHighlights.notebook],
    ])(resource);
  }
  return <div data-test-id='card-type'
              style={{...styles.resourceType, backgroundColor: getColor()}}
       >{fp.startCase(fp.camelCase(getTypeString(resource)))}</div>;
};

function canWrite(resource: WorkspaceResource): boolean {
  return resource.permission === 'OWNER' || resource.permission === 'WRITER';
}

function canDelete(resource: WorkspaceResource): boolean {
  return resource.permission === 'OWNER';
}


interface Props {
  actions: Action[];
  disabled: boolean;
  resource: WorkspaceResource;
  onNavigate: () => void;
  menuOnly: boolean;  // use this component strictly for its actions, without rendering the card
}

// This will be renamed to ResourceCard once the old code is removed
class ResourceCard extends React.Component<Props, {}> {

  constructor(props: Props) {
    super(props);
  }

  static defaultProps = {
    onNavigate: () => {}
  };

  render() {
    const {resource, menuOnly} = this.props;
    return menuOnly ? <ResourceActionsMenu actions={this.props.actions}/> :
        <ResourceCardBase style={styles.card}
                          data-test-id='card'>
          <FlexColumn style={{alignItems: 'flex-start'}}>
            <FlexRow style={{alignItems: 'flex-start'}}>
              <ResourceActionsMenu actions={this.props.actions}/>
              <Clickable disabled={this.props.disabled}>
                <a style={styles.cardName}
                   data-test-id='card-name'
                   href={getResourceUrl(resource)}
                   onClick={e => {
                     this.props.onNavigate();
                     navigateAndPreventDefaultIfNoKeysPressed(e, getResourceUrl(resource));
                   }}>
                  {getDisplayName(resource)}
                </a>
              </Clickable>
            </FlexRow>
            <div style={styles.cardDescription}>{getDescription(resource)}</div>
          </FlexColumn>
          <div style={styles.cardFooter}>
            <div style={styles.lastModified} data-test-id='last-modified'>
              Last Modified: {formatWorkspaceResourceDisplayDate(resource.modifiedTime)}</div>
            <StyledResourceType resource={resource}/>
          </div>
        </ResourceCardBase>;
  }
}

export {
  Action,
  ResourceCard,
  StyledResourceType,
  canWrite,
  canDelete,
};
