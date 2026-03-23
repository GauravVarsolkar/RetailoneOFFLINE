package com.retailone.pos.ui.Activity;

import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import com.common.apiutil.ResultCode;
import com.common.CommonConstants;
import com.common.apiutil.pos.RS232Reader;
import com.common.callback.IRSReaderListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.retailone.pos.R;

import java.text.DecimalFormat;
import java.util.Arrays;


public class RSSerialActivity extends BaseActivity implements IRSReaderListener {
    private TextView TvStatus;
    private RS232Reader mRS232Reader;
    Handler polhandler;
    Runnable polrunnable;
    boolean StartDispensing = false;
    int ProcessStep = 0;
    int DU_STATUS = -1;
    int DU_Retry_Counter = 5;
    DecimalFormat df = new DecimalFormat("#.##");
    private static double LITER_PRICE = 0.0;
    private double parsedQty;

    String preserorderQtyText, presetAmtText;

    enum Last_CMD {STATUS, START_TOTALIZER, END_TOTALIZER, PRESET, LAST_TXN}

    boolean doubleBackToExitPressedOnce = false;
    private boolean isUpdating = false;
    Last_CMD last_issued_cmd;
    private TextView EditTextQty, ProductAmt;
    private EditText presetAmount, presetVol;
    private Button startButton, selectBtn50, selectBtn100, selectBtn200, SelectBtn250, selectBtn400, selectBtnLit2, selectBtnLit5, selectBtnLit50, selectBtnLit100;
    int waitForCallStateTries = 10;
    int DU_Unit_Id = 1;
    int Hose_No = 1;
    float order_qty = 0.0f; // it should be in LITRES
    private float presetAmt = 0.00f;
    float startTotalizer;
    float endTotalizer;
    static int PollingTimer = 1000;
    private float totalDispancedAmount;
    //private float totalDispancedAmount = 0.0f;
    int pro_id = 0;
    int dis_id = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_rsserial);

        mRS232Reader = new RS232Reader(getApplicationContext());
        LITER_PRICE = getIntent().getDoubleExtra("price", 0);
        pro_id = getIntent().getIntExtra("pro_id", 0);
        dis_id = getIntent().getIntExtra("dis_id", 0);
        initView();
        LoopPolling(PollingTimer);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    protected void onStop() {
        super.onStop();
    }

    @SuppressLint("SetTextI18n")
    private void initView() {
        // Setup UI controls
        startButton = (Button) findViewById(R.id.btn_start);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (startButton.getText().equals("Start Dispensing")) {
                    preserorderQtyText = presetVol.getText().toString().trim();
                    presetAmtText = presetAmount.getText().toString().trim();

                    if ((!preserorderQtyText.isEmpty() && presetAmtText.isEmpty()) ||
                            (preserorderQtyText.isEmpty() && !presetAmtText.isEmpty())) {

                        try {
                            TvStatus.setTextColor(Color.BLUE);
                            TvStatus.setText("Initiating dispensing process...");
                            DU_Unit_Id = 1;
                            Hose_No = 1;
                            if (!preserorderQtyText.isEmpty()) {
                                order_qty = Float.parseFloat(preserorderQtyText);
                            } else {
                                presetAmt = Float.parseFloat(presetAmtText);
                            }

                            DU_STATUS = -1;
                            StartDispensing = true;
                            startButton.setEnabled(false);
                        } catch (NumberFormatException e) {
                            Toast.makeText(
                                    v.getContext(),
                                    "Invalid input. Please enter numeric values.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    } else {
                        Toast.makeText(
                                v.getContext(),
                                "Please enter a value in only one field.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                } else if (startButton.getText().equals("Close")) {
                    finishAndRemoveTask();
                }
//
//                double totalDispancedAmountDouble = 50.0;
//
//                if (totalDispancedAmountDouble > 0) {
//                    parsedQty = totalDispancedAmountDouble / LITER_PRICE;
//                }
////
//                final Intent resultIntent = new Intent();
//                resultIntent.putExtra("sucess", "true");
//                // resultIntent.putExtra("id", service.id)
//                // resultIntent.putExtra("service_name", service.service_name)
//                resultIntent.putExtra("pro_id", pro_id);
//                resultIntent.putExtra("dis_id", dis_id);
//                //resultIntent.putExtra("dis_quantity", quantity);
//                resultIntent.putExtra("dis_quantity", parsedQty);
//                setResult(Activity.RESULT_OK, resultIntent);
//                finish();
            }
        });
        EditTextQty = findViewById(R.id.editText_qty);
        ProductAmt = findViewById(R.id.productAmt);
        TvStatus = findViewById(R.id.tvStatus);
        selectBtn50 = findViewById(R.id.btn_1);
        selectBtn100 = findViewById(R.id.btn_2);
        selectBtn200 = findViewById(R.id.btn_3);
        SelectBtn250 = findViewById(R.id.btn_4);
        selectBtn400 = findViewById(R.id.btn_5);
        selectBtnLit2 = findViewById(R.id.qty_1);
        selectBtnLit5 = findViewById(R.id.qty_2);
        selectBtnLit50 = findViewById(R.id.qty_3);
        selectBtnLit100 = findViewById(R.id.qty_4);
        presetAmount = findViewById(R.id.selectAmount);
        presetVol = findViewById(R.id.selectlit);
        selectBtn50.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double qty = 50 / LITER_PRICE;
                String qtyFormatted = String.format("%.2f", qty);
                EditTextQty.setText(qtyFormatted);
                ProductAmt.setText(String.valueOf(50));
                presetAmount.setText(String.valueOf(50));
                presetVol.setVisibility(View.INVISIBLE);
                presetAmount.setVisibility(VISIBLE);
                presetVol.setText("");
            }
        });
        selectBtn100.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double qty = 100 / LITER_PRICE;
                String qtyFormatted = String.format("%.2f", qty);
                EditTextQty.setText(qtyFormatted);
                ProductAmt.setText(String.valueOf(100));
                presetAmount.setText(String.valueOf(100));
                presetVol.setVisibility(View.INVISIBLE);
                presetAmount.setVisibility(VISIBLE);
                presetVol.setText("");
            }
        });
        selectBtn200.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double qty = 200 / LITER_PRICE;
                String qtyFormatted = String.format("%.2f", qty);
                EditTextQty.setText(qtyFormatted);
                ProductAmt.setText(String.valueOf(200));
                presetAmount.setText(String.valueOf(200));
                presetVol.setVisibility(View.INVISIBLE);
                presetAmount.setVisibility(VISIBLE);
                presetVol.setText("");
            }
        });
        SelectBtn250.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double qty = 250 / LITER_PRICE;
                String qtyFormatted = String.format("%.2f", qty);
                EditTextQty.setText(qtyFormatted);
                ProductAmt.setText(String.valueOf(250));
                presetAmount.setText(String.valueOf(250));
                presetVol.setVisibility(View.INVISIBLE);
                presetAmount.setVisibility(VISIBLE);
                presetVol.setText("");
            }
        });
        selectBtn400.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double qty = 400 / LITER_PRICE;
                String qtyFormatted = String.format("%.2f", qty);
                EditTextQty.setText(qtyFormatted);
                ProductAmt.setText(String.valueOf(400));
                presetAmount.setText(String.valueOf(400));
                presetVol.setVisibility(View.INVISIBLE);
                presetAmount.setVisibility(VISIBLE);
                presetVol.setText("");
            }
        });
        selectBtnLit2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double amt = 2.5 * LITER_PRICE;
                String amtFormatted = String.format("%.2f", amt);
                EditTextQty.setText("2.5");
                ProductAmt.setText(amtFormatted);
                presetVol.setText("2.5");
                presetVol.setVisibility(VISIBLE);
                presetAmount.setVisibility(View.INVISIBLE);
                presetAmount.setText("");
            }
        });
        selectBtnLit5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double amt = 5 * LITER_PRICE;
                String amtFormatted = String.format("%.2f", amt);
                EditTextQty.setText("5");
                ProductAmt.setText(amtFormatted);
                presetVol.setText("5");
                presetVol.setVisibility(VISIBLE);
                presetAmount.setVisibility(View.INVISIBLE);
                presetAmount.setText("");
            }
        });
        selectBtnLit50.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double amt = 50 * LITER_PRICE;
                String amtFormatted = String.format("%.2f", amt);
                EditTextQty.setText("50");
                ProductAmt.setText(amtFormatted);
                presetVol.setText("50");
                presetVol.setVisibility(VISIBLE);
                presetAmount.setVisibility(View.INVISIBLE);
                presetAmount.setText("");
            }
        });
        selectBtnLit100.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double amt = 100 * LITER_PRICE;
                String amtFormatted = String.format("%.2f", amt);
                EditTextQty.setText("100");
                ProductAmt.setText(amtFormatted);
                presetVol.setText("100");
                presetVol.setVisibility(VISIBLE);
                presetAmount.setVisibility(View.INVISIBLE);
                presetAmount.setText("");
            }
        });
        presetVol.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String volString = editable.toString();
                if (volString.isEmpty()) {
                    ProductAmt.setText("");
                    EditTextQty.setText("");
                    presetAmount.setVisibility(VISIBLE);
                } else {
                    if (isUpdating) return;
                    String qtyString = editable.toString();
                    if (!qtyString.isEmpty()) {
                        try {
                            double qty = Double.parseDouble(qtyString);
                            double amt = qty * LITER_PRICE;
                            isUpdating = true;
                            String amtFormatted = String.format("%.2f", amt);
                            ProductAmt.setText(amtFormatted);
                            EditTextQty.setText(String.valueOf(qty));
                            presetAmount.setVisibility(View.INVISIBLE);
                            isUpdating = false;
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    }
                }

            }
        });

        presetAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
                // Optional: Can handle actions before text change
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                // Optional: Can handle actions when the text is changing
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String qtyString = editable.toString();
                if (qtyString.isEmpty()) {
                    ProductAmt.setText("");
                    EditTextQty.setText("");
                    presetVol.setVisibility(VISIBLE);
                } else {
                    if (isUpdating) return;
                    String amtString = editable.toString();
                    if (!amtString.isEmpty()) {
                        try {
                            double amt = Double.parseDouble(amtString);
                            if (LITER_PRICE != 0 && !Double.isNaN(LITER_PRICE)) {
                                double qty = amt / LITER_PRICE;
                                isUpdating = true;
                                String qtyFormatted = String.format("%.2f", qty); // Format the quantity
                                EditTextQty.setText(qtyFormatted);
                                ProductAmt.setText(String.valueOf(amt));
                                isUpdating = false;
                                presetVol.setVisibility(View.INVISIBLE);
                            } else {
                                Log.e("ProductAmtWatcher", "Invalid LITER_PRICE value: " + LITER_PRICE);
                            }
                        } catch (NumberFormatException e) {
                            // Handle invalid number format gracefully
                            Log.e("ProductAmtWatcher", "Error parsing amount: " + e.getMessage());
                        }
                    }
                }

            }
        });

        // Setup COM control
        int ret = mRS232Reader.rsOpen(CommonConstants.RS232Type.RS232_1, 4800, 2);
        mRS232Reader.setRSReaderListener(this);

        if (ret == ResultCode.SUCCESS) {
            TvStatus.setTextColor(Color.GREEN);
            TvStatus.setText("Port Open Status: Success");
        } else if (ret == ResultCode.ERR_SYS_NOT_SUPPORT) {
            TvStatus.setTextColor(Color.RED);
            TvStatus.setText("Port Open Status: Not Supported");
        } else {
            TvStatus.setTextColor(Color.RED);
            TvStatus.setText("Port Open Status: Failed");
        }
    }

    public void Last_Txn() {
        last_issued_cmd = Last_CMD.LAST_TXN;
        mRS232Reader.rsSend(HexToByteArr("F" + DU_Unit_Id + Hose_No + "6B0"));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void LoopPolling(final int timer) {
        polhandler = new Handler();
        polrunnable = new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                try {
                    if (StartDispensing) {
                        if (DU_Retry_Counter == 0) {
                            startButton.setText("Close");
                            startButton.setEnabled(true);
                            StartDispensing = false;
                        }
                        if (ProcessStep == 0)
                            ProcessStep = 1;
                        else if (ProcessStep == 1) {
                            last_issued_cmd = Last_CMD.STATUS;
                            mRS232Reader.rsSend(HexToByteArr("F" + DU_Unit_Id + Hose_No + "0B0"));
                            DU_Retry_Counter--;
                            ProcessStep = 2;
                        } else if (ProcessStep == 2) {
                            if (DU_STATUS == -1) {
                                if (DU_Retry_Counter == 0) {
                                    TvStatus.setTextColor(Color.RED);
                                    TvStatus.setText("DU Offline - Aborting");
                                    StartDispensing = false;
                                    showUnSucessDialog("");
                                } else {
                                    TvStatus.setTextColor(Color.RED);
                                    TvStatus.setText("DU Offline - retrying...");
                                    ProcessStep = 1;
                                }
                            } else if (DU_STATUS == 0) {
                                TvStatus.setTextColor(Color.RED);
                                TvStatus.setText("DU in ERROR Status - Restart DU and try again");
                                DU_Retry_Counter--;
                                //StartDispensing = false;
                            } else if (DU_STATUS == 1) {
                                TvStatus.setTextColor(Color.GREEN);
                                TvStatus.setText("DU OFF State - Hose in the boot");
                                ProcessStep = 3; // to get start totalizer reading
                            } else if (DU_STATUS == 2) {
                                TvStatus.setTextColor(Color.GREEN);
                                TvStatus.setText("DU CALL State - Nozzle off, AUTH awaited");
                                if (startTotalizer == 0)
                                    ProcessStep = 3; // First get start totalizer value
                                else ProcessStep = 4; // Authorise with a pre-set Volume transaction
                            } else if (DU_STATUS == 3) {
                                TvStatus.setTextColor(Color.RED);
                                TvStatus.setText("DU BUSY State - Dispensing in progress");
                            } else if (DU_STATUS == 4) {
                                TvStatus.setTextColor(Color.RED);
                                TvStatus.setText("DU FIN State - Txn completed");
                                DU_Retry_Counter--;
                                ProcessStep = 1;
                                Last_Txn();
                            } else if (DU_STATUS == 5) {
                                TvStatus.setTextColor(Color.RED);
                                TvStatus.setText("DU in STANDALONE mode!!!");
                                StartDispensing = false;
                            } else if (DU_STATUS == 6) {
                                TvStatus.setTextColor(Color.RED);
                                TvStatus.setText("DU in LEAK state!!!");
                                StartDispensing = false;
                            } else if (DU_STATUS == 7) {
                                TvStatus.setTextColor(Color.RED);
                                TvStatus.setText("DU Pulser Error!!!");
                                StartDispensing = false;
                            } else if (DU_STATUS == 8) {
                                TvStatus.setTextColor(Color.RED);
                                TvStatus.setText("DU Autherized, Press nozzle to dispense");
                            } else if (DU_STATUS == 9) {
                                TvStatus.setTextColor(Color.RED);
                                TvStatus.setText("DU Paused");
                            }
                        }
                        // Stage where start totalizer needs to be read
                        else if (ProcessStep == 3) {
//                            TvStatus1.setText("In step 3 Start Totalizer command sent...");
                            // updated the command being issued to Du
                            last_issued_cmd = Last_CMD.START_TOTALIZER;
//                            // Send command to read totalizer
//                            mRS232Reader.rsSend(HexToByteArr("F" + DU_Unit_Id + Hose_No + "DB0")); //9B0

                            startTotalizer = 0;

//                            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());
//                            String currentTime = sdf.format(new Date()); // Use current time
//                            String currentText = TvStatus3.getText().toString();
//                            TvStatus3.setText("[" + currentTime + "] In Process 3  command sent" + "\n" + currentText);

                            ProcessStep = 4;
                        }
                        // Wait for operator to lift the Nozzle to get DU in CALL state
                        else if (ProcessStep == 4) {
                            last_issued_cmd = Last_CMD.STATUS;
                            // Send STATUS command
                            mRS232Reader.rsSend(HexToByteArr("F" + DU_Unit_Id + Hose_No + "0B0"));
                            waitForCallStateTries--;
                            TvStatus.setText("Please lift Nozzle...(" + waitForCallStateTries + ")");
                            ProcessStep = 5;
                        }
                        // Send PRESET
                        else if (ProcessStep == 5) {
                            if (DU_STATUS == 2) {
                                if (order_qty > 0 && presetAmt == 0.0) {
                                    // updated the command being issued to Du
                                    last_issued_cmd = Last_CMD.PRESET;

                                    // Create PRESET command string
                                    int qtyToSet = (int) (order_qty * 100);

                                    String[] presetQtyStrArr = new String[]{"E0", "E0", "E0", "E0", "E0", "E0"};
                                    int sumOfDigits = 0, i = 0;
                                    while (qtyToSet > 0) {
                                        int lsb = qtyToSet % 10;
                                        sumOfDigits += lsb;
                                        qtyToSet = (qtyToSet / 10);
                                        presetQtyStrArr[i] = "E" + lsb;
                                        i++;
                                    }
                                    String presetQtyStr = "";
                                    for (String s : presetQtyStrArr)
                                        presetQtyStr = presetQtyStr + s;

                                    sumOfDigits = sumOfDigits + DU_Unit_Id + Hose_No + 2; //2 (Hose no) + 2 (Command no);
                                    String chksum = Integer.toHexString(sumOfDigits);
                                    if (chksum.length() > 1)
                                        chksum = chksum.substring(1);

                                    last_issued_cmd = Last_CMD.PRESET;
                                    String cmd = "F" + DU_Unit_Id + Hose_No + "2" + presetQtyStr + "E" + chksum + "B0";
                                    //String cmd = "F" + DU_Unit_Id + Hose_No + "2" + presetQtyStr + "EAB0";
                                    mRS232Reader.rsSend(HexToByteArr(cmd));
                                    ProcessStep = 6;
                                    TvStatus.setTextColor(Color.BLUE);
                                    TvStatus.setText("PRESET sent, start dispensing...");
                                    ProcessStep = 6;
                                } else if (order_qty <= 0 && presetAmt > 0) {
                                    // updated the command being issued to Du
                                    last_issued_cmd = Last_CMD.PRESET;
                                    // Create PRESET command string
                                    int presetVol = (int) (presetAmt * 100);

                                    String[] presetQtyStrArr = new String[]{"E0", "E0", "E0", "E0", "E0", "E0", "E0", "E0"};
                                    int sumOfDigits = 0, i = 0;
                                    while (presetVol > 0) {
                                        int lsb = presetVol % 10;
                                        sumOfDigits += lsb;
                                        presetVol = (presetVol / 10);
                                        presetQtyStrArr[i] = "E" + lsb;
                                        i++;
                                    }
                                    String presetQtyStr = "";
                                    for (String s : presetQtyStrArr)
                                        presetQtyStr = presetQtyStr + s;

                                    sumOfDigits = sumOfDigits + DU_Unit_Id + Hose_No + 3;
                                    String chksum = Integer.toHexString(sumOfDigits);
                                    if (chksum.length() > 1)
                                        chksum = chksum.substring(1);

                                    last_issued_cmd = Last_CMD.PRESET;
                                    String cmd = "F" + DU_Unit_Id + Hose_No + "3" + presetQtyStr + "E" + chksum + "B0";
                                    //String cmd = "F" + DU_Unit_Id + Hose_No + "2" + presetQtyStr + "EAB0";
                                    mRS232Reader.rsSend(HexToByteArr(cmd));
                                    ProcessStep = 6;
                                    TvStatus.setTextColor(Color.BLUE);
                                    TvStatus.setText("PRESET sent, start dispensing...");
                                    ProcessStep = 6;
                                }
                            } else if (waitForCallStateTries > 0)
                                ProcessStep = 4;
                            else if (startTotalizer <= 0)
                                ProcessStep = 3;
                            else {
                                TvStatus.setTextColor(Color.RED);
                                TvStatus.setText("Operator failed to lift Nozzle, aborting");
                                StartDispensing = false;
                                startButton.setText("Close");
                                startButton.setEnabled(true);
                            }
                        }
                        // Preset is sent -> check status for completion of dispensing
                        else if (ProcessStep == 6) {
                            // updated the command being issued to Du
                            last_issued_cmd = Last_CMD.STATUS;
                            // Send STATUS command
                            mRS232Reader.rsSend(HexToByteArr("F" + DU_Unit_Id + Hose_No + "0B0"));
                            ProcessStep = 7;
                        }
                        // Check response of status command and check if dispesing completed or not
                        // if not completed continue with step 5
                        // if completed, send LAST TXN command
                        else if (ProcessStep == 7) {
                            if (DU_STATUS == 3) {
                                TvStatus.setTextColor(Color.BLUE);
                                TvStatus.setText("Dispensing in progress...");
                                ProcessStep = 6;
                            } else if (DU_STATUS == 4) {
                                TvStatus.setTextColor(Color.BLUE);
                                TvStatus.setText("Dispensing completed...pls put nozzle back to boot");
                                last_issued_cmd = Last_CMD.STATUS;
                                // Send STATUS command
                                mRS232Reader.rsSend(HexToByteArr("F" + DU_Unit_Id + Hose_No + "0B0"));
                                ProcessStep = 8;
                            } else
                                ProcessStep = 6;
                        } else if (ProcessStep == 8) {
                            last_issued_cmd = Last_CMD.LAST_TXN;
                            //get the last transaction to complete the transaction
                            mRS232Reader.rsSend(HexToByteArr("F" + DU_Unit_Id + Hose_No + "6B0"));
                            ProcessStep = 9;
                        } else if (ProcessStep == 9) {
                            if (DU_STATUS == 1) {
                                //TvStatus2.setText("In step 8 End Totalizer command sent...");
                                // updated the command being issued to Du
                                last_issued_cmd = Last_CMD.LAST_TXN;
                                // Send command to LAST TXN
                                String cmd = "F" + DU_Unit_Id + Hose_No + "6B0";
                                mRS232Reader.rsSend(HexToByteArr(cmd));

//                                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());
//                                String currentTime = sdf.format(new Date()); // Use current time
//                                String currentText = TvStatus3.getText().toString();
//                                TvStatus3.setText("[" + currentTime + "] In Process 8  command sent" + "\n" + currentText);

                                ProcessStep = 10;
                            } else {
                                TvStatus.setText("Dispensing completed...pls put nozzle back to boot");
                                last_issued_cmd = Last_CMD.STATUS;
                                // Send STATUS command
                                mRS232Reader.rsSend(HexToByteArr("F" + DU_Unit_Id + Hose_No + "0B0"));
                            }
                        } else if (ProcessStep == 10) {
                            if (startTotalizer == 0 && endTotalizer >= 0 && DU_Retry_Counter > 0) {
                                TvStatus.setTextColor(Color.BLUE);
                                TvStatus.setText("Totalizer: " + df.format(startTotalizer) + "/" + df.format(endTotalizer) + "\nQuantity dispensed " + df.format(endTotalizer - startTotalizer));
                                startButton.setText("Close");
                                startButton.setEnabled(true);
                                StartDispensing = false;

//                                String numberx = df.format(endTotalizer);
//                                // Convert the formatted String to a float
//                                //float parsedNumber = Float.parseFloat(numberx);
//                                // Convert the float to an integer
//                                // int numberAsInt = (int) parsedNumber;
//                                double parsedNumber = Double.parseDouble(numberx);
//                                parsedNumber = Double.parseDouble(df.format(parsedNumber));
                                double totalDispancedAmountDouble = totalDispancedAmount;

                                if (totalDispancedAmountDouble > 0) {
                                    parsedQty = totalDispancedAmountDouble / LITER_PRICE;
                                }

                                final Intent resultIntent = new Intent();
                                resultIntent.putExtra("sucess", "true");
                                // resultIntent.putExtra("id", service.id)
                                // resultIntent.putExtra("service_name", service.service_name)
                                resultIntent.putExtra("pro_id", pro_id);
                                resultIntent.putExtra("dis_id", dis_id);
                                //resultIntent.putExtra("dis_quantity", quantity);
                                resultIntent.putExtra("dis_quantity", parsedQty);
                                setResult(Activity.RESULT_OK, resultIntent);
                                finish();

                            } else {
                                DU_Retry_Counter--;
                                ProcessStep = 8;
                            }
                        }
                    }
                    polhandler.postDelayed(this, timer);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        polhandler.post(polrunnable);
    }

    @Override
    public void onRecvData(byte[] data) {
        String msg = byteArrayToHex(data);
//        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());
//        String currentTime = sdf.format(new Date()); // Use current time
//        String currentText = TvStatus3.getText().toString();
//        TvStatus3.setText("[" + currentTime + "] Response: " + msg + " | Last issued cmd: " + last_issued_cmd + "\n" + currentText);

        // Implies response of status command received
        if ((last_issued_cmd == Last_CMD.STATUS) && (msg.length() == 2)) {
            DU_STATUS = Integer.parseInt(msg.substring(1, 2));
        } else if (last_issued_cmd == Last_CMD.START_TOTALIZER && (msg.length() >= 20)) {
            String tot = msg.substring(4, 20);
            tot = tot.substring(15) + tot.substring(13, 14) + tot.substring(11, 12) + tot.substring(9, 10) + tot.substring(7, 8) + tot.substring(5, 6) + tot.substring(3, 4) + tot.substring(1, 2);
            startTotalizer = ((float) Long.valueOf(tot) / 100);
        } else if (last_issued_cmd == Last_CMD.END_TOTALIZER && (msg.length() >= 20)) {
            String tot = msg.substring(4, 20);
            tot = tot.substring(15) + tot.substring(13, 14) + tot.substring(11, 12) + tot.substring(9, 10) + tot.substring(7, 8) + tot.substring(5, 6) + tot.substring(3, 4) + tot.substring(1, 2);
            endTotalizer = ((float) Long.valueOf(tot) / 100);
        } else if (last_issued_cmd == Last_CMD.LAST_TXN && (msg.length() >= 20)) {
            String dispancedAmount = msg.substring(16, 32);
            dispancedAmount = dispancedAmount.substring(15) + dispancedAmount.substring(13, 14) + dispancedAmount.substring(11, 12) + dispancedAmount.substring(9, 10) + dispancedAmount.substring(7, 8) + dispancedAmount.substring(5, 6)+dispancedAmount.substring(3, 4)+dispancedAmount.substring(1, 2);
            totalDispancedAmount = ((float) Long.valueOf(dispancedAmount) / 100);
            String tot = msg.substring(32, 44);
            tot = tot.substring(11) + tot.substring(9, 10) + tot.substring(7, 8) + tot.substring(5, 6) + tot.substring(3, 4) + tot.substring(1, 2);
            endTotalizer = ((float) Long.valueOf(tot) / 100);
        }

    }

    static public byte[] HexToByteArr(String inHex) {
        int hexlen = inHex.length();
        byte[] result;
        if (isOdd(hexlen) == 1) {//奇数
            hexlen++;
            result = new byte[(hexlen / 2)];
            inHex = "0" + inHex;
        } else {//偶数
            result = new byte[(hexlen / 2)];
        }
        int j = 0;
        for (int i = 0; i < hexlen; i += 2) {
            result[j] = HexToByte(inHex.substring(i, i + 2));
            j++;
        }
        return result;
    }

    static public int isOdd(int num) {
        return num & 0x1;
    }

    //-------------------------------------------------------
    static public byte HexToByte(String inHex) {
        return (byte) Integer.parseInt(inHex, 16);
    }

    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for (byte b : a)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private void showUnSucessDialog(String msg) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.pos_retry_dialog);
        dialog.setCancelable(false);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.setCanceledOnTouchOutside(false);

        MaterialButton confirm = dialog.findViewById(R.id.prefer_confirm);
        TextView logoutMsg = dialog.findViewById(R.id.logout_msg);
        ImageView logoutImg = dialog.findViewById(R.id.dialog_logo);
        MaterialButton printReceipt = dialog.findViewById(R.id.print_receipt);

        //logoutMsg.setText(msg);
        logoutMsg.setTextSize(16F);

        // If you need to set the image or scale type for the ImageView, uncomment these:
        // logoutImg.setImageResource(R.drawable.svg_off);
        // logoutImg.setScaleType(ImageView.ScaleType.FIT_CENTER);

        // If printerUtil is defined somewhere else, use it to print the receipt
        // printerUtil.printReceipt(pos_sale_data);

        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();

                polhandler.removeCallbacks(polrunnable);

                StartDispensing = false;
                ProcessStep = 0;
                DU_STATUS = -1;
                DU_Retry_Counter = 5;
                initView();
                startButton.setEnabled(true);
                startButton.setText("Start Dispensing");


                // Restart polling process
                LoopPolling(PollingTimer);


//                Intent intent = new Intent(RSSerialActivity.this, MPOSDashboardActivity.class);
//                startActivity(intent);
//                finish();
            }
        });

        printReceipt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();

                final Intent resultIntent = new Intent();
                resultIntent.putExtra("sucess", "false");
                // resultIntent.putExtra("id", service.id)
                // resultIntent.putExtra("service_name", service.service_name)
                setResult(Activity.RESULT_CANCELED, resultIntent);
                finish();
