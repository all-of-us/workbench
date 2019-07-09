import * as React from 'react';
import {Clickable, MenuItem} from "../components/buttons";
import {navigateAndPreventDefaultIfNoKeysPressed} from "../utils/navigation";
import * as fp from "lodash";
import {ResourceCardBase} from "../components/card";
import {reactStyles} from "../utils";
import colors from "../styles/colors";
import {ClrIcon} from "../components/icons";
import {PopupTrigger} from "../components/popups";
import {TextModal} from "../components/text-modal";
import {ConfirmDeleteModal} from "./confirm-delete-modal";

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

const defaultProps = {
  marginTop: '1rem'
};

export interface Action {
  displayName: string,
  onClick: Function
}

interface Props {
  actions: Action[];
  actionsDisabled: boolean;
  disabled: boolean;
  resourceUrl: string;
  displayName: string;
  description: string;
  displayDate: string;
  footerText: string;
  footerColor: string;
}

interface State {
  errorModal: JSX.Element;
  confirmDeleteModal: JSX.Element;
}

export class ResourceCardTemplate extends React.Component<Props, State> {

  constructor(props: Props) {
    super(props);
    this.state = {
      errorModal: null,
      confirmDeleteModal: null
    }
  }

  resourceCardFns() {
    return {
      showErrorModal: (title, body) => {
        this.setState({
          errorModal: <TextModal title={title}
                                 body={body}
                                 onConfirm={() => this.setState({errorModal: null})}/>
        });
      },
      showConfirmDeleteModal: (displayName, resourceType, receiveDelete) => {
        let closeModal = () => this.setState({confirmDeleteModal: null});

        this.setState({
          confirmDeleteModal: <ConfirmDeleteModal
            resourceName={displayName}
            resourceType={resourceType}
            receiveDelete={() => receiveDelete(closeModal)}
            closeFunction={closeModal}/>
        })
      }
    }
  }

  render() {
    return <React.Fragment>
      {this.state.errorModal}
      {this.state.confirmDeleteModal}

      <ResourceCardBase style={{...styles.card, marginTop: defaultProps.marginTop}} // TODO eric: this is a modified value
                               data-test-id='card'>
        <div style={{display: 'flex', flexDirection: 'column', alignItems: 'flex-start'}}>
          <div style={{display: 'flex', flexDirection: 'row', alignItems: 'flex-start'}}>
            <PopupTrigger
              data-test-id='resource-card-menu'
              side='bottom'
              closeOnClick
              content={
                <React.Fragment>
                  {this.props.actions.map(action => {
                    return <MenuItem onClick={() => action.onClick(this.resourceCardFns())}> {action.displayName} </MenuItem>
                  })}
                </React.Fragment>
              }
            >
              <Clickable disabled={this.props.actionsDisabled} data-test-id='resource-menu'>
                <ClrIcon shape='ellipsis-vertical' size={21}
                         style={{color: this.props.actionsDisabled ? '#9B9B9B' : '#2691D0', marginLeft: -9,
                           cursor: this.props.actionsDisabled ? 'auto' : 'pointer'}}/>
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
          </div>
          <div style={styles.cardDescription}>{this.props.description}</div>
        </div>
        <div style={styles.cardFooter}>
          <div style={styles.lastModified} data-test-id='last-modified'>
            Last Modified: {this.props.displayDate}</div>
          <div style={{...styles.resourceType, backgroundColor: this.props.footerColor}}
               data-test-id='card-type'>
            {fp.startCase(fp.camelCase(this.props.footerText))}</div>
        </div>
      </ResourceCardBase>
    </React.Fragment>
  }

};
