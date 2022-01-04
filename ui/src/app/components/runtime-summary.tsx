import { reactStyles } from 'app/utils';
import {
  ComputeType,
  findMachineByName,
} from 'app/utils/machines';
import { RuntimeConfig } from 'app/utils/runtime-utils';


const styles = reactStyles({
  bold: {
    fontWeight: 700,
  }
});

export const RuntimeSummary = ({runtimeConfig}: {runtimeConfig: RuntimeConfig}) => {
  return <>
    <label htmlFor='compute-resources' style={{...styles.bold, marginTop: '1rem'}}>Compute Resources</label>
    <div id='compute-resources'>- Compute size of
      <b> {runtimeConfig.machine.cpu} CPUs</b>,
      <b> {runtimeConfig.machine.memory} GB memory</b>, and a
      <b> {runtimeConfig.diskSize} GB disk</b>
    </div>
    {runtimeConfig.computeType === ComputeType.Dataproc && <>
      <label htmlFor='worker-configuration' style={{...styles.bold, marginTop: '1rem'}}>Worker Configuration</label>
      <div id='worker-configuration'>-
        <b> {runtimeConfig.dataprocConfig.numberOfWorkers} worker(s) </b>
        {
          runtimeConfig.dataprocConfig.numberOfPreemptibleWorkers > 0 &&
            <b>and {runtimeConfig.dataprocConfig.numberOfPreemptibleWorkers} preemptible worker(s) </b>
        }
        each with compute size of <b>{findMachineByName(runtimeConfig.dataprocConfig.workerMachineType).cpu} CPUs</b>,
        <b> {findMachineByName(runtimeConfig.dataprocConfig.workerMachineType).memory} GB memory</b>, and a
        <b> {runtimeConfig.dataprocConfig.workerDiskSize} GB disk</b>
      </div>
    </>}
  </>;
};
