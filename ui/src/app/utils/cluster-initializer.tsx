import {notebooksClusterApi} from 'app/services/notebooks-swagger-fetch-clients';
import {clusterApi} from 'app/services/swagger-fetch-clients';
import {isAbortError, reportError} from 'app/utils/errors';
import {Cluster, ClusterStatus} from 'generated/fetch';

// We're only willing to wait 20 minutes total for a cluster to initialize. After that we return
// a rejected promise no matter what.
const DEFAULT_OVERALL_TIMEOUT = 1000 * 60 * 20;
const DEFAULT_INITIAL_POLLING_DELAY = 2000;
const DEFAULT_MAX_POLLING_DELAY = 15000;
// By default, we're willing to retry twice on each of the state-modifying API calls, to allow
// for transient 500s.
const DEFAULT_MAX_CREATE_COUNT = 2;
const DEFAULT_MAX_DELETE_COUNT = 2;
const DEFAULT_MAX_RESUME_COUNT = 2;
const DEFAULT_MAX_SERVER_ERROR_COUNT = 5;


export interface ClusterInitializerOptions {
  // Core options. Most callers should provide these.
  //
  // The workspace namespace to initialize a cluster for.
  workspaceNamespace: string;
  // Callback which is called every time the cluster updates its status. This callback is *not*
  // called when no cluster is found.
  onStatusUpdate?: (ClusterStatus) => void;
  // An optional abort signal which allows the caller to abort the initialization process, including
  // cancelling any outstanding Ajax requests.
  abortSignal?: AbortSignal;

  // Override options. These options all have sensible defaults, but may be overridden for testing
  // or special scenarios (such as an initialization flow which should not take any actions).
  initialPollingDelay?: number;
  maxPollingDelay?: number;
  overallTimeout?: number;
  maxCreateCount?: number;
  maxDeleteCount?: number;
  maxResumeCount?: number;
  maxServerErrorCount?: number;
}

/**
 * A controller class implementing client-side logic to initialize a Leonardo cluster. This class
 * will continue to poll the getCluster endpoint, taking certain actions as required to nudge the
 * cluster towards a running state, and will eventually resolve the Promise with a running cluster,
 * or otherwise reject it with information about the failure mode.
 *
 * This is an unusually heavyweight controller class on the client side. It's worth noting a couple
 * reasons why we ended up with this design:
 *  - Cluster initialization can take up to 10 minutes, which is beyond the scope of a single
 *    App Engine server-side request timeout. To reliably implement this control on the server side
 *    would likely require new database persistence, tasks queues, and additional APIs in order to
 *    provide the client with status updates.
 *  - Ultimately, we might expect this type of functionality ("Get me a cluster for workspace X and
 *    bring it to a running state") to exist as part of the Leonardo application. So rather than
 *    build our own server-side equivalent, we adopted a client-side solution as a holdover.
 */
export class ClusterInitializer {
  // Core properties for interacting with the caller and the cluster APIs.
  private workspaceNamespace: string;
  private onStatusUpdate: (ClusterStatus) => void;
  private abortSignal?: AbortSignal;

  // Properties to track & control the polling loop. We use a capped exponential backoff strategy
  // and a series of "maxFoo" limits to ensure the initialization flow doesn't get out of control.
  private currentDelay: number;
  private maxDelay: number;
  private overallTimeout: number;
  private maxCreateCount: number;
  private maxDeleteCount: number;
  private maxResumeCount: number;
  private maxServerErrorCount: number;

  // Properties to track progress, actions taken, and errors encountered.
  private createCount = 0;
  private deleteCount = 0;
  private resumeCount = 0;
  private serverErrorCount = 0;
  private initializeStartTime?: number;
  // The latest cluster retrieved from getCluster. If the last getCluster call returned a NOT_FOUND
  // response, this will be null.
  private currentCluster?: Cluster;

