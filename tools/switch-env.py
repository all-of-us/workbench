import logging
import subprocess
import sys
import os
from shutil import copyfile
from main_util import configure_logging, get_parser, get_confirmation

#Run options: ./switch-env.py test, ./switch-env.py prod
def environmentSwap(environment):
    environmentPath = '../ui/src/environments/'
    if(environment == "test"):
        copyfile(environmentPath + 'environment.test.ts', environmentPath + 'environment.ts')
    elif(environment == "prod"):
        copyfile(environmentPath + 'environment.prod.ts', environmentPath + 'environment.ts')
    elif(environment == "revert"):
        os.rename(environmentPath + 'environment.stash.ts', environmentPath + 'environment.ts')
    else:
        copyfile(environmentPath + 'environment.dev.ts', environmentPath + 'environment.ts')

environment = 'dev'
try:
    environment = sys.argv[1]
except IndexError:
    pass

environmentSwap(environment)
