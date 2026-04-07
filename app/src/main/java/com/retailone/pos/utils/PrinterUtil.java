package com.retailone.pos.utils;


import static android.provider.MediaStore.Images.Media.getBitmap;

import android.app.AlertDialog;
import android.app.ProgressDialog;

import com.retailone.pos.models.PointofsaleModel.PosSaleModel.PosSalesItem;
import com.retailone.pos.models.PosSalesDetailsModel.CopyReceiptRes;
import com.retailone.pos.models.PosSalesDetailsModel.ReceiptType;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.common.apiutil.CommonException;
import com.common.apiutil.printer.NewUsbThermalPrinter;
import com.common.apiutil.printer.UsbThermalPrinter;
//import com.common.apiutil.util.SDKUtil;
import com.common.apiutil.util.StringUtil;
import com.common.apiutil.util.SystemUtil;
import com.retailone.pos.R;
import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper;
import com.retailone.pos.models.LocalizationModel.LocalizationData;
import com.retailone.pos.models.PosSalesDetailsModel.PosSalesDetails;
import com.retailone.pos.models.PosSalesDetailsModel.SalesItem;
import com.retailone.pos.models.PosSalesDetailsModel.TaxSummary;
import com.retailone.pos.models.PrinterModel.ReceiptData;
import com.retailone.pos.models.ReturnSalesItemModel.ReturnSaleResModel.ReturnSaleRes;
import com.retailone.pos.models.ReturnSalesItemModel.ReturnSaleResModel.ReturnedItem;
import com.retailone.pos.models.PosSalesDetailsModel.SaleReceiptRes;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import org.jetbrains.annotations.NotNull;

import com.retailone.pos.models.PosSalesDetailsModel.VsdcReceipt;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;



public class PrinterUtil {

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

    private final int TSIZE20 = 36;
    private final int TSIZE22 = 34;
    private final int TSIZE24 = 31;
    private final int TSIZE26 = 29;
    private final int TSIZE28 = 27;
    private final int TSIZE30 = 26;
    private final int TSIZE32 = 23;


    private NewUsbThermalPrinter mUsbThermalPrinter;
    private ProgressDialog dialog;
    private ProgressDialog progressDialog;
    private MyHandler handler;
    private boolean LowBattery = false;
    private Context context;
    private int deviceType;
    private boolean nopaper = false;
    private boolean looseOil = false;
    ArrayList<MyItem> itemx ;

    private String Result;

    private String currency;
    private String zone;
    String printType = "";
    LocalizationData localizationData;

    String productname = "";

    int numberOfItems = 0;


    PosSalesDetails posSalesDetails;
    ReturnSaleRes returnSaleRes;
    ReceiptData receipt_data;

    public PrinterUtil(Context context) {
        this.context = context;
        mUsbThermalPrinter = new NewUsbThermalPrinter(context);
        deviceType = SystemUtil.getDeviceType();
        handler = new MyHandler();
        currency =  new LocalizationHelper(context).getLocalizationData().getCurrency();
        zone =  new LocalizationHelper(context).getLocalizationData().getTimezone();

        /// SDKUtil.getInstance(context).initSDK();
        initializePrinter();
    }

    private void initializePrinter() {
        dialog = new ProgressDialog(context);
        dialog.setTitle("Initializing Printer");
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        new Thread(() -> {
            try {
                mUsbThermalPrinter.start(0);
                mUsbThermalPrinter.reset();
            } catch (CommonException e) {
                e.printStackTrace();
            } finally {
                dialog.dismiss();
            }
        }).start();
    }

    public void printReceiptData(PosSalesDetails _posSalesDetails) {
        printType = "SALE";

        //  receipt_data = receiptData;
        posSalesDetails = _posSalesDetails;

        if (LowBattery) {
            handler.sendMessage(handler.obtainMessage(LOWBATTERY, 1, 0, null));
        } else {
            if (!nopaper) {
                // handler.sendMessage(handler.obtainMessage(PRINTPICTURE, 1, 0, null));

                handler.sendMessage(handler.obtainMessage(PRINTCONTENT, 1, 0, null));
                //Toast.makeText(context, "Paper Available", Toast.LENGTH_LONG).show();

            } else {
                Toast.makeText(context, "No paper detected", Toast.LENGTH_LONG).show();
            }
        }

    }

    public void printReturnReceiptData( ReturnSaleRes _returnSaleRes) {
        printType = "RETURN";

        // Toast.makeText(context, "return print 2", Toast.LENGTH_LONG).show();


        returnSaleRes = _returnSaleRes;

        if (LowBattery) {
            handler.sendMessage(handler.obtainMessage(LOWBATTERY, 1, 0, null));
        } else {
            if (!nopaper) {
                // handler.sendMessage(handler.obtainMessage(PRINTPICTURE, 1, 0, null));

                handler.sendMessage(handler.obtainMessage(PRINTCONTENT, 1, 0, null));
                //Toast.makeText(context, "Paper Available", Toast.LENGTH_LONG).show();

            } else {
                Toast.makeText(context, "No paper detected", Toast.LENGTH_LONG).show();
            }
        }

    }

//    public void printReceipt(@NotNull PosSalesDetails posSaleData) {
//
//        posSalesDetails = posSaleData;
//
//        if (LowBattery) {
//            handler.sendMessage(handler.obtainMessage(4, 1, 0, null));
//        } else {
//            if (!nopaper) {
//                handler.sendMessage(handler.obtainMessage(9, 1, 0, null));
//                Toast.makeText(context, "Paper Available", Toast.LENGTH_LONG).show();
//
//            } else {
//                Toast.makeText(context, "No paper detected", Toast.LENGTH_LONG).show();
//            }
//        }
//
//    }

    private class MyHandler extends Handler {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case NOPAPER:
                    //NOPAPER
                    noPaperDlg();
                    break;
                case LOWBATTERY:
                    //LOWBATTERY
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
                    alertDialog.setTitle("Operation Result");
                    alertDialog.setMessage("Low Battery");
                    alertDialog.setPositiveButton("OK", (dialog, which) -> {
                    });
                    alertDialog.show();
                    break;
                case PRINTCONTENT:
                    //Toast.makeText(context, "printContent", Toast.LENGTH_LONG).show();
                    new contentPrintThread().start();
                    break;

                case PRINTPICTURE:
                    new printPicture().start();
                    break;

                case NOBLACKBLOCK:
                    //NOBLACKBLOCK
                    Toast.makeText(context, R.string.maker_not_find, Toast.LENGTH_SHORT).show();
                    break;
//                case 10:
//                    //CANCELPROMPT
//                    if (progressDialog != null && !UsbPrinterActivity.this.isFinishing()) {
//                        progressDialog.dismiss();
//                        progressDialog = null;
//                    }
//                    break;
                default:
                    // Toast.makeText(context, "Print Error!", Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    private void noPaperDlg() {
        AlertDialog.Builder dlg = new AlertDialog.Builder(context);
        dlg.setTitle("No Paper");
        dlg.setMessage("Please load paper and try again.");
        dlg.setCancelable(false);
        dlg.setPositiveButton("OK", (dialog, which) -> {
        });
        dlg.show();
    }



    private class printPicture extends Thread {

        public void run() {
            super.run();
            try {
                mUsbThermalPrinter.reset();
                mUsbThermalPrinter.setGray(3);
                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
                //File file = new File(picturePath);
                //if (file.exists()) {
                mUsbThermalPrinter.printLogo(drawableToBitmap(ContextCompat.getDrawable(context,R.drawable.mlogo)), false);
                mUsbThermalPrinter.walkPaper(20);
				/*} else {
					runOnUiThread(new Runnable() {


						public void run() {
							Toast.makeText(UsbPrinterActivity.this, getString(R.string.not_find_picture),
									Toast.LENGTH_LONG).show();
						}
					});
				}*/
            } catch (Exception e) {
                e.printStackTrace();
                Result = e.toString();
                if (Result.contains("NoPaperException")) {
                    nopaper = true;
                } else if (Result.contains("OverHeatException")) {
                    handler.sendMessage(handler.obtainMessage(OVERHEAT, 1, 0, null));
                } else {
                    handler.sendMessage(handler.obtainMessage(PRINTERR, 1, 0, null));
                }
            } finally {
                handler.sendMessage(handler.obtainMessage(CANCELPROMPT, 1, 0, null));
                if (nopaper) {
                    handler.sendMessage(handler.obtainMessage(NOPAPER, 1, 0, null));
                    nopaper = false;
                    return;
                }
            }
        }
    }


    public Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap;

        if (drawable instanceof BitmapDrawable) {
            // If the drawable is a BitmapDrawable, just return its bitmap
            bitmap = ((BitmapDrawable) drawable).getBitmap();
        } else {
            // Otherwise, create a new bitmap and draw the drawable on it
            int width = drawable.getIntrinsicWidth();
            int height = drawable.getIntrinsicHeight();

            // Ensure dimensions are valid, otherwise, use default dimensions
            width = width > 0 ? width : 1;
            height = height > 0 ? height : 1;

            // Create a bitmap with the specified width and height
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            // Create a canvas to draw on the bitmap
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        }

