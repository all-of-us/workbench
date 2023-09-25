import * as React from 'react';

import { RouteLink } from 'app/components/app-router';
import { styles } from 'app/components/common-env-conf-panels/styles';
import { FlexColumn } from 'app/components/flex';
import { SparkConsolePath } from 'app/utils/runtime-utils';
import { WorkspaceData } from 'app/utils/workspace-data';

const sparkLinkConfigs: {
  name: string;
  description: string;
  path: SparkConsolePath;
}[] = [
  {
    name: 'YARN',
    description:
      'YARN Resource Manager provides information about cluster status ' +
      'and metrics as well as information about the scheduler, nodes, and ' +
      'applications on the cluster.',
    path: SparkConsolePath.Yarn,
  },
  {
    name: 'YARN Application Timeline',
    description:
      'YARN Application Timeline provides information about current and ' +
      'historic applications executed on the cluster.',
    path: SparkConsolePath.YarnTimeline,
  },
  {
    name: 'Spark History Server',
    description:
      'Spark History Server provides information about completed Spark applications on the cluster.',
    path: SparkConsolePath.SparkHistory,
  },
  {
    name: 'MapReduce History Server',
    description:
      'MapReduce History Server displays information about completed MapReduce applications on a cluster.',
    path: SparkConsolePath.JobHistory,
  },
];

export const SparkConsolePanel = ({ namespace, id }: WorkspaceData) => {
  return (
    <FlexColumn style={{ gap: '24px', paddingBottom: '10px' }}>
      <h3 style={{ ...styles.baseHeader, ...styles.bold }}>Spark Console</h3>
      The spark console is used to manage and monitor cluster resources and
      facilities, such as the YARN resource manager, the Hadoop Distributed File
      System (HDFS), MapReduce, and Spark.
      {sparkLinkConfigs.map(({ name, description, path }) => (
        <FlexColumn key={name} style={styles.sparkConsoleSection}>
          <h4 style={styles.sparkConsoleHeader}>{name}</h4>
          <div>{description}</div>
          <div>
            <RouteLink
              path={`/workspaces/${namespace}/${id}/spark/${path}`}
              style={styles.sparkConsoleLaunchButton}
            >
              Launch
            </RouteLink>
          </div>
        </FlexColumn>
      ))}
    </FlexColumn>
  );
};
