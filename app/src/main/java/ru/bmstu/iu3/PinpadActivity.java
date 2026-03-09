package ru.bmstu.iu3;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.DecimalFormat;

public class PinpadActivity extends AppCompatActivity {

    TextView tvPin;
    String pin = "";
    final int MAX_KEYS = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pinpad);

        tvPin = findViewById(R.id.txtPin);

        ShuffleKeys();

        findViewById(R.id.btnOK).setOnClickListener(v -> {
            Intent it = new Intent();
            it.putExtra("pin", pin);
            setResult(RESULT_OK, it);
            finish();
        });

        findViewById(R.id.btnReset).setOnClickListener(v -> {
            pin = "";
            tvPin.setText("");
        });



        TextView ta = findViewById(R.id.txtAmount);
        String amt = String.valueOf(getIntent().getStringExtra("amount"));
        Long f = Long.valueOf(amt);
        DecimalFormat df = new DecimalFormat("#,###,###,##0.00");
        String s = df.format(f);
        ta.setText("Сумма: " + s);

        TextView tp = findViewById(R.id.txtPtс);
        int pts = getIntent().getIntExtra("ptc", 0);
        if (pts == 2)
            tp.setText("Осталось две попытки");
        else if (pts == 1)
            tp.setText("Осталась одна попытка");
    }


    public void keyClick(View v)
    {
        String key = ((TextView)v).getText().toString();
        int sz = pin.length();
        if (sz < 4)
        {
            pin += key;
            tvPin.setText("****".substring(3 - sz));
        }
    }

    protected void ShuffleKeys()
    {
        Button keys[] = new Button[] {
                findViewById(R.id.btnkey0),
                findViewById(R.id.btnkey1),
                findViewById(R.id.btnkey2),
                findViewById(R.id.btnkey3),
                findViewById(R.id.btnkey4),
                findViewById(R.id.btnkey5),
                findViewById(R.id.btnkey6),
                findViewById(R.id.btnkey7),
                findViewById(R.id.btnkey8),
                findViewById(R.id.btnkey9),
        };

        byte[] rnd = MainActivity.randomBytes(MAX_KEYS);
        for(int i = 0; i < MAX_KEYS; i++)
        {
            int idx = (rnd[i] & 0xFF) % 10;
            CharSequence txt = keys[idx].getText();
            keys[idx].setText(keys[i].getText());
            keys[i].setText(txt);
        }
    }
}