import {leoRuntimesApi} from 'app/services/notebooks-swagger-fetch-clients';
import {runtimeApi} from 'app/services/swagger-fetch-clients';
import {isAbortError, reportError} from 'app/utils/errors';
import {applyPresetOverride, runtimePresets} from 'app/utils/runtime-presets';
import {runtimeStore} from 'app/utils/stores';
import {Runtime, RuntimeStatus} from 'generated/fetch';
import {stackdriverReporterStore} from '../services/error-reporter-context';

// We're only willing to wait 20 minutes total for a runtime to initialize. After that we return
// a rejected promise no matter what.
const DEFAULT_OVERALL_TIMEOUT = 1000 * 60 * 20;
const DEFAULT_INITIAL_POLLING_DELAY = 2000;
const DEFAULT_MAX_POLLING_DELAY = 15000;

// By default, we're willing to retry twice on each of the state-modifying API calls, to allow
// for some resilience to errored-out runtimes, while avoiding situations where we end up in an
// endless create-error-delete loop.
const DEFAULT_MAX_CREATE_COUNT = 2;
const DEFAULT_MAX_DELETE_COUNT = 2;
const DEFAULT_MAX_RESUME_COUNT = 2;
// We allow a certain # of server errors to occur before we error-out of the initialization flow.
const DEFAULT_MAX_SERVER_ERROR_COUNT = 10;

export class LeoRuntimeInitializationFailedError extends Error {
  constructor(message: string, public readonly runtime?: Runtime) {
    super(message);
    // Unfortunately, error subclassing is broken in TypeScript without this workaround. See
    // https://github.com/Microsoft/TypeScript/wiki/Breaking-Changes#extending-built-ins-like-error-array-and-map-may-no-longer-work
    // for details.
    Object.setPrototypeOf(this, LeoRuntimeInitializationFailedError.prototype);

    this.name = 'LeoRuntimeInitializationFailedError';
  }
}

export class ExceededActionCountError extends LeoRuntimeInitializationFailedError {
  constructor(message, runtime?: Runtime) {
    super(message, runtime);
    Object.setPrototypeOf(this, ExceededActionCountError.prototype);

    this.name = 'ExceededActionCountError';
  }
}

export class ExceededErrorCountError extends LeoRuntimeInitializationFailedError {
  constructor(message, runtime?: Runtime) {
    super(message, runtime);
    Object.setPrototypeOf(this, ExceededErrorCountError.prototype);

    this.name = 'ExceededErrorCountError';
  }
}

export class LeoRuntimeInitializationAbortedError extends LeoRuntimeInitializationFailedError {
  constructor(message, runtime?: Runtime) {
    super(message, runtime);
    Object.setPrototypeOf(this, LeoRuntimeInitializationAbortedError.prototype);

    this.name = 'LeoRuntimeInitializationAbortedError';
  }
}

export interface LeoRuntimeInitializerOptions {
  // Core options. Most callers should provide these.
  //
  // The workspace namespace to initialize a runtime for.
  workspaceNamespace: string;
  // Callback which is called every time the runtime updates its status. When no runtime is found,
  // the callback is called with a null value.
  onPoll?: (Runtime?) => void;
  // An optional abort signal which allows the caller to abort the initialization process, including
  // cancelling any outstanding Ajax requests.
  pollAbortSignal?: AbortSignal;

  // Override options. These options all have sensible defaults, but may be overridden for testing
  // or special scenarios (such as an initialization flow which should not take any actions).
  initialPollingDelay?: number;
  maxPollingDelay?: number;
  overallTimeout?: number;
  maxCreateCount?: number;
  maxDeleteCount?: number;
  maxResumeCount?: number;
  maxServerErrorCount?: number;
  targetRuntime?: Runtime;
  resolutionCondition?: (Runtime) => boolean;
}