        return bitmap;
    }


    private class contentPrintThread extends Thread {
        public void run() {
            super.run();
            if(printType.equals("SALE")){

                printSaleType(posSalesDetails);

            }else if(printType.equals("RETURN")){

                printReturnType(returnSaleRes);


            }
        }
    }

    public void printReturnType(ReturnSaleRes details) {
        try {
            // â”€â”€ Resolve rcptType once, safely â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            String rcptType = (details.getData().getRcptType() != null)
                    ? details.getData().getRcptType() : "";

            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setLeftIndent(1);
            mUsbThermalPrinter.setLineSpace(3);
            mUsbThermalPrinter.setTextSize(20);
            mUsbThermalPrinter.setGray(6);
            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.addString("*** START OF LEGAL RECEIPT ***");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.setGray(6);
            mUsbThermalPrinter.addString("CIS Version : 1.0.1");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(3);
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);

            Bitmap logoBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.image22);
            logoBitmap = Bitmap.createScaledBitmap(logoBitmap, 400, 200, true);
            mUsbThermalPrinter.printLogo(logoBitmap, false);
            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setTextSize(32);
            mUsbThermalPrinter.setGray(6);
            mUsbThermalPrinter.setBold(true);
            mUsbThermalPrinter.addString(details.getData().getStore().getStore_name().toString().toUpperCase());
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setTextSize(26);
            mUsbThermalPrinter.setGray(6);
            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.addString(details.getData().getStore().getAddress().toString().toUpperCase());
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setTextSize(24);
            mUsbThermalPrinter.setGray(6);
            mUsbThermalPrinter.addString("TIN NO:" + details.getData().getTpin_no());
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            // â”€â”€ TOP: rcptType label (matches printSaleType pattern) â”€â”€â”€â”€â”€â”€
            if (rcptType.equalsIgnoreCase("P") || rcptType.equalsIgnoreCase("Proforma")) {
                mUsbThermalPrinter.setBold(true);
                mUsbThermalPrinter.setTextSize(22);
                mUsbThermalPrinter.addString("PROFORMA");
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.walkPaper(1);
                mUsbThermalPrinter.setBold(false);
                mUsbThermalPrinter.addString("----------------------------------");
                mUsbThermalPrinter.printString();
            } else if (rcptType.equalsIgnoreCase("T") || rcptType.equalsIgnoreCase("Training")) {
                mUsbThermalPrinter.setBold(true);
                mUsbThermalPrinter.setTextSize(22);
                mUsbThermalPrinter.addString("TRAINING MODE");
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.walkPaper(1);
                mUsbThermalPrinter.setBold(false);
                mUsbThermalPrinter.addString("----------------------------------");
                mUsbThermalPrinter.printString();
            } else if (rcptType.equalsIgnoreCase("C") || rcptType.equalsIgnoreCase("Copy")) {
                mUsbThermalPrinter.setBold(true);
                mUsbThermalPrinter.setTextSize(22);
                mUsbThermalPrinter.addString("COPY");
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.walkPaper(1);
                mUsbThermalPrinter.setBold(false);
                mUsbThermalPrinter.addString("----------------------------------");
                mUsbThermalPrinter.printString();
            } else {
                mUsbThermalPrinter.setBold(false);
                mUsbThermalPrinter.setTextSize(22);
                mUsbThermalPrinter.addString("----------------------------------");
                mUsbThermalPrinter.printString();
            }
            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.setBold(true);
            mUsbThermalPrinter.setTextSize(23);
            mUsbThermalPrinter.addString("REFUND");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);

            String refLabel = "Ref. Normal Receipt:";
            String refValue = String.valueOf((long) Double.parseDouble(String.valueOf(details.getData().getOgRcpt_no())));
            mUsbThermalPrinter.addString(refLabel + padLeft(refValue, TSIZE22 - refLabel.length()));
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.addString("----------------------------------");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.addString("REFUND IS APPROVED ONLY FOR ORIGINAL SALES RECEIPT");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.addString("----------------------------------");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.setGray(6);

            String buyerName = details.getData() != null && details.getData().getCustomer_name() != null
                    ? details.getData().getCustomer_name() : "";
            String buyerNameLabel = "BUYER'S NAME:";
            mUsbThermalPrinter.addString(buyerNameLabel + padLeft(buyerName, TSIZE22 - buyerNameLabel.length()));
            mUsbThermalPrinter.printString();

            String buyerTin = (details.getData() != null && details.getData().getBuyers_tpin() != null)
                    ? details.getData().getBuyers_tpin() : "";
            String buyerTinLabel = "BUYER'S TIN:";
            mUsbThermalPrinter.addString(buyerTinLabel + padLeft(buyerTin, TSIZE22 - buyerTinLabel.length()));
            mUsbThermalPrinter.printString();

            String buyerContact = (details.getData() != null && details.getData().getCustomer_mob_no() != null)
                    ? details.getData().getCustomer_mob_no() : "";
            String buyerContactLabel = "BUYER'S CONTACT:";
            mUsbThermalPrinter.addString(buyerContactLabel + padLeft(buyerContact, TSIZE22 - buyerContactLabel.length()));
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.addString("----------------------------------");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            // â”€â”€ Items loop â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            for (ReturnedItem item : details.getData().getReturned_items()) {
                productname = item.getProduct_name();
                looseOil = productname.toLowerCase().startsWith("bulk oil");

                mUsbThermalPrinter.setTextSize(24);
                mUsbThermalPrinter.setGray(6);
                mUsbThermalPrinter.setBold(true);
                mUsbThermalPrinter.addString(item.getProduct_name());
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.setBold(false);
                mUsbThermalPrinter.setTextSize(22);

                String taxCode = "";
                if (item.getTax_details() != null && item.getTax_details().getCode() != null) {
                    taxCode = item.getTax_details().getCode();
                }

                String rateCol   = FunUtils.INSTANCE.formatPrintPrice(Double.toString(item.getRetail_price())) + "x";
                String qtyCol    = FunUtils.INSTANCE.DtoString(item.getReturn_quantity());
                String amountCol = "-" + FunUtils.INSTANCE.formatPrintPrice(Double.toString(item.getTotal_amount())) + taxCode;

                int totalWidth    = TSIZE22;
                int rightColWidth = 10;
                int midColWidth   = 8;
                int leftColWidth  = totalWidth - midColWidth - rightColWidth;

                String line2 = String.format(
                        "%-" + leftColWidth + "s%" + midColWidth + "s%" + rightColWidth + "s",
                        rateCol, qtyCol, amountCol
                );
                mUsbThermalPrinter.addString(line2);
                mUsbThermalPrinter.printString();

                Double discount    = item.getDiscount();
                Double totalAmount = item.getTotal_amount();
                if (discount != null && discount > 0 && totalAmount != null && totalAmount > 0) {
                    Double discountPercent = (discount / totalAmount) * 100.0;
                    String discountText    = "discount -" + FunUtils.INSTANCE.DtoString(discountPercent) + "%";
                    double finalAmount     = totalAmount - discount;
                    String discountAmountStr = FunUtils.INSTANCE.formatPrintPrice(Double.toString(finalAmount));
                    String discountLine = String.format(
                            "%-" + (totalWidth - rightColWidth) + "s%" + rightColWidth + "s",
                            discountText, discountAmountStr
                    );
                    mUsbThermalPrinter.addString(discountLine);
                    mUsbThermalPrinter.printString();
                }
                mUsbThermalPrinter.walkPaper(1);
            }

            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.addString("----------------------------------");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            // â”€â”€ "THIS IS NOT AN OFFICIAL RECEIPT" (fixed: uses equalsIgnoreCase) â”€â”€
            if (rcptType.equalsIgnoreCase("P") || rcptType.equalsIgnoreCase("Proforma") ||
                    rcptType.equalsIgnoreCase("T") || rcptType.equalsIgnoreCase("Training") ||
                    rcptType.equalsIgnoreCase("C") || rcptType.equalsIgnoreCase("Copy")) {
                mUsbThermalPrinter.setGray(6);
                mUsbThermalPrinter.setAlgin(1);
                mUsbThermalPrinter.setBold(true);
                mUsbThermalPrinter.setTextSize(23);
                mUsbThermalPrinter.addString("THIS IS NOT AN OFFICIAL RECEIPT");
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.walkPaper(1);
                mUsbThermalPrinter.setTextSize(22);
                mUsbThermalPrinter.setBold(false);
                mUsbThermalPrinter.addString("----------------------------------");
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.walkPaper(1);
            }

            // â”€â”€ TOTAL â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            mUsbThermalPrinter.setTextSize(26);
            mUsbThermalPrinter.setBold(true);
            String label   = "TOTAL(" + currency + "):";
            String value   = FunUtils.INSTANCE.formatPrintPrice(String.valueOf(details.getData().getGrand_total()));
            int padding    = TSIZE26 - label.length();
            mUsbThermalPrinter.addString(label + padLeft(value, padding));
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.setBold(false);

            List<TaxSummary> taxSummaryList = details.getData().getTax_summery();
            if (taxSummaryList != null && !taxSummaryList.isEmpty()) {
                for (TaxSummary taxSummary : taxSummaryList) {
                    String code         = taxSummary.getCode();
                    Double taxableValue = taxSummary.getTaxable_value();
                    if (code != null) {
                        String taxLabel = "TOTAL " + taxSummary.getCode_name();
                        mUsbThermalPrinter.addString(taxLabel + padLeft(
                                FunUtils.INSTANCE.formatPrintPrice(String.valueOf(taxableValue)),
                                TSIZE22 - taxLabel.length()
                        ));
                        mUsbThermalPrinter.printString();
                    }
                }
                for (TaxSummary taxSummary : taxSummaryList) {
                    String code = taxSummary.getCode();
                    Double taxAmount  = taxSummary.getTax_amount();
                    if (taxAmount != null && taxAmount < 0) {
                        String taxAmountLabel = "TOTAL TAX " + code;
                        mUsbThermalPrinter.addString(taxAmountLabel + padLeft(
                                FunUtils.INSTANCE.formatPrintPrice(String.valueOf(taxAmount)),
                                TSIZE22 - taxAmountLabel.length()
                        ));
                        mUsbThermalPrinter.printString();
                    }
                }
                Double totalTaxAmountVal = details.getData().getTax_amount();
                if (totalTaxAmountVal != null && totalTaxAmountVal != 0.0) {
                    String totalTaxLabel = "TOTAL TAX AMOUNT";
                    mUsbThermalPrinter.addString(totalTaxLabel + padLeft(
                            FunUtils.INSTANCE.formatPrintPrice(String.valueOf(totalTaxAmountVal)),
                            TSIZE22 - totalTaxLabel.length()
                    ));
                    mUsbThermalPrinter.printString();
                }
            }

            mUsbThermalPrinter.walkPaper(1);
            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.addString("----------------------------------");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            // â”€â”€ MID-BOTTOM: rcptType label (matches printSaleType pattern) â”€â”€
            if (rcptType.equalsIgnoreCase("P") || rcptType.equalsIgnoreCase("Proforma")) {
                mUsbThermalPrinter.setGray(6);
                mUsbThermalPrinter.setAlgin(1);
                mUsbThermalPrinter.setBold(true);
                mUsbThermalPrinter.setTextSize(23);
                mUsbThermalPrinter.addString("PROFORMA");
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.setTextSize(22);
                mUsbThermalPrinter.setBold(false);
                mUsbThermalPrinter.addString("----------------------------------");
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.walkPaper(1);
            } else if (rcptType.equalsIgnoreCase("T") || rcptType.equalsIgnoreCase("Training")) {
                mUsbThermalPrinter.setGray(6);
                mUsbThermalPrinter.setAlgin(1);
                mUsbThermalPrinter.setBold(true);
                mUsbThermalPrinter.setTextSize(23);
                mUsbThermalPrinter.addString("TRAINING MODE");
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.setTextSize(22);
                mUsbThermalPrinter.setBold(false);
                mUsbThermalPrinter.addString("----------------------------------");
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.walkPaper(1);
            } else if (rcptType.equalsIgnoreCase("C") || rcptType.equalsIgnoreCase("Copy")) {
                mUsbThermalPrinter.setGray(6);
                mUsbThermalPrinter.setAlgin(1);
                mUsbThermalPrinter.setBold(true);
                mUsbThermalPrinter.setTextSize(23);
                mUsbThermalPrinter.addString("COPY");
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.setTextSize(22);
                mUsbThermalPrinter.setBold(false);
                mUsbThermalPrinter.addString("----------------------------------");
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.walkPaper(1);
            } else {
                mUsbThermalPrinter.walkPaper(1);
            }

            // â”€â”€ Payment / Items count (skip for Proforma) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            if (!rcptType.equalsIgnoreCase("P") && !rcptType.equalsIgnoreCase("Proforma")) {
                mUsbThermalPrinter.setBold(false);
                mUsbThermalPrinter.setTextSize(22);
                String grandTotal = FunUtils.INSTANCE.formatPrintPrice(String.valueOf(details.getData().getGrand_total()));
                String paymentLabel = "CASH:";
                mUsbThermalPrinter.addString(paymentLabel + padLeft(grandTotal, TSIZE22 - paymentLabel.length()));
                mUsbThermalPrinter.printString();

                mUsbThermalPrinter.setBold(false);
                mUsbThermalPrinter.setTextSize(22);
                mUsbThermalPrinter.addString("Items:" + padLeft(
                        Integer.toString(details.getData().getReturned_items().size()), TSIZE22 - 6
                ));
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.walkPaper(1);
                mUsbThermalPrinter.addString("----------------------------------");
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.walkPaper(1);
            } else {
                mUsbThermalPrinter.walkPaper(1); // maintain spacing
                mUsbThermalPrinter.addString("----------------------------------");
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.walkPaper(1);
            }


            // â”€â”€ SDC INFORMATION â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            mUsbThermalPrinter.setAlgin(1);
            mUsbThermalPrinter.setBold(true);
            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.addString("SDC INFORMATION");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.setGray(6);
            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.setAlgin(0);

            VsdcReceipt vsdc = (details.getData().getVsdc_reciept() != null && !details.getData().getVsdc_reciept().isEmpty())
                    ? details.getData().getVsdc_reciept().get(0) : null;

            String rawDateTime = (vsdc != null) ? vsdc.getVsdcRcptPbctDate() : "";
            String formattedDate = "", formattedTime = "";
            if (rawDateTime != null && !rawDateTime.isEmpty()) {
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
                    Date parsedDate = inputFormat.parse(rawDateTime);
                    if (parsedDate != null) {
                        formattedDate = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(parsedDate);
                        formattedTime = new SimpleDateFormat("HH:mm:ss",    Locale.getDefault()).format(parsedDate);
                    }
                } catch (ParseException e) {
                    Log.e("DateFormat", "Parsing failed: " + e.getMessage());
                    formattedDate = rawDateTime;
                }
            }

            String dateLabel = "DATE: " + formattedDate;
            String timeLabel = "TIME: " + formattedTime;
            int spaces = TSIZE22 - dateLabel.length() - timeLabel.length();
            if (spaces < 1) spaces = 1;
            StringBuilder dateLine = new StringBuilder(dateLabel);
            for (int i = 0; i < spaces; i++) dateLine.append(" ");
            dateLine.append(timeLabel);
            mUsbThermalPrinter.addString(dateLine.toString());
            mUsbThermalPrinter.printString();

            String sdcIdLabel = "SDC ID:";
            String sdcIdValue = (vsdc != null && vsdc.getSdcId() != null)
                    ? vsdc.getSdcId() : "";
            mUsbThermalPrinter.addString(sdcIdLabel + padLeft(sdcIdValue, TSIZE22 - sdcIdLabel.length()));
            mUsbThermalPrinter.printString();

            // Build receipttype code
            String rcptTypeValue = details.getData().getRcptType() != null ? details.getData().getRcptType() : "N";
            String receipttype;
            if      (rcptTypeValue.equalsIgnoreCase("N") || rcptTypeValue.equalsIgnoreCase("Normal"))   receipttype = "N";
            else if (rcptTypeValue.equalsIgnoreCase("P") || rcptTypeValue.equalsIgnoreCase("Proforma")) receipttype = "P";
            else if (rcptTypeValue.equalsIgnoreCase("T") || rcptTypeValue.equalsIgnoreCase("Training")) receipttype = "T";
            else if (rcptTypeValue.equalsIgnoreCase("C") || rcptTypeValue.equalsIgnoreCase("Copy"))     receipttype = "C";
            else receipttype = rcptTypeValue;

            String sdcRcptLabel = "RECEIPT NUMBER:";
            String sdcRcptValue = (details.getData().getReturned_invoice_id() != null
                    ? details.getData().getReturned_invoice_id() : "")
                    + "/" + (vsdc != null ? vsdc.getTotRcptNo() : "")
                    + " " + receipttype + "R";
            mUsbThermalPrinter.addString(sdcRcptLabel + padLeft(sdcRcptValue, TSIZE22 - sdcRcptLabel.length()));
            mUsbThermalPrinter.printString();

            // â”€â”€ Internal Data + Signature + QR: SKIP for Proforma & Training â”€â”€
            if (!rcptType.equalsIgnoreCase("T") && !rcptType.equalsIgnoreCase("Training") &&
                    !rcptType.equalsIgnoreCase("P") && !rcptType.equalsIgnoreCase("Proforma")) {

                mUsbThermalPrinter.setAlgin(1);
                mUsbThermalPrinter.addString("INTERNAL DATA:  " + (vsdc != null ? vsdc.getIntrlData() : ""));
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.addString("RECEIPT SIGNATURE:  " + (vsdc != null ? vsdc.getRcptSign() : ""));
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.walkPaper(1);

                try {
                    if (vsdc != null &&
                            vsdc.getQrCodeUrl() != null &&
                            !vsdc.getQrCodeUrl().isEmpty()) {

                        mUsbThermalPrinter.reset();
                        mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);

                        String qrData   = vsdc.getQrCodeUrl();
                        Bitmap qrBitmap = generateQRCodeBitmap(qrData, 250, 250);

                        if (qrBitmap != null) {
                            mUsbThermalPrinter.setGray(6);
                            mUsbThermalPrinter.printLogo(qrBitmap, false);
                            mUsbThermalPrinter.walkPaper(2);
                        } else {
                            Log.w("PrinterUtil", "QR code bitmap is null");
                        }
                    } else {
                        Log.w("PrinterUtil", "No QR code URL available - skipping QR code");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("PrinterUtil", "Exception in QR code section: " + e.getMessage());
                }
            } // END skip block for Proforma / Training

            mUsbThermalPrinter.setAlgin(0);
            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.setGray(6);
            mUsbThermalPrinter.addString("----------------------------------");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            String rcptNumLabel = "RECEIPT NUMBER:";
            String receiptNum   = (details.getData().getReturned_invoice_id() != null)
                    ? details.getData().getReturned_invoice_id() : "";
            mUsbThermalPrinter.addString(rcptNumLabel + padLeft(receiptNum, TSIZE22 - rcptNumLabel.length()));
            mUsbThermalPrinter.printString();

            String rawReturnDateTime = (details.getData().getReturned_date() != null)
                    ? details.getData().getReturned_date() : "";
            String formattedReturnDate = "", formattedReturnTime = "";
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault());
                Date parsedDate = inputFormat.parse(rawReturnDateTime);
                if (parsedDate != null) {
                    formattedReturnDate = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(parsedDate);
                    formattedReturnTime = new SimpleDateFormat("HH:mm:ss",    Locale.getDefault()).format(parsedDate);
                }
            } catch (ParseException e) {
                Log.e("DateFormat", "Parsing failed: " + e.getMessage());
                formattedReturnDate = rawReturnDateTime;
            }

            String returnDateLabel = "DATE: " + formattedReturnDate;
            String returnTimeLabel = "TIME: " + formattedReturnTime;
            int returnSpaces = TSIZE22 - returnDateLabel.length() - returnTimeLabel.length();
            if (returnSpaces < 1) returnSpaces = 1;
            StringBuilder returnDateLine = new StringBuilder(returnDateLabel);
            for (int i = 0; i < returnSpaces; i++) returnDateLine.append(" ");
            returnDateLine.append(returnTimeLabel);
            mUsbThermalPrinter.addString(returnDateLine.toString());
            mUsbThermalPrinter.printString();

            String mrcLabel = "MRC NO.:";
            String mrcValue = (vsdc != null && vsdc.getMrcNo() != null)
                    ? vsdc.getMrcNo() + "." : ".";
            mUsbThermalPrinter.addString(mrcLabel + padLeft(mrcValue, TSIZE22 - mrcLabel.length()));
            mUsbThermalPrinter.printString();

            mUsbThermalPrinter.setAlgin(0);
            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.addString("----------------------------------");
            mUsbThermalPrinter.printString();

            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.setGray(6);
            mUsbThermalPrinter.addString("THANK YOU");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(3);

            mUsbThermalPrinter.setAlgin(1);
            mUsbThermalPrinter.setTextSize(20);
            mUsbThermalPrinter.addString("*** END ***");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(5);
            mUsbThermalPrinter.addString(" ");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(5);
            mUsbThermalPrinter.reset();

        } catch (Exception e) {
            e.printStackTrace();
            Result = e.toString();
            if (Result.contains("NoPaperException")) {
                nopaper = true;
            } else if (Result.contains("OverHeatException")) {
                handler.sendMessage(handler.obtainMessage(OVERHEAT, 1, 0, null));
            } else {
                handler.sendMessage(handler.obtainMessage(PRINTERR, 1, 0, null));
            }
        } finally {
            handler.sendMessage(handler.obtainMessage(CANCELPROMPT, 1, 0, null));
            if (nopaper) {
                handler.sendMessage(handler.obtainMessage(NOPAPER, 1, 0, null));
                nopaper = false;
            }
        }
    }



    /// for debug invoice print
   /* private void printReturnType(ReturnSaleRes   details) {

        try {
            String printStr = "";

            Log.d("Line 1", "* START OF LEGEAL RECEIPT *");
            printStr = details.getData().getStore().getStore_name().toString().toUpperCase();
            Log.d("Line 1", printStr);


            printStr = details.getData().getStore().getAddress().toString().toUpperCase();
            Log.d("Line 1", printStr);

            printStr = "VAT NO:" + padLeft(details.getData().getVat_no(), TSIZE22-7);
            Log.d("Line 1", printStr);
            printStr = "TPIN N0:" + padLeft(details.getData().getTpin_no(), TSIZE22-8);
            Log.d("Line 1", printStr);
            //printStr = "DATE:" + padLeft(DateTimeFormatting.Companion.formatSaleReturndate(details.getData().getReturned_date(),zone), TSIZE22-7);
            printStr = "DATE:" + padLeft(details.getData().getReturned_date(), TSIZE22-7);
            Log.d("Line 1", printStr);

            printStr = "RECEIPT NO:" + padLeft(details.getData().getReturned_invoice_id(), TSIZE22-11);
            Log.d("Line 1", printStr);
            printStr = "BUYERÃ¢â‚¬â„¢S NAME:" + padLeft(details.getData() != null && details.getData().getCustomer_name() != null ? details.getData().getCustomer_name() : "", TSIZE22-13);
            Log.d("Line 1", printStr);

            printStr = "BUYERÃ¢â‚¬â„¢S TPIN:" + padLeft(details.getData().getBuyers_tpin(), TSIZE22-13);
            Log.d("Line 1", printStr);

            printStr = "Description";
            Log.d("Line 1", printStr);
            printStr = "Qty X Rate                 Amount";
            Log.d("Line 1", printStr);

            printStr = "----------------------------------";
            Log.d("Line 1", printStr);


            for ( ReturnedItem item : details.getData().getReturned_items()){
                printStr = item.getProduct_name();
                Log.d("Line 1", printStr);

                //String qtyRate = FunUtils.INSTANCE.DtoString(item.getQuantity()) + " X " + FunUtils.INSTANCE.formatPrintPrice(item.getRetail_price());
                String qtyRate = FunUtils.INSTANCE.DtoString(item.getReturn_quantity())+" X " + FunUtils.INSTANCE.formatPrintPrice(Double.toString(item.getRetail_price()));
                String amount = FunUtils.INSTANCE.formatPrintPrice(Double.toString(item.getTotal_returned_amount()));
                String Line2 = qtyRate + padLeft(amount, TSIZE22 - (qtyRate.length()));
                printStr = Line2;
                Log.d("Line 1", printStr);
            }

            printStr = "----------------------------------";
            Log.d("Line 1", printStr);


            printStr = "TOTAL (" + currency + ") :" + padLeft(FunUtils.INSTANCE.formatPrintPrice(Double.toString(details.getData().getTotal())), TSIZE26 - (("TOTAL (" + currency + ") :").length()));
            Log.d("Line 1", printStr);


            printStr = "Items:" + padLeft(Integer.toString(details.getData().getReturned_items().size()), TSIZE22 - 6 );
            Log.d("Line 1", printStr);
            printStr = "TAX EX:" + padLeft(FunUtils.INSTANCE.formatPrintPrice(details.getData().getTax_ex()), TSIZE22 - 7 );
            Log.d("Line 1", printStr);
            printStr = "TAX VAT @" + padLeft(details.getData().getTax()+"%", TSIZE22 - 9 );
            Log.d("Line 1", printStr);
            printStr = "TOTAL VAT:" + padLeft(FunUtils.INSTANCE.formatPrintPrice(details.getData().getTax_amount()), TSIZE22 - 10 );
            Log.d("Line 1", printStr);
            printStr = "----------------------------------";
            Log.d("Line 1", printStr);
            printStr = "PAYABLE:" + padLeft(FunUtils.INSTANCE.formatPrintPrice(details.getData().getGrand_total()), TSIZE26-8);
            Log.d("Line 1", printStr);



//            String cashAmt = details.getData().getPayment_type().trim().toUpperCase().equals("CASH") ?
//                    FunUtils.INSTANCE.formatPrintPrice(details.getData().getGrand_total()) : "";
//            printStr = "CASH:" + padLeft(cashAmt, TSIZE20 - 5);
//            Log.d("Line 1", printStr);
//            String nonCashAmt = details.getData().getPayment_type().trim().toUpperCase().equals("CARD") ||
//                    details.getData().getPayment_type().trim().toUpperCase().equals("M-MONEY") ?
//                    FunUtils.INSTANCE.formatPrintPrice(details.getData().getGrand_total()) : "";
//            printStr = "M-MONEY:" + padLeft(nonCashAmt, TSIZE20 - 8);
//            Log.d("Line 1", printStr);

            printStr = "----------------------------------";
            Log.d("Line 1", printStr);


            printStr = "Sign/Stamp_________";
            Log.d("Line 1", printStr);

            printStr = "* END OF LEGEAL RECEIPT *";
            Log.d("Line 1", printStr);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.toString());

            Result = e.toString();
            if (Result.contains("NoPaperException")) {
                nopaper = true;
            } else if (Result.contains("OverHeatException")) {
                handler.sendMessage(handler.obtainMessage(OVERHEAT, 1, 0, null));
            } else {
                handler.sendMessage(handler.obtainMessage(PRINTERR, 1, 0, null));
            }
        }finally {
            handler.sendMessage(handler.obtainMessage(CANCELPROMPT, 1, 0, null));
            if (nopaper) {
                handler.sendMessage(handler.obtainMessage(NOPAPER, 1, 0, null));
                nopaper = false;
                return;
            }
        }

    }*/


    public static String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }

    public static String padLeft(String s, int n) {
        return String.format("%" + n + "s", s);
    }

    /// for debug invoice print

    /*private void printSaleType(PosSalesDetails details) {

        try {
            String printStr = "";

            Log.d("Line 1", "* START OF LEGEAL RECEIPT *");
            printStr = details.getData().getStore().getStore_name().toString().toUpperCase();
            Log.d("Line 1", printStr);


            printStr = details.getData().getStore().getAddress().toString().toUpperCase();
            Log.d("Line 1", printStr);

            printStr = "VAT NO:" + padLeft(details.getData().getVat_no(), TSIZE22-7);
            Log.d("Line 1", printStr);
            printStr = "TPIN N0:" + padLeft(details.getData().getVat_no(), TSIZE22-8);
            Log.d("Line 1", printStr);
            printStr = "DATE:" + padLeft(DateTimeFormatting.Companion.formatSaleReturndate(details.getData().getPurchase_date_time(),zone), TSIZE22-7);
            Log.d("Line 1", printStr);

            printStr = "RECEIPT NO:" + padLeft(details.getData().getInvoice_id(), TSIZE22-11);
            Log.d("Line 1", printStr);
            printStr = "BUYERÃ¢â‚¬â„¢S NAME:" + padLeft(details.getData() != null && details.getData().getCustomer_name() != null ? details.getData().getCustomer_name() : "", TSIZE22-13);
            Log.d("Line 1", printStr);

            printStr = "BUYERÃ¢â‚¬â„¢S TPIN:" + padLeft(details.getData().getBuyers_tpin(), TSIZE22-13);
            Log.d("Line 1", printStr);

            printStr = "Description";
            Log.d("Line 1", printStr);
            printStr = "Qty X Rate                 Amount";
            Log.d("Line 1", printStr);

            printStr = "----------------------------------";
            Log.d("Line 1", printStr);


            for (SalesItem item : details.getData().getSalesItem()){
                printStr = item.getProduct_name();
                Log.d("Line 1", printStr);

                //String qtyRate = FunUtils.INSTANCE.DtoString(item.getQuantity()) + " X " + FunUtils.INSTANCE.formatPrintPrice(item.getRetail_price());
                String qtyRate = FunUtils.INSTANCE.DtoString(Double.parseDouble(item.getQuantity()))+" (" + item.getUom()+ ") X " + FunUtils.INSTANCE.formatPrintPrice(item.getRetail_price());
                String amount = FunUtils.INSTANCE.formatPrintPrice(Double.toString(item.getTotal_amount()));
                String Line2 = qtyRate + padLeft(amount, TSIZE22 - (qtyRate.length()));
                printStr = Line2;
                Log.d("Line 1", printStr);
            }

            printStr = "----------------------------------";
            Log.d("Line 1", printStr);


            printStr = "TOTAL (" + currency + ") :" + padLeft(FunUtils.INSTANCE.formatPrintPrice(details.getData().getSub_total()), TSIZE26 - (("TOTAL (" + currency + ") :").length()));
            Log.d("Line 1", printStr);


            printStr = "Items:" + padLeft(Integer.toString(details.getData().getSalesItem().size()), TSIZE22 - 6 );
            Log.d("Line 1", printStr);
            printStr = "TAX EX:" + padLeft(FunUtils.INSTANCE.formatPrintPrice(details.getData().getTax_ex()), TSIZE22 - 7 );
            Log.d("Line 1", printStr);
            printStr = "TAX VAT @" + padLeft(details.getData().getTax()+"%", TSIZE22 - 9 );
            Log.d("Line 1", printStr);
            printStr = "TOTAL VAT:" + padLeft(FunUtils.INSTANCE.formatPrintPrice(details.getData().getTax_amount()), TSIZE22 - 10 );
            Log.d("Line 1", printStr);
            printStr = "----------------------------------";
            Log.d("Line 1", printStr);
            printStr = "PAYABLE:" + padLeft(FunUtils.INSTANCE.formatPrintPrice(details.getData().getGrand_total()), TSIZE26-8);
            Log.d("Line 1", printStr);



            String cashAmt = details.getData().getPayment_type().trim().toUpperCase().equals("CASH") ?
                    FunUtils.INSTANCE.formatPrintPrice(details.getData().getGrand_total()) : "";
            printStr = "CASH:" + padLeft(cashAmt, TSIZE20 - 5);
            Log.d("Line 1", printStr);
            String nonCashAmt = details.getData().getPayment_type().trim().toUpperCase().equals("CARD") ||
                    details.getData().getPayment_type().trim().toUpperCase().equals("M-MONEY") ?
                    FunUtils.INSTANCE.formatPrintPrice(details.getData().getGrand_total()) : "";
            printStr = "M-MONEY:" + padLeft(nonCashAmt, TSIZE20 - 8);
            Log.d("Line 1", printStr);

            printStr = "----------------------------------";
            Log.d("Line 1", printStr);


            printStr = "Sign/Stamp_________";
            Log.d("Line 1", printStr);

            printStr = "* END OF LEGEAL RECEIPT *";
            Log.d("Line 1", printStr);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.toString());

            Result = e.toString();
            if (Result.contains("NoPaperException")) {
                nopaper = true;
            } else if (Result.contains("OverHeatException")) {
                handler.sendMessage(handler.obtainMessage(OVERHEAT, 1, 0, null));
            } else {
                handler.sendMessage(handler.obtainMessage(PRINTERR, 1, 0, null));
            }
        }finally {
            handler.sendMessage(handler.obtainMessage(CANCELPROMPT, 1, 0, null));
            if (nopaper) {
                handler.sendMessage(handler.obtainMessage(NOPAPER, 1, 0, null));
                nopaper = false;
                return;
            }
        }

    }*/

    private Bitmap generateQRCodeBitmap(String data, int width, int height) {
        try {
            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, width, height);

            int matrixWidth = bitMatrix.getWidth();
            int matrixHeight = bitMatrix.getHeight();

            Bitmap bitmap = Bitmap.createBitmap(matrixWidth, matrixHeight, Bitmap.Config.ARGB_8888);

            for (int x = 0; x < matrixWidth; x++) {
                for (int y = 0; y < matrixHeight; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ?
                            android.graphics.Color.BLACK : android.graphics.Color.WHITE);
                }
            }

            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }



    private void printSaleType(PosSalesDetails details) {

        try {
            String rcptType = "";
            if (details.getData() != null && details.getData().getRcptType() != null) {
                rcptType = details.getData().getRcptType();
            }else {
                rcptType = "Proforma";
            }
            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setLeftIndent(1);
            mUsbThermalPrinter.setLineSpace(3);
            mUsbThermalPrinter.setTextSize(20);
            mUsbThermalPrinter.setGray(6);
            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.addString("*** START OF LEGAL RECEIPT ***");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.setGray(6);

            mUsbThermalPrinter.addString("CIS Version : 1.0.1");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(3);

            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);

            // Load logo bitmap
            Bitmap logoBitmap = BitmapFactory.decodeResource(
                    context.getResources(),
                    R.drawable.image22
            );

// Optional: resize logo for printer
            logoBitmap = Bitmap.createScaledBitmap(
                    logoBitmap,
                    400,   // width (safe for 58mm)
                    200,    // height
                    true
            );

            mUsbThermalPrinter.printLogo(logoBitmap, false);
            mUsbThermalPrinter.walkPaper(1);


            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setTextSize(32);
            mUsbThermalPrinter.setGray(6);
            mUsbThermalPrinter.setBold(true);

            mUsbThermalPrinter.addString(details.getData().getStore().getStore_name().toString().toUpperCase());
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setTextSize(26);
            mUsbThermalPrinter.setGray(6);
            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.addString(details.getData().getStore().getAddress().toString().toUpperCase());
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(2);

            //gaurav
            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setTextSize(24);
            mUsbThermalPrinter.setGray(6);

            mUsbThermalPrinter.addString("TIN NO:" +details.getData().getTpin_no());
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);
            if (rcptType.equalsIgnoreCase("P" ) || rcptType.equalsIgnoreCase("Proforma")) {
                mUsbThermalPrinter.setBold(true);
                mUsbThermalPrinter.setTextSize(22);
                mUsbThermalPrinter.addString(details.getData().getRcptType() );
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.walkPaper(1);
                mUsbThermalPrinter.setBold(false);
                mUsbThermalPrinter.addString("----------------------------------");
                mUsbThermalPrinter.printString();
            } else if (rcptType.equalsIgnoreCase("T")|| rcptType.equalsIgnoreCase("Training")) {
                mUsbThermalPrinter.setBold(true);
                mUsbThermalPrinter.setTextSize(22);
                mUsbThermalPrinter.addString(details.getData().getRcptType() + " MODE");
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.walkPaper(1);
                mUsbThermalPrinter.setBold(false);
                mUsbThermalPrinter.addString("----------------------------------");
                mUsbThermalPrinter.printString();
            } else if (rcptType.equalsIgnoreCase("C") || rcptType.equalsIgnoreCase("Copy")) {
                mUsbThermalPrinter.setBold(true);
                mUsbThermalPrinter.setTextSize(22);
                mUsbThermalPrinter.addString(details.getData().getRcptType());
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.walkPaper(1);
                mUsbThermalPrinter.setBold(false);
                mUsbThermalPrinter.addString("----------------------------------");
                mUsbThermalPrinter.printString();
            }
            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.setGray(6);
            if (rcptType == null){
                mUsbThermalPrinter.addString("----------------------------------");
                mUsbThermalPrinter.printString();
            }
            mUsbThermalPrinter.walkPaper(1);
            mUsbThermalPrinter.addString("Welcome to our shop");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);
            mUsbThermalPrinter.addString("----------------------------------");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);
            mUsbThermalPrinter.setGray(6);
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);

            String buyerName = details.getData() != null && details.getData().getCustomer_name() != null
                    ? details.getData().getCustomer_name()
                    : "";

            // â”€â”€ CHANGED: Buyer info with padLeft â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            String buyerNameLabel = "BUYER'S NAME:";
            mUsbThermalPrinter.addString(buyerNameLabel + padLeft(buyerName, TSIZE22 - buyerNameLabel.length()));
            mUsbThermalPrinter.printString();

            mUsbThermalPrinter.setGray(6);

            String buyerTin = "";
            if (details.getData() != null && details.getData().getBuyers_tpin() != null) {
                buyerTin = details.getData().getBuyers_tpin();
            }
            String buyerTinLabel = "BUYER'S TIN:";
            mUsbThermalPrinter.addString(buyerTinLabel + padLeft(buyerTin, TSIZE22 - buyerTinLabel.length()));
            mUsbThermalPrinter.printString();

            String buyerContact = "";
            if (details.getData() != null && details.getData().getCustomer_mob_no() != null) {
                buyerContact = details.getData().getCustomer_mob_no();
            }
            String buyerContactLabel = "BUYER'S CONTACT:";
            mUsbThermalPrinter.addString(buyerContactLabel + padLeft(buyerContact, TSIZE22 - buyerContactLabel.length()));
            mUsbThermalPrinter.printString();

            mUsbThermalPrinter.walkPaper(1);
            mUsbThermalPrinter.addString("----------------------------------");
            mUsbThermalPrinter.printString();

            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
            mUsbThermalPrinter.setTextSize(22);

            mUsbThermalPrinter.setGray(6);

            mUsbThermalPrinter.setGray(6);

            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.printString();

            mUsbThermalPrinter.setGray(6);

            for (SalesItem item : details.getData().getSalesItem()) {
                productname = item.getProduct_name();
                numberOfItems++;
                if (productname.toLowerCase().startsWith("bulk oil")) {
                    looseOil = true;
                } else {
                    looseOil = false;
                }

                // Line 1: Product Name (bold)
                mUsbThermalPrinter.setTextSize(24);
                mUsbThermalPrinter.setGray(6);
                mUsbThermalPrinter.setBold(true);
                mUsbThermalPrinter.addString(item.getProduct_name());
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.setBold(false);
                mUsbThermalPrinter.setTextSize(22);

                // --- Build 3-column line: [rate x]   [qty]   [amount taxCode] ---
                String taxCode = "";
                if (item.getTax_details() != null && item.getTax_details().getCode() != null) {
                    taxCode = item.getTax_details().getCode();
                }

                // Left: "33600.00x"
                String rateCol = FunUtils.INSTANCE.formatPrintPrice(item.getTax_inclusive_price()) + "x";

                // Middle: "0.200 (KG)"  or just quantity + UOM
                String qtyCol = FunUtils.INSTANCE.DtoString(Double.parseDouble(item.getQuantity()));

                // Right: "6720.00B"
                String amountCol = FunUtils.INSTANCE.formatPrintPrice(Double.toString(item.getTotal_amount())) + taxCode;

                // Total line width for textSize 22 (34 chars for 58mm printer)
                int totalWidth = TSIZE22; // e.g. 34

                // Right column occupies fixed 10 chars from right
                int rightColWidth = 10;
                // Middle column occupies fixed 6 chars
                int midColWidth = 8;
                // Left col gets remaining
                int leftColWidth = totalWidth - midColWidth - rightColWidth;

                // Format: left-pad qty to midColWidth, right-pad amount to rightColWidth
                String line2 = String.format(
                        "%-" + leftColWidth + "s%" + midColWidth + "s%" + rightColWidth + "s",
                        rateCol,
                        qtyCol,
                        amountCol
                );

                mUsbThermalPrinter.addString(line2);
                mUsbThermalPrinter.printString();

                // Discount line (if applicable)
                if (item.getDiscount_rate() < 0) {
                    String discountText = "discount " + item.getDiscount_rate() + "%";

                    double discountedTotal = item.getTotal_amount()
                            + (item.getTotal_amount() * item.getDiscount_rate() / 100);

                    String discountAmountStr = FunUtils.INSTANCE.formatPrintPrice(String.valueOf(discountedTotal));

                    // Left: "discount -25%"   Right: "5040.00" right-aligned
                    String discountLine = String.format(
                            "%-" + (totalWidth - rightColWidth) + "s%" + rightColWidth + "s",
                            discountText,
                            discountAmountStr
                    );

                    mUsbThermalPrinter.addString(discountLine);
                    mUsbThermalPrinter.printString();
                }

                mUsbThermalPrinter.walkPaper(1);
            }


            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.addString("----------------------------------");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);
            // âœ… Check by receipt type code
            if (rcptType.equalsIgnoreCase("P") ||
                    rcptType.equalsIgnoreCase("T") ||
                    rcptType.equalsIgnoreCase("C") ||
                    rcptType.equalsIgnoreCase("Proforma") ||
                    rcptType.equalsIgnoreCase("Training") ||
                    rcptType.equalsIgnoreCase("Copy")) {

                mUsbThermalPrinter.setBold(true);
                mUsbThermalPrinter.setTextSize(22);
                mUsbThermalPrinter.setAlgin(1);
                mUsbThermalPrinter.addString("THIS IS NOT AN OFFICIAL RECEIPT");
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.walkPaper(1);
                mUsbThermalPrinter.setBold(false);
                mUsbThermalPrinter.addString("----------------------------------");
                mUsbThermalPrinter.printString();
            }

            mUsbThermalPrinter.setTextSize(26);
            mUsbThermalPrinter.setBold(true);
            String label = "TOTAL(" + currency + "):";
            String value = FunUtils.INSTANCE.formatPrintPrice(details.getData().getGrand_total());
            // Right align the value using padLeft correctly
            int padding = TSIZE26 - label.length();
            mUsbThermalPrinter.addString(label + padLeft(value, padding));
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.walkPaper(2);
            mUsbThermalPrinter.setTextSize(22);

            // Get tax summary list
            List<TaxSummary> taxSummaryList = details.getData().getTax_summery();

            if (taxSummaryList != null && !taxSummaryList.isEmpty()) {

                for (TaxSummary taxSummary : taxSummaryList) {
                    String code = taxSummary.getCode();
                    Double taxableValue = taxSummary.getTaxable_value();

                    if (code != null ) {
                        String taxLabel = "TOTAL " + taxSummary.getCode_name();
                        mUsbThermalPrinter.addString(taxLabel + padLeft(
                                FunUtils.INSTANCE.formatPrintPrice(String.valueOf(taxableValue)),
                                TSIZE22 - taxLabel.length()
                        ));
                        mUsbThermalPrinter.printString();
                    }
                }

                for (TaxSummary taxSummary : taxSummaryList) {
                    String code = taxSummary.getCode();
                    Double taxAmount = taxSummary.getTax_amount();

                    if (taxAmount != null && taxAmount > 0) {
                        String taxAmountLabel = "TOTAL TAX " + code;
                        mUsbThermalPrinter.addString(taxAmountLabel + padLeft(
                                FunUtils.INSTANCE.formatPrintPrice(String.valueOf(taxAmount)),
                                TSIZE22 - taxAmountLabel.length()
                        ));
                        mUsbThermalPrinter.printString();
                    }
                }

                String totalTaxAmount = details.getData().getTax_amount();
                if (totalTaxAmount != null && !totalTaxAmount.equals("0")) {
                    String totalTaxLabel = "TOTAL TAX AMOUNT";
                    mUsbThermalPrinter.addString(totalTaxLabel + padLeft(
                            FunUtils.INSTANCE.formatPrintPrice(totalTaxAmount),
                            TSIZE22 - totalTaxLabel.length()
                    ));
                    mUsbThermalPrinter.printString();
                }
            }
            mUsbThermalPrinter.walkPaper(1);



            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.setTextSize(20);
            // â”€â”€ Payment / Items count (skip for Proforma) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            if (!rcptType.equalsIgnoreCase("P") && !rcptType.equalsIgnoreCase("Proforma")) {
                mUsbThermalPrinter.addString("----------------------------------");
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.walkPaper(1);
                String paymentType = details.getData().getPayment_type().trim();
                String grandTotal = FunUtils.INSTANCE.formatPrintPrice(details.getData().getGrand_total());

                String paymentLabel = "";
                if (paymentType.equals("01"))      paymentLabel = "CASH:";
                else if (paymentType.equals("02")) paymentLabel = "CREDIT:";
                else if (paymentType.equals("03")) paymentLabel = "CASH/CREDIT:";
                else if (paymentType.equals("04")) paymentLabel = "BANK CHECK:";
                else if (paymentType.equals("05")) paymentLabel = "DEBIT/CREDIT CARD:";
                else if (paymentType.equals("06")) paymentLabel = "M-MONEY:";
                else if (paymentType.equals("07")) paymentLabel = "OTHER:";

                if (!paymentLabel.isEmpty()) {
                    mUsbThermalPrinter.addString(paymentLabel + padLeft(grandTotal, TSIZE20 - paymentLabel.length()));
                    mUsbThermalPrinter.printString();
                }

                mUsbThermalPrinter.setBold(false);
                mUsbThermalPrinter.setTextSize(22);
                mUsbThermalPrinter.addString("Items:" + padLeft(Integer.toString(details.getData().getSalesItem().size()), TSIZE22 - 6));
                mUsbThermalPrinter.printString();
            } else {
                mUsbThermalPrinter.walkPaper(1); // maintain spacing for Proforma
            }


            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.setGray(6);
            mUsbThermalPrinter.addString("----------------------------------");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);
            if (rcptType.equalsIgnoreCase("P" ) || rcptType.equalsIgnoreCase("Proforma")) {
                mUsbThermalPrinter.setBold(true);
                mUsbThermalPrinter.setTextSize(22);
                mUsbThermalPrinter.setAlgin(1);
                mUsbThermalPrinter.addString(details.getData().getRcptType() );
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.walkPaper(1);
                mUsbThermalPrinter.setBold(false);
                mUsbThermalPrinter.addString("----------------------------------");
                mUsbThermalPrinter.printString();
            } else if (rcptType.equalsIgnoreCase("T")|| rcptType.equalsIgnoreCase("Training")) {
                mUsbThermalPrinter.setBold(true);
                mUsbThermalPrinter.setTextSize(22);
                mUsbThermalPrinter.setAlgin(1);
                mUsbThermalPrinter.addString(details.getData().getRcptType() + " Mode");
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.walkPaper(1);
                mUsbThermalPrinter.setBold(false);
                mUsbThermalPrinter.addString("----------------------------------");
                mUsbThermalPrinter.printString();
            } else if (rcptType.equalsIgnoreCase("C") || rcptType.equalsIgnoreCase("Copy")) {
                mUsbThermalPrinter.setBold(true);
                mUsbThermalPrinter.setTextSize(22);
                mUsbThermalPrinter.setAlgin(1);
                mUsbThermalPrinter.addString(details.getData().getRcptType());
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.walkPaper(1);
                mUsbThermalPrinter.setBold(false);
                mUsbThermalPrinter.addString("----------------------------------");
                mUsbThermalPrinter.printString();
            }
            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.setGray(6);
            VsdcReceipt vsdc = (details.getData().getVsdc_reciept() != null && !details.getData().getVsdc_reciept().isEmpty())
                    ? details.getData().getVsdc_reciept().get(0) : null;

            String rawDateTime = (vsdc != null) ? vsdc.getVsdcRcptPbctDate() : "";

            String vsdcDate = "";
            String vsdcTime = "";
            if (rawDateTime != null && !rawDateTime.isEmpty()) {
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
                    Date parsedDate = inputFormat.parse(rawDateTime);
                    if (parsedDate != null) {
                        vsdcDate = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(parsedDate);
                        vsdcTime = new SimpleDateFormat("HH:mm:ss",    Locale.getDefault()).format(parsedDate);
                    }
                } catch (ParseException e) {
                    Log.e("DateFormat", "Parsing failed: " + e.getMessage());
                    vsdcDate = rawDateTime; // fallback
                }
            }

            String dateLabel = "DATE: " + vsdcDate;
            String timeLabel = "TIME: " + vsdcTime;
            int spaces = TSIZE22 - dateLabel.length() - timeLabel.length();
            if (spaces < 1) spaces = 1;
            StringBuilder dateLine = new StringBuilder(dateLabel);
            for (int i = 0; i < spaces; i++) dateLine.append(" ");
            dateLine.append(timeLabel);

            mUsbThermalPrinter.addString(dateLine.toString());
            mUsbThermalPrinter.printString();

            // â”€â”€ CHANGED: SDC ID with padLeft â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            String sdcIdLabel = "SDC ID:";
            String sdcIdValue = (vsdc != null && vsdc.getSdcId() != null)
                    ? vsdc.getSdcId() : "";
            mUsbThermalPrinter.addString(sdcIdLabel + padLeft(sdcIdValue, TSIZE22 - sdcIdLabel.length()));
            mUsbThermalPrinter.printString();

            String rcptTypeValue = details.getData().getRcptType() != null ? details.getData().getRcptType() : "N";
            String receipttype = (vsdc != null ? vsdc.getSales_type() : "") + "" + "S";

            // â”€â”€ CHANGED: Receipt Number (SDC section) with padLeft â”€â”€â”€
            String sdcRcptLabel = "RECEIPT NUMBER:";
            String sdcRcptValue = (vsdc != null ? vsdc.getTotRcptNo() : "")
                    + "/" + (vsdc != null ? vsdc.getTotRcptNo() : "")
                    + " " + receipttype;
            mUsbThermalPrinter.addString(sdcRcptLabel + padLeft(sdcRcptValue, TSIZE22 - sdcRcptLabel.length()));
            mUsbThermalPrinter.printString();

            mUsbThermalPrinter.setAlgin(1);
            if (!rcptType.equalsIgnoreCase("T") && !rcptType.equalsIgnoreCase("Training") &&
                    !rcptType.equalsIgnoreCase("P") && !rcptType.equalsIgnoreCase("Proforma")) {
                mUsbThermalPrinter.addString("INTERNAL DATA:  " + (vsdc != null ? vsdc.getIntrlData() : ""));
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.addString("RECEIPT SIGNATURE:  " + (vsdc != null ? vsdc.getRcptSign() : ""));
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.walkPaper(1);
            }

