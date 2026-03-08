package ru.bmstu.iu3;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.view.View;
import android.widget.Toast;

import ru.bmstu.iu3.databinding.ActivityMainBinding;

import android.util.Log;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    ActivityResultLauncher<Intent> activityResultLauncher;
    // Used to load the 'iu3' library on application startup.
    static {
        System.loadLibrary("mbedcrypto");
        System.loadLibrary("iu3");

        System.out.println("Smth");
    }

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Example of a call to a native method
        TextView tv = binding.sampleText;
        tv.setText(stringFromJNI());



        int rc = initRng();
        Log.i("fclient_ndk", "initRng rc=" + rc);

        byte[] rnd = randomBytes(16);
        Log.i("fclient_ndk", "rnd len=" + (rnd == null ? -1 : rnd.length));

        byte[] key = randomBytes(16);
        byte[] msg = "12345678".getBytes(java.nio.charset.StandardCharsets.US_ASCII); // ровно 8 байт
        
        byte[] enc = encrypt(key, msg);
        byte[] dec = decrypt(key, enc);


        Log.i("fclient_ndk", "3des ok=" + Arrays.equals(msg, dec));

        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            String pin = result.getData().getStringExtra("pin");
                            Toast.makeText(MainActivity.this, pin + " Woof!", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

    }

    /**
     * A native method that is implemented by the 'iu3' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
    public static native int initRng();
    public static native byte[] randomBytes(int no);

    public static native byte[] encrypt(byte[] key, byte[] data);
    public static native byte[] decrypt(byte[] key, byte[] data);

    public static byte[] stringToHex(String s)
    {
        byte[] hex;
        try
        {
            hex = Hex.decodeHex(s.toCharArray());
        }
        catch (DecoderException ex)
        {
            hex = null;
        }
        return hex;
    }

    public void onButtonClick(View v)
    {
        Intent it = new Intent(this, PinpadActivity.class);
        //startActivity(it);
        activityResultLauncher.launch(it);
    }

}