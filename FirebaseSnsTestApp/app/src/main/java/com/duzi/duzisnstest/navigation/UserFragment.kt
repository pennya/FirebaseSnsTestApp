package com.duzi.duzisnstest.navigation


import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.duzi.duzisnstest.FCMPush
import com.duzi.duzisnstest.LoginActivity
import com.duzi.duzisnstest.MainActivity
import com.duzi.duzisnstest.R
import com.duzi.duzisnstest.model.AlarmDTO
import com.duzi.duzisnstest.model.ContentDTO
import com.duzi.duzisnstest.model.FollowDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_user.*


class UserFragment : Fragment() {

    val PICK_PROFILE_FROM_ALBUM = 10
    lateinit var auth: FirebaseAuth
    lateinit var firestore: FirebaseFirestore

    var uid: String? = null
    var currentUserUid: String? = null
    var fcmPush: FCMPush? = null

    var followerListenerRegistration: ListenerRegistration? = null
    var followingListenerRegistration: ListenerRegistration? = null
    var imageprofileListenerRegistration: ListenerRegistration? = null
    var recyclerListenerRegistration: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_user, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        fcmPush = FCMPush()

        // 로그인된 사용자 uid
        currentUserUid = auth.currentUser?.uid
        if(arguments != null) {
            // 상세보기할려고하는 유저의 uid
            uid = arguments?.getString("destinationUid")

            // 본인의 계정일 경우
            if(uid != null && uid == currentUserUid) {
                account_btn_follow_signout.text = getString(R.string.signout)
                account_btn_follow_signout.setOnClickListener {
                    activity?.finish()
                    startActivity(Intent(activity, LoginActivity::class.java))
                    auth.signOut()
                }
            } else {
                // 일단 내가 팔로우했는지 안했는지 상관없이 팔로우 출력
                account_btn_follow_signout.text = getString(R.string.follow)
                account_btn_follow_signout.setOnClickListener {
                    requestFollow()
                }

                val mainActivity = activity as MainActivity
                mainActivity.toolbar_title_image.visibility = GONE
                mainActivity.toolbar_btn_back.visibility = VISIBLE
                mainActivity.toolbar_username.visibility = VISIBLE
                mainActivity.toolbar_username.text = arguments?.getString("userId")
                mainActivity.toolbar_btn_back.setOnClickListener {
                    mainActivity.bottom_navigation.selectedItemId = R.id.action_home
                }
            }
        }