// GENERATE AND PRINT QR CODE HERE â¬‡ï¸
            if (!rcptType.equalsIgnoreCase("T") && !rcptType.equalsIgnoreCase("Training") &&
                    !rcptType.equalsIgnoreCase("P") && !rcptType.equalsIgnoreCase("Proforma")) {
                try {
                    if (vsdc != null &&
                            vsdc.getQrCodeUrl() != null &&
                            !vsdc.getQrCodeUrl().isEmpty()) {

                        mUsbThermalPrinter.reset();
                        mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);

                        String qrData = vsdc.getQrCodeUrl();
                        Bitmap qrBitmap = generateQRCodeBitmap(qrData, 250, 250);

                        if (qrBitmap != null) {
                            try {
                                mUsbThermalPrinter.setGray(6);
                                mUsbThermalPrinter.printLogo(qrBitmap, false);
                                mUsbThermalPrinter.walkPaper(2);
                            } catch (Exception e) {
                                e.printStackTrace();
                                Log.e("PrinterUtil", "Error printing QR code: " + e.getMessage());
                            }
                        } else {
                            Log.w("PrinterUtil", "QR code bitmap is null");
                        }
                    } else {
                        Log.w("PrinterUtil", "No QR code URL available - skipping QR code");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("PrinterUtil", "Exception in QR code section: " + e.getMessage());
                }
            } // END QR code condition

            mUsbThermalPrinter.setAlgin(0);
            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.addString("----------------------------------");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            // â”€â”€ CHANGED: Receipt Number (after QR) with padLeft â”€â”€â”€â”€â”€â”€


            String rcptNumLabel = "RECEIPT NUMBER:";
            String receiptNum = (details.getData().getInvoice_id() != null)
                    ? details.getData().getInvoice_id() : "";
            mUsbThermalPrinter.addString(rcptNumLabel + padLeft(receiptNum, TSIZE22 - rcptNumLabel.length()));
            mUsbThermalPrinter.printString();


            String rawDateTimee = details.getData().getPurchase_date_time();

            String vsdcDate2 = "";
            String vsdcTime2 = "";
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault());
                Date parsedDate = inputFormat.parse(rawDateTimee);
                if (parsedDate != null) {
                    vsdcDate2 = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(parsedDate);
                    vsdcTime2 = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(parsedDate);
                }
            } catch (ParseException e) {
                Log.e("DateFormat", "Parsing failed: " + e.getMessage());
                vsdcDate2 = rawDateTimee; // fallback
            }

            String dateLabel2 = "DATE: " + vsdcDate2;
            String timeLabel2 = "TIME: " + vsdcTime2;
            int spaces2 = TSIZE22 - dateLabel2.length() - timeLabel2.length();
            if (spaces2 < 1) spaces2 = 1;
            StringBuilder dateLine2 = new StringBuilder(dateLabel2);
            for (int i = 0; i < spaces2; i++) dateLine2.append(" ");
            dateLine2.append(timeLabel2);

            mUsbThermalPrinter.addString(dateLine2.toString());
            mUsbThermalPrinter.printString();

            // â”€â”€ CHANGED: MRC No with padLeft â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            String mrcLabel = "MRC NO.:";
            String mrcValue = (vsdc != null && vsdc.getMrcNo() != null)
                    ? vsdc.getMrcNo() + "." : ".";
            mUsbThermalPrinter.addString(mrcLabel + padLeft(mrcValue, TSIZE22 - mrcLabel.length()));
            mUsbThermalPrinter.printString();

            mUsbThermalPrinter.setAlgin(0);
            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.addString("----------------------------------");
            mUsbThermalPrinter.printString();

