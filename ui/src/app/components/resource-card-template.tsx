import {Clickable, MenuItem} from 'app/components/buttons';
import {ResourceCardBase} from 'app/components/card';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {SnowmanIcon} from 'app/components/icons';
import {PopupTrigger} from 'app/components/popups';
import colors from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import {navigateAndPreventDefaultIfNoKeysPressed} from 'app/utils/navigation';
import * as fp from 'lodash';
import * as React from 'react';

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
    borderRadius: '4px 4px 0 0',
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

export interface Action {
  icon: string;
  displayName: string;
  onClick: () => void;
  disabled: boolean;
}

interface Props {
  actions: Action[];
  disabled: boolean;
  resourceUrl: string;
  displayName: string;
  description: string;
  displayDate: string;
  footerText: string;
  footerColor: string;
}

// This will be renamed to ResourceCard once the old code is removed
export class ResourceCardTemplate extends React.Component<Props, {}> {

  constructor(props: Props) {
    super(props);
  }

  render() {
    return <React.Fragment>


      <ResourceCardBase style={styles.card}
                        data-test-id='card'>
        <FlexColumn style={{alignItems: 'flex-start'}}>
          <FlexRow style={{alignItems: 'flex-start'}}>
            <PopupTrigger
              data-test-id='resource-card-menu'
              side='bottom'
              closeOnClick
              content={
                <React.Fragment>
                  {this.props.actions.map((action, i) => {
                    return (<MenuItem
                      key={i}
                      icon={action.icon}
                      onClick={() => action.onClick()}
                      disabled={action.disabled}>
                      {action.displayName}
                    </MenuItem>);
                  })}
                </React.Fragment>
              }
            >
              <Clickable data-test-id='resource-menu'>
                <SnowmanIcon disabled={false}/>
              </Clickable>
            </PopupTrigger>

            <Clickable disabled={this.props.disabled}>
              <a style={styles.cardName}
                 data-test-id='card-name'
                 href={this.props.resourceUrl}
                 onClick={e => {
                   navigateAndPreventDefaultIfNoKeysPressed(e, this.props.resourceUrl);
                 }}> {this.props.displayName}
              </a>
            </Clickable>
          </FlexRow>
          <div style={styles.cardDescription}>{this.props.description}</div>
        </FlexColumn>
        <div style={styles.cardFooter}>
          <div style={styles.lastModified} data-test-id='last-modified'>
            Last Modified: {this.props.displayDate}</div>
          <div style={{...styles.resourceType, backgroundColor: this.props.footerColor}}
               data-test-id='card-type'>
            {fp.startCase(fp.camelCase(this.props.footerText))}</div>
        </div>
      </ResourceCardBase>
    </React.Fragment>;
  }

}
