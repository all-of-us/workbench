"""
Basic test of the wrapper.  If it fails, then swagger is not generated
"""
import unittest


class ModuleTest(unittest.TestCase):
    def test_module_imports(self):
        try:
            import aou_workbench_client  #pylint: disable=unused-variable
        except RuntimeError:
            self.fail()