// âœ… IMPORTANT: Reset printer state after QR code attempt
            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.setGray(6);

// Now print THANK YOU
            mUsbThermalPrinter.addString("THANK YOU");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.addString("WE APPRECIATE YOUR BUSINESS");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setLineSpace(3);
            mUsbThermalPrinter.setTextSize(20);

            mUsbThermalPrinter.addString("*** END ***");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(5);
            mUsbThermalPrinter.addString(" ");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(5);
            mUsbThermalPrinter.reset();

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.toString());

            Result = e.toString();
            if (Result.contains("NoPaperException")) {
                nopaper = true;
            } else if (Result.contains("OverHeatException")) {
                handler.sendMessage(handler.obtainMessage(OVERHEAT, 1, 0, null));
            } else {
                handler.sendMessage(handler.obtainMessage(PRINTERR, 1, 0, null));
            }
        } finally {
            handler.sendMessage(handler.obtainMessage(CANCELPROMPT, 1, 0, null));
            if (nopaper) {
                handler.sendMessage(handler.obtainMessage(NOPAPER, 1, 0, null));
                nopaper = false;
                return;
            }
        }
    }


    public void printCopyReceipt(CopyReceiptRes details) {
        try {
            if (details == null || details.getData() == null) {
                Log.e("PrinterUtil", "Receipt data is null");
                handler.sendMessage(handler.obtainMessage(PRINTERR, 1, 0, null));
                return;
            }

            CopyReceiptRes.Data data = details.getData();

            // â”€â”€ HEADER â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setLeftIndent(1);
            mUsbThermalPrinter.setLineSpace(3);
            mUsbThermalPrinter.setTextSize(20);
            mUsbThermalPrinter.setGray(6);
            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.addString("*** START OF LEGAL RECEIPT ***");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.addString("CIS Version : 1.0.1");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(3);

            // Logo
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            Bitmap logoBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.image22);
            logoBitmap = Bitmap.createScaledBitmap(logoBitmap, 400, 200, true);
            mUsbThermalPrinter.printLogo(logoBitmap, false);
            mUsbThermalPrinter.walkPaper(1);

            // â”€â”€ STORE INFO â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setTextSize(32);
            mUsbThermalPrinter.setGray(6);
            mUsbThermalPrinter.setBold(true);
            String storeName = (data.getStore() != null && data.getStore().getStore_name() != null)
                    ? data.getStore().getStore_name().toUpperCase() : "";
            mUsbThermalPrinter.addString(storeName);
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setTextSize(26);
            mUsbThermalPrinter.setGray(6);
            mUsbThermalPrinter.setBold(false);
            String storeAddress = (data.getStore() != null && data.getStore().getAddress() != null)
                    ? data.getStore().getAddress().toUpperCase() : "";
            mUsbThermalPrinter.addString(storeAddress);
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setTextSize(24);
            mUsbThermalPrinter.setGray(6);
            String tpinNo = (data.getTpin_no() != null) ? data.getTpin_no() : "";
            mUsbThermalPrinter.addString("TIN NO: " + tpinNo);
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            // â”€â”€ RECEIPT TYPE â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            String rcptTypeRaw = "COPY";
            mUsbThermalPrinter.setTextSize(24);
            mUsbThermalPrinter.setBold(true);
            mUsbThermalPrinter.addString(rcptTypeRaw);
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.addString("----------------------------------");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            // â”€â”€ REFUND LABEL â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setBold(true);
            mUsbThermalPrinter.setTextSize(23);
            mUsbThermalPrinter.addString("REFUND");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);

            // â”€â”€ CHANGED 1: Ref. Normal Receipt with padLeft â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            String returnedInvoiceId = (data.getReturned_invoice_id() != null)
                    ? data.getReturned_invoice_id() : "";
            String refLabel = "Ref. Normal Receipt:";
            mUsbThermalPrinter.addString(refLabel + padLeft(returnedInvoiceId, TSIZE22 - refLabel.length()));
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.addString("----------------------------------");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.addString("REFUND IS APPROVED ONLY FOR ORIGINAL SALES RECEIPT");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.addString("----------------------------------");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            // â”€â”€ CHANGED 2: Date parsed properly with yyyyMMddHHmmss â”€â”€
            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.setGray(6);

            CopyReceiptRes.VsdcReceipt vsdcRef = (data.getVsdc_reciept() != null && !data.getVsdc_reciept().isEmpty())
                    ? data.getVsdc_reciept().get(0) : null;

            String rawVsdcDate = (vsdcRef != null && vsdcRef.getVsdcRcptPbctDate() != null)
                    ? vsdcRef.getVsdcRcptPbctDate() : "";
            String formattedDate = "";
            String formattedTime = "";
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
                Date parsedVsdcDate = inputFormat.parse(rawVsdcDate);
                if (parsedVsdcDate != null) {
                    formattedDate = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(parsedVsdcDate);
                    formattedTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(parsedVsdcDate);
                }
            } catch (ParseException e) {
                Log.e("DateFormat", "Parsing failed: " + e.getMessage());
                formattedDate = rawVsdcDate; // fallback
            }

            String vsdcDateLabel = "DATE: " + formattedDate;
            String vsdcTimeLabel = "TIME: " + formattedTime;
            int vsdcSpaces = TSIZE22 - vsdcDateLabel.length() - vsdcTimeLabel.length();
            if (vsdcSpaces < 1) vsdcSpaces = 1;
            StringBuilder vsdcDateLine = new StringBuilder(vsdcDateLabel);
            for (int i = 0; i < vsdcSpaces; i++) vsdcDateLine.append(" ");
            vsdcDateLine.append(vsdcTimeLabel);

