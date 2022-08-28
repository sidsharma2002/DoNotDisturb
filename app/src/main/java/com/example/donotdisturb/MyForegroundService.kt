package com.example.donotdisturb

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*


class MyForegroundService : Service() {

    private var listener: ListenerRegistration? = null
    private val scope: CoroutineScope = CoroutineScope(Job())

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (listener != null) {
            listener!!.remove()
        }

        scope.coroutineContext.cancelChildren()

        startListening()
        return START_REDELIVER_INTENT
    }

    private fun startListening() {
        FirebaseApp.initializeApp(this)
        FirebaseApp.getInstance()

        scope.launch(Dispatchers.IO) {
            val fireStore = Firebase.firestore

            val sharedPreferences =
                this@MyForegroundService.getSharedPreferences("DoNotDisturb", Context.MODE_PRIVATE)
            val observerId = sharedPreferences.getLong("observerId", -1)
            if (observerId == -1L) return@launch

            fireStore.collection("users")
                .document(observerId.toString())
                .addSnapshotListener { value, error ->

                    if (error != null) {
                        Toast.makeText(
                            this@MyForegroundService,
                            error.localizedMessage,
                            Toast.LENGTH_SHORT
                        ).show()

                        return@addSnapshotListener
                    }

                    Log.d("MyDNDService ", value.toString())
                    val status = value?.data?.get("status")
                    if (status != null) {
                        stopForeground(true)
                        startForeground(101, getNotification(status.toString()))
                    }
                }
        }
    }

    private fun getNotification(status: String): Notification {
        return NotificationCompat.Builder(this, "donotdisturb")
            .setContentTitle("STATUS : $status")
            .setPriority(Notification.PRIORITY_HIGH)
            .build()
    }

}