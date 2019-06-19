/*
 *  UVCCamera
 *  library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *  All files in the folder are under this Apache License, Version 2.0.
 *  Files in the libjpeg-turbo, libusb, libuvc, rapidjson folder
 *  may have a different license, see the respective files.
 */

package com.serenegiant.usbcameracommon;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.widget.CameraViewInterface;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

abstract class AbstractUVCCameraHandler extends Handler {
  private static final boolean DEBUG = true;  // TODO set false on release
  private static final String TAG = "AbsUVCCameraHandler";
  private static CameraThread sCameraThread;

  public interface CameraCallback {
    public void onOpen();

    public void onClose();

    public void onStartPreview();

    public void onStopPreview();

    public void onError(final Exception e);

    public void onFrame(byte[] data, int width, int height);
  }

  private static final int MSG_OPEN = 0;
  private static final int MSG_CLOSE = 1;
  private static final int MSG_PREVIEW_START = 2;
  private static final int MSG_PREVIEW_STOP = 3;
  private static final int MSG_RELEASE = 9;
  private static final int MSG_GET_FRAME = 10;

  private final WeakReference<CameraThread> mWeakThread;
  private volatile boolean mReleased;

  protected AbstractUVCCameraHandler(final CameraThread thread) {
    sCameraThread = thread;
    mWeakThread = new WeakReference<CameraThread>(thread);
  }

  public int getWidth() {
    final CameraThread thread = mWeakThread.get();
    return thread != null ? thread.getWidth() : 0;
  }

  public int getHeight() {
    final CameraThread thread = mWeakThread.get();
    return thread != null ? thread.getHeight() : 0;
  }

  public boolean isOpened() {
    final CameraThread thread = mWeakThread.get();
    return thread != null && thread.isCameraOpened();
  }

  public boolean isPreviewing() {
    final CameraThread thread = mWeakThread.get();
    return thread != null && thread.isPreviewing();
  }

  public boolean isRecording() {
    final CameraThread thread = mWeakThread.get();
    return thread != null && thread.isRecording();
  }

  public boolean isEqual(final UsbDevice device) {
    final CameraThread thread = mWeakThread.get();
    return (thread != null) && thread.isEqual(device);
  }

  protected boolean isCameraThread() {
    final CameraThread thread = mWeakThread.get();
    return thread != null && (thread.getId() == Thread.currentThread().getId());
  }

  protected boolean isReleased() {
    final CameraThread thread = mWeakThread.get();
    return mReleased || (thread == null);
  }

  protected void checkReleased() {
    if (isReleased()) {
      throw new IllegalStateException("already released");
    }
  }

  public void open(final USBMonitor.UsbControlBlock ctrlBlock) {
    checkReleased();
    sendMessage(obtainMessage(MSG_OPEN, ctrlBlock));
  }

  public void close() {
    if (DEBUG) Log.v(TAG, "close:");
    if (isOpened()) {
      stopPreview();
      sendEmptyMessage(MSG_CLOSE);
    }
    if (DEBUG) Log.v(TAG, "close:finished");
  }

  public void resize(final int width, final int height) {
    checkReleased();
    throw new UnsupportedOperationException("does not support now");
  }

  protected void startPreview(final Object surface) {
    checkReleased();
    if (!((surface instanceof SurfaceHolder) || (surface instanceof Surface) || (surface instanceof SurfaceTexture))) {
      throw new IllegalArgumentException("surface should be one of SurfaceHolder, Surface or SurfaceTexture");
    }
    sendMessage(obtainMessage(MSG_PREVIEW_START, surface));
  }

  public void stopPreview() {
    if (DEBUG) Log.v(TAG, "stopPreview:");
    removeMessages(MSG_PREVIEW_START);
    removeMessages(MSG_GET_FRAME);
    if (isPreviewing()) {
      final CameraThread thread = mWeakThread.get();
      if (thread == null) return;
      synchronized (thread.mSync) {
        sendEmptyMessage(MSG_PREVIEW_STOP);
        if (!isCameraThread()) {
          // wait for actually preview stopped to avoid releasing Surface/SurfaceTexture
          // while preview is still running.
          // therefore this method will take a time to execute
          try {
            thread.mSync.wait();
          } catch (final InterruptedException e) {
          }
        }
      }
    }
    if (DEBUG) Log.v(TAG, "stopPreview:finished");
  }

