import {notebooksClusterApi} from 'app/services/notebooks-swagger-fetch-clients';
import {clusterApi} from 'app/services/swagger-fetch-clients';
import {isAbortError, reportError} from 'app/utils/errors';
import {Cluster, ClusterStatus} from 'generated/fetch';

export interface ClusterInitializerOptions {
  workspaceNamespace: string;
  onStatusUpdate?: (ClusterStatus) => void;
  abortSignal?: AbortSignal;
  initialPollingDelay?: number;
  maxPollingDelay?: number;
  overallTimeout?: number;
}

// We're only willing to wait 20 minutes total for a cluster to initialize. After that we return
// a rejected promise no matter what.
const DEFAULT_OVERALL_TIMEOUT = 1000 * 60 * 20;
const DEFAULT_INITIAL_POLLING_DELAY = 2000;
const DEFAULT_MAX_POLLING_DELAY = 15000;

const MAX_CREATE_COUNT = 1;
const MAX_DELETE_COUNT = 1;

export class ClusterInitializer {
  private workspaceNamespace: string;
  private onStatusUpdate: (ClusterStatus) => void;
  private abortSignal?: AbortSignal;
  private currentDelay;
  private maxDelay;
  private overallTimeout;

  private createCount = 0;
  private deleteCount = 0;
  private isInitializing = false;
  private initializeStartTime?: number;
  private currentCluster?: Cluster;

  constructor(options: ClusterInitializerOptions) {
    this.workspaceNamespace = options.workspaceNamespace;
    this.onStatusUpdate = options.onStatusUpdate ? options.onStatusUpdate : () => {};
    this.abortSignal = options.abortSignal;
    this.currentDelay = options.initialPollingDelay != null ? options.initialPollingDelay : DEFAULT_INITIAL_POLLING_DELAY;
    this.maxDelay = options.maxPollingDelay != null ? options.maxPollingDelay : DEFAULT_MAX_POLLING_DELAY;
    this.overallTimeout = options.overallTimeout != null ? options.overallTimeout : DEFAULT_OVERALL_TIMEOUT;
  }

  private async getCluster(): Promise<Cluster> {
    return await clusterApi().getCluster(this.workspaceNamespace, {signal: this.abortSignal});
  }

  private async createCluster(): Promise<Cluster> {
    if (this.createCount >= MAX_CREATE_COUNT) {
      throw new Error('Reached max cluster create count');
    }
    const cluster = await clusterApi().createCluster(this.workspaceNamespace, {signal: this.abortSignal});
    this.createCount++;
    return cluster;
  }

  private async resumeCluster(): Promise<void> {
    await notebooksClusterApi().startCluster(
      this.currentCluster.clusterNamespace, this.currentCluster.clusterName, {signal: this.abortSignal});
  }

  private async deleteCluster(): Promise<void> {
    if (this.deleteCount >= MAX_DELETE_COUNT) {
      throw new Error('Reached max cluster delete count');
    }
    await clusterApi().deleteCluster(this.workspaceNamespace, {signal: this.abortSignal});
    this.deleteCount++;
  }

  private isClusterRunning(): boolean {
    return this.currentCluster && this.currentCluster.status === ClusterStatus.Running;
  }

  private isClusterStopped(): boolean {
    return this.currentCluster && this.currentCluster.status === ClusterStatus.Stopped;
  }

  private isClusterErrored(): boolean {
    return this.currentCluster && this.currentCluster.status === ClusterStatus.Error;
  }

  private isNotFoundError(e: any): boolean {
    // Our Swagger-generated APIs throw an error of type Response on a non-success status code.
    return e instanceof Response && e.status === 404;
  }

  public async initialize(): Promise<Cluster> {
    console.log('Initializing cluster', this.workspaceNamespace);
    if (this.isInitializing) {
      return Promise.reject('Initialization is already in progress');
    }
    this.initializeStartTime = new Date().getTime();

    // Overall strategy: continue polling the get-cluster endpoint, with capped exponential backoff,
    // until we either reach our goal state (a RUNNING cluster) or run up against the overall
    // timeout threshold.
    //
    // Certain cluster states require active intervention, such as deleting or resuming the cluster;
    // these are handled within the the polling loop.
    while (!this.isClusterRunning()) {
      if (this.abortSignal && this.abortSignal.aborted) {
        console.log('abortSignal received!');
        return Promise.reject('Request was aborted.');
      }
      if (new Date().getTime() - this.initializeStartTime > this.overallTimeout) {
        console.log('Took longer than max init timeout');
        return Promise.reject('Initialization attempt took longer than the max time allowed.');
      }

      // Attempt to take the appropriate next action given the current cluster status.
      try {
        console.log('Fetching cluster status');
        this.currentCluster = await this.getCluster();
        console.log('Cluster status: ', this.currentCluster.status);
        this.onStatusUpdate(this.currentCluster.status);

        if (this.isClusterStopped()) {
          console.log('Cluster is stopped, resuming');
          await this.resumeCluster();
        } else if (this.isClusterErrored()) {
          // If cluster is in error state, delete it so it can be re-created at the next poll loop.
          reportError(
            `Cluster ${this.currentCluster.clusterNamespace}/${this.currentCluster.clusterName}` +
            ` has reached an ERROR status.`);
          console.log('Cluster is in an error state -- deleting for retry');
          await this.deleteCluster();
        } else if (this.isClusterRunning()) {
          console.log('Cluster is running, resolving promise');
          return this.currentCluster;
        }

      } catch (e) {
        if (isAbortError(e)) {
          return Promise.reject();
        } else if (this.isNotFoundError(e)) {
          // If we received a NOT_FOUND error, we need to create a cluster for this workspace.
          this.currentCluster = await this.createCluster();
        } else {
          // Report an unknown error and continue polling.
          reportError(e);
        }
      }

      console.log('Pausing for ' + this.currentDelay + ' ms before polling again');
      await new Promise(resolve => setTimeout(resolve, this.currentDelay));
      // Increment capped exponential backoff for the next poll loop.
      this.currentDelay = Math.min(this.currentDelay * 1.3, this.maxDelay);
    }
  }
}
