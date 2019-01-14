package com.duzi.duzisnstest.navigation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import com.duzi.duzisnstest.R
import com.duzi.duzisnstest.model.ContentDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_add_photo.*
import java.text.SimpleDateFormat
import java.util.*

class AddPhotoActivity : AppCompatActivity() {

    val PICK_IMAGE_FROM_ALBUM = 0
    var photoUri: Uri? = null
    var storage: FirebaseStorage?= null
    var firestore: FirebaseFirestore? = null
    private var auth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_photo)

        storage = FirebaseStorage.getInstance()  // aws s3, ms의 azure와 같은 저장소
        firestore = FirebaseFirestore.getInstance() // db
        auth = FirebaseAuth.getInstance()


        // 열리자마자 사진 선택할 수 있게함
        val photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent, PICK_IMAGE_FROM_ALBUM)



        addphoto_image.setOnClickListener {
            startActivityForResult(photoPickerIntent, PICK_IMAGE_FROM_ALBUM)
        }

        addphoto_btn_upload.setOnClickListener { contentUpload() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == PICK_IMAGE_FROM_ALBUM) {
            if(resultCode == Activity.RESULT_OK) {
                println(data?.data)
                photoUri = data?.data
                addphoto_image.setImageURI(data?.data)
            } else {
                finish()
            }
        }
    }

    private fun contentUpload() {
        progress_bar.visibility = VISIBLE

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timestamp + "_.png"
        val storageRef = storage?.reference?.child("images")?.child(imageFileName)
        storageRef?.putFile(photoUri!!)?.addOnSuccessListener { taskSnapshot ->
            progress_bar.visibility = GONE

            Toast.makeText(this, getString(R.string.upload_success), Toast.LENGTH_SHORT).show()

            val downloadUrlTask = storageRef.downloadUrl
            downloadUrlTask.addOnCompleteListener { task ->
                if(task.isSuccessful) {
                    val url = task.result

                    val contentDTO = ContentDTO()
                    contentDTO.imageUrl = url.toString()
                    contentDTO.uid = auth?.currentUser?.uid
                    contentDTO.explain = addphoto_edit_explain.text.toString()
                    contentDTO.userId = auth?.currentUser?.email
                    contentDTO.timestamp = System.currentTimeMillis()

                    firestore?.collection("images")?.document()?.set(contentDTO)?.addOnSuccessListener {
                        setResult(Activity.RESULT_OK)
                        finish()
                    }?.addOnFailureListener {
                        progress_bar.visibility = GONE
                        it.printStackTrace()
                        finish()
                    }
                }
            }

        }?.addOnFailureListener {
            progress_bar.visibility = GONE
            it.printStackTrace()
            finish()
        }
    }
}