  public void requestOneFrame(long delay) {
    checkReleased();
    sendEmptyMessageDelayed(MSG_GET_FRAME, delay);
  }

  public void release() {
    mReleased = true;
    close();
    sendEmptyMessage(MSG_RELEASE);
    sCameraThread = null;
  }

  public void addCallback(final CameraCallback callback) {
    checkReleased();
    if (!mReleased && (callback != null)) {
      final CameraThread thread = mWeakThread.get();
      if (thread != null) {
        thread.mCallbacks.add(callback);
      }
    }
  }

  public void removeCallback(final CameraCallback callback) {
    if (callback != null) {
      final CameraThread thread = mWeakThread.get();
      if (thread != null) {
        thread.mCallbacks.remove(callback);
      }
    }
  }

  public boolean checkSupportFlag(final long flag) {
    checkReleased();
    final CameraThread thread = mWeakThread.get();
    return thread != null && thread.mUVCCamera != null && thread.mUVCCamera.checkSupportFlag(flag);
  }

  public int getValue(final int flag) {
    checkReleased();
    final CameraThread thread = mWeakThread.get();
    final UVCCamera camera = thread != null ? thread.mUVCCamera : null;
    if (camera != null) {
      if (flag == UVCCamera.PU_BRIGHTNESS) {
        return camera.getBrightness();
      } else if (flag == UVCCamera.PU_CONTRAST) {
        return camera.getContrast();
      }
    }
    throw new IllegalStateException();
  }

  public int setValue(final int flag, final int value) {
    checkReleased();
    final CameraThread thread = mWeakThread.get();
    final UVCCamera camera = thread != null ? thread.mUVCCamera : null;
    if (camera != null) {
      if (flag == UVCCamera.PU_BRIGHTNESS) {
        camera.setBrightness(value);
        return camera.getBrightness();
      } else if (flag == UVCCamera.PU_CONTRAST) {
        camera.setContrast(value);
        return camera.getContrast();
      }
    }
    throw new IllegalStateException();
  }

  public int resetValue(final int flag) {
    checkReleased();
    final CameraThread thread = mWeakThread.get();
    final UVCCamera camera = thread != null ? thread.mUVCCamera : null;
    if (camera != null) {
      if (flag == UVCCamera.PU_BRIGHTNESS) {
        camera.resetBrightness();
        return camera.getBrightness();
      } else if (flag == UVCCamera.PU_CONTRAST) {
        camera.resetContrast();
        return camera.getContrast();
      }
    }
    throw new IllegalStateException();
  }

  @Override
  public void handleMessage(final Message msg) {
    final CameraThread thread = mWeakThread.get();
    if (thread == null) return;
    switch (msg.what) {
      case MSG_OPEN:
        thread.handleOpen((USBMonitor.UsbControlBlock) msg.obj);
        break;
      case MSG_CLOSE:
        thread.handleClose();
        break;
      case MSG_PREVIEW_START:
        thread.handleStartPreview(msg.obj);
        break;
      case MSG_PREVIEW_STOP:
        thread.handleStopPreview();
        break;
      case MSG_RELEASE:
        thread.handleRelease();
        break;
      case MSG_GET_FRAME:
        thread.callOnFrame();
        break;
      default:
        throw new RuntimeException("unsupported message:what=" + msg.what);
    }
  }

  static final class CameraThread extends Thread {
    private static final String TAG_THREAD = "CameraThread";
    private final Object mSync = new Object();
    private final Class<? extends AbstractUVCCameraHandler> mHandlerClass;
    private final WeakReference<Activity> mWeakParent;
    private final WeakReference<CameraViewInterface> mWeakCameraView;
    private final Set<CameraCallback> mCallbacks = new CopyOnWriteArraySet<CameraCallback>();
    private int mWidth, mHeight, mPreviewMode;
    private float mBandwidthFactor;
    private boolean mIsPreviewing;
    //    private int mSoundId;
    private AbstractUVCCameraHandler mHandler;
    /**
     * for accessing UVC camera
     */
    private UVCCamera mUVCCamera;