const DEFAULT_OPTIONS: Partial<LeoRuntimeInitializerOptions> = {
  onPoll: () => {},
  initialPollingDelay: DEFAULT_INITIAL_POLLING_DELAY,
  maxPollingDelay: DEFAULT_MAX_POLLING_DELAY,
  overallTimeout: DEFAULT_OVERALL_TIMEOUT,
  maxCreateCount: DEFAULT_MAX_CREATE_COUNT,
  maxDeleteCount: DEFAULT_MAX_DELETE_COUNT,
  maxResumeCount: DEFAULT_MAX_RESUME_COUNT,
  maxServerErrorCount: DEFAULT_MAX_SERVER_ERROR_COUNT,
  resolutionCondition: (runtime) => runtime.status === RuntimeStatus.Running
};


/**
 * A controller class implementing client-side logic to initialize a Leonardo runtime. This class
 * will continue to poll the getRuntime endpoint, taking certain actions as required to nudge the
 * runtime towards a running state, and will eventually resolve the Promise with a running runtime,
 * or otherwise reject it with information about the failure mode.
 *
 * This is an unusually heavyweight controller class on the client side. It's worth noting a couple
 * reasons why we ended up with this design:
 *  - Runtime initialization can take up to 10 minutes, which is beyond the scope of a single
 *    App Engine server-side request timeout. To reliably implement this control on the server side
 *    would likely require new database persistence, tasks queues, and additional APIs in order to
 *    provide the client with status updates.
 *  - Ultimately, we might expect this type of functionality ("Get me a runtime for workspace X and
 *    bring it to a running state") to exist as part of the Leonardo application. So rather than
 *    build our own server-side equivalent, we adopted a client-side solution as a holdover.
 */
export class LeoRuntimeInitializer {
  // Core properties for interacting with the caller and the runtime APIs.
  private readonly workspaceNamespace: string;
  private readonly onPoll: (Runtime?) => void;
  private readonly pollAbortSignal?: AbortSignal;
  private readonly resolutionCondition: (Runtime) => boolean;

  // Properties to track & control the polling loop. We use a capped exponential backoff strategy
  // and a series of "maxFoo" limits to ensure the initialization flow doesn't get out of control.
  private readonly maxDelay: number;
  private readonly overallTimeout: number;
  private readonly maxCreateCount: number;
  private readonly maxDeleteCount: number;
  private readonly maxResumeCount: number;
  private readonly maxServerErrorCount: number;

  // Properties to track progress, actions taken, and errors encountered.
  private currentDelay: number;
  private createCount = 0;
  private deleteCount = 0;
  private resumeCount = 0;
  private serverErrorCount = 0;
  private initializeStartTime?: number;
  private targetRuntime?: Runtime;

  // The latest runtime retrieved from getRuntime. If the last getRuntime call returned a NOT_FOUND
  // response, this will be null.
  private currentRuntimeValue?: Runtime;

  private get currentRuntime(): Runtime | null {
    return this.currentRuntimeValue;
  }

  private set currentRuntime(nextRuntime: Runtime | null) {
    this.currentRuntimeValue = nextRuntime;
    const storeWorkspaceNamespace = runtimeStore.get().workspaceNamespace;
    if (storeWorkspaceNamespace === this.workspaceNamespace || storeWorkspaceNamespace === undefined ) {
      runtimeStore.set({workspaceNamespace: this.workspaceNamespace, runtime: this.currentRuntimeValue});
    }
  }

  // Properties to control the initialization and promise resolution flow.
  //
  // The resolve and reject function from the promise returned from the call to .run(). We use a
  // deferred-style approach in this class, which allows us to provide a Promise-based API on the
  // .run() method, but to call the resolve() or reject() method from anywhere in this class.
  private resolve: (runtime?: Runtime | PromiseLike<Runtime>) => void;
  private reject: (error: Error) => void;

  /**
   * Creates and runs a runtime initializer. This is the main public entry point to this class.
   * @param options
   */
  public static initialize(options: LeoRuntimeInitializerOptions): Promise<Runtime> {
    return new LeoRuntimeInitializer(options).run();
  }