//            mUsbThermalPrinter.addString(vsdcDateLine.toString());
//            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            // â”€â”€ CHANGED 1: Buyer info with padLeft â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            String buyerName = (data.getCustomer_name() != null) ? data.getCustomer_name() : "";
            String buyerNameLabel = "BUYER'S NAME:";
            mUsbThermalPrinter.addString(buyerNameLabel + padLeft(buyerName, TSIZE22 - buyerNameLabel.length()));
            mUsbThermalPrinter.printString();

            String buyerTin = (data.getBuyers_tpin() != null) ? data.getBuyers_tpin() : "";
            String buyerTinLabel = "BUYER'S TIN:";
            mUsbThermalPrinter.addString(buyerTinLabel + padLeft(buyerTin, TSIZE22 - buyerTinLabel.length()));
            mUsbThermalPrinter.printString();

            String buyerContact = (data.getCustomer_mob_no() != null) ? data.getCustomer_mob_no() : "";
            String buyerContactLabel = "BUYER'S CONTACT:";
            mUsbThermalPrinter.addString(buyerContactLabel + padLeft(buyerContact, TSIZE22 - buyerContactLabel.length()));
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.addString("----------------------------------");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            // â”€â”€ ITEMS LOOP â€” uses returned_items â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            if (data.getReturned_items() != null) {
                for (CopyReceiptRes.ReturnedItem item : data.getReturned_items()) {
                    if (item == null) continue;

                    productname = (item.getProduct_name() != null) ? item.getProduct_name() : "";
                    numberOfItems++;

                    // Line 1: Product Name (bold)
                    mUsbThermalPrinter.setTextSize(24);
                    mUsbThermalPrinter.setGray(6);
                    mUsbThermalPrinter.setBold(true);
                    mUsbThermalPrinter.addString(productname);
                    mUsbThermalPrinter.printString();
                    mUsbThermalPrinter.setBold(false);
                    mUsbThermalPrinter.setTextSize(22);

                    String taxCode = "";
                    if (item.getTax_details() != null && item.getTax_details().getCode() != null) {
                        taxCode = item.getTax_details().getCode();
                    }

                    String rateCol = (item.getRetail_price() != null
                            ? FunUtils.INSTANCE.formatPrintPrice(String.valueOf(item.getRetail_price()))
                            : "0.00") + "x";

                    String qtyCol = FunUtils.INSTANCE.DtoString(
                            item.getReturn_quantity() != null
                                    ? item.getReturn_quantity().doubleValue() : 0.0);

                    String amountCol = "-" + (item.getTotal_amount() != null
                            ? FunUtils.INSTANCE.formatPrintPrice(String.valueOf(item.getTotal_amount()))
                            : "0.00") + taxCode;

                    int totalWidth    = TSIZE22;
                    int rightColWidth = 10;
                    int midColWidth   = 8;
                    int leftColWidth  = totalWidth - midColWidth - rightColWidth;

                    String line2 = String.format(
                            "%-" + leftColWidth + "s%" + midColWidth + "s%" + rightColWidth + "s",
                            rateCol,
                            qtyCol,
                            amountCol
                    );

                    mUsbThermalPrinter.addString(line2);
                    mUsbThermalPrinter.printString();

                    Double discountRate    = item.getDiscount_rate();
                    Double itemTotal       = item.getTotal_amount();
                    Double discountedTotal = item.getDiscounted_total();

                    if (discountRate != null && discountRate < 0
                            && itemTotal != null && itemTotal > 0
                            && discountedTotal != null) {

                        String discountText = "Discount " + discountRate.intValue() + "%";

                        String finalAmountStr = FunUtils.INSTANCE.formatPrintPrice(
                                Double.toString(discountedTotal));

                        String discountLine = String.format(
                                "%-" + (totalWidth - rightColWidth) + "s%" + rightColWidth + "s",
                                discountText,
                                finalAmountStr
                        );

                        mUsbThermalPrinter.addString(discountLine);
                        mUsbThermalPrinter.printString();
                    }

                    mUsbThermalPrinter.walkPaper(1);
                }
            }

            // â”€â”€ TOTALS â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.addString("----------------------------------");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.setGray(6);
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setBold(true);
            mUsbThermalPrinter.setTextSize(23);
            mUsbThermalPrinter.addString("THIS IS NOT AN OFFICIAL RECEIPT");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);
            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.addString("----------------------------------");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            String grandTotal = (data.getGrand_total() != null)
                    ? FunUtils.INSTANCE.formatPrintPrice(data.getGrand_total()) : "0.00";

            mUsbThermalPrinter.setTextSize(26);
            mUsbThermalPrinter.setBold(true);
            String totalLabel = "TOTAL(" + currency + "):";
            mUsbThermalPrinter.addString(totalLabel + padLeft(grandTotal, TSIZE26 - totalLabel.length()));
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.setTextSize(22);

            // â”€â”€ TAX SUMMARY LOOP â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            List<CopyReceiptRes.TaxSummery> taxSummaryList = data.getTax_summery();
            if (taxSummaryList != null && !taxSummaryList.isEmpty()) {

                for (CopyReceiptRes.TaxSummery taxSummary : taxSummaryList) {
                    if (taxSummary.getCode() != null) {
                        String taxLabel = "TOTAL " + taxSummary.getCode_name();
                        double taxableValue = (taxSummary.getTaxable_value() != null)
                                ? taxSummary.getTaxable_value() : 0.0;
                        mUsbThermalPrinter.addString(taxLabel + padLeft(
                                FunUtils.INSTANCE.formatPrintPrice(String.valueOf(taxableValue)),
                                TSIZE22 - taxLabel.length()
                        ));
                        mUsbThermalPrinter.printString();
                    }
                }

                for (CopyReceiptRes.TaxSummery taxSummary : taxSummaryList) {
                    Double taxAmt = taxSummary.getTax_amount();
                    String code = taxSummary.getCode();
                    if (taxAmt != null && taxAmt != 0.0 && code != null) {
                        String taxAmountLabel = "TOTAL TAX " + code;
                        mUsbThermalPrinter.addString(taxAmountLabel + padLeft(
                                FunUtils.INSTANCE.formatPrintPrice(String.valueOf(taxAmt)),
                                TSIZE22 - taxAmountLabel.length()
                        ));
                        mUsbThermalPrinter.printString();
                    }
                }

                String totalTaxAmount = data.getTax_amount();
                if (totalTaxAmount != null && !totalTaxAmount.equals("0")
                        && !totalTaxAmount.equals("0.00")) {
                    String totalTaxLabel = "TOTAL TAX AMOUNT";
                    mUsbThermalPrinter.addString(totalTaxLabel + padLeft(
                            FunUtils.INSTANCE.formatPrintPrice(totalTaxAmount),
                            TSIZE22 - totalTaxLabel.length()
                    ));
                    mUsbThermalPrinter.printString();
                }
            }

            mUsbThermalPrinter.walkPaper(1);
            mUsbThermalPrinter.addString("----------------------------------");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);
            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.setTextSize(22);
            String grandTotall = FunUtils.INSTANCE.formatPrintPrice(String.valueOf(details.getData().getGrand_total()));
            String paymentLabel = "CASH";

            if (!paymentLabel.isEmpty()) {
                mUsbThermalPrinter.addString(paymentLabel + padLeft(grandTotall, TSIZE22 - paymentLabel.length()));
                mUsbThermalPrinter.printString();
            }

            // Items count
            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.setTextSize(22);
            int itemsCount = (data.getReturned_items() != null)
                    ? data.getReturned_items().size() : 0;
            mUsbThermalPrinter.addString("Items:" + padLeft(Integer.toString(itemsCount), TSIZE22 - 6));
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.addString("----------------------------------");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setBold(true);
            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.addString("COPY");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);
            mUsbThermalPrinter.addString("----------------------------------");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            // â”€â”€ SDC INFORMATION â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setBold(true);
            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.addString("SDC INFORMATION");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.setGray(6);
            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);

            if (data.getVsdc_reciept() != null && !data.getVsdc_reciept().isEmpty()) {
                CopyReceiptRes.VsdcReceipt vsdc = data.getVsdc_reciept().get(0);

                // â”€â”€ CHANGED 2: Reuse properly parsed formattedDate/formattedTime â”€â”€
                mUsbThermalPrinter.addString(vsdcDateLine.toString());
                mUsbThermalPrinter.printString();

                // â”€â”€ CHANGED 1: SDC ID with padLeft â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                String sdcId = (vsdc.getSdcId() != null) ? vsdc.getSdcId() : "";
                String sdcIdLabel = "SDC ID:";
                mUsbThermalPrinter.addString(sdcIdLabel + padLeft(sdcId, TSIZE22 - sdcIdLabel.length()));
                mUsbThermalPrinter.printString();

                // â”€â”€ CHANGED 1: Receipt Number (SDC) with padLeft â”€â”€â”€â”€â”€
                String rcptNo = (vsdc.getRcptNo() != null && vsdc.getRcptNo() != 0)
                        ? String.valueOf(vsdc.getRcptNo()) : "";
                String sdcRcptLabel = "RECEIPT NUMBER:";
                String sdcRcptValue = rcptNo + "/" + vsdc.getTotRcptNo() + " CR";
                mUsbThermalPrinter.addString(sdcRcptLabel + padLeft(sdcRcptValue, TSIZE22 - sdcRcptLabel.length()));
                mUsbThermalPrinter.printString();

                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);

                String intrlData = (vsdc.getIntrlData() != null) ? vsdc.getIntrlData() : "";
                mUsbThermalPrinter.addString("INTERNAL DATA:  " + intrlData);
                mUsbThermalPrinter.printString();

                String rcptSign = (vsdc.getRcptSign() != null) ? vsdc.getRcptSign() : "";
                mUsbThermalPrinter.addString("RECEIPT SIGNATURE:  " + rcptSign);
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.walkPaper(1);

                // â”€â”€ CHANGED 3: Receipt Number, Date/Time, MRC No after receipt signature â”€â”€
                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
                mUsbThermalPrinter.addString("----------------------------------");
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.walkPaper(1);

                // Receipt Number
                String invoiceId = (data.getReturned_invoice_id() != null)
                        ? data.getReturned_invoice_id() : "";
                String rcptNumLabel = "RECEIPT NUMBER:";
                mUsbThermalPrinter.addString(rcptNumLabel + padLeft(invoiceId, TSIZE22 - rcptNumLabel.length()));
                mUsbThermalPrinter.printString();

                // â”€â”€ CHANGED 2: Purchase date properly parsed â”€â”€â”€â”€â”€â”€â”€â”€â”€
                String rawPurchaseDateTime = (data.getReturned_date() != null)
                        ? data.getReturned_date() : "";
                String purchaseDate = "";
                String purchaseTime = "";
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault());
                    Date parsedPurchaseDate = inputFormat.parse(rawPurchaseDateTime);
                    if (parsedPurchaseDate != null) {
                        purchaseDate = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(parsedPurchaseDate);
                        purchaseTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(parsedPurchaseDate);
                    }
                } catch (ParseException e) {
                    Log.e("DateFormat", "Parsing failed: " + e.getMessage());
                    purchaseDate = rawPurchaseDateTime; // fallback
                }

                String purchaseDateLabel = "DATE: " + purchaseDate;
                String purchaseTimeLabel = "TIME: " + purchaseTime;
                int purchaseSpaces = TSIZE22 - purchaseDateLabel.length() - purchaseTimeLabel.length();
                if (purchaseSpaces < 1) purchaseSpaces = 1;
                StringBuilder purchaseDateLine = new StringBuilder(purchaseDateLabel);
                for (int i = 0; i < purchaseSpaces; i++) purchaseDateLine.append(" ");
                purchaseDateLine.append(purchaseTimeLabel);

                mUsbThermalPrinter.addString(purchaseDateLine.toString());
                mUsbThermalPrinter.printString();

                // â”€â”€ CHANGED 1: MRC No with padLeft â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                String mrcLabel = "MRC NO.:";
                String mrcValue = (vsdc.getMrcNo() != null) ? vsdc.getMrcNo() + "." : ".";
                mUsbThermalPrinter.addString(mrcLabel + padLeft(mrcValue, TSIZE22 - mrcLabel.length()));
                mUsbThermalPrinter.printString();
            }

            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.addString("----------------------------------");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            // â”€â”€ FOOTER â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.setGray(6);
            mUsbThermalPrinter.addString("THANK YOU");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.addString("WE APPRECIATE YOUR BUSINESS");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(3);

            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setTextSize(20);
            mUsbThermalPrinter.addString("*** END ***");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(5);
            mUsbThermalPrinter.addString(" ");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(5);
            mUsbThermalPrinter.reset();

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("PrinterUtil", "Print error: " + e.getMessage(), e);
            Result = e.toString();
            if (Result.contains("NoPaperException")) {
                nopaper = true;
            } else if (Result.contains("OverHeatException")) {
                handler.sendMessage(handler.obtainMessage(OVERHEAT, 1, 0, null));
            } else {
                handler.sendMessage(handler.obtainMessage(PRINTERR, 1, 0, null));
            }
        } finally {
            handler.sendMessage(handler.obtainMessage(CANCELPROMPT, 1, 0, null));
            if (nopaper) {
                handler.sendMessage(handler.obtainMessage(NOPAPER, 1, 0, null));
                nopaper = false;
                return;
            }
        }
    }


    public void printSaleReceipt(SaleReceiptRes details) {
        try {
            if (details == null || details.getData() == null) {
                Log.e("PrinterUtil", "Sale receipt data is null");
                handler.sendMessage(handler.obtainMessage(PRINTERR, 1, 0, null));
                return;
            }

            SaleReceiptRes.Data data = details.getData();

            // â”€â”€ HEADER â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setLeftIndent(1);
            mUsbThermalPrinter.setLineSpace(3);
            mUsbThermalPrinter.setTextSize(20);
            mUsbThermalPrinter.setGray(6);
            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.addString("*** START OF LEGAL RECEIPT ***");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.addString("CIS Version : 1.0.1");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(3);

            // Logo
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            Bitmap logoBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.image22);
            logoBitmap = Bitmap.createScaledBitmap(logoBitmap, 400, 200, true);
            mUsbThermalPrinter.printLogo(logoBitmap, false);
            mUsbThermalPrinter.walkPaper(1);

            // â”€â”€ STORE INFO â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setTextSize(32);
            mUsbThermalPrinter.setGray(6);
            mUsbThermalPrinter.setBold(true);
            String storeName = (data.getStore() != null && data.getStore().getStore_name() != null)
                    ? data.getStore().getStore_name().toUpperCase() : "";
            mUsbThermalPrinter.addString(storeName);
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setTextSize(26);
            mUsbThermalPrinter.setGray(6);
            mUsbThermalPrinter.setBold(false);
            String storeAddress = (data.getStore() != null && data.getStore().getAddress() != null)
                    ? data.getStore().getAddress().toUpperCase() : "";
            mUsbThermalPrinter.addString(storeAddress);
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setTextSize(24);
            mUsbThermalPrinter.setGray(6);
            String tpinNo = (data.getTpin_no() != null) ? data.getTpin_no() : "";
            mUsbThermalPrinter.addString("TIN NO: " + tpinNo);
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            // â”€â”€ RECEIPT TYPE â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            mUsbThermalPrinter.setTextSize(24);
            mUsbThermalPrinter.setBold(true);
            mUsbThermalPrinter.addString("COPY");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.addString("----------------------------------");
            mUsbThermalPrinter.printString();

            mUsbThermalPrinter.setAlgin(0);
            mUsbThermalPrinter.walkPaper(1);

            // â”€â”€ CHANGED 1: Buyer info with padLeft â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            String buyerName = (data.getCustomer_name() != null) ? data.getCustomer_name() : "";
            String buyerNameLabel = "BUYER'S NAME:";
            mUsbThermalPrinter.addString(buyerNameLabel + padLeft(buyerName, TSIZE22 - buyerNameLabel.length()));
            mUsbThermalPrinter.printString();

            String buyerTin = (data.getBuyers_tpin() != null) ? data.getBuyers_tpin() : "";
            String buyerTinLabel = "BUYER'S TIN:";
            mUsbThermalPrinter.addString(buyerTinLabel + padLeft(buyerTin, TSIZE22 - buyerTinLabel.length()));
            mUsbThermalPrinter.printString();

            String buyerContact = (data.getCustomer_mob_no() != null) ? data.getCustomer_mob_no() : "";
            String buyerContactLabel = "BUYER'S CONTACT:";
            mUsbThermalPrinter.addString(buyerContactLabel + padLeft(buyerContact, TSIZE22 - buyerContactLabel.length()));
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.addString("----------------------------------");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            // â”€â”€ ITEMS LOOP â€” uses salesItem â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            if (data.getSalesItem() != null) {
                for (SaleReceiptRes.SalesItem item : data.getSalesItem()) {
                    if (item == null) continue;

                    String productName = (item.getProduct_name() != null)
                            ? item.getProduct_name() : "";
                    numberOfItems++;

                    boolean looseOil = productName.toLowerCase().startsWith("bulk oil");

                    // Line 1: Product Name (bold)
                    mUsbThermalPrinter.setTextSize(24);
                    mUsbThermalPrinter.setGray(6);
                    mUsbThermalPrinter.setBold(true);
                    mUsbThermalPrinter.addString(productName);
                    mUsbThermalPrinter.printString();
                    mUsbThermalPrinter.setBold(false);
                    mUsbThermalPrinter.setTextSize(22);

                    String taxCode = "";
                    if (item.getTax_details() != null && item.getTax_details().getCode() != null) {
                        taxCode = item.getTax_details().getCode();
                    }

                    String rateCol = FunUtils.INSTANCE.formatPrintPrice(
                            item.getTax_inclusive_price() != null
                                    ? String.valueOf(item.getTax_inclusive_price()) : "0.00") + "x";

                    String qtyCol = FunUtils.INSTANCE.DtoString(
                            item.getQuantity() != null ? item.getQuantity() : 0.0);

                    String amountCol = FunUtils.INSTANCE.formatPrintPrice(
                            item.getTotal_amount() != null
                                    ? String.valueOf(item.getTotal_amount()) : "0.00") + taxCode;

                    int totalWidth    = TSIZE22;
                    int rightColWidth = 10;
                    int midColWidth   = 8;
                    int leftColWidth  = totalWidth - midColWidth - rightColWidth;

                    String line2 = String.format(
                            "%-" + leftColWidth + "s%" + midColWidth + "s%" + rightColWidth + "s",
                            rateCol,
                            qtyCol,
                            amountCol
                    );

                    mUsbThermalPrinter.addString(line2);
                    mUsbThermalPrinter.printString();

                    Double discountRate = item.getDiscount_rate();
                    Double totalAmount  = item.getTotal_amount();

                    if (discountRate != null && discountRate < 0
                            && totalAmount != null && totalAmount > 0) {

                        String discountText = "Discount " + discountRate.intValue() + "%";

                        double discountedTotal = totalAmount + (totalAmount * discountRate / 100);

                        String discountAmountStr = FunUtils.INSTANCE.formatPrintPrice(
                                String.valueOf(discountedTotal));

                        String discountLine = String.format(
                                "%-" + (totalWidth - rightColWidth) + "s%" + rightColWidth + "s",
                                discountText,
                                discountAmountStr
                        );

                        mUsbThermalPrinter.addString(discountLine);
                        mUsbThermalPrinter.printString();
                    }

                    mUsbThermalPrinter.walkPaper(1);
                }
            }
            mUsbThermalPrinter.addString("----------------------------------");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.setGray(6);
            mUsbThermalPrinter.setAlgin(1);
            mUsbThermalPrinter.setBold(true);
            mUsbThermalPrinter.setTextSize(23);
            mUsbThermalPrinter.addString("THIS IS NOT AN OFFICIAL RECEIPT");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.setBold(false);

            // â”€â”€ TOTALS â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.addString("----------------------------------");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            String grandTotal = (data.getGrand_total() != null)
                    ? FunUtils.INSTANCE.formatPrintPrice(data.getGrand_total()) : "0.00";
            mUsbThermalPrinter.setTextSize(26);
            mUsbThermalPrinter.setBold(true);
            String totalLabel = "TOTAL(" + currency + "):";
            mUsbThermalPrinter.addString(totalLabel + padLeft(grandTotal, TSIZE26 - totalLabel.length()));
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.setTextSize(22);

            // â”€â”€ TAX SUMMARY LOOP â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            List<SaleReceiptRes.TaxSummery> taxSummaryList = data.getTax_summery();
            if (taxSummaryList != null && !taxSummaryList.isEmpty()) {

                for (SaleReceiptRes.TaxSummery taxSummary : taxSummaryList) {
                    if (taxSummary.getCode() != null) {
                        String taxLabel = "TOTAL " + taxSummary.getCode_name();
                        double taxableValue = (taxSummary.getTaxable_value() != null)
                                ? taxSummary.getTaxable_value() : 0.0;
                        mUsbThermalPrinter.addString(taxLabel + padLeft(
                                FunUtils.INSTANCE.formatPrintPrice(String.valueOf(taxableValue)),
                                TSIZE22 - taxLabel.length()
                        ));
                        mUsbThermalPrinter.printString();
                    }
                }

                for (SaleReceiptRes.TaxSummery taxSummary : taxSummaryList) {
                    Double taxAmt = taxSummary.getTax_amount();
                    String code = taxSummary.getCode();
                    if (taxAmt != null && taxAmt != 0.0 && code != null) {
                        String taxAmountLabel = "TOTAL TAX " + code;
                        mUsbThermalPrinter.addString(taxAmountLabel + padLeft(
                                FunUtils.INSTANCE.formatPrintPrice(String.valueOf(taxAmt)),
                                TSIZE22 - taxAmountLabel.length()
                        ));
                        mUsbThermalPrinter.printString();
                    }
                }

                String totalTaxAmount = data.getTax_amount();
                if (totalTaxAmount != null && !totalTaxAmount.equals("0")
                        && !totalTaxAmount.equals("0.00")) {
                    String totalTaxLabel = "TOTAL TAX AMOUNT";
                    mUsbThermalPrinter.addString(totalTaxLabel + padLeft(
                            FunUtils.INSTANCE.formatPrintPrice(totalTaxAmount),
                            TSIZE22 - totalTaxLabel.length()
                    ));
                    mUsbThermalPrinter.printString();
                }
            }

            mUsbThermalPrinter.walkPaper(1);
            mUsbThermalPrinter.addString("----------------------------------");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            // Items count
            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.setTextSize(22);
            int itemsCount = (data.getSalesItem() != null) ? data.getSalesItem().size() : 0;
            mUsbThermalPrinter.addString("Items:" + padLeft(Integer.toString(itemsCount), TSIZE22 - 6));
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.addString("----------------------------------");
            mUsbThermalPrinter.printString();
            String rcptTypeRaw = "COPY";
            mUsbThermalPrinter.setTextSize(24);
            mUsbThermalPrinter.setBold(true);
            mUsbThermalPrinter.addString(rcptTypeRaw);
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.addString("----------------------------------");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            // â”€â”€ SDC INFORMATION â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setBold(true);
            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.addString("SDC INFORMATION");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.setGray(6);
            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);

            if (data.getVsdc_reciept() != null && !data.getVsdc_reciept().isEmpty()) {
                SaleReceiptRes.VsdcReceipt vsdc = data.getVsdc_reciept().get(0);

                // â”€â”€ CHANGED 2: Date parsed properly with yyyyMMddHHmmss â”€â”€
                String vsdcRawDate = (vsdc.getVsdcRcptPbctDate() != null)
                        ? vsdc.getVsdcRcptPbctDate() : "";
                String vsdcDate = "";
                String vsdcTime = "";
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
                    Date parsedVsdcDate = inputFormat.parse(vsdcRawDate);
                    if (parsedVsdcDate != null) {
                        vsdcDate = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(parsedVsdcDate);
                        vsdcTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(parsedVsdcDate);
                    }
                } catch (ParseException e) {
                    Log.e("DateFormat", "Parsing failed: " + e.getMessage());
                    vsdcDate = vsdcRawDate; // fallback
                }

                String vsdcDateLabel = "DATE: " + vsdcDate;
                String vsdcTimeLabel = "TIME: " + vsdcTime;
                int vsdcSpaces = TSIZE22 - vsdcDateLabel.length() - vsdcTimeLabel.length();
                if (vsdcSpaces < 1) vsdcSpaces = 1;
                StringBuilder vsdcDateLine = new StringBuilder(vsdcDateLabel);
                for (int i = 0; i < vsdcSpaces; i++) vsdcDateLine.append(" ");
                vsdcDateLine.append(vsdcTimeLabel);

                mUsbThermalPrinter.addString(vsdcDateLine.toString());
                mUsbThermalPrinter.printString();

                // â”€â”€ CHANGED 1: SDC ID with padLeft â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                String sdcId = (vsdc.getSdcId() != null) ? vsdc.getSdcId() : "";
                String sdcIdLabel = "SDC ID:";
                mUsbThermalPrinter.addString(sdcIdLabel + padLeft(sdcId, TSIZE22 - sdcIdLabel.length()));
                mUsbThermalPrinter.printString();

                // â”€â”€ CHANGED 1: Receipt Number (SDC) with padLeft â”€â”€â”€â”€â”€
                String rcptNo = (vsdc.getRcptNo() != null && vsdc.getRcptNo() != 0)
                        ? String.valueOf(vsdc.getRcptNo()) : "";
                String sdcRcptLabel = "RECEIPT NUMBER:";
                String sdcRcptValue = rcptNo + "/" + vsdc.getTotRcptNo() + " CS";
                mUsbThermalPrinter.addString(sdcRcptLabel + padLeft(sdcRcptValue, TSIZE22 - sdcRcptLabel.length()));
                mUsbThermalPrinter.printString();

                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);

                String intrlData = (vsdc.getIntrlData() != null) ? vsdc.getIntrlData() : "";
                mUsbThermalPrinter.addString("INTERNAL DATA:  " + intrlData);
                mUsbThermalPrinter.printString();

                String rcptSign = (vsdc.getRcptSign() != null) ? vsdc.getRcptSign() : "";
                mUsbThermalPrinter.addString("RECEIPT SIGNATURE:  " + rcptSign);
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.walkPaper(1);

                // â”€â”€ RECEIPT NUMBER / DATE / MRC NO â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
                mUsbThermalPrinter.addString("----------------------------------");
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.walkPaper(1);

                // â”€â”€ CHANGED 1: Receipt Number with padLeft â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                String receiptNum = (data.getInvoice_id() != null) ? data.getInvoice_id() : "";
                String rcptNumLabel = "RECEIPT NUMBER:";
                mUsbThermalPrinter.addString(rcptNumLabel + padLeft(receiptNum, TSIZE22 - rcptNumLabel.length()));
                mUsbThermalPrinter.printString();

                // â”€â”€ CHANGED 2: Purchase date properly parsed â”€â”€â”€â”€â”€â”€â”€â”€â”€
                String rawPurchaseDateTime = (data.getPurchase_date_time() != null)
                        ? data.getPurchase_date_time() : "";
                String purchaseDate = "";
                String purchaseTime = "";
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat(
                            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault());
                    Date parsedDate = inputFormat.parse(rawPurchaseDateTime);
                    if (parsedDate != null) {
                        purchaseDate = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
                                .format(parsedDate);
                        purchaseTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                .format(parsedDate);
                    }
                } catch (ParseException e) {
                    Log.e("DateFormat", "Parsing failed: " + e.getMessage());
                    purchaseDate = rawPurchaseDateTime; // fallback
                }

                String purchaseDateLabel = "DATE: " + purchaseDate;
                String purchaseTimeLabel = "TIME: " + purchaseTime;
                int purchaseSpaces = TSIZE22 - purchaseDateLabel.length() - purchaseTimeLabel.length();
                if (purchaseSpaces < 1) purchaseSpaces = 1;
                StringBuilder purchaseDateLine = new StringBuilder(purchaseDateLabel);
                for (int i = 0; i < purchaseSpaces; i++) purchaseDateLine.append(" ");
                purchaseDateLine.append(purchaseTimeLabel);

                mUsbThermalPrinter.addString(purchaseDateLine.toString());
                mUsbThermalPrinter.printString();

                // â”€â”€ CHANGED 1: MRC No with padLeft â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                String mrcLabel = "MRC NO.:";
                String mrcValue = (vsdc.getMrcNo() != null) ? vsdc.getMrcNo() + "." : ".";
                mUsbThermalPrinter.addString(mrcLabel + padLeft(mrcValue, TSIZE22 - mrcLabel.length()));
                mUsbThermalPrinter.printString();

            } else {

                String sdcId = (data.getSdc_id() != null) ? data.getSdc_id() : "";
                String sdcIdLabel = "SDC ID:";
                mUsbThermalPrinter.addString(sdcIdLabel + padLeft(sdcId, TSIZE22 - sdcIdLabel.length()));
                mUsbThermalPrinter.printString();

                String receiptNo = (data.getReceipt_no() != null) ? data.getReceipt_no() : "";
                String fallbackRcptLabel = "RECEIPT NUMBER:";
                mUsbThermalPrinter.addString(fallbackRcptLabel + padLeft(receiptNo + " NS", TSIZE22 - fallbackRcptLabel.length()));
                mUsbThermalPrinter.printString();

                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);

                String internalData = (data.getInternal_data() != null) ? data.getInternal_data() : "";
                mUsbThermalPrinter.addString("INTERNAL DATA:  " + internalData);
                mUsbThermalPrinter.printString();

                String receiptSign = (data.getReceipt_sign() != null) ? data.getReceipt_sign() : "";
                mUsbThermalPrinter.addString("RECEIPT SIGNATURE:  " + receiptSign);
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.walkPaper(1);
            }

            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.addString("----------------------------------");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            // â”€â”€ FOOTER â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.setGray(6);
            mUsbThermalPrinter.addString("THANK YOU");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.addString("WE APPRECIATE YOUR BUSINESS");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(3);

            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setTextSize(20);
            mUsbThermalPrinter.addString("*** END ***");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(5);
            mUsbThermalPrinter.addString(" ");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(5);
            mUsbThermalPrinter.reset();

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("PrinterUtil", "Print error: " + e.getMessage(), e);
            Result = e.toString();
            if (Result.contains("NoPaperException")) {
                nopaper = true;
            } else if (Result.contains("OverHeatException")) {
                handler.sendMessage(handler.obtainMessage(OVERHEAT, 1, 0, null));
            } else {
                handler.sendMessage(handler.obtainMessage(PRINTERR, 1, 0, null));
            }
        } finally {
            handler.sendMessage(handler.obtainMessage(CANCELPROMPT, 1, 0, null));
            if (nopaper) {
                handler.sendMessage(handler.obtainMessage(NOPAPER, 1, 0, null));
                nopaper = false;
                return;
            }
        }
    }










