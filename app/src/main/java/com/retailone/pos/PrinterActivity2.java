package com.retailone.pos;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.retailone.pos.utils.MyItem;
import com.retailone.pos.utils.PrinterUtil;

import java.util.ArrayList;

public class PrinterActivity2 extends AppCompatActivity {

    private Button BnPrint;
    private EditText editcontent;
    private TextView textView;
    private PrinterUtil printerUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_printer_test);

        BnPrint = findViewById(R.id.print_btn);
        editcontent = findViewById(R.id.content);
        textView = findViewById(R.id.textView);

        printerUtil = new PrinterUtil(this);


        BnPrint.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                ArrayList<MyItem> items = new ArrayList<>();
                items.add(new MyItem("item2agsfxgfgsf", 1, 56));
                items.add(new MyItem("item1", 2, 50));
                items.add(new MyItem("item7", 1, 200));
                items.add(new MyItem("item3", 1, 56));
                items.add(new MyItem("item4", 2, 50));
                items.add(new MyItem("item5", 1, 200));
              //  printerUtil.printReceipt(items);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        printerUtil.registerBatteryReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        printerUtil.unregisterBatteryReceiver();
    }
}