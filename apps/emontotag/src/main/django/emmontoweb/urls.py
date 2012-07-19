from django.conf.urls.defaults import *
from django.conf import settings
from django.views.generic.simple import direct_to_template

# Uncomment the next two lines to enable the admin:
from django.contrib import admin
admin.autodiscover()

urlpatterns = patterns('',
    # Example:
    # (r'^emmontoweb/', include('emmontoweb.foo.urls')),

    # Uncomment the admin/doc line below to enable admin documentation:
    # (r'^admin/doc/', include('django.contrib.admindocs.urls')),

    # Uncomment the next line to enable the admin:
    (r'^admin/', include(admin.site.urls)),
    # Mine.
    (r'^$', 'tagger.views.index'),
    (r'logout/$', 'tagger.views.logout'),
    (r'^talks/$', 'tagger.views.talks'),
    (r'^talks/(?P<talk_id>\d+)/$', 'tagger.views.detail'),
    (r'^talks/(?P<talk_id>\d+)/delete/$', 'tagger.views.tag_delete'),
    (r'^talks/(?P<talk_id>\d+)/comment/$', 'tagger.views.talk_comment'),
    (r'^project_evaluation/$', 'tagger.views.project_evaluation'),
    #(r'^talks/$', 'display_talks.views.index'),
    #(r'^talks/(?P<talk_id>\d+)/$', 'display_talks.views.detail'),
    url(r'about/$', direct_to_template, {'template' : 'about.html'}, name='about'),
    url(r'privacy/$', direct_to_template, {'template' : 'privacy.html'}, name='privacy'),
    url(r'contact/$', direct_to_template, {'template' : 'contact.html'}, name='contact'),

)

if settings.DEBUG:
  urlpatterns += patterns('',
    (r'^smedia/(?P<path>.*)$', 'django.views.static.serve', 
      {'document_root': settings.STATIC_DOC_ROOT}),
    )
