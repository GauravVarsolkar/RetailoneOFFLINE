package com.retailone.pos.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by pengxiaolong on 2017/10/31.
 * <p>
 * 二维码生产工具类
 */

public class CodeUtil {

  /**
   * 生成二维码Bitmap
   *
   * @param context
   * @param logoBm  二维码中心的Logo图标（可以为null）
   * @return 合成后的bitmap
   */
  public static Bitmap createQRImage(Context context, String data, Bitmap logoBm) {

    try {

      if(data == null || "".equals(data)) {
        return null;
      }

      int widthPix = ((Activity) context).getWindowManager().getDefaultDisplay()
        .getWidth();
      widthPix = widthPix / 5 * 3;
      int heightPix = widthPix;

      //配置参数
      Map<EncodeHintType, Object> hints = new HashMap<EncodeHintType, Object>();
      hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
      //容错级别
      hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
      //设置空白边距的宽度
      hints.put(EncodeHintType.MARGIN, 2); //default is 4

      // 图像数据转换，使用了矩阵转换
      BitMatrix bitMatrix = new QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, widthPix, heightPix, hints);
      int[] pixels = new int[widthPix * heightPix];
      // 下面这里按照二维码的算法，逐个生成二维码的图片，
      // 两个for循环是图片横列扫描的结果
      for (int y = 0; y < heightPix; y++) {
        for (int x = 0; x < widthPix; x++) {
          if(bitMatrix.get(x, y)) {
            pixels[y * widthPix + x] = 0xff000000;
          }
          else {
            pixels[y * widthPix + x] = 0xffffffff;
          }
        }
      }

      // 生成二维码图片的格式，使用ARGB_8888
      Bitmap bitmap = Bitmap.createBitmap(widthPix, heightPix, Bitmap.Config.ARGB_8888);
      bitmap.setPixels(pixels, 0, widthPix, 0, 0, widthPix, heightPix);

      if(logoBm != null) {
        bitmap = addLogo(bitmap, logoBm);
      }

      return bitmap;
      //必须使用compress方法将bitmap保存到文件中再进行读取。直接返回的bitmap是没有任何压缩的，内存消耗巨大！
      //return bitmap != null && bitmap.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(filePath));
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }

  /**
   * 在二维码中间添加Logo图案
   */
  private static Bitmap addLogo(Bitmap src, Bitmap logo) {
    if(src == null) {
      return null;
    }

    if(logo == null) {
      return src;
    }

    //获取图片的宽高
    int srcWidth = src.getWidth();
    int srcHeight = src.getHeight();
    int logoWidth = logo.getWidth();
    int logoHeight = logo.getHeight();

    if(srcWidth == 0 || srcHeight == 0) {
      return null;
    }

    if(logoWidth == 0 || logoHeight == 0) {
      return src;
    }

    //logo大小为二维码整体大小的1/6
    float scaleFactor = srcWidth * 1.0f / 6 / logoWidth;
    Bitmap bitmap = Bitmap.createBitmap(srcWidth, srcHeight, Bitmap.Config.ARGB_8888);
    try {
      Canvas canvas = new Canvas(bitmap);
      canvas.drawBitmap(src, 0, 0, null);
      canvas.scale(scaleFactor, scaleFactor, srcWidth / 2, srcHeight / 2);
      canvas.drawBitmap(logo, (srcWidth - logoWidth) / 2, (srcHeight - logoHeight) / 2, null);

//      canvas.save(Canvas.ALL_SAVE_FLAG);
      canvas.save();
      canvas.restore();
    }
    catch (Exception e) {
      bitmap = null;
      e.getStackTrace();
    }

    return bitmap;
  }

  /**
   * 生成二维码
   *
   * @param data
   * @param logoBm
   * @return
   */
  public static Bitmap createQRCode(String data, int width, Bitmap logoBm) {

    try {

      if(data == null || "".equals(data)) {
        return null;
      }

      //配置参数
      Map<EncodeHintType, Object> hints = new HashMap<EncodeHintType, Object>();
      hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
      //容错级别
      hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
      //设置空白边距的宽度
      hints.put(EncodeHintType.MARGIN, 2); //default is 4

      // 图像数据转换，使用了矩阵转换
      BitMatrix bitMatrix = new QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, width, width, hints);
      int[] pixels = new int[width * width];
      // 下面这里按照二维码的算法，逐个生成二维码的图片，
      // 两个for循环是图片横列扫描的结果
      for (int y = 0; y < width; y++) {
        for (int x = 0; x < width; x++) {
          if(bitMatrix.get(x, y)) {
            pixels[y * width + x] = 0xff000000;
          }
          else {
            pixels[y * width + x] = 0xffffffff;
          }
        }
      }

      // 生成二维码图片的格式，使用ARGB_8888
      Bitmap bitmap = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888);
      bitmap.setPixels(pixels, 0, width, 0, 0, width, width);

      if(logoBm != null) {
        bitmap = addLogo(bitmap, logoBm);
      }

      return bitmap;
      //必须使用compress方法将bitmap保存到文件中再进行读取。直接返回的bitmap是没有任何压缩的，内存消耗巨大！
      //return bitmap != null && bitmap.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(filePath));
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }


  /**
   * 创建一维码
   *
   * @param content
   * @return
   * @throws WriterException
   */
  public static Bitmap createOneDCode(String content, int w, int h) {
    Bitmap bitmap = null;
    try {
      // 生成一维条码,编码时指定大小,不要生成了图片以后再进行缩放,这样会模糊导致识别失败
      BitMatrix matrix = new MultiFormatWriter().encode(content, BarcodeFormat.CODE_128, w, h);
      int width = matrix.getWidth();
      int height = matrix.getHeight();
      int[] pixels = new int[width * height];
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          if(matrix.get(x, y)) {
            pixels[y * width + x] = 0xff000000;
          }
          else {
            pixels[y * width + x] = 0xffffffff;
          }
        }
      }
      bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
      // 通过像素数组生成bitmap,具体参考api
      bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
    }
    catch (WriterException e) {
      e.printStackTrace();
    }
    return bitmap;
  }
}
