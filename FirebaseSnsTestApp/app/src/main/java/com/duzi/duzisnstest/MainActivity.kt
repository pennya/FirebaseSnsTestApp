package com.duzi.duzisnstest

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.facebook.*
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    private var auth: FirebaseAuth? = null
    private var authListener: FirebaseAuth.AuthStateListener? = null
    private var googleSignInClient: GoogleSignInClient? = null
    private var callbackManager: CallbackManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // firebase
        auth = FirebaseAuth.getInstance()
        authListener = FirebaseAuth.AuthStateListener {
            val user = it.currentUser
            if(user != null) {
                println("${user.email} 로그인")
            } else {
                println("로그아웃 또는 로그인이 안됐을 때")
            }
        }


        // google
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)





        // facebook
        if (BuildConfig.DEBUG) {
            FacebookSdk.setIsDebugEnabled(true)
        }
        callbackManager = CallbackManager.Factory.create();




        // ui..
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

        googleLogin.setOnClickListener {
            googleSignIn()
        }

        fbLogin.setOnClickListener {
            facebookSignIn()
        }

        twLogin.setOnClickListener {
            twitterSignIn()
        }
    }

    private fun twitterSignIn() {

    }

    private fun facebookSignIn() {
        val facebookLoginManager = LoginManager.getInstance()
        facebookLoginManager.logInWithReadPermissions(this, Arrays.asList("public_profile", "email"))
        facebookLoginManager.registerCallback(callbackManager, object: FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                handleFacebookToken(result.accessToken)
            }

            override fun onCancel() {
                // 페이스북 로그인 취소
            }

            override fun onError(error: FacebookException) {
                //페이스북 로그인 실패
                error.printStackTrace()
            }

        })
    }

    private fun handleFacebookToken(accessToken: AccessToken) {
        val credential = FacebookAuthProvider.getCredential(accessToken.token)
        auth?.signInWithCredential(credential)
            ?.addOnCompleteListener { task ->
                if(task.isSuccessful) {
                    println("페이스북 계정 등록 완료")
                } else {
                    task.exception?.printStackTrace()
                }
            }
    }

    private fun googleSignIn() {
        googleSignInClient?.signInIntent?.let {
            startActivityForResult(it, 100)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // 로그인 결과를 callbackManager를 통해 LoginManager로 전달한다
        callbackManager?.onActivityResult(requestCode, resultCode, data)

        if(requestCode == 100) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if(result.isSuccess) {
                val account = result.signInAccount
                val credential = GoogleAuthProvider.getCredential(account?.idToken, null) // idToken은 암호화되있으므로 Firebase로 넘겨줘서 이메일 등록한다.
                createUserWithCredential(credential)
            }
        }
    }


    private fun createUserWithCredential(credential: AuthCredential) {
        auth?.signInWithCredential(credential) // firebase 등록과 동시에 로그인된다.
            ?.addOnCompleteListener { task ->
                if(task.isSuccessful) {
                    println("구글 계정 등록 완료")
                } else {
                    task.exception?.printStackTrace()
                }
            }
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

    override fun onStart() {
        super.onStart()
        auth?.addAuthStateListener(authListener!!)
    }

    override fun onPause() {
        super.onPause()
        auth?.removeAuthStateListener(authListener!!)
    }
}
