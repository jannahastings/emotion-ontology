from django.template import RequestContext
from django.http import HttpResponseRedirect
from django.shortcuts import render_to_response, get_object_or_404
from django import forms
from django.core.urlresolvers import reverse
from django.db.models import Q
from tagger.models import Talk, User, Tag, UserTalk, OntologyTerm
import datetime

### Forms ###

class UserField(forms.Field):
  def to_python(self, value):
    value = super(UserField, self).to_python(value)
    try:
      user = User.objects.get(access_code=value)
      if not user.registration_time:
	user.registration_time = datetime.datetime.now()
	user.save()
      return user
    except User.DoesNotExist:
      raise forms.ValidationError("Invalid access code.")

class LoginForm(forms.Form):
  access_code = UserField()

class TagForm(forms.ModelForm):
  class Meta:
    model = Tag
    fields = ("term", "articulation", "degree")
   
class FeelingTagForm(TagForm):
  feeling_terms = OntologyTerm.objects.filter(Q(category="emotion") |
      Q(category="feeling"))
  term = forms.ModelChoiceField(queryset=feeling_terms)

class ThoughtTagForm(TagForm):
  thought_terms = OntologyTerm.objects.filter(category="thought")
  term = forms.ModelChoiceField(queryset=thought_terms)

class UserTalkForm(forms.ModelForm):
  class Meta:
    model = UserTalk
    fields = ('articulation', 'comment')

TagFormSet = forms.models.modelformset_factory(Tag, extra=0, exclude=(
  'talk', 'user', 'term', 'timestamp', 'articulation', 'degree'))

class ProjectEvaluationForm(forms.ModelForm):
  class Meta:
    model = User
    # Asking for just one field fails. Excluding all but one field works.
    # Go figure.
    exclude = ('access_code', 'registration_time', 'talk_data')

### end Forms ###

### Utility ###

def restricted(myfunc):
  """Decorator to redirect users to index if they aren't logged in."""
  def inner_func(*args, **kwargs):
    if not args[0].session.get('user'):
      return HttpResponseRedirect(reverse(index))
    return myfunc(*args, **kwargs)
  return inner_func

### end Utility ###

### Views ###

def index(request):
  # If logged in, direct to talks.
  if request.session.get('user'):
    return HttpResponseRedirect(reverse(talks))

  # Else, deal with the form.
  if request.method == "POST":
    form = LoginForm(request.POST)
    if form.is_valid():
      request.session['user'] = form.cleaned_data['access_code']
      return HttpResponseRedirect(reverse(talks))
  else:
    form = LoginForm()
  return render_to_response('index.html', {'form':form}, 
      context_instance=RequestContext(request))

@restricted
def logout(request):
  request.session.flush()
  return render_to_response('logout.html', 
      context_instance=RequestContext(request))

@restricted
def talks(request):
  talks_list = Talk.objects.all().order_by('start_time')
  current_talk = None
  # debug.
  now = datetime.datetime.now()
  for talk in talks_list:
    if talk.start_time <= now and talk.end_time >= now:
      current_talk = talk
      break

  return render_to_response("tagger/talks.html", {
    "talks_list" : talks_list,
    "current_talk" : current_talk},
    context_instance=RequestContext(request))

@restricted
def detail(request, talk_id):
  user = request.session['user']
  t = get_object_or_404(Talk, pk=talk_id)
  if request.method == "POST":
    tag = Tag(talk=t, user=user, active=True)
    thoughtform = ThoughtTagForm(request.POST, instance=tag, prefix="thought")
    # Must refresh tag, otherwise second form overwrites first.
    tag = Tag(talk=t, user=user, active=True)
    feelingform = FeelingTagForm(request.POST, instance=tag, prefix="feeling")
    if thoughtform.is_valid() or feelingform.is_valid():
      if thoughtform.is_valid():
	thoughtform.save()
      if feelingform.is_valid():
	feelingform.save()
      return HttpResponseRedirect(reverse(detail, args=[talk_id]))
  else:
    thoughtform = ThoughtTagForm(prefix="thought")
    feelingform = FeelingTagForm(prefix="feeling")
  
  # next/previous buttons to go through talks.
  talk_id = int(talk_id)
  if talk_id == 1:
    previous_talk_id = None
  else:
    previous_talk_id = talk_id - 1
  if talk_id == Talk.objects.count():
    next_talk_id = None
  else:
    next_talk_id = talk_id + 1

  return render_to_response("tagger/detail.html", {
    "talk" : t, 
    "tag_set" : user.tag_set.filter(talk=t, active=True),
    'previous' : previous_talk_id, 
    'next' : next_talk_id,
    'thoughtform' : thoughtform,
    'feelingform' : feelingform,
    },
    context_instance=RequestContext(request))

@restricted
def talk_comment(request, talk_id):
  user = request.session['user']
  t = get_object_or_404(Talk, pk=talk_id)
  if not t.is_publishable:
    return HttpResponseRedirect(reverse(detail, args=[talk_id]))
  try:
    usertalk = UserTalk.objects.get(user=user, talk=t)
  except UserTalk.DoesNotExist:
    usertalk = UserTalk(user=user, talk=t)
  if request.method == "POST":
    form = UserTalkForm(request.POST, instance=usertalk)
    if form.is_valid():
      form.save()
      return HttpResponseRedirect(reverse(detail, args=[talk_id]))
  else:
    form = UserTalkForm(instance=usertalk)

  return render_to_response("tagger/talk_comment.html", {
    "talk" : t,
    "form" : form,
    "tag_set" : user.tag_set.filter(talk=t, active=True),
    },
    context_instance=RequestContext(request))

@restricted
def tag_delete(request, talk_id):
  t = get_object_or_404(Talk, pk=talk_id)
  if not t.is_publishable:
    return HttpResponseRedirect(reverse(detail, args=[talk_id]))
  if request.method == "POST":
    formset = TagFormSet(request.POST)
    if formset.is_valid():
      formset.save()
      return HttpResponseRedirect(reverse(detail, args=[talk_id]))
  tag_set = request.session['user'].tag_set.filter(talk=t, active=True)
  formset = TagFormSet(queryset=tag_set)
  combine = zip(tag_set, formset.forms)
  return render_to_response("tagger/tag_delete.html", {
    "talk" : t,
    "formset" : formset,
    "tag_set" : tag_set,
    "combine" : combine,
    },
    context_instance=RequestContext(request))

@restricted
def project_evaluation(request):
  # Refresh user record from database.
  user = User.objects.get(pk=request.session['user'].id)
  print user.project_evaluation
  if request.method == "POST":
    form = ProjectEvaluationForm(request.POST, instance=user)
    if form.is_valid():
      form.save()
      return HttpResponseRedirect(reverse(talks))
  else:
    form = ProjectEvaluationForm(instance=user)

  return render_to_response("tagger/project_evaluation.html", {
    "form" : form,
    },
    context_instance=RequestContext(request))


### end Views ###