        account_iv_profile.setOnClickListener {
            activity?.let { activity ->
                if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    val photoPickerIntent = Intent(Intent.ACTION_PICK)
                    photoPickerIntent.type = "image/*"
                    activity.startActivityForResult(photoPickerIntent, PICK_PROFILE_FROM_ALBUM)
                }

            }
        }

        account_recyclerview.layoutManager = GridLayoutManager(activity, 3)
        account_recyclerview.adapter = UserFragmentRecyclerViewAdapter()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == PICK_PROFILE_FROM_ALBUM) {
            if(resultCode == Activity.RESULT_OK) {
                println(data?.data)

                data?.data?.let {
                    val photoUri = it

                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                    val storageRef = FirebaseStorage.getInstance().reference.child("userProfileImages").child(uid.toString())
                    storageRef.putFile(photoUri).addOnCompleteListener { taskSnapshot ->
                        val downloadUrlTask = storageRef.downloadUrl
                        downloadUrlTask.addOnCompleteListener { task ->
                            val url = task.result.toString()
                            val map = HashMap<String, Any>()
                            map["image"] = url
                            FirebaseFirestore.getInstance().collection("profileImages")
                                .document(uid.toString()).set(map)

                            activity?.let { activity ->
                                Glide.with(activity)
                                    .load(url)
                                    .apply(RequestOptions().centerCrop())
                                    .into(account_iv_profile)
                            }
                        }
                    }

                }


            } else {
                activity?.finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        getProfileImage()
        getFollowing()  // "내가" 팔로우하고있는 사람
        getFollower()   // "나를" 팔로우하고있는 사람
    }

    override fun onStop() {
        super.onStop()
        followerListenerRegistration?.remove()
        followingListenerRegistration?.remove()
        imageprofileListenerRegistration?.remove()
        recyclerListenerRegistration?.remove()
    }

    private fun getProfileImage() {
        imageprofileListenerRegistration = firestore.collection("profileImages").document(uid.toString())
            .addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                if(documentSnapshot?.data != null) {
                    documentSnapshot.data?.let { map ->
                        val url = map["image"]
                        activity?.let {
                            Glide.with(it).load(url)
                                .apply(RequestOptions().centerCrop())
                                .into(account_iv_profile)
                        }
                    }

                }
            }
    }


    private fun getFollowing() {
        // 상세보기할려고하는 유저의 uid
        followingListenerRegistration = firestore.collection("users").document(uid.toString())
            .addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                val followDTO = documentSnapshot?.toObject(FollowDTO::class.java) ?: return@addSnapshotListener
                account_tv_following_count.text = followDTO.followingCount.toString()
            }
    }


    private fun getFollower() {
        // 상세보기할려고하는 유저의 uid
        followerListenerRegistration = firestore.collection("users").document(uid.toString())
            .addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                val followDTO = documentSnapshot?.toObject(FollowDTO::class.java) ?: return@addSnapshotListener
                account_tv_follower_count.text = followDTO.followerCount.toString()

                // 상대 유저입장에서 팔로워 목록에 내가있으면 내가 그 사람을 팔로잉하는 중
                if(followDTO.followers.containsKey(currentUserUid)) {
                    account_btn_follow_signout.text = getString(R.string.follow_cancle)

                    activity?.let { activity ->
                        account_btn_follow_signout.background.setColorFilter(ContextCompat.getColor(activity, R.color.colorLightGray),
                            PorterDuff.Mode.MULTIPLY)
                    }
                } else {
                    if(uid != currentUserUid) {
                        account_btn_follow_signout.text = getString(R.string.follow)
                        account_btn_follow_signout.background.colorFilter = null
                    }
                }
            }
    }


    private fun requestFollow() {
        // 로그인된 사용자 uid로 document 생성
        val tsDocFollowing = firestore.collection("users").document(currentUserUid.toString())
        firestore.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollowing).toObject(FollowDTO::class.java)
            // 만들어진게 없다면 지금 보고있는 사용자를 팔로잉한다
            if(followDTO == null) {
                followDTO = FollowDTO()
                followDTO.run {
                    followingCount = 1
                    followings[uid.toString()] = true
                    transaction.set(tsDocFollowing, this)
                }

                return@runTransaction
            }

            // 만들어진게 있었다면
            followDTO.run {
                if(followings.containsKey(uid.toString())) {
                    followingCount -= 1
                    followings.remove(uid.toString())
                } else {
                    followingCount += 1
                    followings[uid.toString()] = true
                    followerAlarm(uid!!)
                }
                transaction.set(tsDocFollowing, this)
                return@runTransaction
            }

        }

        // 보고있는 유저의 uid로 접근
        val tsDocFollower = firestore.collection("users").document(uid.toString())
        firestore.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollower).toObject(FollowDTO::class.java)
            if(followDTO == null) {
                followDTO = FollowDTO()
                followDTO?.run {
                    followerCount = 1
                    followers[currentUserUid.toString()] = true
                    transaction.set(tsDocFollower, this)
                    return@runTransaction
                }
            }

            followDTO?.run {
                if(followers.containsKey(uid.toString())) {
                    followerCount -= 1
                    followers.remove(uid.toString())
                } else {
                    followerCount += 1
                    followers[uid.toString()] = true
                }
                transaction.set(tsDocFollower, this)
                return@runTransaction
            }
        }
    }

    private fun followerAlarm(destinationUid: String) {
        val alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = auth.currentUser?.email
        alarmDTO.uid = auth.currentUser?.uid
        alarmDTO.kind = 2 // 팔로워알림
        alarmDTO.timestamp = System.currentTimeMillis()

        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)
        val message = auth.currentUser!!.email + getString(R.string.alarm_favorite)
        fcmPush?.sendMessage(destinationUid, "알림 메시지 입니다", message)
    }





    inner class UserFragmentRecyclerViewAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        val contentDTOs: ArrayList<ContentDTO> = arrayListOf()

        init {
            recyclerListenerRegistration = FirebaseFirestore.getInstance().collection("images").whereEqualTo("uid", uid)
                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    contentDTOs.clear()

                    if(querySnapshot == null ) return@addSnapshotListener
                    for(snapshot in querySnapshot.documents) {
                        snapshot.toObject(ContentDTO::class.java)?.let {
                            contentDTOs.add(it)
                        }
                    }

                    account_tv_post_count.text = contentDTOs.size.toString()
                    notifyDataSetChanged()
                }
        }

        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecyclerView.ViewHolder {
            // 현재 뷰 화면 가로 크기의 1/3 값을 가지고 온다
            val width = resources.displayMetrics.widthPixels / 3

            val imageView = ImageView(p0.context)
            imageView.layoutParams = LinearLayoutCompat.LayoutParams(width, width)

            return CustomViewHolder(imageView)
        }

        override fun getItemCount(): Int = contentDTOs.size

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val imageView = (holder as CustomViewHolder).imageView

            Glide.with(holder.itemView.context)
                .load(contentDTOs[position].imageUrl)
                .apply(RequestOptions().centerCrop())
                .into(imageView)
        }

    }


    inner class CustomViewHolder(var imageView: ImageView): RecyclerView.ViewHolder(imageView)
}
