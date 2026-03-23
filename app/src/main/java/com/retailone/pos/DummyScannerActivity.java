package com.retailone.pos;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanIntentResult;
import com.journeyapps.barcodescanner.ScanOptions;


public class DummyScannerActivity extends AppCompatActivity {

    private Button BnQRCode;
    private static final int PERMISSION_REQUEST_CAMERA = 1;
    private ActivityResultLauncher<Intent> scanQrResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dummy_scanner);

        BnQRCode = findViewById(R.id.button);

        scanQrResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                resultData -> {
                    if (resultData.getResultCode() == RESULT_OK && resultData.getData() != null) {
                        ScanIntentResult result = ScanIntentResult.parseActivityResult(resultData.getResultCode(), resultData.getData());
                        if (result.getContents() == null) {
                            Toast.makeText(this, "Scan cancelled", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
                        }
                    }
                }
        );

        BnQRCode.setOnClickListener(view -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(DummyScannerActivity.this, new String[]{android.Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
                } else {
                    startScanning();
                }
            } else {
                startScanning();
            }
        });
    }

    private void startScanning() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES);
        options.setPrompt("Scan a QR code or barcode");
        options.setOrientationLocked(true);
        scanQrResultLauncher.launch(new ScanContract().createIntent(this, options));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
}



















//public class DummyScannerActivity extends AppCompatActivity {
//
//
//    private Button BnQRCode;
//    private static final int PERMISSION_REQUEST_CAMERA = 1;
//    private ActivityResultLauncher<Intent> scanQrResultLauncher;
//
//
//
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_dummy_scanner);
//
//        BnQRCode = (Button) findViewById(R.id.button);
//
//        scanQrResultLauncher = registerForActivityResult(
//                new ActivityResultContracts.StartActivityForResult(),
//                resultData ->{
//                    if (resultData.getResultCode() == RESULT_OK) {
//                        ScanIntentResult result = ScanIntentResult.parseActivityResult(resultData.getResultCode(), resultData.getData());
//
//                        //this will be qr activity result
//                        if (result.getContents() == null) {
//                            Toast.makeText(getContext(), getString(R.string.canceled), Toast.LENGTH_LONG).show();
//
//                        } else {
//                            String qrContents = result.getContents();
//                            //TODO Handle qr result here
//                        }
//                    }
//                });
//
//
//
//
//        // requestPermission();
//
//
//
//        BnQRCode.setOnClickListener(new View.OnClickListener() {
//
//            public void onClick(View view) {
//
//
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                    if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
//                        ActivityCompat.requestPermissions(DummyScannerActivity.this, new String[]{android.Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
//                    } else {
//                        initQRCodeScanner();
//                    }
//                } else {
//                    initQRCodeScanner();
//                }
//
//            }
//        });
//
//
//    }
//
//
//    private void initQRCodeScanner() {
//        IntentIntegrator integrator = new IntentIntegrator(this);
//        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
//        integrator.setOrientationLocked(false);
//        integrator.setPrompt("Scan a QR code");
//        integrator.initiateScan();
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == PERMISSION_REQUEST_CAMERA) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                initQRCodeScanner();
//            } else {
//                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show();
//                finish();
//            }
//        }
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
//        if (result != null) {
//            if (result.getContents() == null) {
//                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_LONG).show();
//            } else {
//                Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
//            }
//        } else {
//            super.onActivityResult(requestCode, resultCode, data);
//        }
//    }
//
//
//}