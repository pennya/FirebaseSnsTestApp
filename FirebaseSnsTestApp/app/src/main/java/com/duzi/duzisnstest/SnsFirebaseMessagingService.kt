package com.duzi.duzisnstest

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.lang.Exception

class SnsFirebaseMessagingService: FirebaseMessagingService() {

    override fun onNewToken(token: String?) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val map = mutableMapOf<String, Any>()
        map["pushtoken"] = token!!
        FirebaseFirestore.getInstance().collection("pushtokens").document(uid!!).set(map)
    }

    override fun onMessageReceived(p0: RemoteMessage?) {
        super.onMessageReceived(p0)
        println("onMessageReceived")
    }

    override fun onMessageSent(p0: String?) {
        super.onMessageSent(p0)
        println("onMessageSent")
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
        println("onDeletedMessages")
    }

    override fun onSendError(p0: String?, p1: Exception?) {
        super.onSendError(p0, p1)
        println("onSendError")
    }
}