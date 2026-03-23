package com.retailone.pos;

import static com.common.apiutil.util.SystemUtil.PRINTER_80MM_USB_COMMON;
import static com.common.apiutil.util.SystemUtil.PRINTER_PRT_COMMON;
import static com.common.apiutil.util.SystemUtil.PRINTER_SY581;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.common.apiutil.CommonException;
import com.common.apiutil.printer.UsbThermalPrinter;
import com.common.apiutil.util.StringUtil;
import com.common.apiutil.util.SystemUtil;

import java.util.ArrayList;

public class PrinterTestActivity extends AppCompatActivity {


    private String printVersion;
    private final int NOPAPER = 3;
    private final int LOWBATTERY = 4;
    private final int PRINTVERSION = 5;
    private final int PRINTBARCODE = 6;
    private final int PRINTQRCODE = 7;
    private final int PRINTPAPERWALK = 8;
    private final int PRINTCONTENT = 9;
    private final int CANCELPROMPT = 10;
    private final int PRINTERR = 11;
    private final int OVERHEAT = 12;
    private final int MAKER = 13;
    private final int PRINTPICTURE = 14;
    private final int NOBLACKBLOCK = 15;
    private final int PRINTSHORTCONTENT = 16;
    private final int PRINTLONGPICTURE = 17;
    private final int PRINTLONGTEXT = 18;
    private final int PRINTBLACK = 19;
    private final int PRINTCOLUMNS = 20;


    private String Result;
    private Boolean nopaper = false;
    private boolean LowBattery = false;

    private Button BnPrint;
    private EditText editcontent;
    private TextView textView;
    public static String printContent;


    ProgressDialog checkDialog;
    private ProgressDialog progressDialog;

    ProgressDialog dialog;


    UsbThermalPrinter mUsbThermalPrinter;

    MyHandler handler;
    int deviceType;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_printer_test);


        BnPrint = (Button) findViewById(R.id.print_btn);
        editcontent = (EditText) findViewById(R.id.content);
        textView = (TextView) findViewById(R.id.textView);

        checkDialog = new ProgressDialog(PrinterTestActivity.this);
        checkDialog.setTitle(getString(R.string.checkPrinterType));
        checkDialog.setMessage(getText(R.string.watting));
        checkDialog.setCancelable(false);

        mUsbThermalPrinter = new UsbThermalPrinter(PrinterTestActivity.this);
        deviceType = SystemUtil.getDeviceType();

        handler = new MyHandler();


       // SDKUtil.getInstance(this).initSDK();
