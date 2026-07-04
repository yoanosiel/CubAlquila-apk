package com.example.data.repository

import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull

object SharedSyncService {
    fun parse(mediaType: String): MediaType? {
        return mediaType.toMediaTypeOrNull()
    }
}
