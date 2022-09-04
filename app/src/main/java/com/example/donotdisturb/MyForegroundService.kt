package com.example.donotdisturb

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (listener != null) {
            listener!!.remove()
        }

        scope.coroutineContext.cancelChildren()

        startListening()
        return START_REDELIVER_INTENT
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startListening() {
        FirebaseApp.initializeApp(this)
        FirebaseApp.getInstance()

        scope.launch(Dispatchers.IO) {
            val fireStore = Firebase.firestore

            val sharedPreferences =
                this@MyForegroundService.getSharedPreferences("DoNotDisturb", Context.MODE_PRIVATE)
            val observerId = sharedPreferences.getLong("observerId", -1)
            if (observerId == -1L) return@launch

            listener = fireStore.collection("users")
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
                        startForeground(1, getNotification(status.toString(), observerId.toString()))
                    } else {
                        Toast.makeText(
                            this@MyForegroundService,
                            "Observer not found",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getNotification(status: String, observerId : String): Notification {
        val channel = NotificationChannel(
            "MyChannelId",
            "DND",
            NotificationManager.IMPORTANCE_DEFAULT
        )

        val manager = (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
        manager.createNotificationChannel(channel)

        val notificationBuilder = NotificationCompat.Builder(
            this, "MyChannelId"
        )

        return notificationBuilder
            .setSmallIcon(IconCompat.createWithResource(this, R.drawable.ic_launcher_foreground))
            .setContentTitle("STATUS ($observerId) : $status ")
            .setPriority(NotificationManager.IMPORTANCE_DEFAULT)
            .setChannelId("MyChannelId")
            .build()
    }

}