package com.duzi.duzisnstest

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progress_bar.visibility = View.VISIBLE

        bottom_navigation.setOnNavigationItemSelectedListener(this)
        bottom_navigation.selectedItemId = R.id.action_home

        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
    }

    fun setToolbarDefault() {
        toolbar_title_image.visibility = View.VISIBLE
        toolbar_btn_back.visibility = View.GONE
        toolbar_username.visibility = View.GONE
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        setToolbarDefault()
        when(item.itemId) {
            R.id.action_home -> {
                val detailViewFragment = DetailviewFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.main_content, detailViewFragment)
                    .commit()

                return true
            }
            R.id.action_search -> {
                val gridFragment = GridFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.main_content, gridFragment)
                    .commit()
                return true
            }
            R.id.action_add_photo -> {
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    startActivity(Intent(this, AddPhotoActivity::class.java))
                } else {
                    Toast.makeText(this, "저장소 권한이 없습니다", Toast.LENGTH_SHORT).show()
                }
                return true
            }
            R.id.action_favorite_alarm -> {
                val alarmFragment = AlarmFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.main_content, alarmFragment)
                    .commit()
                return true
            }
            R.id.action_account -> {
                val userFragment = UserFragment()
                val uid = FirebaseAuth.getInstance().currentUser!!.uid

                val bundle = Bundle()
                bundle.putString("destinationUid", uid)
                userFragment.arguments = bundle
                supportFragmentManager.beginTransaction()
                    .replace(R.id.main_content, userFragment)
                    .commit()
                return true
            }
        }
        return false
    }
}
