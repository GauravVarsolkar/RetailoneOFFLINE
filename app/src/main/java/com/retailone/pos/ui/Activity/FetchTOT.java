package com.retailone.pos.ui.Activity;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.common.CommonConstants;
import com.common.apiutil.pos.RS232Reader;
import com.common.callback.IRSReaderListener;
import com.retailone.pos.databinding.ActivityFetchTotdataBinding;

public class FetchTOT extends AppCompatActivity implements IRSReaderListener {

    private ActivityFetchTotdataBinding binding;
    private Last_CMD lastIssuedCmd = null;
    private Handler polhandler;
    private Runnable polrunnable;
    private RS232Reader mRS232Reader;
    private String startTotalizer = "";
    private String endTotalizer = "";
    private final int DU_Unit_Id = 1;
    private final int Hose_No = 1;
    private int DU_STATUS = -1;
    private int ProcessStep = 0;
    private String startTotalizerString = "";

    private String mode;
    private boolean startPolling = false;

    private String startTOT = "";
    private int DU_Retry_Counter = 0;

    public enum Last_CMD {
        STATUS, START_TOTALIZER, END_TOTALIZER, PRESET, LAST_TXN
    }

    static int PollingTimer = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFetchTotdataBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SharedPreferences userSettings = getSharedPreferences("totalizer_value", MODE_PRIVATE);
        try {
            mRS232Reader = new RS232Reader(getApplicationContext());
            mRS232Reader.rsOpen(CommonConstants.RS232Type.RS232_1, 4800, 2);
            mRS232Reader.setRSReaderListener(this);
            Toast.makeText(this, "mRS232Reader INIT Successfully", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Serial port errot : "+ e, Toast.LENGTH_LONG).show();
            throw new RuntimeException(e);

        }

        String userStatus = getIntent().getStringExtra("USER_STATUS");
        if ("LoggedIn".equals(userStatus)) {
            binding.btnReadTotalizer.setOnClickListener(v -> {
                LoopPolling(PollingTimer);
                startPolling = true;
                binding.btnReadTotalizer.setVisibility(View.INVISIBLE);
            });

            binding.proceedBtn.setOnClickListener(v -> {
                try {
                    startTOT = startTotalizer;
                    if (startTOT.isEmpty()) {
                        startTOT = binding.etTotalizerValue.getText().toString().trim();
                    }
                    // Check if the value is still empty
                    if (startTOT.isEmpty()) {
                        new AlertDialog.Builder(FetchTOT.this)
                                .setTitle("Error")
                                .setMessage("Please enter a valid start totalizer value")
                                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                                .show();
                    } else {
                        stopPolling();
                        SharedPreferences.Editor userSettingsEditor = userSettings.edit();
                        userSettingsEditor.putString("startTOT", startTOT);
                        userSettingsEditor.putString("startTOTMode", mode);
                        userSettingsEditor.apply();
                        Intent intent = new Intent(FetchTOT.this, MPOSDashboardActivity.class);
                        startActivity(intent);

                        finish();
                    }
                } catch (Exception e) {
                    new AlertDialog.Builder(FetchTOT.this)
                            .setTitle("Error")
                            .setMessage("An unexpected error occurred: " + e.getMessage())
                            .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                            .show();
                }
            });



        }
    }

    public void LoopPolling(final int timer) {
        polhandler = new Handler();
        polrunnable = new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                try {
                    if (startPolling) {
                        if (DU_Retry_Counter <= 3) {
                            if (ProcessStep == 0) {
                                lastIssuedCmd = Last_CMD.STATUS;
                                mRS232Reader.rsSend(HexToByteArr("F" + DU_Unit_Id + Hose_No + "0B0"));
                                DU_Retry_Counter++;
                            } else if (ProcessStep == 1) {
                                if (DU_STATUS == 1) {
                                    lastIssuedCmd = Last_CMD.START_TOTALIZER;
                                    mode = "ONLINE";
                                    mRS232Reader.rsSend(HexToByteArr("F" + DU_Unit_Id + Hose_No + "9B0"));
                                    binding.tvPleaseWait.setText(startTotalizer);
                                    binding.proceedBtn.setVisibility(View.VISIBLE);
                                }
                            }
                        } else {
                            startPolling = false;
                            binding.tvPleaseWait.setVisibility(View.GONE);
                            binding.etTotalizerValue.setVisibility(View.VISIBLE);
                            binding.btnReadTotalizer.setVisibility(View.INVISIBLE);
                            binding.proceedBtn.setVisibility(View.VISIBLE);
                            mode = "OFFLINE";
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

    public void stopPolling() {
        startPolling = false;
        if (polhandler != null && polrunnable != null) {
            polhandler.removeCallbacks(polrunnable);
            Log.d("MY Data", "Polling stopped");
        }
    }

    private byte[] HexToByteArr(String inHex) {
        int hexlen = inHex.length();
        byte[] result;
        if (isOdd(hexlen) == 1) { // Odd
            hexlen++;
            result = new byte[hexlen / 2];
            inHex = "0" + inHex;
        } else { // Even
            result = new byte[hexlen / 2];
        }

        int j = 0;
        int i = 0;
        while (i < hexlen) {
            result[j] = HexToByte(inHex.substring(i, i + 2));
            j++;
            i += 2;
        }
        return result;
    }

    private int isOdd(int num) {
        return num & 0x1;
    }

    private byte HexToByte(String inHex) {
        return (byte) Integer.parseInt(inHex, 16);
    }

    @Override
    public void onRecvData(byte[] data) {
        String msg = RSSerialActivity.byteArrayToHex(data);
        if (lastIssuedCmd == Last_CMD.STATUS && msg.length() == 2) {
            try {
                DU_STATUS = Integer.parseInt(msg.substring(1, 2));
                if (DU_STATUS > 0) {
                    ProcessStep = 1;
                }
            } catch (Exception e) {
                Log.e("FetchTOT", "Invalid DU_STATUS: " + e.getMessage());
                DU_STATUS = -1; // Assign a default value if parsing fails
            }

        } else if (lastIssuedCmd == Last_CMD.START_TOTALIZER && msg.length() >= 20) {
            String tot = msg.substring(4, 16);
            tot = (tot.substring(11) + tot.charAt(9) + tot.charAt(7) + tot.charAt(5)
                    + tot.charAt(3) + tot.charAt(1));
            startTotalizer =tot;
        } else if (lastIssuedCmd == Last_CMD.END_TOTALIZER && msg.length() >= 20) {
            String tot = msg.substring(4, 16);
            tot = (tot.substring(11) + tot.charAt(9) + tot.charAt(7) + tot.charAt(5)
                    + tot.charAt(3) + tot.charAt(1));
            endTotalizer = tot;
        }
    }
}