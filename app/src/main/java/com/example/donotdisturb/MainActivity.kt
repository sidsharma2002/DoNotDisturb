package com.example.donotdisturb

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var tvUserId: TextView
    private lateinit var btnStatus: Button
    private lateinit var etObserver: EditText

    private var userId: Long? = null
    private val scope = CoroutineScope(Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        setupViews()
        setupUserId()
        setOnClickListener()
        setTextWatchListener()
    }

    private fun setTextWatchListener() {
        etObserver.setOnEditorActionListener { textView, i, keyEvent ->
            if (i == EditorInfo.IME_ACTION_DONE) {
                addObserverAndStartListening(textView.text.toString())
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
    }

    private fun addObserverAndStartListening(id: String) {
        try {
            setObserverId(java.lang.Long.parseLong(id)) // TODO : toLong() was not found
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "some error occurred", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setOnClickListener() {
        btnStatus.setOnClickListener {
            if (userId == null) {
                setupUserId()
                return@setOnClickListener
            }

            if (btnStatus.text != Keys.SET_TO_BUSY) {
                btnStatus.text = Keys.SET_TO_BUSY
                setUserIdToFirestore(userId!!, "BUSY")
            } else {
                btnStatus.text = Keys.SET_TO_AVAIL
                setUserIdToFirestore(userId!!, "AVAIL")
            }
        }
    }

    private fun setupViews() {
        tvUserId = findViewById(R.id.tv_userId)
        btnStatus = findViewById(R.id.btn_status)
        etObserver = findViewById(R.id.et_observer)
    }

    @SuppressLint("SetTextI18n")
    private fun setupUserId() {
        val sharedPreferences = this.getSharedPreferences("DoNotDisturb", Context.MODE_PRIVATE)
        userId = sharedPreferences.getLong("userId", (Math.random() * 100000).toLong())
        sharedPreferences.edit {
            this.putLong("userId", userId!!)
            this.commit()
        }
        tvUserId.text = "Your observer id : " + userId.toString()
    }

    private fun setObserverId(oid: Long) {
        val sharedPreferences = this.getSharedPreferences("DoNotDisturb", Context.MODE_PRIVATE)
        sharedPreferences.edit {
            this.putLong("observerId", oid)
            this.commit()
        }
    }

    private fun setUserIdToFirestore(userId: Long, status: String) {
        scope.launch(Dispatchers.IO) {

            val map: MutableMap<String, String> = HashMap()
            map.put("id", userId.toString())
            map.put("status", status)

            FirebaseApp.initializeApp(this@MainActivity)
            FirebaseApp.getInstance()

            val fireStore = Firebase.firestore
            fireStore.collection("users")
                .document(userId.toString())
                .set(map)
                .addOnSuccessListener {
                    Log.d("MainAct", "success")
                }.addOnFailureListener {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, it.localizedMessage, Toast.LENGTH_SHORT)
                            .show()
                    }
                }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        scope.coroutineContext.cancelChildren()
    }
}