  private constructor(options: LeoRuntimeInitializerOptions) {
    // Assign default values to certain options, which will be overridden by the input options
    // if present.
    options = {...DEFAULT_OPTIONS, ...options};

    this.workspaceNamespace = options.workspaceNamespace;
    this.onPoll = options.onPoll ? options.onPoll : () => {};
    this.pollAbortSignal = options.pollAbortSignal;
    this.currentDelay = options.initialPollingDelay;
    this.maxDelay = options.maxPollingDelay;
    this.overallTimeout = options.overallTimeout;
    this.maxCreateCount = options.maxCreateCount;
    this.maxDeleteCount = options.maxDeleteCount;
    this.maxResumeCount = options.maxResumeCount;
    this.maxServerErrorCount = options.maxServerErrorCount;
    this.targetRuntime = options.targetRuntime;
    this.resolutionCondition = options.resolutionCondition;
  }

  private async createRuntime(): Promise<void> {
    if (this.createCount >= this.maxCreateCount) {
      throw new ExceededActionCountError(
        `Reached max runtime create count (${this.maxCreateCount})`, this.currentRuntime);
    }

    let runtime: Runtime;
    if (this.targetRuntime) {
      runtime = this.targetRuntime;
    } else if (this.currentRuntime) {
      runtime = applyPresetOverride(this.currentRuntime);
    } else {
      runtime = {...runtimePresets.generalAnalysis.runtimeTemplate};
    }

    await runtimeApi().createRuntime(this.workspaceNamespace,
      runtime,
      {signal: this.pollAbortSignal});
    this.createCount++;
  }

  private async resumeRuntime(): Promise<void> {
    if (this.resumeCount >= this.maxResumeCount) {
      throw new ExceededActionCountError(
        `Reached max runtime resume count (${this.maxResumeCount})`, this.currentRuntime);
    }
    await leoRuntimesApi().startRuntime(
      this.currentRuntime.googleProject, this.currentRuntime.runtimeName, {signal: this.pollAbortSignal});
    this.resumeCount++;
  }

  private async deleteRuntime(): Promise<void> {
    if (this.deleteCount >= this.maxDeleteCount) {
      throw new ExceededActionCountError(
        `Reached max runtime delete count (${this.maxDeleteCount})`, this.currentRuntime);
    }
    await runtimeApi().deleteRuntime(this.workspaceNamespace, {signal: this.pollAbortSignal});
    this.deleteCount++;
  }

  private reachedResolution(): boolean {
    return this.currentRuntime && this.resolutionCondition(this.currentRuntime);
  }

  private isRuntimeDeleted(): boolean {
    return this.currentRuntime && this.currentRuntime.status === RuntimeStatus.Deleted;
  }

  private isRuntimeStopped(): boolean {
    return this.currentRuntime && this.currentRuntime.status === RuntimeStatus.Stopped;
  }

  private isRuntimeErrored(): boolean {
    return this.currentRuntime && this.currentRuntime.status === RuntimeStatus.Error;
  }

  private isNotFoundError(e: any): boolean {
    // Our Swagger-generated APIs throw an error of type Response on a non-success status code.
    return e instanceof Response && e.status === 404;
  }

  private handleUnknownError(e: any) {
    if (e instanceof Response && e.status >= 500 && e.status < 600) {
      this.serverErrorCount++;
    }
    reportError(e, stackdriverReporterStore.get().reporter);
  }

  private hasTooManyServerErrors(): boolean {
    return this.serverErrorCount > this.maxServerErrorCount;
  }

