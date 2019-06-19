package com.serenegiant.usbcameracommon;


/**
 * Created by wangzhi on 2017/8/1.
 */

public abstract class SimpleCameraCallback implements AbstractUVCCameraHandler.CameraCallback {

  String TAG = SimpleCameraCallback.class.getSimpleName();

  @Override
  public void onOpen() {

  }

  @Override
  public void onClose() {

  }

  @Override
  public void onStartPreview() {

  }

  @Override
  public void onStopPreview() {

  }

  @Override
  public void onError(Exception e) {

  }

  @Override
  public void onFrame(byte[] data, int width, int height) {

  }
}
