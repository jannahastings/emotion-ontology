"""Generate the access codes for this website."""

codes = []

allowed_characters = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789"

import sys

length = int(sys.argv[1])
N = int(sys.argv[2])
print "# Generating %d %d character codes:" % (length, N)

import random
random.seed("There once was a man named Jake. He climbed a beanstalk.")

for i in range(N):
  is_unique = False
  while is_unique == False:
    code = ""
    for j in range(length):
      code = code + random.choice(allowed_characters)
    try:
      codes.index(code)
    except ValueError:
      is_unique = True
      codes.append(code)

for code in codes:
  print code
