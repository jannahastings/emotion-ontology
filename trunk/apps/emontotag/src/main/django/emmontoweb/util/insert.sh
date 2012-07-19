#!/bin/bash
# runs insert_talks, terms and access_codes

export CLASSPATH="/home/colin/code/java/*"

JYTHONPATH="/home/colin/projects/django/" DJANGO_SETTINGS_MODULE="emmontoweb.settings" jython insert_talks.py talks.pickle
echo "Talks inserted."
JYTHONPATH="/home/colin/projects/django/" DJANGO_SETTINGS_MODULE="emmontoweb.settings" jython insert_access_codes.py access_codes.txt
echo "Access codes inserted."
JYTHONPATH="/home/colin/projects/django/" DJANGO_SETTINGS_MODULE="emmontoweb.settings" jython insert_terms.py
echo "Terms inserted."
