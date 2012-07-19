from django.db import models

# Using a MySQL db eventually, max_length=255 on unique varchar or on all 
# varchar if using MySQL <v5.0.3

ARTICULATION_CHOICES = (
  (1, "1 badly"),
  (2, "2"),
  (3, "3"),
  (4, "4"),
  (5, "5 well"))

class Talk(models.Model):
  title = models.CharField(max_length=255)
  authors = models.CharField(max_length=255)
  start_time = models.DateTimeField()
  end_time = models.DateTimeField()
  is_publishable = models.BooleanField(default=False)

  def __unicode__(self):
    return self.title

class User(models.Model):
  access_code = models.CharField(max_length=10, unique=True)
  registration_time = models.DateTimeField(null=True, blank=True)
  project_evaluation = models.TextField(blank=True)
  talk_data = models.ManyToManyField(Talk, through="UserTalk")

  def __unicode__(self):
    return self.access_code
  
class OntologyTerm(models.Model):
  id = models.IntegerField("term id from the ontology. Not an auto field.",
      primary_key=True)
  label = models.CharField("The full label of the term.", max_length=50)
  tag_label = models.CharField("The phrase to use in tag sentences.", 
      max_length=50)
  definition = models.CharField(max_length=300, blank=True)
  category = models.CharField(max_length=7, choices=(
    ("emotion", "emotion"),
    ("feeling", "feeling"),
    ("thought", "thought")))

  def __unicode__(self):
    return self.tag_label
    #return "%s -- %s" % (self.tag_label, self.category)

class Tag(models.Model):
  talk = models.ForeignKey(Talk)
  user = models.ForeignKey(User)
  term = models.ForeignKey(OntologyTerm)
  timestamp = models.DateTimeField(auto_now_add=True)
  articulation = models.IntegerField("How well does this capture your emotion?",
      choices=ARTICULATION_CHOICES)
  degree = models.IntegerField("Strength of response", default=3, choices = (
    (1, "1 weak"),
    (2, "2"),
    (3, "3 medium"),
    (4, "4"),
    (5, "5 strong")))
  active = models.BooleanField()

  def __unicode__(self):
    return "%s -- %s -- %s" % (self.user, self.talk, self.term)

class UserTalk(models.Model):
  user = models.ForeignKey(User)
  talk = models.ForeignKey(Talk)
  articulation = models.IntegerField("How well does this capture your emotion?",
      choices=ARTICULATION_CHOICES)
  comment = models.TextField()

  def __unicode__(self):
    return "%s -- %s -- %d" % (self.user, self.talk, self.articulation)
