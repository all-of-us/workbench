#!/usr/bin/env python
"""Self-test / simple example: Make a simple authenticated API call."""

from __future__ import print_function

from aou_workbench_client.auth import get_authenticated_swagger_client
from aou_workbench_client.swagger_client.apis.workspaces_api import WorkspacesApi


if __name__ == '__main__':
    print('Listing workspaces via authenticated API:')
    client = WorkspacesApi(api_client=get_authenticated_swagger_client())
    workspace_list = client.get_workspaces()
    for ws in workspace_list.items:
        print('%s/%s\t%s\t%s' % (ws.namespace, ws.id, ws.name, ws.description))
