#!/usr/bin/env python
"""Deploy API and UI to AppEngine.

Usage:
    deploy.py --project all-of-us-workbench-test --account $USER@pmi-ops.org
"""

import logging
import subprocess

from main_util import configure_logging, get_parser, get_confirmation
import paths
import gcloud_auth


def deploy(args):
  gcloud_auth.do_auth(args)

  get_confirmation(
      'Deploy to %r%s%s?' %
      (args.project, ' (skip API)' if args.skip_api else '', ' (skip UI)' if args.skip_ui else ''))

  if args.skip_api:
    logging.info('Skipping deploy of API.')
  else:
    logging.info('Deploying API')
    subprocess.check_call(
        ['./gradlew', ':appengineDeploy'], cwd=paths.get_api_dir())

  if args.skip_ui:
    logging.info('Skipping deploy of UI.')
  else:
    logging.info('Building UI using %r', paths.get_ng())
    subprocess.check_call([paths.get_ng(), 'build'], cwd=paths.get_ui_dir())
    logging.info('Deploying UI')
    subprocess.check_call(['gcloud', 'app', 'deploy'], cwd=paths.get_ui_dir())

  logging.info('Deploy complete.')


if __name__ == '__main__':
  configure_logging()
  parser = get_parser()
  parser.add_argument('--skip_api', action='store_true')
  parser.add_argument('--skip_ui', action='store_true')
  args = gcloud_auth.add_parse_and_check_args(parser)
  deploy(args)