  // Properties to control the initialization and promise resolution flow.
  //
  // Tracks whether the .run() method has been called yet on this class. Each instance is only
  // allowed to be used for one initialization flow.
  private isInitializing = false;
  // The resolve and reject function from the promise returned from the call to .run(). We use a
  // deferred-style approach in this class, which allows us to provide a Promise-based API on the
  // .run() method, but to call the resolve() or reject() method from anywhere in this class.
  private resolve: (cluster?: Cluster | PromiseLike<Cluster>) => void;
  private reject: (error: Error) => void;

  constructor(options: ClusterInitializerOptions) {
    this.workspaceNamespace = options.workspaceNamespace;
    this.onStatusUpdate = options.onStatusUpdate ? options.onStatusUpdate : () => {};
    this.abortSignal = options.abortSignal;
    this.currentDelay = options.initialPollingDelay != null ? options.initialPollingDelay : DEFAULT_INITIAL_POLLING_DELAY;
    this.maxDelay = options.maxPollingDelay != null ? options.maxPollingDelay : DEFAULT_MAX_POLLING_DELAY;
    this.overallTimeout = options.overallTimeout != null ? options.overallTimeout : DEFAULT_OVERALL_TIMEOUT;
    this.maxCreateCount = options.maxCreateCount != null ? options.maxCreateCount : DEFAULT_MAX_CREATE_COUNT;
    this.maxDeleteCount = options.maxDeleteCount != null ? options.maxDeleteCount : DEFAULT_MAX_DELETE_COUNT;
    this.maxResumeCount = options.maxResumeCount != null ? options.maxResumeCount : DEFAULT_MAX_RESUME_COUNT;
    this.maxServerErrorCount = options.maxServerErrorCount != null ? options.maxServerErrorCount : DEFAULT_MAX_SERVER_ERROR_COUNT;
  }

  private async getCluster(): Promise<Cluster> {
    return await clusterApi().getCluster(this.workspaceNamespace, {signal: this.abortSignal});
  }

  private async createCluster(): Promise<Cluster> {
    if (this.createCount >= this.maxCreateCount) {
      throw new ExceededActionCountError('Reached max cluster create count', this.currentCluster);
    }
    const cluster = await clusterApi().createCluster(this.workspaceNamespace, {signal: this.abortSignal});
    this.createCount++;
    return cluster;
  }

  private async resumeCluster(): Promise<void> {
    if (this.resumeCount >= this.maxResumeCount) {
      throw new ExceededActionCountError('Reached max cluster resume count', this.currentCluster);
    }
    await notebooksClusterApi().startCluster(
      this.currentCluster.clusterNamespace, this.currentCluster.clusterName, {signal: this.abortSignal});
    this.resumeCount++;
  }

