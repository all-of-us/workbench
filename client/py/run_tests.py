#!/usr/bin/env python
"""Runs Python Workbench client tests."""

import argparse
import os
import sys
import unittest


def run_tests(root_dir, pattern):
    suite = unittest.loader.defaultTestLoader.discover(root_dir, pattern)
    return unittest.TextTestRunner(verbosity=2).run(suite)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument(
            '--pattern',
            default='*_test.py',
            help='Shell glob for test files to include.')
    args = parser.parse_args()
    result = run_tests(os.path.abspath(os.path.dirname(__file__)), args.pattern)
    sys.exit(0 if result.wasSuccessful() else 1)
