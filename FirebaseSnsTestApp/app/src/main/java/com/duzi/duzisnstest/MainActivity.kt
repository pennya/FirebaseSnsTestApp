package com.duzi.duzisnstest

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private var auth: FirebaseAuth? = null
    private var authListener: FirebaseAuth.AuthStateListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        authListener = FirebaseAuth.AuthStateListener {
            val user = it.currentUser
            if(user != null) {
                println("${user.email} 로그인")
            } else {
                println("로그아웃 또는 로그인이 안됐을 때")
            }
        }

        signup.setOnClickListener {
            createUserId(email.text.toString(), password.text.toString())
        }

        login.setOnClickListener {
            loginUserId(email.text.toString(), password.text.toString())
        }

        status.setOnClickListener {
            println("${auth?.currentUser?.displayName} ${auth?.currentUser?.email} ${auth?.currentUser?.isEmailVerified} ")
        }

        verifyemail.setOnClickListener {
            verifyEmail()
        }

        delete.setOnClickListener {
            deleteId()
        }
    }

    override fun onStart() {
        super.onStart()
        auth?.addAuthStateListener(authListener!!)
    }

    override fun onPause() {
        super.onPause()
        auth?.removeAuthStateListener(authListener!!)
    }

    private fun createUserId(email: String, password: String) {
        auth?.createUserWithEmailAndPassword(email, password)
            ?.addOnCompleteListener { task ->
                if(task.isSuccessful) {
                    // id 생성이 완료
                    val user = auth?.currentUser
                    println(user)
                } else {
                    // id 생성이 실패
                    print(task.exception.toString())
                    task.exception?.printStackTrace()
                }
            }
    }

    private fun loginUserId(email: String, password: String) {
        auth?.signInWithEmailAndPassword(email, password)
            ?.addOnCompleteListener { task ->
                if(task.isSuccessful) {
                    println("로그인완료")
                } else {
                    println("로그인실패")
                    task.exception?.printStackTrace()
                }
            }
    }

    private fun verifyEmail() {
        auth?.currentUser?.sendEmailVerification()
            ?.addOnCompleteListener { task ->
                if(task.isSuccessful) {
                    println("이메일 유효성검사 성공")
                } else {
                    println("이메일 유효성검사 실패")
                    task.exception?.printStackTrace()
                }

            }
    }

    private fun deleteId() {
        auth?.currentUser?.delete()
            ?.addOnCompleteListener { task ->
                if(task.isSuccessful) {
                    println("앱 탈퇴 성공")
                } else {
                    println("앱 탈퇴 실패")
                    task.exception?.printStackTrace()
                }
            }
    }
}