//                Log.d("xxx", new Gson().toJson(pos_sale_data));
//
//                ReceiptData receiptContent = formatReceiptContent(pos_sale_data);
//
//                // If printerUtil is defined, use it to print the receipt data
//                printerUtil.printReceiptData(pos_sale_data);
            }
        });

        dialog.show();
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Snackbar.make(this.TvStatus, "Please wait until the dispensation is complete.", Snackbar.LENGTH_SHORT)
                .setAction("Action", null).show();


        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 1500);
    }

//    private void setupListeners() {
//        // Listener for Order Quantity EditText
//        EditTextQty.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
//
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {}
//
//            @Override
//            public void afterTextChanged(Editable s) {
//                if (!s.toString().isEmpty()) {
//                    try {
//                        double quantity = Double.parseDouble(s.toString());
//                        double amount = quantity * LITER_PRICE;
//                        ProductAmt.setText(String.format("%.2f", amount));
//                    } catch (NumberFormatException e) {
//                        e.printStackTrace();
//                    }
//                } else {
//                    ProductAmt.setText("");
//                }
//            }
//        });
//
//
//
//
//    }


}

//public class RSSerialActivity extends BaseActivity implements IRSReaderListener {
//    private TextView TvStatus;
//    private TextView TvStatus1;
//    private TextView TvStatus2;
//    private RS232Reader mRS232Reader;
//    Handler polhandler;
//    Runnable polrunnable;
//    boolean StartDispensing = false;
//    int ProcessStep = 0;
//    int DU_STATUS = -1;
//    int DU_Retry_Counter = 5;
//    DecimalFormat df = new DecimalFormat("#.##");
//
//    enum Last_CMD {STATUS, START_TOTALIZER, END_TOTALIZER, PRESET, LAST_TXN}
//
//    ;
//
//    Last_CMD last_issued_cmd;
//    private Spinner SpinnerDuId;
//    private Spinner SpinnerHoseNo;
//    private EditText EditTextQty;
//    private Button startButton;
//    int waitForCallStateTries = 10;
//
//    int DU_Unit_Id = 0;
//    int Hose_No = 0;
//    float order_qty = 0.0f; // it should be in LITRES
//    float startTotalizer;
//    float endTotalizer;
//    static int PollingTimer = 1000;
//
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        setContentView(R.layout.activity_rsserial);
//
//
//        mRS232Reader = new RS232Reader(getApplicationContext());
//        quantity = getIntent().getIntExtra("quantity",0);
//        pro_id = getIntent().getIntExtra("pro_id",0);
//        dis_id = getIntent().getIntExtra("dis_id",0);
//        initView();
//        LoopPolling(PollingTimer);
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//    }
//
//    protected void onStop() {
//        super.onStop();
//    }
//
//    @SuppressLint("SetTextI18n")
//    private void initView() {
//
//        // Setup UI controls
//        startButton = (Button) findViewById(R.id.btn_start);
//        startButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (startButton.getText().equals("Start Dispensing")) {
//                    TvStatus.setTextColor(Color.BLUE);
//                    TvStatus.setText("Initiating dispensing process...");
//                    DU_Unit_Id = SpinnerDuId.getSelectedItemPosition() + 1;
//                    Hose_No = SpinnerHoseNo.getSelectedItemPosition() + 1;
//                    order_qty = Float.parseFloat(EditTextQty.getText().toString());
//                    DU_STATUS = -1;
//                    StartDispensing = true;
//                    startButton.setEnabled(false);
//                } else if (startButton.getText().equals("Close")) {
//                    finishAndRemoveTask();
//                }
//            }
//        });
//
//        SpinnerDuId = (Spinner) findViewById(R.id.spinner_du_id);
//        ArrayAdapter<CharSequence> adapterDU = ArrayAdapter.createFromResource(
//                this, R.array.Du_Id,
//                android.R.layout.simple_spinner_item);
//        adapterDU.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        SpinnerDuId.setAdapter(adapterDU);
//        SpinnerDuId.setSelection(1);
//
//        SpinnerHoseNo = (Spinner) findViewById(R.id.spinner_hose_no);
//        ArrayAdapter<CharSequence> adapterHose = ArrayAdapter.createFromResource(
//                this, R.array.Hose_Id,
//                android.R.layout.simple_spinner_item);
//        adapterHose.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        SpinnerHoseNo.setAdapter(adapterHose);
//        SpinnerHoseNo.setSelection(1);
//
//        EditTextQty = (EditText) findViewById(R.id.editText_qty);
//        TvStatus = findViewById(R.id.tvStatus);
//        TvStatus1 = findViewById(R.id.tvStatus1);
//        TvStatus2 = findViewById(R.id.tvStatus2);
//
//
//        EditTextQty.setText( Editable.Factory.getInstance().newEditable(Integer.toString(quantity)));
//
//
//
//
//
//        // Setup COM control
//        int ret = mRS232Reader.rsOpen(CommonConstants.RS232Type.RS232_1, 4800, 2);
//        mRS232Reader.setRSReaderListener(this);
//
//        if (ret == ResultCode.SUCCESS) {
//            TvStatus.setTextColor(Color.GREEN);
//            TvStatus.setText("Port Open Status: Success");
//        } else if (ret == ResultCode.ERR_SYS_NOT_SUPPORT) {
//            TvStatus.setTextColor(Color.RED);
//            TvStatus.setText("Port Open Status: Not Supported");
//        } else {
//            TvStatus.setTextColor(Color.RED);
//            TvStatus.setText("Port Open Status: Failed");
//        }
//    }
////    val resultIntent = Intent()
////            resultIntent.putExtra("susess","true")
////                    // resultIntent.putExtra("id", service.id)
////                    // resultIntent.putExtra("service_name", service.service_name)
////                    this.setResult(Activity.RESULT_OK, resultIntent)
////            this.finish()
//
//
//    public void LoopPolling(final int timer) {
//        polhandler = new Handler();
//        polrunnable = new Runnable() {
//            @SuppressLint("SetTextI18n")
//            @Override
//            public void run() {
//                try {
//                    if (StartDispensing) {
//                        // If first run, then change Step to 1
//                        if (ProcessStep == 0)
//                            ProcessStep = 1;
//                            // Step 1 is to query DU status
//                        else if (ProcessStep == 1) {
//                            // updated the command being issued to Du
//                            last_issued_cmd = Last_CMD.STATUS;
//                            // Send STATUS command
//                            mRS232Reader.rsSend(HexToByteArr("F" + DU_Unit_Id + Hose_No + "0B0"));
//                            DU_Retry_Counter--;
//                            ProcessStep = 2;
//                        }
//                        // Implies get DU status command was sent and now take action as per status
//                        else if (ProcessStep == 2) {
//                            if (DU_STATUS == -1) {
//                                if (DU_Retry_Counter == 0) {
//                                    TvStatus.setTextColor(Color.RED);
//                                    TvStatus.setText("DU Offline - Aborting");
//                                    StartDispensing = false;
//
//                                   showUnSucessDialog("");
//
//
////                                   final Intent  resultIntent = new Intent();
////                            resultIntent.putExtra("sucess","true");
////                            // resultIntent.putExtra("id", service.id)
////                            // resultIntent.putExtra("service_name", service.service_name)
////                             resultIntent.putExtra("pro_id", pro_id);
////                             resultIntent.putExtra("dis_id", dis_id);
////                             resultIntent.putExtra("dis_quantity", quantity);
////                            setResult (Activity.RESULT_OK, resultIntent);
////                            finish();
//
//
//                                } else {
//                                    TvStatus.setTextColor(Color.RED);
//                                    TvStatus.setText("DU Offline - retrying...");
//                                    ProcessStep = 1;
//                                }
//                            } else if (DU_STATUS == 0) {
//                                TvStatus.setTextColor(Color.RED);
//                                TvStatus.setText("DU in ERROR Status - Restart DU and try again");
//                                StartDispensing = false;
//                            } else if (DU_STATUS == 1) {
//                                TvStatus.setTextColor(Color.GREEN);
//                                TvStatus.setText("DU OFF State - Hose in the boot");
//                                ProcessStep = 3; // to get start totalizer reading
//                            } else if (DU_STATUS == 2) {
//                                TvStatus.setTextColor(Color.GREEN);
//                                TvStatus.setText("DU CALL State - Nozzle off, AUTH awaited");
//                                if (startTotalizer == 0)
//                                    ProcessStep = 3; // First get start totalizer value
//                                else ProcessStep = 4; // Authorise with a pre-set Volume transaction
//                            } else if (DU_STATUS == 3) {
//                                TvStatus.setTextColor(Color.RED);
//                                TvStatus.setText("DU BUSY State - Dispensing in progress");
//
//                            } else if (DU_STATUS == 4) {
//                                TvStatus.setTextColor(Color.RED);
//                                TvStatus.setText("DU FIN State - Txn completed");
//                                ProcessStep = 5; // Get Last TXN details
//                            } else if (DU_STATUS == 5) {
//                                TvStatus.setTextColor(Color.RED);
//                                TvStatus.setText("DU in STANDALONE mode!!!");
//                                StartDispensing = false;
//                            } else if (DU_STATUS == 6) {
//                                TvStatus.setTextColor(Color.RED);
//                                TvStatus.setText("DU in LEAK state!!!");
//                                StartDispensing = false;
//                            } else if (DU_STATUS == 7) {
//                                TvStatus.setTextColor(Color.RED);
//                                TvStatus.setText("DU Pulser Error!!!");
//                                StartDispensing = false;
//                            } else if (DU_STATUS == 8) {
//                                TvStatus.setTextColor(Color.RED);
//                                TvStatus.setText("DU Autherized, Press nozzle to dispense");
//                            } else if (DU_STATUS == 9) {
//                                TvStatus.setTextColor(Color.RED);
//                                TvStatus.setText("DU Paused");
//                            }
//                        }
//                        // Stage where start totalizer needs to be read
//                        else if (ProcessStep == 3) {
//                            TvStatus1.setText("In step 3 Start Totalizer command sent...");
//                            // updated the command being issued to Du
//                            last_issued_cmd = Last_CMD.START_TOTALIZER;
//                            // Send command to read totalizer
//                            mRS232Reader.rsSend(HexToByteArr("F" + DU_Unit_Id + Hose_No + "DB0"));
//                            ProcessStep = 4;
//                        }
//                        // Wait for operator to lift the Nozzle to get DU in CALL state
//                        else if (ProcessStep == 4) {
//                            last_issued_cmd = Last_CMD.STATUS;
//                            // Send STATUS command
//                            mRS232Reader.rsSend(HexToByteArr("F" + DU_Unit_Id + Hose_No + "0B0"));
//                            waitForCallStateTries--;
//                            TvStatus.setText("Please lift Nozzle...(" + waitForCallStateTries + ")");
//                            ProcessStep = 5;
//                        }
//                        // Send PRESET
//                        else if (ProcessStep == 5) {
//                            if (DU_STATUS == 2) {
//                                // updated the command being issued to Du
//                                last_issued_cmd = Last_CMD.PRESET;
//
//                                // Create PRESET command string
//                                int qtyToSet = (int) (order_qty * 100);
//
//                                String[] presetQtyStrArr = new String[]{"E0", "E0", "E0", "E0", "E0", "E0"};
//                                int sumOfDigits = 0, i = 0;
//                                while (qtyToSet > 0) {
//                                    int lsb = qtyToSet % 10;
//                                    sumOfDigits += lsb;
//                                    qtyToSet = (qtyToSet / 10);
//                                    presetQtyStrArr[i] = "E" + lsb;
//                                    i++;
//                                }
//                                String presetQtyStr = "";
//                                for (String s : presetQtyStrArr)
//                                    presetQtyStr = presetQtyStr + s;
//
//                                sumOfDigits = sumOfDigits + DU_Unit_Id + Hose_No + 2; //2 (Hose no) + 2 (Command no);
//                                String chksum = Integer.toHexString(sumOfDigits);
//                                if (chksum.length() > 1)
//                                    chksum = chksum.substring(1);
//
//                                last_issued_cmd = Last_CMD.PRESET;
//                                String cmd = "F" + DU_Unit_Id + Hose_No + "2" + presetQtyStr+ "E" + chksum + "B0";
//                                //String cmd = "F" + DU_Unit_Id + Hose_No + "2" + presetQtyStr + "EAB0";
//                                mRS232Reader.rsSend(HexToByteArr(cmd));
//                                ProcessStep = 6;
//                                TvStatus.setTextColor(Color.BLUE);
//                                TvStatus.setText("PRESET sent, start dispensing...");
//                                ProcessStep = 6;
//                            } else if (waitForCallStateTries > 0)
//                                ProcessStep = 4;
//                            else {
//                                TvStatus.setTextColor(Color.RED);
//                                TvStatus.setText("Operator failed to lift Nozzle, aborting");
//                                StartDispensing = false;
//                                startButton.setText("Close");
//                                startButton.setEnabled(true);
//                            }
//                        }
//                        // Preset is sent -> check status for completion of dispensing
//                        else if (ProcessStep == 6) {
//                            // updated the command being issued to Du
//                            last_issued_cmd = Last_CMD.STATUS;
//                            // Send STATUS command
//                            mRS232Reader.rsSend(HexToByteArr("F" + DU_Unit_Id + Hose_No + "0B0"));
//                            ProcessStep = 7;
//                        }
//                        // Check response of status command and check if dispesing completed or not
//                        // if not completed continue with step 5
//                        // if completed, send LAST TXN command
//                        else if (ProcessStep == 7) {
//                            if (DU_STATUS == 3) {
//                                TvStatus.setTextColor(Color.BLUE);
//                                TvStatus.setText("Dispensing in progress...");
//                                ProcessStep = 6;
//                            } else if (DU_STATUS == 4) {
//                                TvStatus.setTextColor(Color.BLUE);
//                                TvStatus.setText("Dispensing completed");
//                                // dispensing completed, send LAST TXN command
//                                //last_issued_cmd = Last_CMD.END_TOTALIZER;
//                                //mRS232Reader.rsSend(HexToByteArr("F" + DU_Unit_Id + Hose_No + "DB0"));
//                                ProcessStep = 8;
//                            } else
//                                ProcessStep = 6;
//                        } else if (ProcessStep == 8) {
//                            TvStatus2.setText("In step 8 End Totalizer command sent...");
//                            // updated the command being issued to Du
//                            last_issued_cmd = Last_CMD.END_TOTALIZER;
//                            // Send command to read totalizer
//                            mRS232Reader.rsSend(HexToByteArr("F" + DU_Unit_Id + Hose_No + "DB0"));
//                            ProcessStep = 9;
//                        } else if (ProcessStep == 9) {
//                            TvStatus.setTextColor(Color.BLUE);
//                            TvStatus.setText("Totalizer: " + df.format(startTotalizer) + "/" + df.format(endTotalizer));
//                            TvStatus.append("\nQuantity dispensed " + df.format(endTotalizer - startTotalizer));
//                            ProcessStep = 10;
//                        } else if (ProcessStep == 10) {
//                            last_issued_cmd = Last_CMD.LAST_TXN;
//                            //get the last transaction to complete the transaction
//                            mRS232Reader.rsSend(HexToByteArr("F" + DU_Unit_Id + Hose_No + "6B0"));
//                            startButton.setText("Close");
//                            startButton.setEnabled(true);
//                            StartDispensing = false;
//
//
//                            final Intent  resultIntent = new Intent();
//                            resultIntent.putExtra("sucess","true");
//                            // resultIntent.putExtra("id", service.id)
//                            // resultIntent.putExtra("service_name", service.service_name)
//                             resultIntent.putExtra("pro_id", pro_id);
//                             resultIntent.putExtra("dis_id", dis_id);
//                             resultIntent.putExtra("dis_quantity", quantity);
//                            setResult (Activity.RESULT_OK, resultIntent);
//                            finish();
//
//                        }
//                    }
//                    polhandler.postDelayed(this, timer);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        };
//        polhandler.post(polrunnable);
//    }
//
//    @Override
//    public void onRecvData(byte[] data) {
//        String msg = byteArrayToHex(data);
//        TvStatus.append("\nResponse" + msg);
//
//        // Implies response of status command received
//        if ((last_issued_cmd == Last_CMD.STATUS) && (msg.length() == 2)) {
//            DU_STATUS = Integer.parseInt(msg.substring(1, 2));
//            TvStatus.setText("Dispenser Status: " + DU_STATUS);
//        } else if (last_issued_cmd == Last_CMD.START_TOTALIZER) {
//            String tot = msg.substring(4, 20);
//            tot = tot.substring(15) + tot.substring(13, 14) + tot.substring(11, 12) + tot.substring(9, 10) + tot.substring(7, 8) + tot.substring(5, 6) + tot.substring(3, 4) + tot.substring(1, 2);
//            startTotalizer = ((float) Long.valueOf(tot) / 100);
//            TvStatus1.setText("Totalizer Start value : " + startTotalizer);
//        } else if (last_issued_cmd == Last_CMD.END_TOTALIZER) {
//            String tot = msg.substring(4, 20);
//            tot = tot.substring(15) + tot.substring(13, 14) + tot.substring(11, 12) + tot.substring(9, 10) + tot.substring(7, 8) + tot.substring(5, 6) + tot.substring(3, 4) + tot.substring(1, 2);
//            endTotalizer = ((float) Long.valueOf(tot) / 100);
//            TvStatus2.setText("Totalizer End value : " + endTotalizer);
//        }
//    }
//
//    static public byte[] HexToByteArr(String inHex) {
//        int hexlen = inHex.length();
//        byte[] result;
//        if (isOdd(hexlen) == 1) {//奇数
//            hexlen++;
//            result = new byte[(hexlen / 2)];
//            inHex = "0" + inHex;
//        } else {//偶数
//            result = new byte[(hexlen / 2)];
//        }
//        int j = 0;
//        for (int i = 0; i < hexlen; i += 2) {
//            result[j] = HexToByte(inHex.substring(i, i + 2));
//            j++;
//        }
//        return result;
//    }
//
//    static public int isOdd(int num) {
//        return num & 0x1;
//    }
//
//    //-------------------------------------------------------
//    static public byte HexToByte(String inHex) {
//        return (byte) Integer.parseInt(inHex, 16);
//    }
//
//    public static String byteArrayToHex(byte[] a) {
//        StringBuilder sb = new StringBuilder(a.length * 2);
//        for (byte b : a)
//            sb.append(String.format("%02x", b));
//        return sb.toString();
//    }
//
//
//    private void showUnSucessDialog(String msg) {
//        Dialog dialog = new Dialog(this);
//        dialog.setContentView(R.layout.pos_retry_dialog);
//        dialog.setCancelable(false);
//        if (dialog.getWindow() != null) {
//            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
//        }
//        dialog.setCanceledOnTouchOutside(false);
//
//        MaterialButton confirm = dialog.findViewById(R.id.prefer_confirm);
//        TextView logoutMsg = dialog.findViewById(R.id.logout_msg);
//        ImageView logoutImg = dialog.findViewById(R.id.dialog_logo);
//        MaterialButton printReceipt = dialog.findViewById(R.id.print_receipt);
//
//        //logoutMsg.setText(msg);
//        logoutMsg.setTextSize(16F);
//
//        // If you need to set the image or scale type for the ImageView, uncomment these:
//        // logoutImg.setImageResource(R.drawable.svg_off);
//        // logoutImg.setScaleType(ImageView.ScaleType.FIT_CENTER);
//
//        // If printerUtil is defined somewhere else, use it to print the receipt
//        // printerUtil.printReceipt(pos_sale_data);
//
//        confirm.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                dialog.dismiss();
//
//                polhandler.removeCallbacks(polrunnable);
//
//                 StartDispensing = false;
//                 ProcessStep = 0;
//                 DU_STATUS = -1;
//                 DU_Retry_Counter = 5;
//                initView();
//                startButton.setEnabled(true);
//
//
//                // Restart polling process
//                LoopPolling(PollingTimer);
//
//
//
////                Intent intent = new Intent(RSSerialActivity.this, MPOSDashboardActivity.class);
////                startActivity(intent);
////                finish();
//            }
//        });
//
//        printReceipt.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                dialog.dismiss();
//
//                final Intent  resultIntent = new Intent();
//                resultIntent.putExtra("sucess","false");
//                // resultIntent.putExtra("id", service.id)
//                // resultIntent.putExtra("service_name", service.service_name)
//                setResult (Activity.RESULT_CANCELED, resultIntent);
//                finish();
////                Log.d("xxx", new Gson().toJson(pos_sale_data));
////
////                ReceiptData receiptContent = formatReceiptContent(pos_sale_data);
////
////                // If printerUtil is defined, use it to print the receipt data
////                printerUtil.printReceiptData(pos_sale_data);
//            }
//        });
//
//        dialog.show();
//    }
//
//}
