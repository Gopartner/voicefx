package com.voicefx.core.overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.voicefx.MainActivity

/**
 * Receiver khusus untuk mendeteksi ketika pengguna mengetik kode rahasia di dialpad panggilan.
 * Komponen ini mendengarkan sinyal sistem "NEW_OUTGOING_CALL".
 */
class DialLaunchReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Memastikan action intent yang masuk adalah sinyal panggilan keluar
        if (intent.action == Intent.ACTION_NEW_OUTGOING_CALL) {
            
            // Mengambil nomor telepon yang sedang ditekan/dipanggil oleh user
            val phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)

            // Memeriksa apakah nomor yang di-dial cocok dengan kode rahasiamu
            if (phoneNumber == "*#4804314#*") {
                Log.d("DialReceiver", "Kode rahasia terdeteksi! Membuka aplikasi...")

                // 1. Batalkan panggilan telepon palsu tersebut agar tidak memotong pulsa/terjadi error panggilan
                resultData = null

                // 2. Siapkan intent untuk meluncurkan kembali MainActivity (Layar Onboarding/Utama)
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    // FLAG_ACTIVITY_NEW_TASK wajib digunakan karena kita membuka Activity dari luar konteks UI (dari Background Receiver)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    // FLAG_ACTIVITY_CLEAR_TOP memastikan jika app sudah terbuka di background, dia akan diangkat ke atas kembali
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }

                // 3. Jalankan pemanggilan untuk memunculkan aplikasi ke layar pengguna
                context.startActivity(launchIntent)
            }
        }
    }
}