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
  targets = (
      _TargetChoices.ALL_TARGETS if args.target == _TargetChoices.ALL
      else (args.target,))
  get_confirmation('Deploy to %r (%s)?' % (args.project, ', '.join(targets)))

  if _TargetChoices.API in targets:
    logging.info('Deploying API')
    subprocess.check_call(
        [
            './gradlew',
            ':appengineDeploy',
            # TODO(danrodney)
            #'-Pproject=%s' % args.project,
            #'-Paccount=%s' % args.account,
        ],
        cwd=paths.get_api_dir())
  else:
    logging.info('Skipping deploy of API.')

  if _TargetChoices.UI in targets:
    logging.info('Building UI using %r', paths.get_ng())
    subprocess.check_call([paths.get_ng(), 'build'], cwd=paths.get_ui_dir())
    logging.info('Deploying UI')
    subprocess.check_call(
        [
          'gcloud',
          'app',
          'deploy',
          '--project',
          args.project,
          '--account',
          args.account,
        ],
        cwd=paths.get_ui_dir())
  else:
    logging.info('Skipping deploy of UI.')

  logging.info('Deploy complete.')


class _TargetChoices(object):
  ALL = 'all'
  API = 'api'
  UI = 'ui'
  ALL_TARGETS = [API, UI]
  ALL_TARGET_CHOICES = [ALL] + ALL_TARGETS


if __name__ == '__main__':
  configure_logging()
  parser = get_parser()
  parser.add_argument(
      '-t', '--target',
      default=_TargetChoices.ALL, choices=_TargetChoices.ALL_TARGET_CHOICES,
      help='Which part of the Workbench to deploy.')
  # Set auth/project here for general usage, but also pass them explicitly
  # to commands that support it as a safeguard against other gcloud commands
  # altering the configured values (which would cause a race condition).
  args = gcloud_auth.add_parse_and_do_auth(parser)
  deploy(args)
