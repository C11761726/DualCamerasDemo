package com.techshino.utils;

import android.graphics.Bitmap;

/**
 * Created by wangzhi on 2017/8/1.
 */

public class BitmapUtil {

  public static Bitmap rgbByteToBitmap(byte[] rgb, int width, int height) {
    if (rgb == null)
      return null;
    int[] colors = convertByteToColor(rgb);
    if (colors == null)
      return null;
    Bitmap bmp = Bitmap.createBitmap(colors,width,height,Bitmap.Config.ARGB_8888);
    return bmp;
  }

  // 将一个byte数转成int
// 实现这个函数的目的是为了将byte数当成无符号的变量去转化成int
  public static int convertByteToInt(int data) {

    int heightBit = (int) ((data >> 4) & 0x0F);
    int lowBit = (int) (0x0F & data);
    return heightBit * 16 + lowBit;
  }


  // 将纯RGB数据数组转化成int像素数组
  public static int[] convertByteToColor(byte[] data) {
    int size = data.length;
    if (size == 0) {
      return null;
    }

    int arg = 0;
    if (size % 3 != 0) {
      arg = 1;
    }

    // 一般情况下data数组的长度应该是3的倍数，这里做个兼容，多余的RGB数据用黑色0XFF000000填充
    int[] color = new int[size / 3 + arg];
    int red, green, blue;

    if (arg == 0) {
      for (int i = 0; i < color.length; ++i) {
        red = convertByteToInt(data[i * 3]);
        green = convertByteToInt(data[i * 3 + 1]);
        blue = convertByteToInt(data[i * 3 + 2]);

        // 获取RGB分量值通过按位或生成int的像素值
        color[i] = (red << 16) | (green << 8) | blue | 0xFF000000;
      }
    } else {
      for (int i = 0; i < color.length - 1; ++i) {
        red = convertByteToInt(data[i * 3]);
        green = convertByteToInt(data[i * 3 + 1]);
        blue = convertByteToInt(data[i * 3 + 2]);
        color[i] = (red << 16) | (green << 8) | blue | 0xFF000000;
      }

      color[color.length - 1] = 0xFF000000;
    }

    return color;
  }
}
