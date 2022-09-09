package com.sarada.videosplayer.network

import com.sarada.videosplayer.models.VideosResponse
import kotlinx.coroutines.Deferred
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

interface VideosService {

    @Headers("Authorization: 563492ad6f917000010000017a2121d95b75469aaaffa12cf80039a2")
    @GET("popular")
    fun getPopularVideos(): Deferred<VideosResponse>

    /*@GET("movie/top_rated")
    fun getTopRatedMovies(@Query("api_key") apiKey: String?): Deferred<MoviesResponse>

    @GET("movie/{movie_id}/videos")
    fun getMovieTrailer(
        @Path("movie_id") id: Int,
        @Query("api_key") apiKey: String?
    ): Deferred<TrailerResponse>

    @GET("movie/{movie_id}/reviews")
    fun getMovieReviews(
        @Path("movie_id") id: Int,
        @Query("api_key") apiKey: String?
    ): Deferred<ReviewResponse>*/
}