//
//        int printerCheck = SystemUtil.checkPrinter581(this);
//
//
//        if(printerCheck == PRINTER_80MM_USB_COMMON){
//            Toast.makeText(this,"SY581 USB printer",Toast.LENGTH_SHORT).show();
////SY581 USB printer
//        }else if(printerCheck == PRINTER_SY581){
//            Toast.makeText(this,"SY581 Serial printer",Toast.LENGTH_SHORT).show();
//
////SY581 Serial printer
//        }else if(printerCheck == PRINTER_PRT_COMMON){
//            Toast.makeText(this,"Usb printer",Toast.LENGTH_SHORT).show();
//
////58mm Serial/USB printer
//        }else{
//            Toast.makeText(this,"No printer Detected "+printerCheck +SystemUtil.getPrinterType() ,Toast.LENGTH_SHORT).show();
//        }


        //Print


        BnPrint.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {

				/*String width,height;
				width = enlarge_width.getText().toString();
				height = enlarge_height.getText().toString();
				if(!"".equals(width) && !"".equals(height)){
					enlargeWidth = Integer.valueOf(width);
					enlargeHeight = Integer.valueOf(height);
				}*/


                ArrayList<Item> item = new ArrayList<>();
                item.add(new Item("item2agsfxgfgsf", 1, 56));
                item.add(new Item("item1", 2, 50));
                item.add(new Item("item7", 1, 200));
                item.add(new Item("item3", 1, 56));
                item.add(new Item("item4", 2, 50));
                item.add(new Item("item5fasddas9dtccccccccccc", 1, 200));
                item.add(new Item("item6", 1, 56));
                item.add(new Item("V7", 1, 200));

                ///  String receipt = generateReceipt(item);

                ///  textView.setText(receipt);

                String receiptHtml = generateReceipt(item);

                textView.setText(receiptHtml);

//                if (LowBattery == true) {
//                    handler.sendMessage(handler.obtainMessage(LOWBATTERY, 1, 0, null));
//                } else {
//                    if (!nopaper) {
////                        if(progressDialog == null) {
////                            progressDialog = ProgressDialog.show(PrinterTestActivity.this, getString(R.string.bl_dy),
////                                    getString(R.string.printing_wait));
////                        }
//                        handler.sendMessage(handler.obtainMessage(PRINTCONTENT, 1, 0, null));
//                    } else {
//                        Toast.makeText(PrinterTestActivity.this, getString(R.string.ptintInit), Toast.LENGTH_LONG)
//                                .show();
//                    }
//                }

            }
        });


    }

    public static String generateReceipt(ArrayList<Item> items) {
        StringBuilder printContent = new StringBuilder();
        printContent.append("\n             RetailOne\n")
                .append("---------------------------\n")
                .append("Date：2015-01-01 16:18:20\n")
                .append("invoice：12378945664\n")
                .append("id：1001000000000529142\n")
                .append("---------------------------\n")
                .append("    item        quantity   Price  total\n");


        double total = 0;
        for (Item item : items) {
            double itemTotal = item.quantity * item.price;
            printContent.append(String.format("%-14s %8d %10.2f %10.2f\n", formatItemName(item.name), item.quantity, item.price, itemTotal));
            total += itemTotal;
        }

        printContent.append("----------------------------\n")
                .append(String.format(" tax：%10.2f\n", 1000.00))
                .append("----------------------------\n")
                .append(String.format("paid：%10.2f\n", 10000.00))
                .append(String.format("tender：%10.2f\n", 1000.00))
                .append(String.format("paid：%10.2f\n", 9000.00))
                .append("----------------------------\n")
                .append(" Thanks for shopping with us\n")
                .append("tel :1111111111\n");

        return printContent.toString();
    }

    private class contentPrintThread extends Thread {

        public void run() {
            super.run();
            try {
                mUsbThermalPrinter.reset();
                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
                mUsbThermalPrinter.setLeftIndent(5);
                mUsbThermalPrinter.setLineSpace(2);
                ///  mUsbThermalPrinter.setBold(false);
                mUsbThermalPrinter.setTextSize(20);
				/*if (isSupportAutoBreak()) {
					mUsbThermalPrinter.autoBreakSet(button_auto_linefeed.isChecked());
				}*/
                mUsbThermalPrinter.setGray(4);
                //mUsbThermalPrinter.enlargeFontSize(enlargeWidth, enlargeHeight);

                if (false) {
                    mUsbThermalPrinter.setItalic(true);
                }

                if (false) {
                    mUsbThermalPrinter.setThripleHeight(true);
                }

                if (false) {
                    mUsbThermalPrinter.enlargeFontSize(2, 1);
                }

                String companyName = "RetailOne";
                String companyAddress = "123 Market St, City, Country";
                String companyPhone = "123-456-7890";
                String invoiceId = "INV123456";
                String dateTime = "2024-06-26 14:30";
                String[][] items = {
                        {"item1", "2", "100"},
                        {"item2", "3", "200"}
                };
                String subtotal = "700";  // Sum of item total prices
                String taxRate = "18";
                String grandTotal = "826";  // Subtotal + Tax amount
                String paymentMode = "Credit Card";
                String paymentStatus = "Paid";

                ///  String invoiceHtml = generateHtml(companyName, companyAddress, companyPhone, invoiceId, dateTime, items, subtotal, taxRate, grandTotal, paymentMode, paymentStatus);


                /// mUsbThermalPrinter.addString( Html.fromHtml(editcontent.getText().toString()).toString());


                // Convert HTML to plain text
                Spanned spanned;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    ///  spanned = Html.fromHtml(invoiceHtml, Html.FROM_HTML_MODE_LEGACY);
                } else {
                    /// spanned = Html.fromHtml(invoiceHtml);
                }
                //// String plainText = spanned.toString();

                //// mUsbThermalPrinter.addString(plainText);

                //// textView.setText(plainText);


                mUsbThermalPrinter.printString();
                // mUsbThermalPrinter.walkPaper(12);
                mUsbThermalPrinter.walkPaper(5);
            } catch (Exception e) {
                e.printStackTrace();
//                Result = e.toString();
//                if (Result.contains("NoPaperException")) {
//                    nopaper = true;
//                } else if (Result.contains("OverHeatException")) {
//                    handler.sendMessage(handler.obtainMessage(OVERHEAT, 1, 0, null));
//                } else {
//                    handler.sendMessage(handler.obtainMessage(PRINTERR, 1, 0, null));
//                }
            } finally {
//                if(isCircle) {
//                    handler.sendMessageDelayed(handler.obtainMessage(PRINTCONTENT), /*interval * */1000);
//                } else {
//                    runOnUiThread(new Runnable() {
//
//
//                        public void run() {
//                            // TODO Auto-generated method stub
//                            button_cricle_print.setEnabled(true);
//                            button_cricle_print.setText(getString(R.string.print_cricle_start));
//                        }
//                    });
//                }
//                handler.sendMessage(handler.obtainMessage(CANCELPROMPT, 1, 0, null));
//                if (nopaper) {
//                    handler.sendMessage(handler.obtainMessage(NOPAPER, 1, 0, null));
//                    nopaper = false;
//                    return;
//                }
            }
        }
    }


    private class MyHandler extends Handler {

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case NOPAPER:
                    noPaperDlg();
                    break;
                case LOWBATTERY:
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(PrinterTestActivity.this);
                    alertDialog.setTitle(R.string.operation_result);
                    alertDialog.setMessage(getString(R.string.LowBattery));
                    alertDialog.setPositiveButton(getString(R.string.dialog_comfirm),
                            new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialogInterface, int i) {

                                }
                            });
                    alertDialog.show();
                    break;
