package com.duzi.duzisnstest

import com.duzi.duzisnstest.model.PushDTO
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import okhttp3.*
import java.io.IOException

class FCMPush {
    val JSON = MediaType.parse("application/json; charset=utf-8")
    val url = "https://fcm.googleapis.com/fcm/send"
    val serverKey = "AAAAtxACDmY:APA91bETSDPip8LWQWDsPd-Ce6HdvcmVRRjTLNUsCCLNT9suLG2uIEKAld_sLTzTBFkdaHKrrps6We14CL77vDjU2ZNuvYvpgFxvU0itsA_gwm5L8AUL37Npw1TCpzkZ2ttCBaaa02TK"

    var okHttpClient: OkHttpClient? = null
    var gson: Gson? = null

    init {
        gson = Gson()
        okHttpClient = OkHttpClient()
    }

    fun sendMessage(destinationUid: String, title: String, message: String) {
        FirebaseFirestore.getInstance().collection("pushtokens").document(destinationUid)
            .get().addOnCompleteListener { task ->
                if(task.isSuccessful) {
                    val token = task.result!!["pushtoken"].toString()

                    val pushDTO = PushDTO()
                    pushDTO.to = token
                    pushDTO.notification?.title = title
                    pushDTO.notification?.body = message

                    val body = RequestBody.create(JSON, gson!!.toJson(pushDTO))
                    val request = Request.Builder()
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Authorization", "key=$serverKey")
                        .url(url)
                        .post(body)
                        .build()

                    okHttpClient?.newCall(request)?.enqueue(object: Callback {
                        override fun onFailure(call: Call, e: IOException) {

                        }

                        override fun onResponse(call: Call, response: Response) {
                            println(response.body()?.string())
                        }

                    })
                }
            }
    }

}