    /**
     * @param clazz           Class extends AbstractUVCCameraHandler
     * @param parent          parent Activity
     * @param cameraView      for still capturing
     * @param encoderType     0: use MediaSurfaceEncoder, 1: use MediaVideoEncoder, 2: use MediaVideoBufferEncoder
     * @param width
     * @param height
     * @param format          either FRAME_FORMAT_YUYV(0) or FRAME_FORMAT_MJPEG(1)
     * @param bandwidthFactor
     */
    CameraThread(final Class<? extends AbstractUVCCameraHandler> clazz,
                 final Activity parent, final CameraViewInterface cameraView,
                 final int encoderType, final int width, final int height, final int format,
                 final float bandwidthFactor) {

      super("CameraThread");
      mHandlerClass = clazz;
      mWidth = width;
      mHeight = height;
      mPreviewMode = format;
      mBandwidthFactor = bandwidthFactor;
      mWeakParent = new WeakReference<Activity>(parent);
      mWeakCameraView = new WeakReference<CameraViewInterface>(cameraView);
    }

    private byte[] mYuvBytes;

    @Override
    protected void finalize() throws Throwable {
      Log.i(TAG, "CameraThread#finalize");
      super.finalize();
    }

    public AbstractUVCCameraHandler getHandler() {
      if (DEBUG) Log.v(TAG_THREAD, "getHandler:");
      synchronized (mSync) {
        if (mHandler == null)
          try {
            mSync.wait();
          } catch (final InterruptedException e) {
          }
      }
      return mHandler;
    }

    public int getWidth() {
      synchronized (mSync) {
        return mWidth;
      }
    }

    public int getHeight() {
      synchronized (mSync) {
        return mHeight;
      }
    }

    public boolean isCameraOpened() {
      synchronized (mSync) {
        return mUVCCamera != null;
      }
    }

    public boolean isPreviewing() {
      synchronized (mSync) {
        return mUVCCamera != null && mIsPreviewing;
      }
    }

    public boolean isRecording() {
      synchronized (mSync) {
        return (mUVCCamera != null);
      }
    }

    public boolean isEqual(final UsbDevice device) {
      return (mUVCCamera != null) && (mUVCCamera.getDevice() != null) && mUVCCamera.getDevice().equals(device);
    }

    public void handleOpen(final USBMonitor.UsbControlBlock ctrlBlock) {
      if (DEBUG) Log.v(TAG_THREAD, "handleOpen:");
      handleClose();
      try {
        final UVCCamera camera = new UVCCamera();
        camera.open(ctrlBlock);
        synchronized (mSync) {
          mUVCCamera = camera;
        }
        callOnOpen();
      } catch (final Exception e) {
        callOnError(e);
      }
      if (DEBUG)
        Log.i(TAG, "supportedSize:" + (mUVCCamera != null ? mUVCCamera.getSupportedSize() : null));
    }

    public void handleClose() {
      if (DEBUG) Log.v(TAG_THREAD, "handleClose:");
      handleStopRecording();
      final UVCCamera camera;
      synchronized (mSync) {
        camera = mUVCCamera;
        mUVCCamera = null;
      }
      if (camera != null) {
//        camera.stopPreview();
        camera.destroy();
        callOnClose();
      }
    }

    public void handleStartPreview(final Object surface) {
      if (DEBUG) Log.v(TAG_THREAD, "handleStartPreview:");
      if ((mUVCCamera == null) || mIsPreviewing) return;
      try {
        mUVCCamera.setPreviewSize(mWidth, mHeight, 1, 31, mPreviewMode, mBandwidthFactor);
      } catch (final IllegalArgumentException e) {
        try {
          // fallback to YUV mode
          mUVCCamera.setPreviewSize(mWidth, mHeight, 1, 31, UVCCamera.DEFAULT_PREVIEW_MODE, mBandwidthFactor);
        } catch (final IllegalArgumentException e1) {
          callOnError(e1);
          return;
        }
      }
      if (surface instanceof SurfaceHolder) {
        mUVCCamera.setPreviewDisplay((SurfaceHolder) surface);
      }
      if (surface instanceof Surface) {
        mUVCCamera.setPreviewDisplay((Surface) surface);
      } else {
        mUVCCamera.setPreviewTexture((SurfaceTexture) surface);
      }
      mUVCCamera.startPreview();
      mUVCCamera.updateCameraParams();
      synchronized (mSync) {
        mIsPreviewing = true;
      }
      mUVCCamera.setFrameCallback(mIFrameCallback, UVCCamera.PIXEL_FORMAT_YUV420SP);
      callOnStartPreview();
    }

