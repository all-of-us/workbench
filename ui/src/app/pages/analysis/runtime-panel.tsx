import {Button, Clickable, MenuItem} from 'app/components/buttons';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {ClrIcon} from 'app/components/icons';
import {PopupTrigger} from 'app/components/popups';
import {Spinner} from 'app/components/spinners';
import {runtimeApi} from 'app/services/swagger-fetch-clients';
import colors, {addOpacity} from 'app/styles/colors';
import {reactStyles, withCurrentWorkspace} from 'app/utils';
import {allMachineTypes, validLeonardoMachineTypes} from 'app/utils/machines';
import {WorkspaceData} from 'app/utils/workspace-data';
import {Dropdown} from 'primereact/dropdown';
import {InputNumber} from 'primereact/inputnumber';

import {Runtime} from 'generated/fetch';

import * as fp from 'lodash/fp';
import * as React from 'react';


const styles = reactStyles({
  sectionHeader: {
    color: colors.primary,
    fontSize: '16px',
    fontWeight: 700,
    lineHeight: '1rem',
    marginBottom: '12px',
    marginTop: '12px'
  },
  controlSection: {
    backgroundColor: String(addOpacity(colors.white, .75)),
    borderRadius: '3px',
    padding: '.75rem',
    marginTop: '.75rem'
  },
  presetMenuItem: {
    color: colors.primary,
    fontSize: '14px'
  }
});

const defaultMachineType = allMachineTypes.find(({name}) => name === 'n1-standard-4');

export interface Props {
  workspace: WorkspaceData;
}

interface State {
  // Whether the initial runtime load is still in progress.
  loading: boolean;
  // Whether there was an error in loading the runtime data.
  error: boolean;
  // The runtime. null if none exists, or if there was an error in loading the
  // runtime.
  runtime: Runtime|null;
}

export const RuntimePanel = withCurrentWorkspace()(
  class extends React.Component<Props, State> {
    private aborter = new AbortController();

    constructor(props: Props) {
      super(props);
      this.state = {
        loading: true,
        error: false,
        runtime: null
      };
    }

    async componentDidMount() {
      // TODO(RW-5420): Centralize a runtimeStore.
      let runtime = null;
      let error = false;
      try {
        runtime = await runtimeApi().getRuntime(this.props.workspace.namespace, {signal: this.aborter.signal});
      } catch (e) {
        // 404 is expected if the runtime doesn't exist, represent this as a null
        // runtime rather than an error mode.
        if (e.status !== 404) {
          error = true;
        }
      }
      this.setState({
        runtime,
        error,
        loading: false
      });
    }

    render() {
      const {loading, error, runtime} = this.state;
      if (loading) {
        return <Spinner style={{width: '100%', marginTop: '5rem'}}/>;
      } else if (error) {
        return <div>Error loading compute configuration</div>;
      } else if (!runtime) {
        // TODO(RW-5591): Create runtime page goes here.
        return <div>No runtime exists yet</div>;
      }

      const isDataproc = !!runtime.dataprocConfig;

      let masterMachineName;
      let masterDiskSize;
      if (isDataproc) {
        masterMachineName = runtime.dataprocConfig.masterMachineType;
        masterDiskSize = runtime.dataprocConfig.masterDiskSize;
      } else {
        masterMachineName = runtime.gceConfig.machineType;
        masterDiskSize = runtime.gceConfig.bootDiskSize;
      }
      const machineType = allMachineTypes.find(({name}) => name === masterMachineName) || defaultMachineType;

      return <div data-test-id='runtime-panel'>
        <h3 style={styles.sectionHeader}>Cloud analysis environment</h3>
        <div>
          Your analysis environment consists of an application and compute resources.
          Your cloud environment is unique to this workspace and not shared with other users.
        </div>
        {/* TODO(RW-5419): Cost estimates go here. */}
        <div style={styles.controlSection}>
          {/* Recommended runtime: pick from default templates or change the image. */}
          <PopupTrigger side='bottom'
                        closeOnClick
                        content={
                          <React.Fragment>
                            <MenuItem style={styles.presetMenuItem}>General purpose analysis</MenuItem>
                            <MenuItem style={styles.presetMenuItem}>Genomics analysis</MenuItem>
                          </React.Fragment>
                        }>
            <Clickable data-test-id='runtime-presets-menu'
                       disabled={true}>
              Recommended environments <ClrIcon shape='caret down'/>
            </Clickable>
          </PopupTrigger>
          <h3 style={styles.sectionHeader}>Application configuration</h3>
          {/* TODO(RW-5413): Populate the image list with server driven options. */}
          <Dropdown style={{width: '100%'}}
                    data-test-id='runtime-image-dropdown'
                    disabled={true}
                    options={[runtime.toolDockerImage]}
                    value={runtime.toolDockerImage}/>
          {/* Runtime customization: change detailed machine configuration options. */}
          <h3 style={styles.sectionHeader}>Cloud compute profile</h3>
          <FlexRow style={{justifyContent: 'space-between'}}>
            <div>
              <label htmlFor='runtime-cpu'
                     style={{marginRight: '.25rem'}}>CPUs</label>
              <Dropdown id='runtime-cpu'
                        disabled={true}
                        options={fp.flow(
                          // Show all CPU options.
                          fp.map('cpu'),
                          // In the event that was remove a machine type from our set of valid
                          // configs, we want to continue to allow rendering of the value here.
                          // Union also makes the CPU values unique.
                          fp.union([machineType.cpu]),
                          fp.sortBy(fp.identity)
                        )(validLeonardoMachineTypes)}
                        value={machineType.cpu}/>
            </div>
            <div>
              <label htmlFor='runtime-ram'
                     style={{marginRight: '.25rem'}}>RAM (GB)</label>
              <Dropdown id='runtime-ram'
                        disabled={true}
                        options={fp.flow(
                          // Show valid memory options as constrained by the currently selected CPU.
                          fp.filter(({cpu}) => cpu === machineType.cpu),
                          fp.map('memory'),
                          // See above comment on CPU union.
                          fp.union([machineType.memory]),
                          fp.sortBy(fp.identity)
                        )(validLeonardoMachineTypes)}
                        value={machineType.memory}/>
            </div>
            <div>
              <label htmlFor='runtime-disk'
                     style={{marginRight: '.25rem'}}>Disk (GB)</label>
              <InputNumber id='runtime-disk'
                           disabled={true}
                           showButtons
                           decrementButtonClassName='p-button-secondary'
                           incrementButtonClassName='p-button-secondary'
                           value={masterDiskSize}
                           inputStyle={{padding: '.75rem .5rem', width: '2rem'}}
                           min={50 /* Runtime API has a minimum 50GB requirement. */}/>
            </div>
          </FlexRow>
          <FlexColumn style={{marginTop: '1rem'}}>
            <label htmlFor='runtime-compute'>Compute type</label>
            <Dropdown id='runtime-compute'
                      style={{width: '10rem'}}
                      disabled={true}
                      options={['Dataproc cluster', 'Standard VM']}
                      value={isDataproc ? 'Dataproc cluster' : 'Standard VM'}/>
          </FlexColumn>
        </div>
        <FlexRow style={{justifyContent: 'flex-end', marginTop: '.75rem'}}>
          <Button disabled={true}>Create</Button>
        </FlexRow>
      </div>;
    }

    componentWillUnmount() {
      this.aborter.abort();
    }
  });
