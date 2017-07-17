"""Determine common absolute paths."""

import os


def get_repo_base():
  tools_dir = os.path.dirname(os.path.abspath(__file__))
  repo_base, _ = os.path.split(tools_dir)
  return repo_base


def get_api_dir():
  return os.path.join(get_repo_base(), 'api')


def get_ui_dir():
  return os.path.join(get_repo_base(), 'ui')


def get_ng():
  return os.path.join(get_ui_dir(), 'node_modules', '@angular', 'cli', 'bin', 'ng')


if __name__ == '__main__':
  # simple self-test
  print 'repo base', get_repo_base()
  print 'api dir', get_api_dir()
  print 'ui dir', get_ui_dir()
  print 'ng', get_ng()