  /**
   * Runs the runtime intiailizer flow.
   *
   * The strategy here is to poll the getRuntime endpoint for runtime status, waiting for the
   * runtime to reach the ready state (RuntimeStatus.Running) or an error state which can be
   * recovered from. Action will be taken where possible: a stopped runtime will trigger a call to
   * startRuntime, a nonexistent runtime will trigger a call to createRuntime, and an errored
   * runtime will trigger a call to deleteRuntime in an attempt to retry runtime creation.
   *
   * @return A Promise which resolves with a Runtime or rejects with a
   * LeoRuntimeInitializationFailedError, which holds a message and the current Runtime object (if one
   * existed at the time of failure).
   */
  private async run(): Promise<Runtime> {
    this.initializeStartTime = Date.now();

    return new Promise((resolve, reject) => {
      this.resolve = resolve;
      this.reject = reject as (error: Error) => {};
      this.poll();
    }) as Promise<Runtime>;
  }

  private async poll() {

    // Overall strategy: continue polling the get-runtime endpoint, with capped exponential backoff,
    // until we either reach our goal state (a RUNNING runtime) or run up against the overall
    // timeout threshold.
    //
    // Certain runtime states require active intervention, such as deleting or resuming the runtime;
    // these are handled within the the polling loop.
    if (this.pollAbortSignal && this.pollAbortSignal.aborted) {
      // We'll bail out early if an abort signal was triggered while waiting for the poll cycle.
      return this.reject(
        new LeoRuntimeInitializationAbortedError('Request was aborted', this.currentRuntime));
    }
    if (Date.now() - this.initializeStartTime > this.overallTimeout) {
      return this.reject(
        new LeoRuntimeInitializationFailedError(
          `Initialization attempt took longer than the max time allowed (${this.overallTimeout}ms)`,
          this.currentRuntime));
    }

    // Fetch the current runtime status, with some graceful error handling for NOT_FOUND response
    // and abort signals.
    try {
      this.currentRuntime = await runtimeApi().getRuntime(this.workspaceNamespace, {signal: this.pollAbortSignal});
      this.onPoll(this.currentRuntime);
    } catch (e) {
      if (isAbortError(e)) {
        return this.reject(
          new LeoRuntimeInitializationAbortedError('Abort signal received during runtime API call',
            this.currentRuntime));
      } else if (this.isNotFoundError(e)) {
        // A not-found error is somewhat expected, if a runtime has recently been deleted or
        // hasn't been created yet.
        this.currentRuntime = null;
        this.onPoll(null);
      } else {
        this.handleUnknownError(e);
        if (this.hasTooManyServerErrors()) {
          return this.reject(
            new ExceededErrorCountError(
              `Reached max server error count (${this.maxServerErrorCount})`, this.currentRuntime));
        }
      }
    }

    // Attempt to take the appropriate next action given the current runtime status.
    try {
      if (this.reachedResolution()) {
        // We've reached the goal - resolve the Promise.
        return this.resolve(this.currentRuntime);
      } else if (this.currentRuntime === null || this.isRuntimeDeleted()) {
        await this.createRuntime();
      } else if (this.isRuntimeStopped()) {
        await this.resumeRuntime();
      } else if (this.isRuntimeErrored()) {
        // If runtime is in error state, delete it so it can be re-created at the next poll loop.
        reportError(
          `Runtime ${this.currentRuntime.googleProject}/${this.currentRuntime.runtimeName}` +
          ` has reached an ERROR status`, stackdriverReporterStore.get().reporter);
        await this.deleteRuntime();
      }
    } catch (e) {
      if (isAbortError(e)) {
        return this.reject(
          new LeoRuntimeInitializationAbortedError('Abort signal received during runtime API call',
          this.currentRuntime));
      } else if (e instanceof ExceededActionCountError) {
        // This is a signal that we should hard-abort the polling loop due to reaching the max
        // number of delete or create actions allowed.
        return this.reject(e);
      } else {
        this.handleUnknownError(e);
        if (this.hasTooManyServerErrors()) {
          return this.reject(
            new ExceededErrorCountError(
              `Reached max server error count (${this.maxServerErrorCount})`, this.currentRuntime));
        }
      }
    }

    setTimeout(() => {
      this.poll();
    }, this.currentDelay);
    // Increment capped exponential backoff for the next poll loop.
    this.currentDelay = Math.min(this.currentDelay * 1.3, this.maxDelay);
  }
}
