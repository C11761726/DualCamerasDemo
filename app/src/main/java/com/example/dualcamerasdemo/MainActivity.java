package com.example.dualcamerasdemo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.widget.UVCCameraTextureView;
import com.techshino.config.DualFaceConfig;
import com.techshino.facespoof.Algorithm;
import com.techshino.utils.FileUtils;
import com.techshino.utils.Logs;

import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {
    private static final boolean DEBUG = true;  // FIXME set false when production
    private static final String TAG = "MainActivity";
    private final static Object mSync = new Object();

    private UVCCamera mUVCCameraL;
    private UVCCameraTextureView mUVCCameraViewL;
    private Surface mLeftPreviewSurface;
    private UVCCamera mUVCCameraR;
    private UVCCameraTextureView mUVCCameraViewR;
    private Surface mRightPreviewSurface;

    private USBMonitor.UsbControlBlock mLeftControlBlock;
    private USBMonitor.UsbControlBlock mRightControlBlock;

    private boolean bUsedL = false;
    boolean leftFrame = false;
    boolean rightFrame = false;
    int leftSuc = -1;
    int rightSuc = -1;
    int index = 0;
    private int width = 640;
    private int height = 480;
    private boolean isDetecting = false;
    private DualFaceConfig mDualFaceConfig;
    private boolean isLive = false;
    private long mDetectTime;

    // for accessing USB and USB camera
    private USBMonitor mUSBMonitor;
    Algorithm mAlgorithm;

    private static final float[] BANDWIDTH_FACTORS = {0.5f, 0.5f};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
    }

    private void initViews() {
        mUVCCameraViewL = findViewById(R.id.camera_view_L);
        mUVCCameraViewL.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / (float) UVCCamera.DEFAULT_PREVIEW_HEIGHT);
        mUVCCameraViewR = findViewById(R.id.camera_view_R);
        mUVCCameraViewR.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / (float) UVCCamera.DEFAULT_PREVIEW_HEIGHT);

        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
        mDualFaceConfig = new DualFaceConfig();
        mDetectTime = System.currentTimeMillis();
    }

    private final USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
            Logs.v(TAG, "onAttach:" + device);
            mUSBMonitor.requestPermission(device);
        }

        @Override
        public void onConnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock, final boolean createNew) {
            Logs.v(TAG, "onConnect:" + device);
            openCameraDevice(device, ctrlBlock);
        }

        @Override
        public void onDisconnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock) {
            if (DEBUG) Log.v(TAG, "onDisconnect:" + device);

        }

        @Override
        public void onDettach(final UsbDevice device) {
            Toast.makeText(MainActivity.this, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
//            if (device.getProductId() == mDualFaceConfig.getPidL()) {
//                mLeftControlBlock = null;
//            }
//            if (device.getProductId() == mDualFaceConfig.getPidR()) {
//                mRightControlBlock = null;
//            }
        }

        @Override
        public void onCancel(final UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onCancel:");
        }
    };

    private void openCameraDevice(UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock) {

        if (device.getProductName().contains("Camera")) {
            if ((mUVCCameraL == null) && !bUsedL) {
                bUsedL = true;
                new Thread(() -> {
                    final UVCCamera camera = new UVCCamera();
                    camera.open(ctrlBlock);
                    if (mLeftControlBlock == null)
                        mLeftControlBlock = ctrlBlock;

                    Log.d(TAG, "camera.open:" + ctrlBlock);
                    try {
                        camera.setPreviewSize(
                                UVCCamera.DEFAULT_PREVIEW_WIDTH,
                                UVCCamera.DEFAULT_PREVIEW_HEIGHT,
                                UVCCamera.FRAME_FORMAT_MJPEG, BANDWIDTH_FACTORS[0]);
                    } catch (final IllegalArgumentException e) {
                        // fallback to YUV mode
                        try {
                            camera.setPreviewSize(
                                    UVCCamera.DEFAULT_PREVIEW_WIDTH,
                                    UVCCamera.DEFAULT_PREVIEW_HEIGHT,
                                    UVCCamera.DEFAULT_PREVIEW_MODE);
                        } catch (final IllegalArgumentException e1) {
                            camera.destroy();
                            return;
                        }
                    }

                    final SurfaceTexture st = mUVCCameraViewL.getSurfaceTexture();
                    if (st != null) {
                        mLeftPreviewSurface = new Surface(st);
                        camera.setPreviewDisplay(mLeftPreviewSurface);
                        camera.setFrameCallback(leftCallback,
                                UVCCamera.PIXEL_FORMAT_YUV420SP/*
                                 * UVCCamera.
                                 * PIXEL_FORMAT_NV21
                                 */);
                        leftFrame = true;
                        camera.startPreview();
                    }
                    synchronized (mSync) {
                        mUVCCameraL = camera;
                    }
                }).start();
            } else {
                if (mUVCCameraR == null) {
                    new Thread(() -> {
                        final UVCCamera camera = new UVCCamera();
                        camera.open(ctrlBlock);
                        if (mRightControlBlock == null) {
                            mRightControlBlock = ctrlBlock;
                        }

                        Log.d(TAG, "camera.open:" + ctrlBlock);
                        try {
                            camera.setPreviewSize(
                                    UVCCamera.DEFAULT_PREVIEW_WIDTH,
                                    UVCCamera.DEFAULT_PREVIEW_HEIGHT,
                                    UVCCamera.FRAME_FORMAT_MJPEG, BANDWIDTH_FACTORS[1]);
                        } catch (final IllegalArgumentException e) {
                            // fallback to YUV mode
                            try {
                                camera.setPreviewSize(
                                        UVCCamera.DEFAULT_PREVIEW_WIDTH,
                                        UVCCamera.DEFAULT_PREVIEW_HEIGHT,
                                        UVCCamera.DEFAULT_PREVIEW_MODE);
                            } catch (final IllegalArgumentException e1) {
                                camera.destroy();
                                return;
                            }
                        }

                        final SurfaceTexture st = mUVCCameraViewR.getSurfaceTexture();
                        if (st != null) {
                            mRightPreviewSurface = new Surface(st);
                            camera.setPreviewDisplay(mRightPreviewSurface);
                            camera.setFrameCallback(rightCallback,
                                    UVCCamera.PIXEL_FORMAT_YUV420SP/*
                                     * UVCCamera.
                                     * PIXEL_FORMAT_NV21
                                     */);
                            rightFrame = true;
                            camera.startPreview();
                        }
                        synchronized (mSync) {
                            mUVCCameraR = camera;
//              callOnOpen(3000);
                        }
                    }).start();
                }
            }
        }
    }

    /**
     * 近红外检测回调
     */
    IFrameCallback leftCallback = new IFrameCallback() {

        @Override
        public void onFrame(ByteBuffer frame) {


            if (leftFrame) {
                leftSuc++;
                leftFrame = false;
            }
            byte[] yuv = null;
            if (frame.limit() > 0) {
                yuv = new byte[frame.limit()];
                frame.get(yuv);
//        mYuvBytesR = yuv;
            }

            if (yuv == null) {
                return;
            }
            if (!isDetecting) {
                return;
            }

            long start = System.currentTimeMillis();
            byte[] rgb24 = new byte[width * height * 3];

            mAlgorithm.RgbFromYuv420SP(rgb24, yuv, width, height);
            Logs.d(TAG, "Yuv转换时间：" + (System.currentTimeMillis() - start) + "ms" + Thread.currentThread().getName());

            //saveRgb24ToDisk(rgb24);

            float[] score = new float[1];
            double[] feature = new double[20];
            int[] faceRect = new int[4];
            int status = mAlgorithm.nir(rgb24, 0, width, height, faceRect, score, feature);
//      Logs.i(TAG, "x:" + faceRect[0] + " y:" + faceRect[1] + " w:" + faceRect[2] + " h:" + faceRect[3]);
            Logs.i(TAG, "x:" + feature[0] + " y:" + feature[1] + " w:" + feature[2] + " h:" + feature[3] + " score:" + feature[4]);
//      Logs.d(TAG, "检活时间：" + (System.currentTimeMillis() - start) + "ms");
//      Logs.d(TAG, "status:" + status + " score:" + score[0]);


            if (score[0] > mDualFaceConfig.getThreshold()) {
                Logs.d(TAG, "检活时间：" + (System.currentTimeMillis() - start) + "ms");
                index++;


            }

            if (mDualFaceConfig.getIsActived() == 0) {
                index = mDualFaceConfig.getNirCount();
                isLive = true;
            } else if (mDualFaceConfig.getIsActived() == 1) {
                isLive = true;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };

    IFrameCallback rightCallback = new IFrameCallback() {

        @Override
        public void onFrame(final ByteBuffer frame) {
            if (rightFrame) {
                rightSuc++;
                rightFrame = false;
            }
            byte[] yuv = null;
            if (frame.limit() > 0) {
                yuv = new byte[frame.limit()];
                frame.get(yuv);
//        mYuvBytesR = yuv;
            }

            if (yuv == null) {
                return;
            }
            if (!isDetecting) {
                return;
            }

            long start = System.currentTimeMillis();
            byte[] rgb24 = new byte[width * height * 3];

            mAlgorithm.RgbFromYuv420SP(rgb24, yuv, width, height);
            Logs.d(TAG, "Yuv转换时间：" + (System.currentTimeMillis() - start) + "ms");

            if (mDualFaceConfig.getIsActived() == 2) {
                int status = mAlgorithm.colorSimple(rgb24, width, height, new int[4], new int[0], new double[20]);
                if (status == 0)
                    isLive = true;
            } else if (mDualFaceConfig.getIsActived() == 3) {
                int status = mAlgorithm.colorNormal(rgb24, width, height, new int[4], new float[0], new double[20]);
                if (status == 0)
                    isLive = true;
            }

        }
    };

    @Override
    protected void onStart() {
        super.onStart();

        mUSBMonitor.register();
        if (mUVCCameraViewR != null)
            mUVCCameraViewR.onResume();
        if (mUVCCameraViewL != null)
            mUVCCameraViewL.onResume();
    }

    @Override
    protected void onStop() {
        if (mUVCCameraViewR != null)
            mUVCCameraViewR.onPause();
        if (mUVCCameraViewL != null)
            mUVCCameraViewL.onPause();
        mUSBMonitor.unregister();

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        stopCamera();
        if (mUSBMonitor != null) {
            mUSBMonitor.destroy();
            mUSBMonitor = null;
        }
        mUVCCameraViewR = null;
        mUVCCameraViewL = null;

        super.onDestroy();
    }

    public void stopCamera() {
        synchronized (mSync) {
            if (mUVCCameraL != null) {
                try {
                    mUVCCameraL.setStatusCallback(null);
                    mUVCCameraL.setButtonCallback(null);
                    mUVCCameraL.close();
                    mUVCCameraL.destroy();
                } catch (final Exception e) {
                    //
                }
                mUVCCameraL = null;
                leftFrame = false;
                leftSuc = -1;
            }
            if (mLeftPreviewSurface != null) {
                mLeftPreviewSurface.release();
                mLeftPreviewSurface = null;
            }
        }

        synchronized (mSync) {
            if (mUVCCameraR != null) {
                try {
                    mUVCCameraR.setStatusCallback(null);
                    mUVCCameraR.setButtonCallback(null);
                    mUVCCameraR.close();
                    mUVCCameraR.destroy();
                } catch (final Exception e) {
                    //
                }
                mUVCCameraR = null;
                rightFrame = false;
                rightSuc = -1;
            }
            if (mRightPreviewSurface != null) {
                mRightPreviewSurface.release();
                mRightPreviewSurface = null;
            }
        }
    }
}
