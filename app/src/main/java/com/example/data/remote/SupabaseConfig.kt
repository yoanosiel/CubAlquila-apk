package com.example.data.remote

import com.example.BuildConfig

object SupabaseConfig {
    const val URL = BuildConfig.SUPABASE_URL
    const val PUBLISHABLE_KEY = BuildConfig.SUPABASE_PUBLISHABLE_KEY
    const val LISTINGS_TABLE = "rental_listings"
    const val IMAGES_BUCKET = "rental-images"
}
