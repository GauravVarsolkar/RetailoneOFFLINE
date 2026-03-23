package com.retailone.pos.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.text.TextUtils;

/**
 * @author 17093029
 * 名称： CommonPrintUtil
 * 创建日期： 2018/8/27
 * 包名： com.cnsuning.mobile.apos.module.hardware.core
 */
public class CommonPrintUtil {

  /**
   * 左对齐
   */
  public static final int DIRECTION_LEFT = 0;
  /**
   * 居中
   */
  public static final int DIRECTION_MIDDLE = 1;
  /**
   * 右对齐
   */
  public static final int DIRECTION_RIGHT = 2;
  
  private static final int CODE_SPACE = 40;
  private static final int CODE_HEIGHT = 300;

  /**
   * 获取字符串限制长度 区分中文
   *
   * @param value 字符串
   * @param limit 限制最大长度
   */
  public static int getLimitLength(String value, int limit) {
    int valueLength = 0;
    String chinese = "[\u4e00-\u9fa5|\u3002|\uff1f|\uff01|\uff0c|\u3001|\uff1b|\uff1a|\u201c|\u201d" +
      "|\u2018|\u2019|\uff08|\uff09|\u300a|\u300b|\u3008|\u3009|\u3010|\u3011|\u300e|\u300f|\u300c" +
      "|\u300d|\ufe43|\ufe44|\u3014|\u3015|\u2026|\u2014|\uff5e|\ufe4f|\uffe5]";
    for (int i = 0; i < value.length(); i++) {
      String temp = value.substring(i, i + 1);
      if (temp.matches(chinese)) {
        valueLength += 2;
      }
      else {
        valueLength += 1;
      }
      if (valueLength > limit) {
        return i;
      }
    }
    return value.length();
  }

  /**
   * 调整字符串长度
   */
  public static String adjustStringLength(String src, int limit, int direction) {
    StringBuilder formatBuilder = new StringBuilder();
    int len = StringsUtil.getStringLength(src);
    if (len == limit) {
      return src;
    } else if (len > limit) {
      int newLimit = getLimitLength(src, limit);
      formatBuilder.append(src.substring(0, newLimit));
      formatBuilder.append("\n");
      formatBuilder.append(src.substring(newLimit, src.length()));
      return formatBuilder.toString();
    }
    int over = limit - len;
    switch (direction) {
      case DIRECTION_LEFT:
        formatBuilder.append(src);
        for (int i = 0; i < over; ++i) {
          formatBuilder.append(" ");
        }
        break;
      case DIRECTION_RIGHT:
        for (int i = 0; i < over; ++i) {
          formatBuilder.append(" ");
        }
        formatBuilder.append(src);
        break;
      case DIRECTION_MIDDLE:
        int half = over / 2;
        int mod = over % 2;
        for (int i = 0; i < half; ++i) {
          formatBuilder.append(" ");
        }
        for (int i = 0; i < mod; ++i) {
          formatBuilder.append(" ");
        }
        formatBuilder.append(src);
        for (int i = 0; i < half; ++i) {
          formatBuilder.append(" ");
        }
        break;
    }
    return formatBuilder.toString();
  }

  /**
   * 获取二维码
   */
  public static Bitmap generateCode(String leftString, String rightString, int size) {

    Bitmap logo = null;
    Bitmap leftBitmap = null;
    Bitmap rightBitmap = null;
    Bitmap resultBitmap = null;
    try {
      // logo图标
//      logo = BitmapUtil.getBitmapFromAssets(AppUtil.getApp(), PATH_LOGO_CODE);
      // 生成左边的二维码
      if (!TextUtils.isEmpty(leftString)) {
        leftBitmap = CodeUtil.createQRCode(leftString, size, logo);
      }
      // 生成右边的二维码
      if (!TextUtils.isEmpty(rightString)) {
        rightBitmap = CodeUtil.createQRCode(rightString, size, logo);
      }
      // 合成两张图
      if (leftBitmap != null && rightBitmap != null) {
        resultBitmap = Bitmap.createBitmap(size + size + CODE_SPACE, CODE_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(resultBitmap);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(leftBitmap, 0, 0, null);
        canvas.drawBitmap(rightBitmap, leftBitmap.getWidth() + CODE_SPACE, 0, null);
      }
      else {
        resultBitmap = Bitmap.createBitmap(size + size + CODE_SPACE, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(resultBitmap);
        canvas.drawColor(Color.WHITE);
        if (leftBitmap != null || rightBitmap != null) {
          canvas.drawBitmap(leftBitmap == null ? rightBitmap : leftBitmap, (size + size + CODE_SPACE) / 2 -
            size / 2, 0, null);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return resultBitmap;
  }




}
