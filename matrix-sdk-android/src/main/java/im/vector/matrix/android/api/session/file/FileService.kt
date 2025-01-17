/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.api.session.file

import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.internal.crypto.attachments.ElementToDecrypt
import java.io.File

/**
 * This interface defines methods to get files.
 */
interface FileService {

    enum class DownloadMode {
        /**
         * Download file in external storage
         */
        TO_EXPORT,
        /**
         * Download file in cache
         */
        FOR_INTERNAL_USE
    }

    /**
     * Download a file.
     * Result will be a decrypted file, stored in the cache folder. id parameter will be used to create a sub folder to avoid name collision.
     * You can pass the eventId
     */
    fun downloadFile(
            downloadMode: DownloadMode,
            id: String,
            fileName: String,
            url: String?,
            elementToDecrypt: ElementToDecrypt?,
            callback: MatrixCallback<File>)
}
