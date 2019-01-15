package com.duzi.duzisnstest.navigation


import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.duzi.duzisnstest.R
import com.duzi.duzisnstest.model.ContentDTO
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.android.synthetic.main.fragment_grid.*


class GridFragment : Fragment() {

    lateinit var imagesSnapshot: ListenerRegistration

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_grid, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        gridfragment_recyclerview.adapter = GridFragmentRecyclerViewAdapter()
        gridfragment_recyclerview.layoutManager = GridLayoutManager(activity, 3)
    }

    override fun onStop() {
        super.onStop()
        if(::imagesSnapshot.isInitialized) imagesSnapshot.remove()
    }

    inner class GridFragmentRecyclerViewAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        val contentDTOs: ArrayList<ContentDTO> = arrayListOf()

        init {
            imagesSnapshot = FirebaseFirestore.getInstance().collection("images").orderBy("timestamp")
                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    contentDTOs.clear()

                    if(querySnapshot == null ) return@addSnapshotListener
                    for(snapshot in querySnapshot.documents) {
                        snapshot.toObject(ContentDTO::class.java)?.let {
                            contentDTOs.add(it)
                        }
                    }
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
