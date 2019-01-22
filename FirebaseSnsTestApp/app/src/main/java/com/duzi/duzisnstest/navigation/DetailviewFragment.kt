package com.duzi.duzisnstest.navigation


import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.duzi.duzisnstest.FCMPush
import com.duzi.duzisnstest.MainActivity
import com.duzi.duzisnstest.R
import com.duzi.duzisnstest.model.AlarmDTO
import com.duzi.duzisnstest.model.ContentDTO
import com.duzi.duzisnstest.model.FollowDTO
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_detailview.*
import kotlinx.android.synthetic.main.item_detail.view.*


class DetailviewFragment : Fragment() {

    var user: FirebaseUser? = null
    var firestore: FirebaseFirestore? = null
    var imagesSnapshot: ListenerRegistration? = null
    var fcmPush: FCMPush? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        user = FirebaseAuth.getInstance().currentUser
        firestore = FirebaseFirestore.getInstance()
        fcmPush = FCMPush()
        return inflater.inflate(R.layout.fragment_detailview, container, false)
    }

    override fun onResume() {
        super.onResume()
        detailviewfragment_recyclerview.layoutManager = LinearLayoutManager(activity)
        detailviewfragment_recyclerview.adapter = DetailRecyclerViewAdapter()

        val mainActivity = activity as MainActivity
        mainActivity.progress_bar.visibility = View.INVISIBLE
    }

    override fun onStop() {
        super.onStop()
        imagesSnapshot?.remove()
    }

    inner class DetailRecyclerViewAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val contentDTOs: ArrayList<ContentDTO> = ArrayList()
        private val contentUidList: ArrayList<String> = ArrayList()

        init {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            println("uid : $uid")

            // pull driven 방식으로 users 컬렉션에서 나의 uid에 해당하는 문서를 찾아온다.
            firestore?.collection("users")?.document(uid!!)?.get()
                ?.addOnCompleteListener { task ->
                    if(task.isSuccessful) {
                        val userDTO = task.result?.toObject(FollowDTO::class.java)
                        if(userDTO?.followings != null) {
                            // 내가 구독하고있는 사람들의 리스트를 보여준다
                           getContents(userDTO.followings)
                        }
                    }
                }
        }

        private fun getContents(followers: MutableMap<String, Boolean>?) {
            // images 컬렉션에서 timestamp 순으로 정렬하여 보여준다.
            imagesSnapshot = firestore?.collection("images")?.orderBy("timestamp")
                ?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    contentDTOs.clear()
                    contentUidList.clear()
                    if(querySnapshot == null) return@addSnapshotListener

                    // 모든 글을 가져와서 내가 팔로잉한 사람들의 글을 보여준다.
                    for(snapshot in querySnapshot.documents) {
                        val item = snapshot.toObject(ContentDTO::class.java)!!
                        println(item.uid)
                        if(followers?.keys?.contains(item.uid)!!) {
                            contentDTOs.add(item)
                            contentUidList.add(snapshot.id)
                        }
                    }
                    notifyDataSetChanged()
                }
        }

        private fun favoriteAlarm(destinationUid: String) {
            val alarmDTO = AlarmDTO()
            alarmDTO.destinationUid = destinationUid
            alarmDTO.userId = user?.email
            alarmDTO.uid = user?.uid

            alarmDTO.kind = 0  // 좋아요
            alarmDTO.timestamp = System.currentTimeMillis()

            FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

            val message = user?.email + getString(R.string.alarm_favorite)
            fcmPush?.sendMessage(destinationUid, "알림 메시지 입니다", message)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_detail, parent, false)
            return CustomViewHolder(view)
        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val viewHolder = (holder as CustomViewHolder).itemView

            // 회원가입을 하면서 프로필 등록하면 profileImages 컬렉션에 저장이 됨.
            // profileImages 컬렉션에 저장된 유저의 이미지를 가져온다
            firestore?.collection("profileImages")?.document(contentDTOs[position].uid!!)
                ?.get()?.addOnCompleteListener { task ->
                    if(task.isSuccessful) {
                        val url = task.result!!["image"]
                        Glide.with(holder.itemView.context)
                            .load(url)
                            .apply(RequestOptions().circleCrop())
                            .into(viewHolder.detailviewitem_profile_image)
                    }
                }

            // 프로필 이미지를 클릭하면 유저 정보 페이지로 이동한다
            viewHolder.detailviewitem_profile_image.setOnClickListener {
                val fragment = UserFragment()
                val bundle = Bundle()

                bundle.putString("destinationUid", contentDTOs[position].uid)
                bundle.putString("userId", contentDTOs[position].userId)

                fragment.arguments = bundle
                activity!!.supportFragmentManager.beginTransaction()
                    .replace(R.id.main_content, fragment)
                    .commit()
            }

            viewHolder.detailviewitem_profile_textview.text = contentDTOs[position].userId
            Glide.with(holder.itemView.context)
                .load(contentDTOs[position].imageUrl)
                .into(viewHolder.detailviewitem_imageview_content)

            viewHolder.detailviewitem_explain_textview.text = contentDTOs[position].explain
            viewHolder.detailviewitem_favorite_imageview.setOnClickListener {
                favoriteEvent(position)
            }



            if(contentDTOs[position].favorites.containsKey(FirebaseAuth.getInstance().currentUser!!.uid)) {
                viewHolder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite)
            } else {
                viewHolder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite_border)
            }

            viewHolder.detailviewitem_favoritecounter_textview.text = "좋아요 ${contentDTOs[position].favoriteCount} 개"

            viewHolder.detailviewitem_comment_imageview.setOnClickListener {
                val intent = Intent(activity, CommentActivity::class.java)
                intent.putExtra("contentUid", contentUidList[position])
                intent.putExtra("destinationUid", contentDTOs[position].uid)
                startActivity(intent)
            }
        }

        private fun favoriteEvent(position: Int) {
            val tsDoc = firestore?.collection("images")?.document(contentUidList[position])
            firestore?.runTransaction { transaction ->
                val uid = FirebaseAuth.getInstance().currentUser!!.uid
                val contentDTO = transaction.get(tsDoc!!).toObject(ContentDTO::class.java)

                // 글에서 유저가 좋아요 누른것을 map으로 관리
                if(contentDTO!!.favorites.containsKey(uid)) {
                    contentDTO.favoriteCount -= 1
                    contentDTO.favorites.remove(uid)

                    contentDTOs[position].favoriteCount = contentDTOs[position].favoriteCount - 1
                    contentDTOs[position].favorites.remove(uid)
                } else {
                    contentDTO.favoriteCount += 1
                    contentDTO.favorites[uid] = true

                    contentDTOs[position].favoriteCount = contentDTOs[position].favoriteCount + 1
                    contentDTOs[position].favorites[uid] = true
                    favoriteAlarm(contentDTOs[position].uid!!)
                }
                transaction.set(tsDoc, contentDTO)
            }?.addOnSuccessListener {
                notifyItemChanged(position)
            }
        }

    }

    inner class CustomViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)
}
