package fuweichen.camera2use;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Size;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Range;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Administrator on 2017/7/12.
 */

public class Camera2VideoFragment extends Fragment implements TextureView.SurfaceTextureListener{
    private static final String TAG = "Camera2VideoFragment";
    private static final int CAMERA_PERMISSION = 100;
    private TextureView mTextureView;

    private HandlerThread handlerThread;
    private Handler mHandler;

    private CameraDevice cameraDevice;
    private String mCurCameraId;
    private CaptureRequest.Builder captureRequestBuilder;
    private CameraCaptureSession mCaptureSession;

    private Size previewSize;
    private ImageReader imageReader;
    private byte[] mImageData;
    private byte[] mRealData;

    int nTotalCount = 30;
    private ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = null;
            FileOutputStream fos = null;
            Bitmap bitmap = null;

            try {
                image = imageReader.acquireLatestImage();
                if (image != null) {

                    long nTime1 = Calendar.getInstance().getTimeInMillis();
//                    if(nTime1 - nTime > 1000)
//                    {
//                        GenseeLog.i(TAG, "onImageAvailable read framecount count per = " + frameCount);
//                        nTime = nTime1;
//                        frameCount = 0;
//                    }
//                    frameCount ++;


                    Image.Plane[] planes = image.getPlanes();
                    Log.i(TAG,"image format: " +image.getFormat() + " planes length = " + planes.length);
                    for (int i = 0; i < planes.length; i++) {
                        ByteBuffer iBuffer = planes[i].getBuffer();
                        int iSize = iBuffer.remaining();
                        Log.i(TAG, "pixelStride  " + planes[i].getPixelStride());
                        Log.i(TAG, "rowStride   " + planes[i].getRowStride());
                        Log.i(TAG, "width  " + image.getWidth());
                        Log.i(TAG, "height  " + image.getHeight());
                        Log.i(TAG, "buffersize " + iSize);
                        Log.i(TAG, "Finished reading data from plane  " + i);
                    }

                    byte[] bytes = null;
                    if(image.getFormat() == ImageFormat.JPEG) {
                        ByteBuffer buffer = planes[0].getBuffer();
                        bytes = new byte[buffer.remaining()];
                        buffer.get(bytes);
                    }else if(image.getFormat() == ImageFormat.YUV_420_888)
                    {
                        byte[] uData = null;
                        byte[] vData = null;
                        ByteBuffer yBuffer = planes[0].getBuffer();
                        byte[] yData = new byte[yBuffer.remaining()];
                        yBuffer.get(yData);

                        ByteBuffer buffer1 = planes[1].getBuffer();
                        int pixelStride = planes[1].getPixelStride();
                        if(pixelStride == 1)
                        {
                            uData = new byte[buffer1.remaining()];
                            buffer1.get(uData);

                            vData = new byte[planes[2].getBuffer().remaining()];
                            planes[2].getBuffer().get(vData);

                            if(nTotalCount > 40)
                            {
                                return;
                            }
                            nTotalCount += 1;
                        }else if(pixelStride == 2)
                        {
                            byte[] data = new byte[buffer1.remaining()];
                            buffer1.get(data);
                            uData = new byte[(data.length + 1)/2];
                            vData = new byte[(data.length + 1)/2];
                            int j = 0;
                            for(int i = 0; i< data.length; i+=2)
                            {
                                uData[j] = data[i];
                                if(i + 1 < data.length) {
                                    vData[j] = data[i + 1];
                                }else
                                {
                                    vData[j] = 0;
                                }
                                j++;
                            }
                        }

                        bytes = new byte[yData.length + uData.length + vData.length];
                        System.arraycopy(yData, 0, bytes, 0, yData.length);
                        System.arraycopy(uData, 0, bytes, yData.length, uData.length);
                        System.arraycopy(vData, 0, bytes, yData.length + uData.length, vData.length);
                    }

                    String storeDirectory = Environment.getExternalStorageDirectory() + "/ccc/";
                    File file = new File(storeDirectory);
                    System.err.println("file file.exists():"+file.exists());
                    if(!file.exists()){
                        file.mkdirs();
                    }
                    File file1 = new File(storeDirectory + "333" + ".png");
                    if(!file1.exists())
                    {
                        file1.createNewFile();
                    }
                    fos = new FileOutputStream(file1, true);

                    fos.write(bytes);

                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (fos!=null) {
                    try {
                        fos.close();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }

                if (bitmap!=null) {
                    bitmap.recycle();
                }

                if (image!=null) {
                    image.close();
                }
            }
        }
    };

