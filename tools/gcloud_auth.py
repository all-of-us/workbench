"""Project setting and account login for gcloud."""

import logging
import subprocess


def add_parse_and_do_auth(parser):
  """Takes in a parser, does all the auth stuff, and returns parsed args.

  This function composes the rest of the functions in this module, for the
  expected/common use case.
  """
  add_args(parser)
  args = check_args(parser, parser.parse_args())
  do_auth(args)
  return args


def add_args(parser):
  """Adds command-line flags necessary for gcloud settings to an argparse.Parser."""
  parser.add_argument(
      '-a', '--account',
      help='Account to perform AppEngine admin tasks. Typically @pmi-ops.org.')
  parser.add_argument(
      '-p', '--project', default='all-of-us-workbench-test',
      help='AppEngine project to operate upon.')


def check_args(parser, args):
  if not args.account:
    parser.error('--account is required')
  if not args.project:
    parser.error('--project is required')
  return args


def do_auth(args):
  """Executes gcloud commands to log in and set the current project.

  Args:
    args: Parsed command-line flags, as from add_args.
  """
  subprocess.check_call(['gcloud', 'auth', 'login', args.account])
  subprocess.check_call(['gcloud', 'config', 'set', 'project', args.project])
