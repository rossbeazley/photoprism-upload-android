package ulk.co.rossbeazley.photoprism.upload

import android.os.FileObserver

fun Int.eventToString(): String = when(this) {
    FileObserver.ACCESS -> "ACCESS"
    FileObserver.ALL_EVENTS -> "ALL_EVENTS"
    FileObserver.ATTRIB -> "ATTRIB"
    FileObserver.CLOSE_NOWRITE -> "CLOSE_NOWRITE"
    FileObserver.CLOSE_WRITE -> "CLOSE_WRITE"
    FileObserver.CREATE -> "CREATE"
    FileObserver.DELETE -> "DELETE"
    FileObserver.DELETE_SELF -> "DELETE_SELF"
    FileObserver.MODIFY -> "MODIFY"
    FileObserver.MOVED_FROM -> "MOVED_FROM"
    FileObserver.MOVED_TO -> "MOVED_TO"
    FileObserver.MOVE_SELF -> "MOVE_SELF"
    FileObserver.OPEN -> "OPEN"
    else -> "unknown event $this"
}