import {clusterApi} from 'app/services/swagger-fetch-clients';
import {isAbortError} from 'app/utils/errors';
import {Cluster, ClusterStatus} from 'generated/fetch';
import {notebooksClusterApi} from 'app/services/notebooks-swagger-fetch-clients';

interface ClusterInitializerOptions {
  workspaceNamespace: string;
  onStatusUpdate?: (ClusterStatus) => void;
  abortSignal?: AbortSignal;
}

// We're only willing to wait 20 minutes total for a cluster to initialize. After that we return
// a rejected promise no matter what.
const INITIALIZE_TIMEOUT_MS = 1000 * 60 * 20;
// While polling the get endpoint,
const INITIAL_POLLING_DELAY_MS = 2000;
const MAX_POLLING_DELAY_MS = 15000;
const MAX_CREATE_COUNT = 1;
const MAX_DELETE_COUNT = 1;

export class ClusterInitializer {
  private workspaceNamespace: string;
  private onStatusUpdate: (ClusterStatus) => void;
  private abortSignal?: AbortSignal;
  private currentDelayMs = INITIAL_POLLING_DELAY_MS;
  private createCount = 0;
  private deleteCount = 0;
  private isInitializing = false;
  private initializeStartTime?: number;
  private currentCluster?: Cluster;

  constructor(options: ClusterInitializerOptions) {
    this.workspaceNamespace = options.workspaceNamespace;
    this.onStatusUpdate = options.onStatusUpdate ? options.onStatusUpdate : () => {};
    this.abortSignal = options.abortSignal;
  }

  private async getCluster(): Promise<Cluster> {
    const getClusterResponse = await clusterApi().getCluster(this.workspaceNamespace, {signal: this.abortSignal});
    return getClusterResponse.cluster;
  }

  private async createCluster(): Promise<Cluster> {
    if (this.createCount >= MAX_CREATE_COUNT) {
      throw new Error('Reached max cluster create count');
    }
    const createClusterResponse = await clusterApi().createCluster(this.workspaceNamespace, {signal: this.abortSignal});
    this.createCount++;
    return createClusterResponse.cluster;
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

  async initialize(): Promise<Cluster> {
    console.log('Initializing cluster', this.workspaceNamespace);
    if (this.isInitializing) {
      return Promise.reject('Initialization is already in progress');
    }
    this.initializeStartTime = new Date().getTime();

    // Overall strategy: continue polling the get-cluster endpoint, with capped exponential backoff,
    // until we either reach our goal state (a RUNNING cluster) or run up against the overall
    // timeout threshold.
    while (!this.isClusterRunning()) {
      if (this.abortSignal.aborted) {
        console.log('abortSignal received!');
        return Promise.reject('Request was aborted.');
      }
      if (new Date().getTime() - this.initializeStartTime > INITIALIZE_TIMEOUT_MS) {
        console.log('Took longer than max init timeout');
        return Promise.reject('Initialization attempt took longer than the max time allowed.');
      }

      // Fetch the cluster and update status.
      try {
        console.log('Getting cluster');
        this.currentCluster = await this.getCluster();
        console.log('Cluster status', this.currentCluster.status);
        this.onStatusUpdate(this.currentCluster.status);
      } catch (e) {
        console.log('getCluster error', e);
        if (isAbortError(e)) {
          return Promise.reject();
        }
        // If we received a NOT_FOUND error, we need to create a cluster for this workspace.
        this.currentCluster = await this.createCluster();
      }

      // Resolve the promise if this cluster is all set.
      if (this.isClusterRunning()) {
        console.log('Cluster is running, resolving promise');
        return this.currentCluster;
      }

      if (this.isClusterStopped()) {
        console.log('Cluster is stopped, resuming');
        await this.resumeCluster();
      }

      // If cluster is in error state, delete it so it can be re-created at the next poll loop.
      if (this.isClusterErrored()) {
        // TODO: report errors to Stackdriver here.
        console.log('Cluster error -- deleting for retry');
        await this.deleteCluster();
      }

      console.log('Pausing for ' + this.currentDelayMs + ' ms before polling again');
      await new Promise(resolve => setTimeout(resolve, this.currentDelayMs));
      // Increment capped exponential backoff for the next poll loop.
      this.currentDelayMs = Math.min(this.currentDelayMs * 1.5, MAX_POLLING_DELAY_MS);
    }
  }
}
