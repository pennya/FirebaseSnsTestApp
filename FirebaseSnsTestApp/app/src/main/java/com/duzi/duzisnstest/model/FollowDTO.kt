package com.duzi.duzisnstest.model

/**
 * followers    내가 따라간 사람
 * followings   나를 따라온 사람
 */
data class FollowDTO(var followerCount: Int = 0,
                     var followers : MutableMap<String, Boolean> = HashMap(),
                     var followingCount : Int = 0,
                     var followings: MutableMap<String, Boolean> = HashMap())