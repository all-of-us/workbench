# Notebook servers

Notebook servers are lazily allocated per researcher, per workspace as needed.
The primary function of these servers is to provide a secure environment to
run analysis via the [Jupyter Notebooks UI](https://jupyter.org).

All of Us leverages Terra's [Leonardo](https://github.com/DataBiosphere/leonardo)
API to launch and manage notebook servers. This service handles server state
tracking, monitoring, authenticated notebook UI access, and autopausing of idle
servers, among other features.

"Runtime" and "notebook server" will often be used interchangeably in code or
discussion. "Runtime" is the RESTful name of the notebook server resource in
Leonardo. In All of Us production, we currently only allow users to create
GCE VM (single-node) runtimes, but Leonardo supports using Dataproc as well - this
capability will be launched with the introduction of genomics data.

Historical note: "runtime" used to be named less generically as "cluster" (originating
from when Leo exclusively supported Dataproc). You may find comments / documentation
which still refer to it this way.

## Configuration

Leo notebook servers are configured [here](https://github.com/all-of-us/workbench/blob/478d6eb6ae08a8e670ccba3bb26a3f9ab5661085/api/src/main/java/org/pmiops/workbench/notebooks/LeonardoNotebooksClientImpl.java#L82).
All of Us uses a custom docker image for our notebook servers, which is defined
[here](https://github.com/DataBiosphere/terra-docker/tree/master/terra-jupyter-aou).
Notably, this image is configured for client-side [egress limiting via wondershaper](https://docs.google.com/document/d/1SO77UGE41lH5ffa0Gg6KoiMszc6I33dIbm48sA-_5-U/edit)
Finally, All of Us configures all Leo notebooks with a set of startup scripts and
Jupyter extensions such as code snippets and data use reminders, as described
[here](../src/main/webapp/static). These static assets are all hosted publicly via
the Workench API server, allowing Leonardo to pull them onto the VM at runtime.
Of particular interest is the [snippets-menu](../snippets-menu) extension which
mirrors code snippets from https://github.com/all-of-us/workbench-snippets and
is dynamically generated at deployment time.

Note: some of the configuration choices here are necessitated by our use of
VPC-SC in production. See [details here](https://docs.google.com/document/d/1BLfrlNC6UpZuTU38QNBXrFcqIHewAx0i2mYaP3GIrV0/edit).

## Auth and access

Notebook servers are created within a Terra billing project, which corresponds
to a single AoU workspace (http://broad.io/1ppw). The Jupyter notebook UI on a
notebook server is only accessible by the creator of that notebook server.
However, some REST operations can be performed by OWNERs on the same billing
project, e.g. start, stop, delete.

For code that runs on the Notebook VM itself, application default credentials
are initialized with the researcher's pet service account, which has access
mirroring that of the researcher within Terra.

### Browser authentication

Leonardo proxies all access to the notebook server. It requires an oauth bearer
token to be present for all proxied web requests. All of Us configures creds
as follows for use with Leonardo.

- AoU calls `/setCookie` on Leonardo, bootstrapping Workbench UI oauth2 token
  for subsequent requests
- AoU creates runtimes with a `defaultClientId`, which gets injected into the
  Leo Jupyter UI, and is used to periodically refresh the auth token while a
  browser session is active.
- For this reason, the Workbench UI client ID whitelists notebooks.firecloud.org
  as a valid domain.

By default, All of Us opens the Jupyter UI in an iframe, but the authentication
approach also works in separate popped out Leonardo windows/tabs.

## Notebook server lifecycle

See this [writeup](https://github.com/all-of-us/workbench/blob/478d6eb6ae08a8e670ccba3bb26a3f9ab5661085/ui/src/app/utils/leo-runtime-initializer.tsx#L101-L114)
for details on how a notebook server is created.

- When necessitated by a user action in the Workbench, e.g. opening a notebook
  in a workspace, a new notebook server will be created if none exists.
- If a notebook server exists in the STOPPED state when needed (e.g. due to autopause)
  it will be resumed.
- If a notebook server remains idle for >30m (no UI interaction or active kernels), it
  will be autopaused.
- A user can manually request a "reset" of their notebook server if there is an
  issue, which is implemented as a deleteRuntime -> createRuntime.
- Finally, a notebook server will be deleted by an offline cronjob after they
  exceed a certain age. This ensures that all runtimes have a maximum age which
  allows us to reason about rolling out new runtime configurations/features. The
  logic for runtime deletion is described [here](https://github.com/all-of-us/workbench/blob/478d6eb6ae08a8e670ccba3bb26a3f9ab5661085/api/src/main/java/org/pmiops/workbench/api/OfflineRuntimeController.java#L53-L66).

## Persistence

Notebooks are persisted by an extension to the Jupyter server. Ultimately
notebook files are synchronized from the notebook server to/from the
corresponding workspace scratch bucket. See the [data sync design](
https://docs.google.com/document/d/1rTq3DwsB2h7l_9f2I5pcpa9Z-PJJkMBw7APBqYs1NW8/edit)
for more details.