//    private class contentPrintThread extends Thread {
//        public void run() {
//            super.run();
//            try {
//
////                val top: String,
////                        val storeName: String,
////                        val storeAddress: String,
////                        val vatInfo: String,
////                        val dateTime: String,
////                        val receiptInfo: String,
////                        val itemInfo:String,
////                        val totalPrice: String,
////                        val itemCount: String,
////                        val taxInfo: String,
////                        val totalVat: String,
////                        val payableAmount: String,
////                        val paymentModeInfo: String,
////                        val ejInfo: String,
////                        val bottom: String
//
//
//                ReceiptData receipt = receipt_data;
////
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
////                mUsbThermalPrinter.setLeftIndent(1);
////                mUsbThermalPrinter.setLineSpace(3);
////                mUsbThermalPrinter.setTextSize(20);
////                mUsbThermalPrinter.setGray(3);
////                mUsbThermalPrinter.setBold(false);
////               // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getTop());
////                mUsbThermalPrinter.printString();
////               mUsbThermalPrinter.walkPaper(1);
////
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
////                mUsbThermalPrinter.setTextSize(22);
////                mUsbThermalPrinter.setGray(6);
////                mUsbThermalPrinter.setBold(true);
////                // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getStoreName());
////                mUsbThermalPrinter.printString();
////                mUsbThermalPrinter.walkPaper(1);
////
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
////                mUsbThermalPrinter.setTextSize(20);
////                mUsbThermalPrinter.setGray(6);
////                mUsbThermalPrinter.setBold(false);
////                // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getStoreAddress());
////                mUsbThermalPrinter.printString();
////                mUsbThermalPrinter.walkPaper(1);
////
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
////                mUsbThermalPrinter.setTextSize(20);
////                mUsbThermalPrinter.setGray(6);
////                mUsbThermalPrinter.setBold(false);
////                // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getVatInfo());
////                mUsbThermalPrinter.printString();
////                mUsbThermalPrinter.walkPaper(1);
////
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
////                mUsbThermalPrinter.setTextSize(20);
////                mUsbThermalPrinter.setGray(6);
////                mUsbThermalPrinter.setBold(false);
////                // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getDateTime());
////                mUsbThermalPrinter.printString();
////                mUsbThermalPrinter.walkPaper(1);
////
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
////                mUsbThermalPrinter.setTextSize(20);
////                mUsbThermalPrinter.setGray(6);
////                mUsbThermalPrinter.setBold(false);
////                // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getReceiptInfo());
////                mUsbThermalPrinter.printString();
////                mUsbThermalPrinter.walkPaper(1);
////
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.walkPaper(1);
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
////                mUsbThermalPrinter.setTextSize(20);
////                mUsbThermalPrinter.setGray(6);
////                mUsbThermalPrinter.setBold(false);
////                // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getItemInfo());
////                mUsbThermalPrinter.setMonoSpace(true);
////                mUsbThermalPrinter.printString();
////                mUsbThermalPrinter.walkPaper(1);
////
////
////
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
////                mUsbThermalPrinter.setTextSize(22);
////                mUsbThermalPrinter.setGray(6);
////                mUsbThermalPrinter.setBold(true);
////                // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getTotalPrice());
////                mUsbThermalPrinter.printString();
////                mUsbThermalPrinter.walkPaper(2);
////
////
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
////                mUsbThermalPrinter.setTextSize(20);
////                mUsbThermalPrinter.setGray(6);
////                mUsbThermalPrinter.setBold(false);
////                // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getItemCount());
////                mUsbThermalPrinter.printString();
////                mUsbThermalPrinter.walkPaper(1);
////
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
////                mUsbThermalPrinter.setTextSize(20);
////                mUsbThermalPrinter.setGray(6);
////                mUsbThermalPrinter.setBold(false);
////                // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getTaxInfo());
////                mUsbThermalPrinter.printString();
////                mUsbThermalPrinter.walkPaper(1);
////
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
////                mUsbThermalPrinter.setTextSize(22);
////                mUsbThermalPrinter.setGray(6);
////                mUsbThermalPrinter.setBold(true);
////                // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getTotalVat());
////                mUsbThermalPrinter.printString();
////                mUsbThermalPrinter.walkPaper(1);
////
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
////                mUsbThermalPrinter.setTextSize(22);
////                mUsbThermalPrinter.setGray(6);
////                mUsbThermalPrinter.setBold(true);
////                // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getPayableAmount());
////                mUsbThermalPrinter.printString();
////                mUsbThermalPrinter.walkPaper(1);
////
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
////                mUsbThermalPrinter.setTextSize(20);
////                mUsbThermalPrinter.setGray(6);
////                mUsbThermalPrinter.setBold(false);
////                // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getPaymentModeInfo());
////                mUsbThermalPrinter.printString();
////                mUsbThermalPrinter.walkPaper(1);
//
//
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
////                mUsbThermalPrinter.setTextSize(20);
////                mUsbThermalPrinter.setGray(6);
////                mUsbThermalPrinter.setBold(false);
////                // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getEjInfo());
////                mUsbThermalPrinter.printString();
////                mUsbThermalPrinter.walkPaper(1);
//
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
////                mUsbThermalPrinter.setTextSize(20);
////                mUsbThermalPrinter.setGray(6);
////                mUsbThermalPrinter.setBold(false);
////                // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getBottom());
////                mUsbThermalPrinter.printString();
////                mUsbThermalPrinter.walkPaper(1);
//
//
//                mUsbThermalPrinter.reset();
//                //mUsbThermalPrinter.set(1);
//
//                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
//               // mUsbThermalPrinter.setTextSize(20);
//                mUsbThermalPrinter.setGray(6);
//                mUsbThermalPrinter.setBold(false);
//
//                // mUsbThermalPrinter.setItalic(true);
//
////                for  ( SalesItem item : receipt.getProduct_item()  ){
////
////                    String[] colsTestArr = {item.getProduct_name()+"-"+item.getDistribution_pack().getProduct_description()
////                            , item.getQuantity() +" X "+item.getRetail_price(), item.getTotal_amount()+"0000"};
////                    int[] colsWidthArr = {100, 50, 50};
////                    int[] colsAlign = {0, 1, 2}; // Example alignment values
////                    int colsTextSize = 20;
////
////                     mUsbThermalPrinter.addColumnsString(colsTestArr, colsWidthArr, colsAlign, colsTextSize);
////
////                     mUsbThermalPrinter.printString();
////                     mUsbThermalPrinter.walkPaper(1);
////
////
////                }
//
//
////                   mUsbThermalPrinter.printColumnsString(new String[]{"Item 1","$2000.00",}, new int[]{6,2}, new int[]{0,0}, 20);
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setBold(true);
////                mUsbThermalPrinter.setGray(6);
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
////
////                mUsbThermalPrinter.printColumnsString(new String[]{"Item 1123456","$200023.00",}, new int[]{6,3}, new int[]{0,0}, 22);
//
//                mUsbThermalPrinter.reset();
//                mUsbThermalPrinter.setBold(true);
//                mUsbThermalPrinter.setGray(6);
//                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
//
//                mUsbThermalPrinter.setUnderline(false);
//                mUsbThermalPrinter.setAlgin(1);
//                mUsbThermalPrinter.printStringAndWalk(0,0,2);
//
//
//
//                mUsbThermalPrinter.printColumnsString(new String[]{"COLUMN Aaaaaaa","Culoaaa B","20000aaaa"}, new int[]{6,3,3}, new int[]{0,1,2}, 22);
//
//
//
//                mUsbThermalPrinter.walkPaper(1);
//
//                mUsbThermalPrinter.autoBreakSet(true);
//                mUsbThermalPrinter.addString("----------------------------");
//                mUsbThermalPrinter.printString();
//
//
    ////                String[] colsTestArr = {"Column1", "Column2", "Column3"};
    //////                int[] colsWidthArr = {100, 200, 150};
    //////                int[] colsAlign = {1, 2, 3}; // Example alignment values
    //////                int colsTextSize = 12;
//
//                // Calling the method
//               // mUsbThermalPrinter.addColumnsString(colsTestArr, colsWidthArr, colsAlign, colsTextSize);
//               // mUsbThermalPrinter.printColumnsString(colsTestArr, colsWidthArr, colsAlign, colsTextSize);
//              //  mUsbThermalPrinter.addColumnsString(new String[]{"AAA","BBB","CCC"}, new int[]{6,6,6}, new int[]{0,0,0}, 16);
//
//               // mUsbThermalPrinter.printString();
//                mUsbThermalPrinter.walkPaper(1);
//
//
//
//                mUsbThermalPrinter.walkPaper(1);
//                mUsbThermalPrinter.reset();
//
//
//                Toast.makeText(context, "Printing initiated", Toast.LENGTH_LONG).show();
//
//            } catch (Exception e) {
//                e.printStackTrace();
//                System.out.println(e.toString());
//
//                Result = e.toString();
//                if (Result.contains("NoPaperException")) {
//                    nopaper = true;
//                } else if (Result.contains("OverHeatException")) {
//                    handler.sendMessage(handler.obtainMessage(OVERHEAT, 1, 0, null));
//                } else {
//                    handler.sendMessage(handler.obtainMessage(PRINTERR, 1, 0, null));
//                }
//            }finally {
//                handler.sendMessage(handler.obtainMessage(CANCELPROMPT, 1, 0, null));
//                if (nopaper) {
//                    handler.sendMessage(handler.obtainMessage(NOPAPER, 1, 0, null));
//                    nopaper = false;
//                    return;
//                }
//            }
//        }
//    }

    private String generateReceipt(ArrayList<MyItem> items) {
        StringBuilder printContent = new StringBuilder();
        printContent.append("\n             RetailOne\n")
                .append("---------------------------\n")
                .append("DateÃ¯Â¼Å¡2015-01-01 16:18:20\n")
                .append("invoiceÃ¯Â¼Å¡12378945664\n")
                .append("idÃ¯Â¼Å¡1001000000000529142\n")
                .append("---------------------------\n")
                .append("    item        quantity   Price  total\n");

        double total = 0;
        for (SalesItem item : posSalesDetails.getData().getSalesItem()) {
            // double itemTotal = item.getQuantity() * item.getPrice();
            printContent.append(String.format("%-14s %8d %10.2f %10.2f\n", formatItemName(item.getProduct_name()), item.getQuantity(), item.getTax_inclusive_price(), item.getTotal_amount()));
            //total += itemTotal;
        }

        printContent.append("----------------------------\n")
                .append(String.format(" taxÃ¯Â¼Å¡%10.2f\n", 1000.00))
                .append("----------------------------\n")
                .append(String.format("paidÃ¯Â¼Å¡%10.2f\n", 10000.00))
                .append(String.format("tenderÃ¯Â¼Å¡%10.2f\n", 1000.00))
                .append(String.format("paidÃ¯Â¼Å¡%10.2f\n", 9000.00))
                .append("----------------------------\n")
                .append(" Thanks for shopping with us\n")
                .append("tel :1111111111\n");

        return printContent.toString();
    }

    private static String formatItemName(String name) {
        if (name.length() > 14) {
            return name.substring(0, 14);
        } else {
            return String.format("%-14s", name);
        }
    }

    public void registerBatteryReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction("android.intent.action.BATTERY_CAPACITY_EVENT");
        context.registerReceiver(printReceive, filter);
    }

    public void unregisterBatteryReceiver() {
        context.unregisterReceiver(printReceive);
    }

    private final BroadcastReceiver printReceive = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_NOT_CHARGING);
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 0);

                if (deviceType == StringUtil.DeviceModelEnum.TPS390.ordinal()) {
                    LowBattery = level * 5 <= scale;
                } else if (SystemUtil.getInternalModel().equals("M8")) {
                    LowBattery = level * 10 <= scale;
                } else {
                    LowBattery = status != BatteryManager.BATTERY_STATUS_CHARGING && level * 5 <= scale;
                }
            } else if (action.equals("android.intent.action.BATTERY_CAPACITY_EVENT")) {
                int status = intent.getIntExtra("action", 0);
                int level = intent.getIntExtra("level", 0);
                LowBattery = status == 0 && level < 1;
            }
        }
    };
}

































