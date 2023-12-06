import { reactStyles } from 'app/utils';
import { AnalysisConfig } from 'app/utils/analysis-config';
import { ComputeType, findMachineByName } from 'app/utils/machines';

const styles = reactStyles({
  bold: {
    fontWeight: 700,
  },
});

export const RuntimeSummary = ({
  analysisConfig,
}: {
  analysisConfig: AnalysisConfig;
}) => {
  return (
    <>
      <label
        htmlFor='compute-resources'
        style={{ ...styles.bold, marginTop: '1.5rem' }}
      >
        Compute Resources
      </label>
      <div id='compute-resources'>
        - Compute size of
        <b> {analysisConfig.machine.cpu} CPUs</b>,
        <b> {analysisConfig.machine.memory} GB memory</b>, and a
        <b> {analysisConfig.diskConfig.size} GB disk</b>
      </div>
      {analysisConfig.computeType === ComputeType.Dataproc && (
        <>
          <label
            htmlFor='worker-configuration'
            style={{ ...styles.bold, marginTop: '1.5rem' }}
          >
            Worker Configuration
          </label>
          <div id='worker-configuration'>
            -<b> {analysisConfig.dataprocConfig.numberOfWorkers} worker(s) </b>
            {analysisConfig.dataprocConfig.numberOfPreemptibleWorkers > 0 && (
              <b>
                and {analysisConfig.dataprocConfig.numberOfPreemptibleWorkers}{' '}
                preemptible worker(s){' '}
              </b>
            )}
            each with compute size of{' '}
            <b>
              {
                findMachineByName(
                  analysisConfig.dataprocConfig.workerMachineType
                ).cpu
              }{' '}
              CPUs
            </b>
            ,
            <b>
              {' '}
              {
                findMachineByName(
                  analysisConfig.dataprocConfig.workerMachineType
                ).memory
              }{' '}
              GB memory
            </b>
            , and a
            <b> {analysisConfig.dataprocConfig.workerDiskSize} GB disk</b>
          </div>
        </>
      )}
    </>
  );
};