    public void handleStopPreview() {
      if (DEBUG) Log.v(TAG_THREAD, "handleStopPreview:");
      if (mIsPreviewing) {
        if (mUVCCamera != null) {
          mUVCCamera.stopPreview();
        }
        synchronized (mSync) {
          mIsPreviewing = false;
          mSync.notifyAll();
        }
        callOnStopPreview();
      }
      if (DEBUG) Log.v(TAG_THREAD, "handleStopPreview:finished");
    }

    public void handleStopRecording() {
    }

    public byte[] getFrameBytes() {
      if (DEBUG) Log.v(TAG_THREAD, "handleStopPreview:");
      if (mIsPreviewing) {
        if (mUVCCamera != null) {
          return mYuvBytes;
        }
      }
      return null;
    }

    private final IFrameCallback mIFrameCallback = new IFrameCallback() {
      @Override
      public void onFrame(final ByteBuffer frame) {
        byte[] arrayOfByte;
        if (frame.limit() > 0) {
          arrayOfByte = new byte[frame.limit()];
          frame.get(arrayOfByte);
          mYuvBytes = arrayOfByte;
        }
      }
    };

    public void handleRelease() {
      if (DEBUG) Log.v(TAG_THREAD, "handleRelease:mIsRecording=");
      handleClose();
      mCallbacks.clear();
      mHandler.mReleased = true;
      Looper.myLooper().quit();
      if (DEBUG) Log.v(TAG_THREAD, "handleRelease:finished");
    }

    @Override
    public void run() {
      Looper.prepare();
      AbstractUVCCameraHandler handler = null;
      try {
        final Constructor<? extends AbstractUVCCameraHandler> constructor = mHandlerClass.getDeclaredConstructor(CameraThread.class);
        handler = constructor.newInstance(this);
      } catch (final NoSuchMethodException e) {
        Log.w(TAG, e);
      } catch (final IllegalAccessException e) {
        Log.w(TAG, e);
      } catch (final InstantiationException e) {
        Log.w(TAG, e);
      } catch (final InvocationTargetException e) {
        Log.w(TAG, e);
      }
      if (handler != null) {
        synchronized (mSync) {
          mHandler = handler;
          mSync.notifyAll();
        }
        Looper.loop();
        if (mHandler != null) {
          mHandler.mReleased = true;
        }
      }
      mCallbacks.clear();
      synchronized (mSync) {
        mHandler = null;
        mSync.notifyAll();
      }
    }

    private void callOnOpen() {
      for (final CameraCallback callback : mCallbacks) {
        try {
          callback.onOpen();
        } catch (final Exception e) {
          mCallbacks.remove(callback);
          Log.w(TAG, e);
        }
      }
    }

    private void callOnClose() {
      for (final CameraCallback callback : mCallbacks) {
        try {
          callback.onClose();
        } catch (final Exception e) {
          mCallbacks.remove(callback);
          Log.w(TAG, e);
        }
      }
    }

    private void callOnStartPreview() {
      for (final CameraCallback callback : mCallbacks) {
        try {
          callback.onStartPreview();
        } catch (final Exception e) {
          mCallbacks.remove(callback);
          Log.w(TAG, e);
        }
      }
    }

    private void callOnStopPreview() {
      for (final CameraCallback callback : mCallbacks) {
        try {
          callback.onStopPreview();
        } catch (final Exception e) {
          mCallbacks.remove(callback);
          Log.w(TAG, e);
        }
      }
    }

    private void callOnError(final Exception e) {
      for (final CameraCallback callback : mCallbacks) {
        try {
          callback.onError(e);
        } catch (final Exception e1) {
          mCallbacks.remove(callback);
          Log.w(TAG, e);
        }
      }
    }

    private void callOnFrame() {
      if ((mUVCCamera == null) || !mIsPreviewing) return;
      for (final CameraCallback callback : mCallbacks) {
        try {
          callback.onFrame(mYuvBytes, getWidth(), getHeight());
        } catch (final Exception e1) {
          mCallbacks.remove(callback);
          Log.w(TAG, e1);
        }
      }
    }
  }
}
