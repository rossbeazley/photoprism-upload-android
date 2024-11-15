package ulk.co.rossbeazley.photoprism.upload

fun log(message: String) {
    when (System.getProperty("java.vendor")) {
        "The Android Project" -> android.util.Log.w("SYNC", message)
        else -> println("SYNC : $message")
    }

}