/*
package com.retailone.pos.utils;


import static android.provider.MediaStore.Images.Media.getBitmap;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.Spanned;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.common.apiutil.CommonException;
import com.common.apiutil.printer.NewUsbThermalPrinter;
import com.common.apiutil.printer.UsbThermalPrinter;
//import com.common.apiutil.util.SDKUtil;
import com.common.apiutil.util.StringUtil;
import com.common.apiutil.util.SystemUtil;
import com.retailone.pos.R;
import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper;
import com.retailone.pos.models.LocalizationModel.LocalizationData;
import com.retailone.pos.models.PosSalesDetailsModel.PosSalesDetails;
import com.retailone.pos.models.PosSalesDetailsModel.SalesItem;
import com.retailone.pos.models.PrinterModel.ReceiptData;
import com.retailone.pos.models.ReturnSalesItemModel.ReturnSaleResModel.ReturnSaleRes;
import com.retailone.pos.models.ReturnSalesItemModel.ReturnSaleResModel.ReturnedItem;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;

public class PrinterUtil {

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

    private NewUsbThermalPrinter mUsbThermalPrinter;
    private ProgressDialog dialog;
    private ProgressDialog progressDialog;
    private MyHandler handler;
    private boolean LowBattery = false;
    private Context context;
    private int deviceType;
    private boolean nopaper = false;
    ArrayList<MyItem> itemx ;

    private String Result;

    private String currency;
    private String zone;
    String printType = "";
    LocalizationData localizationData;




    PosSalesDetails posSalesDetails;
    ReturnSaleRes returnSaleRes;
    ReceiptData receipt_data;

    public PrinterUtil(Context context) {
        this.context = context;
        mUsbThermalPrinter = new NewUsbThermalPrinter(context);
        deviceType = SystemUtil.getDeviceType();
        handler = new MyHandler();
        currency =  new LocalizationHelper(context).getLocalizationData().getCurrency();
        zone =  new LocalizationHelper(context).getLocalizationData().getTimezone();

       /// SDKUtil.getInstance(context).initSDK();
        initializePrinter();
    }

    private void initializePrinter() {
        dialog = new ProgressDialog(context);
        dialog.setTitle("Initializing Printer");
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        new Thread(() -> {
            try {
                mUsbThermalPrinter.start(0);
                mUsbThermalPrinter.reset();
            } catch (CommonException e) {
                e.printStackTrace();
            } finally {
                dialog.dismiss();
            }
        }).start();
    }

    public void printReceiptData(PosSalesDetails _posSalesDetails) {
        printType = "SALE";

      //  receipt_data = receiptData;
        posSalesDetails = _posSalesDetails;

        if (LowBattery) {
            handler.sendMessage(handler.obtainMessage(LOWBATTERY, 1, 0, null));
        } else {
            if (!nopaper) {
               // handler.sendMessage(handler.obtainMessage(PRINTPICTURE, 1, 0, null));

                handler.sendMessage(handler.obtainMessage(PRINTCONTENT, 1, 0, null));
                //Toast.makeText(context, "Paper Available", Toast.LENGTH_LONG).show();

            } else {
                Toast.makeText(context, "No paper detected", Toast.LENGTH_LONG).show();
            }
        }

    }

    public void printReturnReceiptData( ReturnSaleRes _returnSaleRes) {
        printType = "RETURN";

        returnSaleRes = _returnSaleRes;

        if (LowBattery) {
            handler.sendMessage(handler.obtainMessage(LOWBATTERY, 1, 0, null));
        } else {
            if (!nopaper) {
               // handler.sendMessage(handler.obtainMessage(PRINTPICTURE, 1, 0, null));

                handler.sendMessage(handler.obtainMessage(PRINTCONTENT, 1, 0, null));
                //Toast.makeText(context, "Paper Available", Toast.LENGTH_LONG).show();

            } else {
                Toast.makeText(context, "No paper detected", Toast.LENGTH_LONG).show();
            }
        }

    }

//    public void printReceipt(@NotNull PosSalesDetails posSaleData) {
//
//        posSalesDetails = posSaleData;
//
//        if (LowBattery) {
//            handler.sendMessage(handler.obtainMessage(4, 1, 0, null));
//        } else {
//            if (!nopaper) {
//                handler.sendMessage(handler.obtainMessage(9, 1, 0, null));
//                Toast.makeText(context, "Paper Available", Toast.LENGTH_LONG).show();
//
//            } else {
//                Toast.makeText(context, "No paper detected", Toast.LENGTH_LONG).show();
//            }
//        }
//
//    }

    private class MyHandler extends Handler {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case NOPAPER:
                    //NOPAPER
                    noPaperDlg();
                    break;
                case LOWBATTERY:
                    //LOWBATTERY
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
                    alertDialog.setTitle("Operation Result");
                    alertDialog.setMessage("Low Battery");
                    alertDialog.setPositiveButton("OK", (dialog, which) -> {
                    });
                    alertDialog.show();
                    break;
                case PRINTCONTENT:
                    //Toast.makeText(context, "printContent", Toast.LENGTH_LONG).show();
                    new contentPrintThread().start();
                    break;

                case PRINTPICTURE:
                    new printPicture().start();
                    break;

                case NOBLACKBLOCK:
                    //NOBLACKBLOCK
                    Toast.makeText(context, R.string.maker_not_find, Toast.LENGTH_SHORT).show();
                    break;
//                case 10:
//                    //CANCELPROMPT
//                    if (progressDialog != null && !UsbPrinterActivity.this.isFinishing()) {
//                        progressDialog.dismiss();
//                        progressDialog = null;
//                    }
//                    break;
                default:
                   // Toast.makeText(context, "Print Error!", Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    private void noPaperDlg() {
        AlertDialog.Builder dlg = new AlertDialog.Builder(context);
        dlg.setTitle("No Paper");
        dlg.setMessage("Please load paper and try again.");
        dlg.setCancelable(false);
        dlg.setPositiveButton("OK", (dialog, which) -> {
        });
        dlg.show();
    }



    private class printPicture extends Thread {

        public void run() {
            super.run();
            try {
                mUsbThermalPrinter.reset();
                mUsbThermalPrinter.setGray(3);
                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
                //File file = new File(picturePath);
                //if (file.exists()) {
                mUsbThermalPrinter.printLogo(drawableToBitmap(ContextCompat.getDrawable(context,R.drawable.mlogo)), false);
                mUsbThermalPrinter.walkPaper(20);
				*/
