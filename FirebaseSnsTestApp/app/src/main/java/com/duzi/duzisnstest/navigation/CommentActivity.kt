package com.duzi.duzisnstest.navigation

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.duzi.duzisnstest.R
import com.duzi.duzisnstest.model.AlarmDTO
import com.duzi.duzisnstest.model.ContentDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.item_comment.view.*

class CommentActivity : AppCompatActivity() {

    var contentUid: String? = null
    var user: FirebaseUser? = null
    var destinationUid: String? = null
    var commentSnapshot: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment)

        user = FirebaseAuth.getInstance().currentUser
        destinationUid = intent.getStringExtra("destinationUid")
        contentUid = intent.getStringExtra("contentUid")

        comment_btn_send.setOnClickListener {
            val comment = ContentDTO.Comment()
            comment.userId = user!!.email
            comment.comment = comment_edit_message.text.toString()
            comment.uid = user!!.uid
            comment.timestamp = System.currentTimeMillis()

            FirebaseFirestore.getInstance()
                .collection("images")
                .document(contentUid!!)
                .collection("comments")
                .document()
                .set(comment)

            commentAlarm(destinationUid!!, comment_edit_message.text.toString())
            comment_edit_message.setText("")
        }

        comment_recyclerview.adapter = CommentRecyclerAdapter()
        comment_recyclerview.layoutManager = LinearLayoutManager(this)
    }

    override fun onStop() {
        super.onStop()
        commentSnapshot?.remove()
    }

    private fun commentAlarm(destinationUid: String, message: String) {
        val alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = user?.email
        alarmDTO.uid = user?.uid
        alarmDTO.kind = 1 // 댓글 알림
        alarmDTO.message = message
        alarmDTO.timestamp = System.currentTimeMillis()

        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)
    }

    inner class CommentRecyclerAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        val comments = arrayListOf<ContentDTO.Comment>()
        init {
            commentSnapshot = FirebaseFirestore.getInstance()
                .collection("images")
                .document(contentUid!!)
                .collection("comments")
                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    comments.clear()
                    if(querySnapshot == null) return@addSnapshotListener
                    for(snapshot in querySnapshot.documents) {
                        comments.add(snapshot.toObject(ContentDTO.Comment::class.java)!!)
                    }
                    notifyDataSetChanged()
                }
        }

        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecyclerView.ViewHolder {
            return CustomViewHolder(LayoutInflater.from(p0.context).inflate(R.layout.item_comment, p0, false))
        }

        override fun getItemCount(): Int {
            return comments.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val view = holder.itemView

            FirebaseFirestore.getInstance()
                .collection("profileImages")
                .document(comments[position].uid!!)
                .addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                    if(documentSnapshot?.data != null) {
                        val url = documentSnapshot.data!!["image"] as String
                        Glide.with(view.context)
                            .load(url)
                            .apply(RequestOptions().circleCrop())
                            .into(view.commentviewitem_imageview_profile)
                    }
                }


            view.commentviewitem_textview_profile.text = comments[position].userId
            view.commentviewitem_textview_comment.text = comments[position].comment
        }

    }

    inner class CustomViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)
}