    private CameraDevice.StateCallback mDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.i(TAG, "onOpened camera = " + camera);
            cameraDevice = camera;
            SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());

            imageReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(), ImageFormat.YUV_420_888, 2);
            imageReader.setOnImageAvailableListener(onImageAvailableListener, mHandler);
            Surface mSurface = new Surface(surfaceTexture);
            try {
                captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                captureRequestBuilder.addTarget(mSurface);
                captureRequestBuilder.addTarget(imageReader.getSurface());
                camera.createCaptureSession(Arrays.asList(mSurface, imageReader.getSurface()), captureStateCallback, mHandler);
            }catch (CameraAccessException e)
            {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.i(TAG, "onDisconnected camera = " + camera);
        }

        @Override
        public void onError(@NonNull CameraDevice camera,  int error) {
            Log.i(TAG, "onError camera = " + camera + " error = " + error);

        }
    };

    private CameraCaptureSession.StateCallback captureStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            Log.i(TAG, "onConfigured session = " + session);
            mCaptureSession = session;
            try {
                session.setRepeatingRequest(captureRequestBuilder.build(), captureCallback, mHandler);
            }catch (CameraAccessException e)
            {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Log.i(TAG, "onConfigureFailed session = " + session);
        }
    };

    private CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
            Log.i(TAG, "onCaptureStarted session = " + session + " request = " + request);
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
            Log.i(TAG, "onCaptureProgressed session = " + session + " request = " + request);

        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            Log.i(TAG, "onCaptureCompleted session = " + session + " request = " + request);
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
            Log.i(TAG, "onCaptureFailed session = " + session + " request = " + request);

        }

        @Override
        public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session, int sequenceId, long frameNumber) {
            super.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
            Log.i(TAG, "onCaptureSequenceCompleted session = " + session + " sequenceId = " + sequenceId);

        }

        @Override
        public void onCaptureSequenceAborted(@NonNull CameraCaptureSession session, int sequenceId) {
            super.onCaptureSequenceAborted(session, sequenceId);
            Log.i(TAG, "onCaptureSequenceAborted session = " + session + " sequenceId = " + sequenceId);

        }

        @Override
        public void onCaptureBufferLost(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull Surface target, long frameNumber) {
            super.onCaptureBufferLost(session, request, target, frameNumber);
            Log.i(TAG, "onCaptureBufferLost session = " + session + " request = " + request);
        }
    };

    public static Camera2VideoFragment newInstance()
    {
        Camera2VideoFragment camera2VideoFragment = new Camera2VideoFragment();
        return camera2VideoFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera2_layout, null);
        mTextureView = (TextureView)view.findViewById(R.id.textureview);
        mTextureView.setSurfaceTextureListener(this);

        handlerThread = new HandlerThread("camera2");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper())
        {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Log.i(TAG, "handleMessage: msg = " + msg );
            }
        };

        int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
        Log.i(TAG, "activity rotaion = " + rotation);
        return view;
    }

    private void configureTransform(int viewWidth, int viewHeight)
    {
        int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
        RectF srcRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF desRect = new RectF(0, 0,  previewSize.getHeight(),previewSize.getWidth());
        float srcCenterX = srcRect.centerX();
        float srcCenterY = srcRect.centerY();
        Matrix matrix = new Matrix();
        if(rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270)
        {
            //前置摄像头，Surface.ROTATION_90 本地需旋转-90度，imagereader获取的yuv数据正常  ROTATION_270 本地需旋转90度， imagereader获取的yuv数据倒立
            //后置摄像头，Surface.ROTATION_90 本地需旋转-90度, imagereader获取yuv数据正常    ROTATION_270 本地需旋转90度   imagereader获取的yuv数据倒立

            desRect.offset(srcCenterX - desRect.centerX(), srcCenterY - desRect.centerY());
            matrix.setRectToRect(srcRect, desRect, Matrix.ScaleToFit.FILL);
            float scale = Math.min((float)viewHeight / previewSize.getHeight(), (float)viewWidth / previewSize.getWidth());
            matrix.postRotate(90 * (rotation - 2), srcCenterX, srcCenterY);
            matrix.postScale(scale, scale, srcCenterX, srcCenterY);

        }else if(rotation == Surface.ROTATION_180)
        {
            matrix.postRotate(180, srcCenterX, srcCenterY);
        }else if(rotation == Surface.ROTATION_0)
        {
            //前置摄像头，本地预览不需要旋转，需缩放。imagereader获取的yuv数据向右
            //后置摄像头，本地预览不需要旋转，需缩放。imagereader获取的yuv数据向左
            desRect.offset(srcCenterX - desRect.centerX(), srcCenterY - desRect.centerY());
            matrix.setRectToRect(srcRect, desRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max((float)viewHeight / previewSize.getWidth(), (float)viewWidth / previewSize.getHeight());
            matrix.postScale(scale, scale, srcCenterX, srcCenterY);
        }
        mTextureView.setTransform(matrix);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        openCamera(width, height);
        configureTransform(width, height);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        configureTransform(width, height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    private void openCamera(int width, int height)
    {

        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    CAMERA_PERMISSION);
        }else{

        }


        CameraManager cameraManager = (CameraManager)getActivity().getSystemService(Context.CAMERA_SERVICE);
        String[] cameraIds = null;
        try
        {
            cameraIds = cameraManager.getCameraIdList();
            CameraCharacteristics cameraCharacteristics = null;
            for(String cameraId : cameraIds)
            {
                cameraCharacteristics = (CameraCharacteristics)cameraManager.getCameraCharacteristics(cameraId);
                if(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING).equals(CameraCharacteristics.LENS_FACING_BACK))
                {
                    mCurCameraId = cameraId;
                    break;
                }
            }

            // 获取摄像头支持的配置属性
            StreamConfigurationMap map = cameraCharacteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            android.util.Size[] previewSizes = map.getOutputSizes(SurfaceTexture.class);
            for(android.util.Size size : previewSizes)
            {
                Log.i(TAG, "supportpreview size: " + size.getWidth() + " : " + size.getHeight());
            }

            android.util.Size[] jpegSizes = map.getOutputSizes(ImageFormat.JPEG);
            for(android.util.Size size : jpegSizes)
            {
                Log.i(TAG, "supportJpeg size: " + size.getWidth() + " : " + size.getHeight());
            }

            int[] formats = map.getOutputFormats();
            for(int value : formats)
            {
                Log.i(TAG, "getOutputFormats format: " +value);
            }

            android.util.Size[] highSpeedSizes = map.getHighSpeedVideoSizes();
            for(android.util.Size size : highSpeedSizes)
            {
                Log.i(TAG, "highSpeedSizes size: " + size.getWidth() + " : " + size.getHeight());
            }

            Range<Integer>[] highSpeedFps =  map.getHighSpeedVideoFpsRanges();
            for(Range<Integer> size : highSpeedFps)
            {
                Log.i(TAG, "highSpeedFps Range: " + size.getLower() + " : " + size.getUpper());
            }



//            android.util.Size[] highResolutionSizes = map.getHighResolutionOutputSizes(ImageFormat.JPEG);
//            for(android.util.Size size : highResolutionSizes)
//            {
//                Log.i(TAG, "highResolutionSizes size: " + size.getWidth() + " : " + size.getHeight());
//            }

            Size aspectRatio = new Size(640, 480);
            previewSize = getOptimalPreviewSize(map.getOutputSizes(SurfaceTexture.class), aspectRatio.getWidth(), aspectRatio.getHeight());
            if (ContextCompat.checkSelfPermission(getActivity(),
                    Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.CAMERA},
                        CAMERA_PERMISSION);
            }else{
                cameraManager.openCamera(mCurCameraId, mDeviceStateCallback, mHandler);
            }
        }catch (CameraAccessException e)
        {
            e.printStackTrace();
        }

    }

    private Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<Size>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
//            if (option.getHeight() == option.getWidth() * h / w &&
//                    option.getWidth() >= width && option.getHeight() >= height) {
//                bigEnough.add(option);
//            }
            if (option.getHeight() == option.getWidth() * h / w ) {
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }


    public Size getOptimalPreviewSize(android.util.Size[] sizes, int w, int h) {
        if (sizes == null) {
            return null;
        }

        final double ASPECT_TOLERANCE = 0.05;
        double targetRatio = (double) w / h;
        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            if(size.getWidth() < w || size.getHeight() < h)
            {
                continue;
            }
            double ratio = (double) size.getWidth() / size.getHeight();
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.getHeight() - h) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.getHeight() - h);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.getHeight() - h) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.getHeight() - h);
                }
            }
        }
        return optimalSize;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
        closeCamera();
    }

    private void closeCamera() {
        try {
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != cameraDevice) {
                cameraDevice.close();
                cameraDevice = null;
            }
            if (null != imageReader) {
                imageReader.close();
                imageReader = null;
            }
        } catch (Exception e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handlerThread.quitSafely();
        try
        {
            handlerThread.join();
            handlerThread = null;
            mHandler = null;
        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
