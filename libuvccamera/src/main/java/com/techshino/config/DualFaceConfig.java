package com.techshino.config;

import com.techshino.utils.Logs;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;

import static android.content.ContentValues.TAG;

/**
 * 双模人脸检活配置
 * <p>
 * Created by wangzhi on 2017/8/2.
 */
public class DualFaceConfig implements Serializable {

  private int mWidth = 640;
  private int mHeight = 480;
  private int mImgCompress = 80; // 图像压缩率（1-100）
  private int mNirCount = 1; // 检活通过次数
  private int isActived = 2; // 检活类型
  private long mTimeout = 30000; // 检活超时ms
  private float mThreshold = 0.7f; // 检活默认阈值
  private int mPidL = 514;
  private int mPidR = 560;

  private static String sMessage;

  public static String getMessage() {
    return sMessage;
  }

  public int getWidth() {
    return mWidth;
  }

  public void setWidth(int width) {
    mWidth = width;
  }

  public int getHeight() {
    return mHeight;
  }

  public void setHeight(int height) {
    mHeight = height;
  }

  public int getImgCompress() {
    return mImgCompress;
  }

  public void setImgCompress(int imgCompress) {
    mImgCompress = imgCompress;
  }

  public int getNirCount() {
    return mNirCount;
  }

  public void setNirCount(int nirCount) {
    mNirCount = nirCount;
  }

  public int getIsActived() {
    return isActived;
  }

  public void setIsActived(int isActived) {
    this.isActived = isActived;
  }

  public long getTimeout() {
    return mTimeout;
  }

  public void setTimeout(long timeout) {
    mTimeout = timeout;
  }

  public float getThreshold() {
    return mThreshold;
  }

  public void setThreshold(float threshold) {
    mThreshold = threshold;
  }

  public int getPidL() {
    return mPidL;
  }

  public void setPidL(int pidL) {
    mPidL = pidL;
  }

  public int getPidR() {
    return mPidR;
  }

  public void setPidR(int pidR) {
    mPidR = pidR;
  }

  public static void setMessage(String message) {
    sMessage = message;
  }

  @Override
  public String toString() {
    return "DualFaceConfig{" +
        "mWidth=" + mWidth +
        ", mHeight=" + mHeight +
        ", mImgCompress=" + mImgCompress +
        ", mNirCount=" + mNirCount +
        ", isActived=" + isActived +
        ", mTimeout=" + mTimeout +
        ", mThreshold=" + mThreshold +
        ", mPidL=" + mPidL +
        ", mPidR=" + mPidR +
        '}';
  }

  public boolean parseXML(String param) {
    try {
      XmlPullParserFactory pullParserFactory = XmlPullParserFactory
          .newInstance();
      XmlPullParser xmlPullParser = pullParserFactory.newPullParser();
      StringReader reader = new StringReader(param);
      xmlPullParser.setInput(reader);
      int eventType = xmlPullParser.getEventType();
      while (eventType != XmlPullParser.END_DOCUMENT) {
        String nodeName = xmlPullParser.getName();
        switch (eventType) {
          case XmlPullParser.START_TAG:
            if (!checkParams(xmlPullParser, nodeName)) {
              return false;
            }
            break;
          default:
            break;
        }
        eventType = xmlPullParser.next();
      }
    } catch (XmlPullParserException e) {
      sMessage = "解析异常XmlPullParserException，请检查是否有非法参数或是参数越界";
      Logs.e(TAG, e.getMessage());
      return false;
    } catch (Exception e) {
      sMessage = "参数错误，请检查是否有非法参数或是参数越界";
      Logs.e(TAG, e.getMessage());
      return false;
    }
    return true;
  }

  private boolean checkParams(XmlPullParser xmlPullParser, String nodeName)
      throws XmlPullParserException, IOException {
    try {
      String text = null;
      if ("imgWidth".equals(nodeName)) {
        int imgWidth = Integer.valueOf(xmlPullParser.nextText());
        text = "imgWidth";
        if (!(imgWidth >= 200)) {
          sMessage = text + "参数错误，请检查是否有非法参数或是参数越界";
          return false;
        }
        setWidth(imgWidth);
      }
      if ("imgHeight".equals(nodeName)) {
        int imgHeight = Integer.valueOf(xmlPullParser.nextText());
        text = "imgHeight";
        if (!(imgHeight >= 200)) {
          sMessage = text + "参数错误，请检查是否有非法参数或是参数越界";
          return false;
        }
        setHeight(imgHeight);
      }
      if ("imgCompress".equals(nodeName)) {
        int imgCompress = Integer.valueOf(xmlPullParser.nextText());
        text = "imgCompress";
        if (!(imgCompress >= 0 && imgCompress <= 100)) {
          sMessage = text + "参数错误，请检查是否有非法参数或是参数越界";
          return false;
        }
        setImgCompress(imgCompress);
      }
      if ("NirCount".equals(nodeName)) {
        int nirCount = Integer.valueOf(xmlPullParser.nextText());
        text = "NirCount";
        if (nirCount < 0) {
          sMessage = text + "参数错误，请检查是否有非法参数或是参数越界";
          return false;
        }
        setNirCount(nirCount);
      }
      if ("isActived".equals(nodeName)) {
        int isActived = Integer.valueOf(xmlPullParser.nextText());
        text = "isActived";
        if (!(isActived == 0 || isActived == 1 || isActived == 2 || isActived == 3)) {
          sMessage = text + "参数错误，请检查是否有非法参数或是参数越界";
          return false;
        }
        setIsActived(isActived);
      }
      if ("timeOut".equals(nodeName)) {
        int timeout = Integer.valueOf(xmlPullParser.nextText());
        text = "timeOut";
        if (!(timeout >= 10 && timeout <= 120)) {
          sMessage = text + "参数错误，请检查是否有非法参数或是参数越界(10-120)";
          return false;
        }
        setTimeout(timeout * 1000);
      }
      if ("liveThreshold".equals(nodeName)) {
        float liveThreshold = Float.valueOf(xmlPullParser.nextText());
        text = "liveThreshold";
        if (!(liveThreshold >= 0 && liveThreshold <= 1)) {
          sMessage = text + "参数错误，请检查是否有非法参数或是参数越界(0-1)";
          return false;
        }
        setThreshold(liveThreshold);
      }
      if ("pidL".equals(nodeName)) {
        int pidL = Integer.parseInt(xmlPullParser.nextText(), 16);
        text = "pidL";
        if (!(pidL == 0x2203
            || pidL == 0x2204
            || pidL == 0x2205
            || pidL == 0x2206
            || pidL == 0x2207
            || pidL == 0x2208
            || pidL == 0x2209
            || pidL == 0x2210)) {
          sMessage = text + "参数错误，请检查pid是否合法";
          return false;
        }
        setPidL(pidL);
      }
      if ("pidR".equals(nodeName)) {
        int pidR = Integer.parseInt(xmlPullParser.nextText(), 16);
        text = "pidR";
        if (!(pidR == 0x2203
            || pidR == 0x2204
            || pidR == 0x2205
            || pidR == 0x2206
            || pidR == 0x2207
            || pidR == 0x2208
            || pidR == 0x2209
            || pidR == 0x2210)) {
          sMessage = text + "参数错误，请检查pid是否合法";
          return false;
        }
        setPidR(pidR);
      }
    } catch (Exception e) {
      sMessage = "参数格式错误，请检查是否有非法参数或是参数越界";
      return false;
    }

    return true;
  }
}
