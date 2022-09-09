package com.sarada.videosplayer.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarada.videosplayer.models.Videos
import com.sarada.videosplayer.models.VideosResponse
import com.sarada.videosplayer.network.VideosApi
import kotlinx.coroutines.launch

class MainViewModel: ViewModel() {

    private var currentVideo = 0
    private var mostViewedVideosHashMap: HashMap<String, Int> = HashMap<String, Int> ()
    private var mostViewedVideos: List<Videos>? = null

    var playTime = 0L // in ms
    var pauseTime = 0L // in ms
    private var totalTime = 0L // in ms
    var pressedPaused = 0

    private val _videosResponse = MutableLiveData<VideosResponse>()
    val videosResponse: LiveData<VideosResponse>
        get() = _videosResponse

    init {
        getVideos()
    }

    private fun getVideos() {
        viewModelScope.launch {
            _videosResponse.value = VideosApi.RETROFIT_SERVICE.getPopularVideos().await()
            Log.d("Videos Data", _videosResponse.value.toString())
        }
    }

    fun setPosition(isIncrement: Boolean = true){
        if(isIncrement){
            if(videosResponse.value?.videos?.size?.minus(1) == currentVideo)
                currentVideo = 0
            else ++currentVideo
        } else {
            if(currentVideo == 0)
                currentVideo = videosResponse.value?.videos?.size?.minus(1) ?: 0
            else --currentVideo
        }
    }

    fun getCurrentVideoIndex(): Int{
        return currentVideo
    }

    fun setCurrentVideoIndex(index: Int){
        currentVideo = index
    }

    fun updateHashMap(){
        val key = videosResponse.value?.videos?.get(currentVideo)?.id.toString()
        if( mostViewedVideosHashMap[key] != null){
            mostViewedVideosHashMap[key] = mostViewedVideosHashMap[key]?.plus(1) ?: 0
        }else{
            mostViewedVideosHashMap[key] = 1
        }
        Log.d("Hashvalue", "${key} : ${mostViewedVideosHashMap[key]}")
    }

    fun initHashMapWithMissingVideoIds(){
        for(video in videosResponse.value?.videos!!){
            val key = video.id.toString()
            if( mostViewedVideosHashMap[key] == null){
                mostViewedVideosHashMap[key] = 0
            }
        }
    }

    fun getHashMap(): HashMap<String, Int>{
        return mostViewedVideosHashMap
    }

    fun updateMostViewedVideos(){
        mostViewedVideos = videosResponse.value?.videos?.sortedByDescending {
            mostViewedVideosHashMap.getValue(it.id.toString())
        }
    }

    fun getMostViewedVideos(): List<Videos>?{
        return mostViewedVideos
    }

    fun calculateVideoStats(isPlaying: Boolean): Long {
        totalTime = playTime+pauseTime
        Log.e("onIsPlaying", "PLAYTIME: $playTime")
        Log.e("onIsPlaying", "PRESSEDPAUSE: $pressedPaused")
        Log.e("onIsPlaying", "PAUSETIME: $pauseTime")
        Log.e("onIsPlaying", "TOTALTIME: $totalTime")

        var totalVideoDuration = 0L
        totalVideoDuration = videosResponse.value?.videos?.get(currentVideo)?.duration!! * 1000L
        totalVideoDuration = playTime.div(totalVideoDuration) * 100
        Log.e("onIsPlaying", "DIVTIME: $totalVideoDuration")

        return totalVideoDuration
    }

    fun resetVideoStats() {
        playTime = 0L // in ms
        pauseTime = 0L // in ms
        totalTime = 0L // in ms
        pressedPaused = 0
    }

}