//                case NOBLACKBLOCK:
//                    Toast.makeText(UsbPrinterActivity.this, R.string.maker_not_find, Toast.LENGTH_SHORT).show();
//                    break;
                case PRINTVERSION:
                    dialog.dismiss();
                    if (msg.obj.equals("1")) {
                        // textPrintVersion.setText(printVersion);
                    } else {
                        //  Toast.makeText(UsbPrinterActivity.this, R.string.operation_fail, Toast.LENGTH_LONG).show();
                    }
                    break;
//                case PRINTBARCODE:
//                    new barcodePrintThread().start();
//                    break;
//                case PRINTQRCODE:
//                    new qrcodePrintThread().start();
//                    break;
//                case PRINTPAPERWALK:
//                    new paperWalkPrintThread().start();
//                    break;
                case PRINTCONTENT:
                    new contentPrintThread().start();
                    break;

                default:
                    Toast.makeText(PrinterTestActivity.this, "Print Error!", Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }


    private void noPaperDlg() {
        AlertDialog.Builder dlg = new AlertDialog.Builder(PrinterTestActivity.this);
        dlg.setTitle(getString(R.string.noPaper));
        dlg.setMessage(getString(R.string.noPaperNotice));
        dlg.setCancelable(false);
        dlg.setPositiveButton(R.string.sure, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        dlg.show();
    }


    private final BroadcastReceiver printReceive = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS,
                        BatteryManager.BATTERY_STATUS_NOT_CHARGING);
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 0);
                // TPS390 can not print,while in low battery,whether is charging or not charging
                if (deviceType == StringUtil.DeviceModelEnum.TPS390.ordinal() /*||
						"TPS320".equals(SystemUtil.getInternalModel())*/) {
                    if (level * 5 <= scale) {
                        LowBattery = true;
                    } else {
                        LowBattery = false;
                    }
                } else if (SystemUtil.getInternalModel().equals("M8")) {
                    if (level * 10 <= scale) {
                        LowBattery = true;
                    } else {
                        LowBattery = false;
                    }
                } else {
                    if (status != BatteryManager.BATTERY_STATUS_CHARGING) {
                        if (level * 5 <= scale) {
                            LowBattery = true;
                        } else {
                            LowBattery = false;
                        }
                    } else {
                        LowBattery = false;
                    }
                }
            }
            // Only use for TPS550MTK devices
            else if (action.equals("android.intent.action.BATTERY_CAPACITY_EVENT")) {
                int status = intent.getIntExtra("action", 0);
                int level = intent.getIntExtra("level", 0);
                if (status == 0) {
                    if (level < 1) {
                        LowBattery = true;
                    } else {
                        LowBattery = false;
                    }
                } else {
                    LowBattery = false;
                }
            }
        }
    };


    protected void onStart() {
        // TODO Auto-generated method stub

        IntentFilter pIntentFilter = new IntentFilter();
        pIntentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        pIntentFilter.addAction("android.intent.action.BATTERY_CAPACITY_EVENT");
        registerReceiver(printReceive, pIntentFilter);

        dialog = new ProgressDialog(PrinterTestActivity.this);
        dialog.setTitle("idcard_czz");
        dialog.setMessage(getText(R.string.watting));
        dialog.setCancelable(false);
        dialog.show();
        new Thread(new Runnable() {


            public void run() {
                try {
                    mUsbThermalPrinter.start(0);//低速
//					mUsbThermalPrinter.start(1);//高速
                    mUsbThermalPrinter.reset();
                    printVersion = mUsbThermalPrinter.getVersion();
                } catch (CommonException e) {
                    e.printStackTrace();
                } finally {
                    if (printVersion != null) {
                        Message message = new Message();
                        message.what = PRINTVERSION;
                        message.obj = "1";
                        handler.sendMessage(message);
                    } else {
                        Message message = new Message();
                        message.what = PRINTVERSION;
                        message.obj = "0";
                        handler.sendMessage(message);
                    }
                }
            }
        }).start();
        super.onStart();
    }


    protected void onStop() {
        // TODO Auto-generated method stub
        if (progressDialog != null && !PrinterTestActivity.this.isFinishing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }
        //isCircle = false;
        unregisterReceiver(printReceive);
        mUsbThermalPrinter.stop();
        //circleQinCheng = false;
        super.onStop();
    }

    private static String formatItemName(String name) {
        if (name.length() > 14) {
            return name.substring(0, 14);
        } else {
            return String.format("%-14s", name);
        }
    }
}


class Item {
    String name;
    int quantity;
    double price;

    Item(String name, int quantity, double price) {
        this.name = name;
        this.quantity = quantity;
        this.price = price;
    }

}