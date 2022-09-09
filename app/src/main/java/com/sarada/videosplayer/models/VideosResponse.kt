package com.sarada.videosplayer.models

import android.os.Parcelable
import com.squareup.moshi.Json
import kotlinx.parcelize.Parcelize

@Parcelize
data class VideosResponse(

    @Json(name = "page"          ) var page         : Int?              = null,
    @Json(name = "per_page"      ) var perPage      : Int?              = null,
    @Json(name = "total_results" ) var totalResults : Int?              = null,
    @Json(name = "url"           ) var url          : String?           = null,
    @Json(name = "videos"        ) var videos       : List<Videos> = arrayListOf()

): Parcelable

@Parcelize
data class Videos (

    @Json(name = "id"             ) var id            : Int?                     = null,
    @Json(name = "width"          ) var width         : Int?                     = null,
    @Json(name = "height"         ) var height        : Int?                     = null,
    @Json(name = "url"            ) var url           : String?                  = null,
    @Json(name = "image"          ) var image         : String?                  = null,
    @Json(name = "duration"       ) var duration      : Int?                     = null,
    @Json(name = "user"           ) var user          : User?                    = User(),
    @Json(name = "video_files"    ) var videoFiles    : List<VideoFile>    = arrayListOf(),
    @Json(name = "video_pictures" ) var videoPictures : List<VideoPicture> = arrayListOf()

): Parcelable

@Parcelize
data class User (

    @Json(name = "id"   ) var id   : Int?    = null,
    @Json(name = "name" ) var name : String? = null,
    @Json(name = "url"  ) var url  : String? = null

): Parcelable

@Parcelize
data class VideoFile (

    @Json(name = "id"        ) var id       : Int?    = null,
    @Json(name = "quality"   ) var quality  : String? = null,
    @Json(name = "file_type" ) var fileType : String? = null,
    @Json(name = "width"     ) var width    : Int?    = null,
    @Json(name = "height"    ) var height   : Int?    = null,
    @Json(name = "link"      ) var link     : String? = null

): Parcelable

@Parcelize
data class VideoPicture (

    @Json(name = "id"      ) var id      : Int?    = null,
    @Json(name = "picture" ) var picture : String? = null,
    @Json(name = "nr"      ) var nr      : Int?    = null

): Parcelable