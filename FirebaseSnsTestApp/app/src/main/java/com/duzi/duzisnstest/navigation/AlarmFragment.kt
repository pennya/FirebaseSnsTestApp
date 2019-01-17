package com.duzi.duzisnstest.navigation


import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.duzi.duzisnstest.R
import com.duzi.duzisnstest.model.AlarmDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_alarm.*
import kotlinx.android.synthetic.main.item_comment.view.*

class AlarmFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_alarm, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        alarmfragment_recyclerview.adapter = AlarmRecyclerViewAdapter()
        alarmfragment_recyclerview.layoutManager = LinearLayoutManager(activity)
    }


    inner class AlarmRecyclerViewAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        val alarmDTOs = arrayListOf<AlarmDTO>()
        init {
            val uid = FirebaseAuth.getInstance().currentUser!!.uid
            FirebaseFirestore.getInstance()
                .collection("alarms")
                .whereEqualTo("destinationUid", uid)
                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    alarmDTOs.clear()
                    if(querySnapshot == null) return@addSnapshotListener
                    for(snapshot in querySnapshot.documents) {
                        alarmDTOs.add(snapshot.toObject(AlarmDTO::class.java)!!)
                    }
                    alarmDTOs.sortByDescending { it.timestamp }
                    notifyDataSetChanged()
                }
        }

        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecyclerView.ViewHolder {
            return CustomViewHolder(LayoutInflater.from(p0.context).inflate(R.layout.item_comment, p0, false))
        }

        override fun getItemCount(): Int {
            return alarmDTOs.size
        }

        override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {
            val profileImage = p0.itemView.commentviewitem_imageview_profile
            val commentTextView = p0.itemView.commentviewitem_textview_comment

            FirebaseFirestore.getInstance().collection("profileImages")
                .document(alarmDTOs[p1].uid!!).get().addOnCompleteListener { task ->
                    if(task.isSuccessful) {
                        val url = task.result!!["image"] as String
                        Glide.with(activity!!)
                            .load(url)
                            .apply(RequestOptions().circleCrop())
                            .into(profileImage)
                    }
                }

            when(alarmDTOs[p1].kind) {
                0 -> {
                    val str_0 = alarmDTOs[p1].userId + getString(R.string.alarm_favorite)
                    commentTextView.text = str_0
                }
                1 -> {
                    val str_1 = alarmDTOs[p1].userId + getString(R.string.alarm_who) + alarmDTOs[p1].message +
                            getString(R.string.alarm_comment)
                    commentTextView.text = str_1
                }
                2 ->  {
                    val str_2 = alarmDTOs[p1].userId + getString(R.string.alarm_follow)
                    commentTextView.text = str_2
                }
            }

        }

    }

    inner class CustomViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)
}