  private async deleteCluster(): Promise<void> {
    if (this.deleteCount >= this.maxDeleteCount) {
      throw new ExceededActionCountError('Reached max cluster delete count', this.currentCluster);
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

  private handleUnknownError(e: any) {
    if (e instanceof Response && e.status >= 500 && e.status < 600) {
      this.serverErrorCount++;
    }
    reportError(e);
  }

  private tooManyServerErrors(): boolean {
    return this.serverErrorCount > this.maxServerErrorCount;
  }

  /**
   * Runs the cluster intiailizer flow.
   *
   * The strategy here is to poll the getCluster endpoint for cluster status, waiting for the
   * cluster to reach the ready state (ClusterStatus.Running) or an error state which can be
   * recovered from. Action will be taken where possible: a stopped cluster will trigger a call to
   * startCluster, a nonexistent cluster will trigger a call to createCluster, and an errored
   * cluster will trigger a call to deleteCluster in an attempt to retry cluster creation.
   *
   * @return A Promise which resolves with a Cluster or rejects with a
   * ClusterInitializationFailedError, which holds a message and the current Cluster object (if one
   * existed at the time of failure).
   */
  public async run(): Promise<Cluster> {
    if (this.isInitializing) {
      throw new Error('Initialization is already in progress');
    }
    this.initializeStartTime = Date.now();

    return new Promise((resolve, reject) => {
      this.resolve = resolve;
      this.reject = reject as (error: Error) => {};
      this.poll();
    }) as Promise<Cluster>;
  }

  private async poll() {
    // Overall strategy: continue polling the get-cluster endpoint, with capped exponential backoff,
    // until we either reach our goal state (a RUNNING cluster) or run up against the overall
    // timeout threshold.
    //
    // Certain cluster states require active intervention, such as deleting or resuming the cluster;
    // these are handled within the the polling loop.
    if (this.abortSignal && this.abortSignal.aborted) {
      // We'll bail out early if an abort signal was triggered while waiting for the poll cycle.
      return this.reject(
        new ClusterInitializationFailedError('Request was aborted.', this.currentCluster));
    }
    if (Date.now() - this.initializeStartTime > this.overallTimeout) {
      return this.reject(
        new ClusterInitializationFailedError(
          'Initialization attempt took longer than the max time allowed.',
          this.currentCluster));
    }

    // Fetch the current cluster status, with some graceful error handling for NOT_FOUND response
    // and abort signals.
    try {
      this.currentCluster = await this.getCluster();
      this.onStatusUpdate(this.currentCluster.status);
    } catch (e) {
      if (isAbortError(e)) {
        return this.reject(
          new ClusterInitializationFailedError('Abort signal received during cluster API call',
            this.currentCluster));
      } else if (this.isNotFoundError(e)) {
        // A not-found error is somewhat expected, if a cluster has recently been deleted or
        // hasn't been created yet.
        this.currentCluster = null;
      } else {
        this.handleUnknownError(e);
        if (this.tooManyServerErrors()) {
          return this.reject(
            new ExceededErrorCountError('Reached max server error count', this.currentCluster));
        }
      }
    }

    // Attempt to take the appropriate next action given the current cluster status.
    try {
      if (this.currentCluster === null) {
        await this.createCluster();
      } else if (this.isClusterStopped()) {
        await this.resumeCluster();
      } else if (this.isClusterErrored()) {
        // If cluster is in error state, delete it so it can be re-created at the next poll loop.
        reportError(
          `Cluster ${this.currentCluster.clusterNamespace}/${this.currentCluster.clusterName}` +
          ` has reached an ERROR status.`);
        await this.deleteCluster();
      } else if (this.isClusterRunning()) {
        // We've reached the goal - resolve the Promise.
        return this.resolve(this.currentCluster);
      }
    } catch (e) {
      if (isAbortError(e)) {
        return this.reject(
          new ClusterInitializationFailedError('Abort signal received during cluster API call',
          this.currentCluster));
      } else if (e instanceof ExceededActionCountError) {
        // This is a signal that we should hard-abort the polling loop due to reaching the max
        // number of delete or create actions allowed.
        return this.reject(e);
      } else {
        this.handleUnknownError(e);
        if (this.tooManyServerErrors()) {
          return this.reject(
            new ExceededErrorCountError('Reached max server error count', this.currentCluster));
        }
      }
    }

    setTimeout(() => this.poll(), this.currentDelay);
    // Increment capped exponential backoff for the next poll loop.
    this.currentDelay = Math.min(this.currentDelay * 1.3, this.maxDelay);
  }
}

export class ClusterInitializationFailedError extends Error {
  public readonly cluster: Cluster;

  constructor(message: string, cluster?: Cluster) {
    super(message);
    Object.setPrototypeOf(this, ClusterInitializationFailedError.prototype);

    this.name = 'ClusterInitializationFailedError';
    this.cluster = cluster;
  }
}

export class ExceededActionCountError extends ClusterInitializationFailedError {
  constructor(message, cluster?: Cluster) {
    super(message, cluster);
    Object.setPrototypeOf(this, ExceededActionCountError.prototype);

    this.name = 'ExceededActionCountError';
  }
}

export class ExceededErrorCountError extends ClusterInitializationFailedError {
  constructor(message, cluster?: Cluster) {
    super(message, cluster);
    Object.setPrototypeOf(this, ExceededErrorCountError.prototype);

    this.name = 'ExceededErrorCountError';
  }
}
