package edu.washington.awty

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.telephony.PhoneNumberUtils
import android.telephony.SmsManager
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    var nagMessage = "(425) 555-1212: Are we there yet?"
    var sendEnabled = false
    inner class IntentListener : BroadcastReceiver() {
        init {
            Log.i("IntentListener", "The current msg is " + nagMessage)
            sendTextMsg()
        }
        override fun onReceive(p0: Context?, intent: Intent?) {
            val sdf = SimpleDateFormat("dd/M/yyyy hh:mm:ss")
            val currentDate = sdf.format(Date())
            Toast.makeText(p0, nagMessage , Toast.LENGTH_LONG).show()
            sendTextMsg()
            Log.i("IntentListener", "Message emmitted at " + currentDate + " was " + nagMessage)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (checkSelfPermission(android.Manifest.permission.SEND_SMS) ==
            PackageManager.PERMISSION_GRANTED) {
            myOnCreate()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.SEND_SMS),
                0
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,permissions: Array<out String>,grantResults: IntArray)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.i("Hopium", "Did this work")
        myOnCreate();
    }

    fun myOnCreate() {
        val sharedPrefs = getSharedPreferences("awty", MODE_PRIVATE)

        val phoneInputBox = findViewById(R.id.phoenNumBox) as EditText
        val messageBox = findViewById(R.id.messageBox) as EditText
        val minutesInputBox = findViewById(R.id.minutesBox) as EditText

        minutesInputBox.setText(sharedPrefs.getString("minutes", "minutes"))
        messageBox.setText(sharedPrefs.getString("message", "message"))
        phoneInputBox.setText(sharedPrefs.getString("phoneNum", "Phone Number"))

        updateNagMessage(sharedPrefs)

        val isStop = sharedPrefs.getString("startOrStop", "Start").equals("Stop")
        val btnStartStop = findViewById(R.id.btnStartStop) as Button
        if(isStop){
            btnStartStop.text = "Stop"
        }
        if(btnStartStop.text.equals("Stop")) {
            btnStartStop.setOnClickListener { onClickStop(btnStartStop, this) }
        }
        else{
            btnStartStop.setOnClickListener { onClickStart(btnStartStop, this) }
        }
    }

    fun sendTextMsg(){
        if(sendEnabled)
        {
            val currentDate = "" + SimpleDateFormat("dd/M/yyyy hh:mm:ss").format(Date())

            Log.i("awty","Trying to send a message on " + currentDate)
            val sharedPrefs = getSharedPreferences("awty", MODE_PRIVATE)
            val txmsg = sharedPrefs.getString("message", "Are we there yet?")
            val recipient = sharedPrefs.getString("phoneNum", "9546047405")
            //val smsManager = this.getSystemService(SmsManager::class.java)
            val smsManager = SmsManager.getDefault();
            if(smsManager == null){
                Log.i("awty", "Houston, we have a problem")
            }
            Log.i("awty", "SMSmanager: " + smsManager)
            //val pendingIntent = getPendingIntent()
            smsManager.sendTextMessage(recipient, null,
                /* currentDate + ": " +  */ txmsg, null, null)
            Log.i("awty","Sent a message")
        }

    }
    fun onClickStart (btn : Button, context: Context) {
        updateValues(context)
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val sharedPrefs : SharedPreferences = getSharedPreferences("awty", MODE_PRIVATE)
        val time = sharedPrefs.getString("minutes", "60").toString().toInt() * 1000 * 60
        val pendingIntent = getPendingIntent()
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + time.toLong(),
            time.toLong(), pendingIntent)

        btn.text = "Stop"
        sharedPrefs.edit().putString("startOrStop", "Stop").apply()
        Log.i("btnStartStop", "You clicked while on Start mode")
        btn.setOnClickListener { onClickStop(btn, context) }
    }

    fun onClickStop (btn : Button, context: Context) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val sharedPrefs : SharedPreferences = getSharedPreferences("awty", MODE_PRIVATE)
        val pendingIntent = getPendingIntent()
        alarmManager.cancel(pendingIntent)
        btn.text = "Start"
        sharedPrefs.edit().putString("startOrStop", "Start").apply()
        Log.i("btnStartStop", "You clicked while on Stop mode")
        btn.setOnClickListener { onClickStart(btn, context) }
    }

    fun updateValues(context: Context) {
        val sharedPrefs = getSharedPreferences("awty", MODE_PRIVATE)
        var errormsg = "";
        val phoneInputBox = findViewById(R.id.phoenNumBox) as EditText
        val phoneInput = phoneInputBox.text.toString()
        if(PhoneNumberUtils.isGlobalPhoneNumber(phoneInput)){
            sharedPrefs.edit().putString("phoneNum", phoneInput).apply()
        }
        else {
            errormsg = errormsg + " Invalid phone #"
        }
        val minutesInputBox = findViewById(R.id.minutesBox) as EditText
        val minutesInput = minutesInputBox.text.toString().toIntOrNull()
        if(minutesInput != null && (minutesInput as Int) > 0){
            sharedPrefs.edit().putString("minutes", minutesInput.toString()).apply()
        }
        else {
            errormsg = errormsg + " Invalid amount of minutes"
        }
        val messageBox = findViewById(R.id.messageBox) as EditText
        val message = messageBox.text.toString()
        sharedPrefs.edit().putString("message", message).apply()
        if(!(errormsg.equals("")))
        {
            Log.i("awty", "Trying to enable")
            Toast.makeText(context, errormsg, Toast.LENGTH_SHORT).show()
            sendEnabled = true
        }
        else{
            Log.i("awty", "Trying to enable")
            sendEnabled = true
        }
        updateNagMessage(sharedPrefs)
    }
    fun getPendingIntent(): PendingIntent {
        val receiver = IntentListener()
        val intFilter = IntentFilter()
        intFilter.addAction("HELLO")
        registerReceiver(receiver, intFilter)
        val intent = Intent("HELLO")
        val pendingIntent =
            PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT)
        return pendingIntent;
    }

    fun updateNagMessage(sharedPrefs : SharedPreferences) {
        nagMessage = "" + sharedPrefs.getString("phoneNum", "(425) 555-1212") + ": " +
                sharedPrefs.getString("message", "Are we there yet?")
    }

}