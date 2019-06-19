package com.techshino.facespoof;

import android.content.Context;
import android.os.Environment;

import com.techshino.utils.Logs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Created by wangzhi on 2017/8/1.
 */
public class Algorithm {

  private static final String TAG = Algorithm.class.getSimpleName();

  private String VERSION_CODE = "version_code";
  private String VERSION = "version";
  private String DAT_NAME = "DualFace.dat";

  private Context mContext;
  private static Algorithm sInstance = null;

  private boolean isOk = false;

  private static boolean isLoaded;

  public boolean isOk() {
    return isOk;
  }

  public static Algorithm getInstance(Context context) {
    if (sInstance == null)
      sInstance = new Algorithm(context);
    return sInstance;
  }

  public Algorithm() {

  }

  public Algorithm(Context context) {
    mContext = context;
    init();
  }

  static /* 加载人脸算法库 */ {
    if (!isLoaded) {
      System.loadLibrary("FaceDual");
      isLoaded = true;
    }
  }

  /**
   * 算法初始化： 输入model路径 返回值： 0成功， >0为失败
   */
  public native int DoInit(String datPath, Context context);

  /**
   * 算法卸载 返回值： 0成功， >0为失败
   */
  public native int DoUnInit();

  /**
   * 利用近红外镜头检活 返回值： 0成功， >0为失败
   */
  public native int nir(byte[] rgb24, int colorFlag, int width, int height, int[] faceRect,
                        float[] score, double[] feature);

  /**
   * 利用可见光镜头检活，简化版的算法（只进行颜色判断），安全性低些 返回值： 0成功， >0为失败
   */
  public native int colorSimple(byte[] rgb24, int width, int height, int[] faceRect,
                                int[] isLive, double[] feature);

  /**
   * 利用可见光镜头检活，正常的算法，安全性高 返回值： 0成功， >0为失败
   */
  public native int colorNormal(byte[] rgb24, int width, int height, int[] faceRect,
                                float[] score, double[] feature);

  /**
   * rgb to jpg
   *
   * @param jpgB
   * @param rgb24
   * @param width
   * @param height
   * @param quality
   * @return
   */
  public native int Rgb2Jpg(byte[] jpgB, byte[] rgb24, int width, int height,
                            int quality);

  /**
   * yuv420p转RGB24
   *
   * @param rgb24
   * @param yuv
   * @param width
   * @param height
   * @return
   */
  public native int RgbFromYuv420SP(byte[] rgb24, byte[] yuv, int width,
                                    int height);

  public void init() {
    if (mContext == null)
      return;
    String sdmdat = mContext.getFilesDir().getAbsolutePath() + "/"
        + DAT_NAME;
    File file = new File(sdmdat);

    try {
      if (!file.exists()) {
        InputStream is = mContext.getResources().getAssets()
            .open(DAT_NAME);
        FileOutputStream fos = new FileOutputStream(sdmdat);
        byte[] buffer = new byte[1024];
        int count;
        while ((count = is.read(buffer)) > 0) {
          fos.write(buffer, 0, count);
        }
        fos.flush();
        fos.close();
        is.close();
      }
      int loadresult = DoInit(sdmdat, mContext);
      Logs.e(TAG, "加载算法数据库完成:" + loadresult);
      Logs.e(TAG, "dat路径：" + file.exists() + " " + sdmdat + " "
          + Environment.getExternalStorageDirectory());
      if (loadresult == 0) {
        isOk = true;
      } else {
        isOk = false;
      }
    } catch (Exception e) {
      Logs.e(TAG, "加载算法数据库失败，程序异常");
      e.printStackTrace();
      isOk = false;
    }
  }
}
