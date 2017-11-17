"""
AoU Workbench Python Client

This module wraps the swagger generated Python client.
"""
from __future__ import absolute_import

__version__ = '0.1.2a1'

try:
    from .swagger_client import *
except ImportError:
    raise RuntimeError('No generated swagger client found')
