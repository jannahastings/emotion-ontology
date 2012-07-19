#!/usr/bin/python
"""Simple program to grab talk data that I care about from a csv file.

Prepare csv file by running 'grep -e "talks " <original_file> > input.csv'

I'm interested in fields 2, 3 and 6. These represent the start and end times 
and the title and authors.

"""

import csv
import datetime

class Talk(object):
  def __init__(self, start, end, title, authors):
    self.start_time = start
    self.end_time = end
    self.title = title
    self.authors = authors

  def __str__(self):
    return "{}\n{}\nStart: {}, End: {}".format(self.title, self.authors, \
	self.start_time, self.end_time)

def talk_grabber(reader):
  # The starting date of the conference.
  this_date = datetime.date(2012, 7, 23)
  talks = []
  first_row = reader.next()
  # Time of the last talk we iterated over, for detecting when to increment day.
  last_time = datetime.time(*map(int, first_row[0].split(":")))
  end_time = datetime.time(*map(int, first_row[1].split(":")))
  start_dt = datetime.datetime.combine(this_date, last_time)
  end_dt = datetime.datetime.combine(this_date, end_time)
  authors = ".".join(first_row[3].split(".")[1:]).strip()
  title = first_row[3].split(".")[0].strip()
  talks.append(Talk(start_dt, end_dt, title, authors))

  for row in reader:
    start_time = datetime.time(*map(int, row[0].split(":")))
    if (start_time < last_time):
      this_date = datetime.date(2012, 7, this_date.day + 1)
    last_time = start_time
    end_time = datetime.time(*map(int, row[1].split(":")))
    start_dt = datetime.datetime.combine(this_date, last_time)
    end_dt = datetime.datetime.combine(this_date, end_time)
    authors = ".".join(row[3].split(".")[1:]).strip().strip(".")
    title = row[3].split(".")[0].strip()
    talks.append(Talk(start_dt, end_dt, title, authors))

  return talks

if __name__ == "__main__":
  import sys

  reader = csv.reader(open(sys.argv[1], "r"))
  talks = talk_grabber(reader)

  print "Debug: "
  for talk in talks:
    print talk
    print

  import pickle

  #Adjusting for Jython 2.5.2
  f = file("talks.pickle", "w")
  pickle.dump(talks, f)
  f.close()

  size = len(talks[0].authors)
  for talk in talks:
    size = max(size, len(talk.authors))

  print size
