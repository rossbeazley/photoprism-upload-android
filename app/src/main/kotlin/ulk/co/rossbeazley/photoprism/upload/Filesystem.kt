package ulk.co.rossbeazley.photoprism.upload

import kotlinx.coroutines.flow.Flow

interface Filesystem {
    fun watch(path: String): Flow<String>
    fun list(path: String): List<String>
}