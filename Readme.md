This will watch the camera folder and use webdav to upload images

VERY NEXT: Remove callback in background job system

Sort of a task list:

✓ - done
✇ - doing
❌ - dropped
😎 - re-work

Next up:
✓ - rework callback for work manager to promote to explicit public primary port
✓ - write adapter for file watcher service
✓ - write status event observable flow
✓ - write adapter to persist sync queue
✓ - implement work manager integration
✓ - implement AuditRepo adapter for now
✓ - complete rest of audit logs
✓ - wire everything together
✓ - implement keep alive task
- do upload service integration tests against test service (needs deploying)
  ✓ - audit log screen
  ✓ - real audit log service, sharedprefs at first, maybe with json then sqlite or summit

- WORK OUT WHATS LEFT, DRAW A DIAGRAM

- "share" photo to sync
  ✓ - notice what the last file was and discover newer photos, maybe when we detect a new photo or on button press
  ✓ - render the sync queue
  ✓ - retry a failed download
  ✓ - jetpack compose
  ✇ - configuration screen, server url, creds, retry count, backoff time, directory to watch, upload over wifi only
 - screen navigation
 - detect unconfigured app and show settings
 - image in sync queue
 - remove row in sync queue
 - cancel upload from sync queue
 - maybe multiselect in sync queue

audit logs: 
 rework audit logs so they are "proper", maybe a json object?
✓ add an audit log screen with actual list
✓ add a way to clear logs
  add "level" filter

photo upload screen - stub:
✓ add list of synced photos, path and status
✓ render text list of synced photos
 add thumbnail of photo to list

upload task:
✓ actually upload something using work manager
 include progress notification
✓ add to failed queue

failed uploads:
 rework sync list ✓
 add filter so just failed items ✓
 add re-sync button ✓
 add re-sync all 

later:
 show current workmanager items if possible
 restart background service

configuration screen:
 address, username, password
 reconfigure http without restart
 link to docs on generating app password
 Test Connection
 encryption of credentials, maybe credentials manager


System Collaborators:

File watcher service: ✓
 collaborates with upload service ✓
 filewatcher adapter plugs into this ✓
 

Foreground service:
 just shows a low priority service ✓
 could "house" the file watcher, but no need ✓

Upload service:
 takes a file path ✓
 collaborates with sync queue ✓
 collaborates with work manager to run work in background ✓
 uses webdav in some way ✓

Sync queue: ✓
 has a View ✓
 can retry an item - removes from queue as its not failed ✓
 can enqueue a failed item ✓
 needs persistence ✓

Audit Logs:
 has a view ✓
 can add an entry ✓
 can get all the entries ✓
 needs persistence ✓

