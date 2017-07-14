# Copied from github.com/vanderbilt/pmi-data/blob/master/common/main_util.py

import argparse
import inspect
import logging
import sys
import time


def configure_logging():
  logging.Formatter.converter = time.gmtime  # Log in UTC.
  logging.basicConfig(
      stream=sys.stdout,
      level=logging.INFO,
      format='%(asctime)s %(levelname)s: %(message)s')


def get_parser(description=None):
  """Gets an ArgumentParser, defaulting to the caller's __doc__ as the description."""
  if description is None:
    doc = inspect.getmodule(inspect.stack()[1]).__doc__
  else:
    doc = description
  return argparse.ArgumentParser(
      description=doc,
      formatter_class=argparse.RawDescriptionHelpFormatter)


def get_confirmation(message, exit_on_failure=True):
  try:
    confirmation = raw_input('%s [Yy] ' % message)
  except KeyboardInterrupt:
    logging.info('Interpreting ^C as a "no."')
    confirmation = 'n'
  confirmed = (confirmation in ('Y', 'y'))

  if not confirmed:
    logging.info('Cancelled by user.')
    if exit_on_failure:
      sys.exit(1)
  return confirmed
