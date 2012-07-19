"""Loads a file of access codes into the database. Env. variables must be set.

Need to set some environment variables when you run this. Do that like so:
  `PYTHONPATH="/home/colin/projects/django/" DJANGO_SETTINGS_MODULE="emmontoweb.settings" python insert_access_codes.py <access_codes_file>`

"""
import sys

if len(sys.argv) == 1:
  print __doc__
  sys.exit()

from tagger.models import User

#Adjusting for Jython 2.5.2
f = file(sys.argv[1], "r")
for line in f:
  if not line.startswith("#"):
    User(access_code=line.strip()).save()
f.close()
