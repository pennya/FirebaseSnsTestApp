package com.duzi.duzisnstest

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.activity_login.*
import java.util.*

class LoginActivity : AppCompatActivity() {

    // firebase
    var auth: FirebaseAuth? = null

    // google login
    var googleSignInClient: GoogleSignInClient? = null

    // facebook login callback
    var callbackManager: CallbackManager? = null

    private val GOOGLE_LOGIN_CODE = 9001


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // firebase를 통합 관리하는 싱글톤 객체를 만든다
        auth = FirebaseAuth.getInstance()

        // 구글 로그인을 위한 옵션
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        // 구글 로그인 객체
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // 페이스북 로그인 관리 객체 생성
        callbackManager = CallbackManager.Factory.create()


        google_sign_in_button.setOnClickListener { googleLogin() }

        facebook_sign_in_button.setOnClickListener { facebookLogin() }

        email_login_button.setOnClickListener { emailLogin() }


    }

    private fun moveMainPage(user: FirebaseUser?) {
        if(user != null) {
            Toast.makeText(this, getString(R.string.signin_complete), Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun googleLogin() {
        progress_bar.visibility = View.VISIBLE
        val signInIntent = googleSignInClient?.signInIntent

        // 구글 로그인 관리 객체에서 구글 로그인 다이얼로그를 띄우고 콜백을 받는다
        startActivityForResult(signInIntent, GOOGLE_LOGIN_CODE)
    }

    private fun facebookLogin() {
        progress_bar.visibility = View.VISIBLE

        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile", "email"))
        LoginManager.getInstance().registerCallback(callbackManager, object: FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                handleFacebookAccessToken(result.accessToken)
            }

            override fun onCancel() {
                progress_bar.visibility = View.GONE
            }

            override fun onError(error: FacebookException?) {
                progress_bar.visibility = View.GONE
            }

        })
    }


    private fun emailLogin() {
        if(email_edittext.text.toString().isEmpty() ||
                password_edittext.text.toString().isEmpty()) {
            Toast.makeText(this, getString(R.string.signout_fail_null), Toast.LENGTH_SHORT).show()
        } else {
            progress_bar.visibility = View.VISIBLE
            createAndLoginEmail()
        }
    }

    private fun signinEmail() {
        auth?.signInWithEmailAndPassword(email_edittext.text.toString(), password_edittext.text.toString())
            ?.addOnCompleteListener { task ->
                progress_bar.visibility = View.GONE
                if(task.isSuccessful) {
                    moveMainPage(auth?.currentUser)
                } else {
                    Toast.makeText(this, task.exception!!.message, Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        // 페이스북 토큰으로 credential을 만들어서 firebase에 로그인한다.
        val credential = FacebookAuthProvider.getCredential(token.token)
        auth?.signInWithCredential(credential)?.addOnCompleteListener { task ->
            progress_bar.visibility = View.GONE
            if(task.isSuccessful) {
                moveMainPage(auth?.currentUser)
            }
        }
    }

    private fun createAndLoginEmail() {
        auth?.createUserWithEmailAndPassword(email_edittext.text.toString(),
            password_edittext.text.toString())?.addOnCompleteListener { task ->
            progress_bar.visibility = View.GONE
            if(task.isSuccessful) {
                Toast.makeText(this, getString(R.string.signup_complete), Toast.LENGTH_SHORT).show()
                moveMainPage(auth?.currentUser)
            } else if(task.exception?.message.isNullOrEmpty()) {
                Toast.makeText(this, task.exception!!.message, Toast.LENGTH_SHORT).show()
            } else {
                signinEmail()
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        // 구글 로그인 성공한 토큰으로 credential을 만들어서 firebase에 계정을 생성/로그인한다
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth?.signInWithCredential(credential)?.addOnCompleteListener { task ->
            progress_bar.visibility = View.GONE
            if(task.isSuccessful) {
                moveMainPage(auth?.currentUser)
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // 페이스북 로그인을 성공하면 페이스북 로그인 관리 객체로 결과값을 알려준다. (내부적으로 결과를 저장하고 있음)
        callbackManager?.onActivityResult(requestCode, resultCode, data)

        if(requestCode == GOOGLE_LOGIN_CODE) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if(result.isSuccess) {
                val account = result.signInAccount
                firebaseAuthWithGoogle(account!!)
            } else {
                progress_bar.visibility = View.GONE
            }
        }
    }

    override fun onStart() {
        super.onStart()

        //자동로그인
        moveMainPage(auth?.currentUser)
    }

}
