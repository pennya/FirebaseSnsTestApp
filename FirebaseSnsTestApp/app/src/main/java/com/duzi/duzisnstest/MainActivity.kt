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
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    private var auth: FirebaseAuth? = null
    private var authListener: FirebaseAuth.AuthStateListener? = null
    private var googleSignInClient: GoogleSignInClient? = null
    private var callbackManager: CallbackManager? = null
    private var firestore: FirebaseFirestore? = null

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



        // firestore
        firestore = FirebaseFirestore.getInstance()



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
            saveData()

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

        pullDriven.setOnClickListener {
            // Collection과 DocumentId를 이미 알고있는 경우
            // ex) 상세보기 페이지
            firestore?.collection("User")?.document(auth?.currentUser?.email!!)?.get()
                ?.addOnCompleteListener { task ->
                    if(task.isSuccessful) {
                        val user = task.result?.toObject(User::class.java)
                        println("${user?.address} ${user?.phoneNumber} 데이터 가져옴")
                    }
                }

            // query방식
            // 1. WhereEqualTo
            // sql where을 쓴것처럼 사용한다. 정확하게 일치하는 경우에만 검색된다.
            // LIKE 와 같은 검색은 안드로이드 내부에서 직접 구현해야함
            firestore?.collection("User")?.whereEqualTo("address", "SEOUL")?.get()
                ?.addOnCompleteListener { task ->
                    if(task.isSuccessful) {
                        for(ds in task.result?.documents!!) {
                            val user = ds.toObject(User::class.java)
                            println("${user?.address} ${user?.phoneNumber} 데이터 가져옴")
                        }
                    }
                }

            // 2. WhereGreaterThan
            // 입력된 값보다 초과되는 값만 검색된다
            firestore?.collection("User")?.whereGreaterThan("age", 25)?.get()
                ?.addOnCompleteListener { task ->
                    if(task.isSuccessful) {
                        for(ds in task.result?.documents!!) {
                            val user = ds.toObject(User::class.java)
                            println("${user?.address} ${user?.phoneNumber} 데이터 가져옴")
                        }
                    }
                }

            // 3. WhereGreaterThanOrEqualTo
            // 입력된 값 이상인 값들만 검색

            // 4. WhereLessThan
            // 입력된 값 미만 데이터 검색

            // 5. WhereLessThanOrEqualTo
            // 입력된 값 이하 데이터 검색
        }

        pushDriven.setOnClickListener {
            // 데이터가 변경될때마다 트리거 동작
            // ex) 채팅, 리스트 뷰 등

            // DocumentSnapshot
            // document id에 해당되는 document의 변화를 감지하는 것이 아닌 collection에 포함된 모든 document들을 감지하고있어서
            // document들중 한개라도 변경되면 모든 document를 다시 받는다. 과연 이 케이스는 언제 쓸수있을까? 예를들어 새로고침?
            //
            // DocumentChanges
            // document id에 해당되는 document만 감지한다
            //
            firestore?.collection("User")?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                val dcRef = querySnapshot?.documentChanges
                for((count, ds) in dcRef!!.withIndex()) {
                    val user = ds.document.toObject(User::class.java)
                    println("#$count ${user.address} ${user.phoneNumber}")
                }
            }

            firestore?.collection("User")?.whereEqualTo("address", "SEOUL")
                ?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    for(dc in querySnapshot?.documentChanges!!) {
                        val user = dc.document.toObject(User::class.java)
                        println("PullDriven WhereEqualTo ${user.address} ${user.phoneNumber}")
                    }
                }
        }
    }


    private fun saveData() {
        /**
         * runTransaction은 여러 사용자가 FireStore에 접근하여 데이터를 변경하려고할 때 현재 변경중일 경우 락을 걸어서 다른 사용자가 대기하도록 한다.
         * 중복쓰기방지를 위해 사용한다.  collection-document 객체를 받아서 transaction을 통해 data 객체를 얻은 뒤 값을 변경해서 저장한다.
         */

        val userRef = firestore?.collection("User")?.document(auth?.currentUser?.email!!)
        firestore?.runTransaction {  transaction ->
            val user = transaction.get(userRef!!).toObject(User::class.java)
            println("address : ${user?.address}  phoneNumber ${user?.phoneNumber}  age ${user?.age}")

            user?.phoneNumber = "01077771123"
            transaction.set(userRef, user!!)
        }

        val googleUserRef = firestore?.collection("User")?.document("rlawlgns077@gmail.com")
        firestore?.runTransaction { transition ->
            var googleUser = transition.get(googleUserRef!!).toObject(User::class.java)

            if(googleUser == null) {
                googleUser = User("SEOUL", "01077716565", 25)
            }

            transition.set(googleUserRef, googleUser) // set 해줘야 Firestore에 반영됨
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

                    val u = User("LA", null, 30)
                    firestore?.collection("User")?.document(auth?.currentUser?.email!!)?.set(u)
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