/*} else {
					runOnUiThread(new Runnable() {


						public void run() {
							Toast.makeText(UsbPrinterActivity.this, getString(R.string.not_find_picture),
									Toast.LENGTH_LONG).show();
						}
					});
				}*//*

            } catch (Exception e) {
                e.printStackTrace();
                Result = e.toString();
                if (Result.contains("NoPaperException")) {
                    nopaper = true;
                } else if (Result.contains("OverHeatException")) {
                    handler.sendMessage(handler.obtainMessage(OVERHEAT, 1, 0, null));
                } else {
                    handler.sendMessage(handler.obtainMessage(PRINTERR, 1, 0, null));
                }
            } finally {
                handler.sendMessage(handler.obtainMessage(CANCELPROMPT, 1, 0, null));
                if (nopaper) {
                    handler.sendMessage(handler.obtainMessage(NOPAPER, 1, 0, null));
                    nopaper = false;
                    return;
                }
            }
        }
    }


    public Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap;

        if (drawable instanceof BitmapDrawable) {
            // If the drawable is a BitmapDrawable, just return its bitmap
            bitmap = ((BitmapDrawable) drawable).getBitmap();
        } else {
            // Otherwise, create a new bitmap and draw the drawable on it
            int width = drawable.getIntrinsicWidth();
            int height = drawable.getIntrinsicHeight();

            // Ensure dimensions are valid, otherwise, use default dimensions
            width = width > 0 ? width : 1;
            height = height > 0 ? height : 1;

            // Create a bitmap with the specified width and height
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            // Create a canvas to draw on the bitmap
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        }

        return bitmap;
    }


    private class contentPrintThread extends Thread {
        public void run() {
            super.run();


                if(printType.equals("SALE")){

                    printSaleType(posSalesDetails);

                }else if(printType.equals("RETURN")){

                    printReturnType(returnSaleRes);


            }
        }
    }

    private void printReturnType(ReturnSaleRes details) {
        try {

            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setLeftIndent(1);
            mUsbThermalPrinter.setLineSpace(3);
            mUsbThermalPrinter.setTextSize(20);
            mUsbThermalPrinter.setGray(3);
            mUsbThermalPrinter.setBold(false);
            // mUsbThermalPrinter.setItalic(true);
            mUsbThermalPrinter.addString("*** START OF LEGEAL RECEIPT ***");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(3);


            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.setGray(4);
            mUsbThermalPrinter.setBold(true);
            // mUsbThermalPrinter.setItalic(true);
            mUsbThermalPrinter.addString(details.getData().getStore().getStore_name().toString().toUpperCase());
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);


            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setTextSize(20);
            mUsbThermalPrinter.setGray(4);
            mUsbThermalPrinter.setBold(false);
            // mUsbThermalPrinter.setItalic(true);
            mUsbThermalPrinter.addString(details.getData().getStore().getAddress().toString().toUpperCase());
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(2);

            mUsbThermalPrinter.addString("CREDIT NOTE");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);


            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
            mUsbThermalPrinter.setTextSize(20);
            mUsbThermalPrinter.setGray(4);
            mUsbThermalPrinter.setBold(false);
            // mUsbThermalPrinter.setItalic(true);
            mUsbThermalPrinter.printColumnsString(new String[]{"VAT NOÃ¯Â¼Å¡",details.getData().getVat_no(),}, new int[]{3,6}, new int[]{0,1}, 22);
            mUsbThermalPrinter.printColumnsString(new String[]{"TPIN NOÃ¯Â¼Å¡",details.getData().getTpin_no(),}, new int[]{3,6}, new int[]{0,1}, 22);

            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.printColumnsString(new String[]{"DATEÃ¯Â¼Å¡",DateTimeFormatting.Companion.formatSaleReturndate(details.getData().getReturned_date(),zone),}, new int[]{3,6}, new int[]{0,1}, 22);

            mUsbThermalPrinter.walkPaper(2);
            mUsbThermalPrinter.printColumnsString(new String[]{"Credit Note NoÃ¯Â¼Å¡",details.getData().getReturned_invoice_id(),}, new int[]{5,5}, new int[]{0,1}, 22);
            mUsbThermalPrinter.printColumnsString(new String[]{"BUYERÃ¢â‚¬â„¢S NAMEÃ¯Â¼Å¡",details.getData().getCustomer_name(),}, new int[]{5,5}, new int[]{0,1}, 22);
            mUsbThermalPrinter.printColumnsString(new String[]{"BUYERÃ¢â‚¬â„¢S TPINÃ¯Â¼Å¡",details.getData().getBuyers_tpin(),}, new int[]{5,5}, new int[]{0,1}, 22);
            mUsbThermalPrinter.walkPaper(2);


            mUsbThermalPrinter.addString("----------------------------");
            mUsbThermalPrinter.printString();

            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.reset();
            //mUsbThermalPrinter.set(1);

            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            // mUsbThermalPrinter.setTextSize(20);
            mUsbThermalPrinter.setGray(4);
            mUsbThermalPrinter.setBold(false);
            for (ReturnedItem item : details.getData().getReturned_items()){
                String[] colsTestArr = {item.getProduct_name()+"("+item.getDistribution_pack_name()+")"
                        , " "+FunUtils.INSTANCE.DtoString(item.getQuantity()) +" X "+FunUtils.INSTANCE.formatPrintPrice(Double.toString(item.getRetail_price()))," "+ FunUtils.INSTANCE.formatPrintPrice(Double.toString(item.getTotal_amount()))};
                int[] colsWidthArr = {5, 4, 3};
                int[] colsAlign = {0, 1, 2}; // Example alignment values
                int colsTextSize = 20;

                mUsbThermalPrinter.addColumnsString(colsTestArr, colsWidthArr, colsAlign, colsTextSize);
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.walkPaper(1);
            }


            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
            mUsbThermalPrinter.setTextSize(20);
            mUsbThermalPrinter.setGray(4);
            mUsbThermalPrinter.setBold(false);
            //mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.addString("----------------------------");
            mUsbThermalPrinter.printString();

            mUsbThermalPrinter.walkPaper(1);


            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.setGray(4);
            mUsbThermalPrinter.setBold(true);
            // mUsbThermalPrinter.setItalic(true);
            mUsbThermalPrinter.printColumnsString(new String[]{"TOTAL ("+currency+") Ã¯Â¼Å¡",FunUtils.INSTANCE.formatPrintPrice(Double.toString(details.getData().getTotal())),}, new int[]{6,3}, new int[]{0,1}, 22);

            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
            mUsbThermalPrinter.setTextSize(20);
            mUsbThermalPrinter.setGray(4);
            mUsbThermalPrinter.setBold(false);
            //mUsbThermalPrinter.walkPaper(1);
            mUsbThermalPrinter.printColumnsString(new String[]{"ItemsÃ¯Â¼Å¡",Integer.toString(details.getData().getReturned_items().size())}, new int[]{6,3}, new int[]{0,1}, 22);


            mUsbThermalPrinter.addString("----------------------------");
            mUsbThermalPrinter.printString();

            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.printColumnsString(new String[]{"TAX EXÃ¯Â¼Å¡",FunUtils.INSTANCE.formatPrintPrice(details.getData().getTax_ex())}, new int[]{6,3}, new int[]{0,1}, 22);
            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.printColumnsString(new String[]{"TAX VAT@"+details.getData().getTax()+"% ",FunUtils.INSTANCE.formatPrintPrice(details.getData().getTax_amount())}, new int[]{6,3}, new int[]{0,1}, 22);
            mUsbThermalPrinter.addString("----------------------------");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);


            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.setGray(4);
            mUsbThermalPrinter.setBold(true);
            // mUsbThermalPrinter.setItalic(true);
            mUsbThermalPrinter.printColumnsString(new String[]{"TOTAL VATÃ¯Â¼Å¡",FunUtils.INSTANCE.formatPrintPrice(details.getData().getTax_amount()),}, new int[]{6,3}, new int[]{0,1}, 22);

            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
            mUsbThermalPrinter.setTextSize(20);
            mUsbThermalPrinter.setGray(4);
            mUsbThermalPrinter.setBold(false);
            //mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.addString("----------------------------");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.setGray(4);
            mUsbThermalPrinter.setBold(true);
            // mUsbThermalPrinter.setItalic(true);
            mUsbThermalPrinter.printColumnsString(new String[]{"PAYABLEÃ¯Â¼Å¡",FunUtils.INSTANCE.formatPrintPrice(details.getData().getGrand_total()),}, new int[]{6,3}, new int[]{0,1}, 22);

            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
            mUsbThermalPrinter.setTextSize(20);
            mUsbThermalPrinter.setGray(4);
            mUsbThermalPrinter.setBold(false);
            //mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.addString("----------------------------");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);


            mUsbThermalPrinter.printColumnsString(new String[]{"EJ NO:",details.getData().getEj_no(),}, new int[]{5,5}, new int[]{0,1}, 22);
            mUsbThermalPrinter.printColumnsString(new String[]{"EJ ACTIVATION DATE:",details.getData().getEj_activation_date(),}, new int[]{5,5}, new int[]{0,1}, 22);
            mUsbThermalPrinter.printColumnsString(new String[]{"SDC ID:",details.getData().getSdc_id(),}, new int[]{5,5}, new int[]{0,1}, 22);
            mUsbThermalPrinter.printColumnsString(new String[]{"RECEIPT NO:",details.getData().getReceipt_no(),}, new int[]{5,5}, new int[]{0,1}, 22);
            mUsbThermalPrinter.printColumnsString(new String[]{"INTERNAL DATA:",details.getData().getInternal_data(),}, new int[]{5,5}, new int[]{0,1}, 22);
            mUsbThermalPrinter.walkPaper(2);

            mUsbThermalPrinter.printColumnsString(new String[]{"Receipt Sign:",details.getData().getReceipt_sign(),}, new int[]{5,5}, new int[]{0,1}, 22);


            mUsbThermalPrinter.addString("----------------------------");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(2);


            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setLeftIndent(1);
            mUsbThermalPrinter.setLineSpace(3);
            mUsbThermalPrinter.setTextSize(20);
            mUsbThermalPrinter.setGray(3);
            mUsbThermalPrinter.setBold(false);
            // mUsbThermalPrinter.setItalic(true);
            mUsbThermalPrinter.addString("*** END OF LEGEAL RECEIPT ***");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(15);

            mUsbThermalPrinter.reset();


            // Toast.makeText(context, "Printing initiated", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.toString());

            Result = e.toString();
            if (Result.contains("NoPaperException")) {
                nopaper = true;
            } else if (Result.contains("OverHeatException")) {
                handler.sendMessage(handler.obtainMessage(OVERHEAT, 1, 0, null));
            } else {
                handler.sendMessage(handler.obtainMessage(PRINTERR, 1, 0, null));
            }
        }finally {
            handler.sendMessage(handler.obtainMessage(CANCELPROMPT, 1, 0, null));
            if (nopaper) {
                handler.sendMessage(handler.obtainMessage(NOPAPER, 1, 0, null));
                nopaper = false;
                return;
            }
        }
    }

    private void printSaleType(PosSalesDetails details) {

        try {

            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setLeftIndent(1);
            mUsbThermalPrinter.setLineSpace(3);
            mUsbThermalPrinter.setTextSize(20);
            mUsbThermalPrinter.setGray(3);
            mUsbThermalPrinter.setBold(false);
            // mUsbThermalPrinter.setItalic(true);
            mUsbThermalPrinter.addString("*** START OF LEGEAL RECEIPT ***");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(3);


            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.setGray(4);
            mUsbThermalPrinter.setBold(true);
            // mUsbThermalPrinter.setItalic(true);
            mUsbThermalPrinter.addString(details.getData().getStore().getStore_name().toString().toUpperCase());
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);


            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setTextSize(20);
            mUsbThermalPrinter.setGray(4);
            mUsbThermalPrinter.setBold(false);
            // mUsbThermalPrinter.setItalic(true);
            mUsbThermalPrinter.addString(details.getData().getStore().getAddress().toString().toUpperCase());
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(2);

            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
            mUsbThermalPrinter.setTextSize(20);
            mUsbThermalPrinter.setGray(4);
            mUsbThermalPrinter.setBold(false);
            // mUsbThermalPrinter.setItalic(true);
            mUsbThermalPrinter.printColumnsString(new String[]{"VAT NOÃ¯Â¼Å¡",details.getData().getVat_no(),}, new int[]{3,6}, new int[]{0,1}, 20);
            mUsbThermalPrinter.printColumnsString(new String[]{"TPIN NOÃ¯Â¼Å¡",details.getData().getTpin_no(),}, new int[]{3,6}, new int[]{0,1}, 20);

            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.printColumnsString(new String[]{"DATEÃ¯Â¼Å¡",DateTimeFormatting.Companion.formatSaleReturndate(details.getData().getPurchase_date_time(),zone),}, new int[]{3,6}, new int[]{0,1}, 22);

            mUsbThermalPrinter.walkPaper(2);
            mUsbThermalPrinter.printColumnsString(new String[]{"RECEIPT NOÃ¯Â¼Å¡",details.getData().getInvoice_id(),}, new int[]{5,5}, new int[]{0,1}, 20);
            mUsbThermalPrinter.printColumnsString(new String[]{"BUYERÃ¢â‚¬â„¢S NAMEÃ¯Â¼Å¡",details.getData().getCustomer_name(),}, new int[]{5,5}, new int[]{0,1}, 20);
            mUsbThermalPrinter.printColumnsString(new String[]{"BUYERÃ¢â‚¬â„¢S TPINÃ¯Â¼Å¡",details.getData().getBuyers_tpin(),}, new int[]{5,5}, new int[]{0,1}, 20);
            mUsbThermalPrinter.walkPaper(2);


            mUsbThermalPrinter.addString("----------------------------");
            mUsbThermalPrinter.printString();

            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.reset();
            //mUsbThermalPrinter.set(1);

            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            // mUsbThermalPrinter.setTextSize(20);
            mUsbThermalPrinter.setGray(4);
            mUsbThermalPrinter.setBold(false);
            for (SalesItem item : details.getData().getSalesItem()){
                String[] colsTestArr = {item.getProduct_name()+"("+item.getDistribution_pack().getProduct_description()+")"
                        , " "+FunUtils.INSTANCE.DtoString(item.getQuantity()) +" X "+ FunUtils.INSTANCE.formatPrintPrice(item.getRetail_price())," "+ FunUtils.INSTANCE.formatPrintPrice(item.getTotal_amount())};
                int[] colsWidthArr = {5, 4, 3};
                int[] colsAlign = {0, 1, 2}; // Example alignment values
                int colsTextSize = 18;

                mUsbThermalPrinter.addColumnsString(colsTestArr, colsWidthArr, colsAlign, colsTextSize);
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.walkPaper(1);
            }


            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
            mUsbThermalPrinter.setTextSize(20);
            mUsbThermalPrinter.setGray(4);
            mUsbThermalPrinter.setBold(false);
            //mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.addString("----------------------------");
            mUsbThermalPrinter.printString();

            mUsbThermalPrinter.walkPaper(1);


            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
            mUsbThermalPrinter.setTextSize(20);
            mUsbThermalPrinter.setGray(4);
            mUsbThermalPrinter.setBold(true);
            // mUsbThermalPrinter.setItalic(true);
            mUsbThermalPrinter.printColumnsString(new String[]{"TOTAL ("+currency+") Ã¯Â¼Å¡",FunUtils.INSTANCE.formatPrintPrice(details.getData().getSub_total()),}, new int[]{5,5}, new int[]{0,1}, 20);

            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
            mUsbThermalPrinter.setTextSize(20);
            mUsbThermalPrinter.setGray(4);
            mUsbThermalPrinter.setBold(false);
            //mUsbThermalPrinter.walkPaper(1);
            mUsbThermalPrinter.printColumnsString(new String[]{"ItemsÃ¯Â¼Å¡",Integer.toString(details.getData().getSalesItem().size())}, new int[]{5,5}, new int[]{0,1}, 20);


            mUsbThermalPrinter.addString("----------------------------");
            mUsbThermalPrinter.printString();

            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.printColumnsString(new String[]{"TAX EXÃ¯Â¼Å¡",FunUtils.INSTANCE.formatPrintPrice(details.getData().getTax_ex())}, new int[]{5,5}, new int[]{0,1}, 20);
            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.printColumnsString(new String[]{"TAX VAT@"+details.getData().getTax()+"% ",FunUtils.INSTANCE.formatPrintPrice(details.getData().getTax_amount())}, new int[]{5,5}, new int[]{0,1}, 20);
            mUsbThermalPrinter.addString("----------------------------");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);


            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
            mUsbThermalPrinter.setTextSize(20);
            mUsbThermalPrinter.setGray(4);
            mUsbThermalPrinter.setBold(true);
            // mUsbThermalPrinter.setItalic(true);
            mUsbThermalPrinter.printColumnsString(new String[]{"TOTAL VATÃ¯Â¼Å¡",FunUtils.INSTANCE.formatPrintPrice(details.getData().getTax_amount()),}, new int[]{5,5}, new int[]{0,1}, 20);

            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
            mUsbThermalPrinter.setTextSize(20);
            mUsbThermalPrinter.setGray(4);
            mUsbThermalPrinter.setBold(false);
            //mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.addString("----------------------------");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
            mUsbThermalPrinter.setTextSize(20);
            mUsbThermalPrinter.setGray(4);
            mUsbThermalPrinter.setBold(true);
            // mUsbThermalPrinter.setItalic(true);
            mUsbThermalPrinter.printColumnsString(new String[]{"PAYABLEÃ¯Â¼Å¡",FunUtils.INSTANCE.formatPrintPrice(details.getData().getGrand_total()),}, new int[]{5,5}, new int[]{0,1}, 20);

            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
            mUsbThermalPrinter.setTextSize(20);
            mUsbThermalPrinter.setGray(4);
            mUsbThermalPrinter.setBold(false);
            //mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.addString("----------------------------");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.printColumnsString(new String[]{"CASHÃ¯Â¼Å¡",details.getData().getPayment_type().trim().toUpperCase().equals("CASH") ?
                    FunUtils.INSTANCE.formatPrintPrice(details.getData().getGrand_total()) : "XXX"}, new int[]{5,5}, new int[]{0,1}, 20);
            mUsbThermalPrinter.printColumnsString(new String[]{"CARD/M-MONEYÃ¯Â¼Å¡",
                    details.getData().getPayment_type().trim().toUpperCase().equals("CARD") ||
                            details.getData().getPayment_type().trim().toUpperCase().equals("M-MONEY") ?
                            FunUtils.INSTANCE.formatPrintPrice(details.getData().getGrand_total()) : "xxx"}, new int[]{5,5}, new int[]{0,1}, 20);


            mUsbThermalPrinter.addString("----------------------------");
            mUsbThermalPrinter.printString();

            mUsbThermalPrinter.printColumnsString(new String[]{"EJ NO:",details.getData().getEj_no(),}, new int[]{5,5}, new int[]{0,1}, 20);
            mUsbThermalPrinter.printColumnsString(new String[]{"EJ ACTIVATION DATE:",details.getData().getEj_activation_date(),}, new int[]{5,5}, new int[]{0,1}, 20);
            mUsbThermalPrinter.printColumnsString(new String[]{"SDC ID:",details.getData().getTax_sdc_idamount(),}, new int[]{5,5}, new int[]{0,1}, 20);
            mUsbThermalPrinter.printColumnsString(new String[]{"RECEIPT NO:",details.getData().getReceipt_no(),}, new int[]{5,5}, new int[]{0,1}, 20);
            mUsbThermalPrinter.printColumnsString(new String[]{"INTERNAL DATA:",details.getData().getInternal_data(),}, new int[]{5,5}, new int[]{0,1}, 20);
            mUsbThermalPrinter.walkPaper(2);

            mUsbThermalPrinter.printColumnsString(new String[]{"Receipt Sign:",details.getData().getReceipt_sign(),}, new int[]{5,5}, new int[]{0,1}, 20);


            mUsbThermalPrinter.addString("----------------------------");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(2);


            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setLeftIndent(1);
            mUsbThermalPrinter.setLineSpace(3);
            mUsbThermalPrinter.setTextSize(20);
            mUsbThermalPrinter.setGray(3);
            mUsbThermalPrinter.setBold(false);
            // mUsbThermalPrinter.setItalic(true);
            mUsbThermalPrinter.addString("*** END OF LEGEAL RECEIPT ***");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(15);

            mUsbThermalPrinter.reset();


            // Toast.makeText(context, "Printing initiated", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.toString());

            Result = e.toString();
            if (Result.contains("NoPaperException")) {
                nopaper = true;
            } else if (Result.contains("OverHeatException")) {
                handler.sendMessage(handler.obtainMessage(OVERHEAT, 1, 0, null));
            } else {
                handler.sendMessage(handler.obtainMessage(PRINTERR, 1, 0, null));
            }
        }finally {
            handler.sendMessage(handler.obtainMessage(CANCELPROMPT, 1, 0, null));
            if (nopaper) {
                handler.sendMessage(handler.obtainMessage(NOPAPER, 1, 0, null));
                nopaper = false;
                return;
            }
        }

    }


//    private class contentPrintThread extends Thread {
//        public void run() {
//            super.run();
//            try {
//
////                val top: String,
////                        val storeName: String,
////                        val storeAddress: String,
////                        val vatInfo: String,
////                        val dateTime: String,
////                        val receiptInfo: String,
////                        val itemInfo:String,
////                        val totalPrice: String,
////                        val itemCount: String,
////                        val taxInfo: String,
////                        val totalVat: String,
////                        val payableAmount: String,
////                        val paymentModeInfo: String,
////                        val ejInfo: String,
////                        val bottom: String
//
//
//                ReceiptData receipt = receipt_data;
////
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
////                mUsbThermalPrinter.setLeftIndent(1);
////                mUsbThermalPrinter.setLineSpace(3);
////                mUsbThermalPrinter.setTextSize(20);
////                mUsbThermalPrinter.setGray(3);
////                mUsbThermalPrinter.setBold(false);
////               // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getTop());
////                mUsbThermalPrinter.printString();
////               mUsbThermalPrinter.walkPaper(1);
////
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
////                mUsbThermalPrinter.setTextSize(22);
////                mUsbThermalPrinter.setGray(4);
////                mUsbThermalPrinter.setBold(true);
////                // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getStoreName());
////                mUsbThermalPrinter.printString();
////                mUsbThermalPrinter.walkPaper(1);
////
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
////                mUsbThermalPrinter.setTextSize(20);
////                mUsbThermalPrinter.setGray(4);
////                mUsbThermalPrinter.setBold(false);
////                // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getStoreAddress());
////                mUsbThermalPrinter.printString();
////                mUsbThermalPrinter.walkPaper(1);
////
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
////                mUsbThermalPrinter.setTextSize(20);
////                mUsbThermalPrinter.setGray(4);
////                mUsbThermalPrinter.setBold(false);
////                // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getVatInfo());
////                mUsbThermalPrinter.printString();
////                mUsbThermalPrinter.walkPaper(1);
////
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
////                mUsbThermalPrinter.setTextSize(20);
////                mUsbThermalPrinter.setGray(4);
////                mUsbThermalPrinter.setBold(false);
////                // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getDateTime());
////                mUsbThermalPrinter.printString();
////                mUsbThermalPrinter.walkPaper(1);
////
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
////                mUsbThermalPrinter.setTextSize(20);
////                mUsbThermalPrinter.setGray(4);
////                mUsbThermalPrinter.setBold(false);
////                // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getReceiptInfo());
////                mUsbThermalPrinter.printString();
////                mUsbThermalPrinter.walkPaper(1);
////
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.walkPaper(1);
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
////                mUsbThermalPrinter.setTextSize(20);
////                mUsbThermalPrinter.setGray(4);
////                mUsbThermalPrinter.setBold(false);
////                // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getItemInfo());
////                mUsbThermalPrinter.setMonoSpace(true);
////                mUsbThermalPrinter.printString();
////                mUsbThermalPrinter.walkPaper(1);
////
////
////
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
////                mUsbThermalPrinter.setTextSize(22);
////                mUsbThermalPrinter.setGray(4);
////                mUsbThermalPrinter.setBold(true);
////                // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getTotalPrice());
////                mUsbThermalPrinter.printString();
////                mUsbThermalPrinter.walkPaper(2);
////
////
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
////                mUsbThermalPrinter.setTextSize(20);
////                mUsbThermalPrinter.setGray(4);
////                mUsbThermalPrinter.setBold(false);
////                // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getItemCount());
////                mUsbThermalPrinter.printString();
////                mUsbThermalPrinter.walkPaper(1);
////
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
////                mUsbThermalPrinter.setTextSize(20);
////                mUsbThermalPrinter.setGray(4);
////                mUsbThermalPrinter.setBold(false);
////                // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getTaxInfo());
////                mUsbThermalPrinter.printString();
////                mUsbThermalPrinter.walkPaper(1);
////
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
////                mUsbThermalPrinter.setTextSize(22);
////                mUsbThermalPrinter.setGray(4);
////                mUsbThermalPrinter.setBold(true);
////                // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getTotalVat());
////                mUsbThermalPrinter.printString();
////                mUsbThermalPrinter.walkPaper(1);
////
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
////                mUsbThermalPrinter.setTextSize(22);
////                mUsbThermalPrinter.setGray(4);
////                mUsbThermalPrinter.setBold(true);
////                // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getPayableAmount());
////                mUsbThermalPrinter.printString();
////                mUsbThermalPrinter.walkPaper(1);
////
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
////                mUsbThermalPrinter.setTextSize(20);
////                mUsbThermalPrinter.setGray(4);
////                mUsbThermalPrinter.setBold(false);
////                // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getPaymentModeInfo());
////                mUsbThermalPrinter.printString();
////                mUsbThermalPrinter.walkPaper(1);
//
//
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
////                mUsbThermalPrinter.setTextSize(20);
////                mUsbThermalPrinter.setGray(4);
////                mUsbThermalPrinter.setBold(false);
////                // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getEjInfo());
////                mUsbThermalPrinter.printString();
////                mUsbThermalPrinter.walkPaper(1);
//
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
////                mUsbThermalPrinter.setTextSize(20);
////                mUsbThermalPrinter.setGray(4);
////                mUsbThermalPrinter.setBold(false);
////                // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getBottom());
////                mUsbThermalPrinter.printString();
////                mUsbThermalPrinter.walkPaper(1);
//
//
//                mUsbThermalPrinter.reset();
//                //mUsbThermalPrinter.set(1);
//
//                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
//               // mUsbThermalPrinter.setTextSize(20);
//                mUsbThermalPrinter.setGray(4);
//                mUsbThermalPrinter.setBold(false);
//
//                // mUsbThermalPrinter.setItalic(true);
//
////                for  ( SalesItem item : receipt.getProduct_item()  ){
////
////                    String[] colsTestArr = {item.getProduct_name()+"-"+item.getDistribution_pack().getProduct_description()
////                            , item.getQuantity() +" X "+item.getRetail_price(), item.getTotal_amount()+"0000"};
////                    int[] colsWidthArr = {100, 50, 50};
////                    int[] colsAlign = {0, 1, 2}; // Example alignment values
////                    int colsTextSize = 20;
////
////                     mUsbThermalPrinter.addColumnsString(colsTestArr, colsWidthArr, colsAlign, colsTextSize);
////
////                     mUsbThermalPrinter.printString();
////                     mUsbThermalPrinter.walkPaper(1);
////
////
////                }
//
//
////                   mUsbThermalPrinter.printColumnsString(new String[]{"Item 1","$2000.00",}, new int[]{6,2}, new int[]{0,0}, 20);
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setBold(true);
////                mUsbThermalPrinter.setGray(4);
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
////
////                mUsbThermalPrinter.printColumnsString(new String[]{"Item 1123456","$200023.00",}, new int[]{6,3}, new int[]{0,0}, 22);
//
//                mUsbThermalPrinter.reset();
//                mUsbThermalPrinter.setBold(true);
//                mUsbThermalPrinter.setGray(4);
//                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
//
//                mUsbThermalPrinter.setUnderline(false);
//                mUsbThermalPrinter.setAlgin(1);
//                mUsbThermalPrinter.printStringAndWalk(0,0,2);
//
//
//
//                mUsbThermalPrinter.printColumnsString(new String[]{"COLUMN Aaaaaaa","Culoaaa B","20000aaaa"}, new int[]{6,3,3}, new int[]{0,1,2}, 22);
//
//
//
//                mUsbThermalPrinter.walkPaper(1);
//
//                mUsbThermalPrinter.autoBreakSet(true);
//                mUsbThermalPrinter.addString("----------------------------");
//                mUsbThermalPrinter.printString();
//
//
////                String[] colsTestArr = {"Column1", "Column2", "Column3"};
//////                int[] colsWidthArr = {100, 200, 150};
//////                int[] colsAlign = {1, 2, 3}; // Example alignment values
//////                int colsTextSize = 12;
//
//                // Calling the method
//               // mUsbThermalPrinter.addColumnsString(colsTestArr, colsWidthArr, colsAlign, colsTextSize);
//               // mUsbThermalPrinter.printColumnsString(colsTestArr, colsWidthArr, colsAlign, colsTextSize);
//              //  mUsbThermalPrinter.addColumnsString(new String[]{"AAA","BBB","CCC"}, new int[]{6,6,6}, new int[]{0,0,0}, 16);
//
//               // mUsbThermalPrinter.printString();
//                mUsbThermalPrinter.walkPaper(1);
//
//
//
//                mUsbThermalPrinter.walkPaper(1);
//                mUsbThermalPrinter.reset();
//
//
//                Toast.makeText(context, "Printing initiated", Toast.LENGTH_LONG).show();
//
//            } catch (Exception e) {
//                e.printStackTrace();
//                System.out.println(e.toString());
//
//                Result = e.toString();
//                if (Result.contains("NoPaperException")) {
//                    nopaper = true;
//                } else if (Result.contains("OverHeatException")) {
//                    handler.sendMessage(handler.obtainMessage(OVERHEAT, 1, 0, null));
//                } else {
//                    handler.sendMessage(handler.obtainMessage(PRINTERR, 1, 0, null));
//                }
//            }finally {
//                handler.sendMessage(handler.obtainMessage(CANCELPROMPT, 1, 0, null));
//                if (nopaper) {
//                    handler.sendMessage(handler.obtainMessage(NOPAPER, 1, 0, null));
//                    nopaper = false;
//                    return;
//                }
//            }
//        }
//    }

    private String generateReceipt(ArrayList<MyItem> items) {
        StringBuilder printContent = new StringBuilder();
        printContent.append("\n             RetailOne\n")
                .append("---------------------------\n")
                .append("DateÃ¯Â¼Å¡2015-01-01 16:18:20\n")
                .append("invoiceÃ¯Â¼Å¡12378945664\n")
                .append("idÃ¯Â¼Å¡1001000000000529142\n")
                .append("---------------------------\n")
                .append("    item        quantity   Price  total\n");

        double total = 0;
        for (SalesItem item : posSalesDetails.getData().getSalesItem()) {
           // double itemTotal = item.getQuantity() * item.getPrice();
            printContent.append(String.format("%-14s %8d %10.2f %10.2f\n", formatItemName(item.getProduct_name()), item.getQuantity(), item.getRetail_price(), item.getTotal_amount()));
            //total += itemTotal;
        }

        printContent.append("----------------------------\n")
                .append(String.format(" taxÃ¯Â¼Å¡%10.2f\n", 1000.00))
                .append("----------------------------\n")
                .append(String.format("paidÃ¯Â¼Å¡%10.2f\n", 10000.00))
                .append(String.format("tenderÃ¯Â¼Å¡%10.2f\n", 1000.00))
                .append(String.format("paidÃ¯Â¼Å¡%10.2f\n", 9000.00))
                .append("----------------------------\n")
                .append(" Thanks for shopping with us\n")
                .append("tel :1111111111\n");

        return printContent.toString();
    }

    private static String formatItemName(String name) {
        if (name.length() > 14) {
            return name.substring(0, 14);
        } else {
            return String.format("%-14s", name);
        }
    }

    public void registerBatteryReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction("android.intent.action.BATTERY_CAPACITY_EVENT");
        context.registerReceiver(printReceive, filter);
    }

    public void unregisterBatteryReceiver() {
        context.unregisterReceiver(printReceive);
    }

    private final BroadcastReceiver printReceive = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_NOT_CHARGING);
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 0);

                if (deviceType == StringUtil.DeviceModelEnum.TPS390.ordinal()) {
                    LowBattery = level * 5 <= scale;
                } else if (SystemUtil.getInternalModel().equals("M8")) {
                    LowBattery = level * 10 <= scale;
                } else {
                    LowBattery = status != BatteryManager.BATTERY_STATUS_CHARGING && level * 5 <= scale;
                }
            } else if (action.equals("android.intent.action.BATTERY_CAPACITY_EVENT")) {
                int status = intent.getIntExtra("action", 0);
                int level = intent.getIntExtra("level", 0);
                LowBattery = status == 0 && level < 1;
            }
        }
    };
}

*/