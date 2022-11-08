This will watch the camera folder and use webdav to upload images

Sort of a task list:

audit logs: 
 rework audit logs so they are "proper", maybe a json object?
 add an audit log screen with actual list
 add a way to clear logs

photo upload screen - stub:
 add list of synced photos, path and status
 render text list of synced photos
 add thumbnail of photo to list

upload task:
 actually upload something using work manager
 include progress notification
 add to failed queue

failed uploads:
 rework sync list so just failed items
 add re-sync button

later:
 show current workmanager items if possible
 restart background service





System Collaborators:

File watcher service:
 collaborates with upload service
 filewatcher adapter plugs into this
 

Foreground service:
 just shows a low priority service
 could "house" the file watcher, but no need

Upload service:
 takes a file path
 collaborates with sync queue
 collaborates with work manager to run work in background
 uses webdav in some way

Sync queue:
 has a View
 can retry an item - removes from queue as its not failed
 can enqueue a failed item
 needs persistence

Audit Logs:
 has a view
 can add an entry
 can get all the entries
 needs persistence

