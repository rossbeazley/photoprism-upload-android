package ulk.co.rossbeazley.photoprism.upload.fakes

import ulk.co.rossbeazley.photoprism.upload.syncqueue.LastUploadRepository

class FakeLastUploadRepositoy : LastUploadRepository {
    var filePath : String = "NONE"
    override fun remember(filePath : String) { this.filePath = filePath}
    override fun recall() : String = filePath
}