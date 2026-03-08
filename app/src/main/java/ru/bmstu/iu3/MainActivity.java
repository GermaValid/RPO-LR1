package ru.bmstu.iu3;

import androidx.appcompat.app.AppCompatActivity;

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
        byte[] key = stringToHex("0123456789ABCDEF0123456789ABCDE0");
        byte[] enc = encrypt(key, stringToHex("000000000000000102"));
        byte[] dec = decrypt(key, enc);
        String s = new String(Hex.encodeHex(dec)).toUpperCase();
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

}