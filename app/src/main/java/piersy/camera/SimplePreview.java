package piersy.camera;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

public class SimplePreview {
    private static final String TAG = SimplePreview.class.getName();
    private final Camera camera;
    private final SurfaceView previewSurface;
    private StartPreviewCallback startPreviewCallback;

    public SimplePreview(Camera camera, SurfaceView previewSurface, int targetWidth, int targetHeight, Camera.PreviewCallback previewCallback) {
        this.camera = camera;
        this.previewSurface = previewSurface;

        Camera.Size size = getOptimalPreviewSize(this.camera, targetWidth, targetHeight, 0);
        setPreviewSize(this.camera, size);

        int bufferSize = (ImageFormat.getBitsPerPixel(ImageFormat.NV21) * size.width * size.height) / 8;
        this.camera.setPreviewCallbackWithBuffer(previewCallback);
        this.camera.addCallbackBuffer(new byte[bufferSize]);

        startPreviewCallback = new StartPreviewCallback(this.camera);
        previewSurface.getHolder().addCallback(startPreviewCallback);
    }

    public void stopPreview(){
        previewSurface.getHolder().removeCallback(startPreviewCallback);
        camera.stopPreview();
        camera.release();
    }

    public static Camera findCamera(CameraFacing facing, int previewImageFormat) {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int cameraCount = Camera.getNumberOfCameras();
        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == facing.value()) {
                Camera camera = Camera.open(camIdx);
                camera.getParameters().setPreviewFormat(previewImageFormat);
                return camera;
            }
        }
        throw new RuntimeException("No camera found");
    }

    private Camera.Size getOptimalPreviewSize(Camera camera, int targetWidth, int targetHeight, int maxAspectRatioDiff) {
        List<Camera.Size> sizes = camera.getParameters().getSupportedPreviewSizes();
        if (sizes == null) throw new RuntimeException("No camera preview sizes available");
        double targetRatio = (double) targetWidth/targetHeight;
        Log.e(TAG,"Target width: "+targetWidth);
        Log.e(TAG,"Target height: "+targetHeight);
        Log.e(TAG,"Target aspect ratio: "+targetRatio);

        Camera.Size optimalSize = null;

        double minDiff = Double.MAX_VALUE;

        for (Camera.Size size : sizes) {
            Log.e(TAG,"--------------------");
            Log.e(TAG,"Width:"+size.width);
            Log.e(TAG,"Height:"+size.height);
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > maxAspectRatioDiff) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }
        if (optimalSize == null) throw new RuntimeException("No suitable camera preview size found");
        Log.e(TAG,"--------------------");
        Log.e(TAG,"OptimalWidth:"+optimalSize.width);
        Log.e(TAG,"OptimalHeight:"+optimalSize.height);
        return optimalSize;
    }

    private void setPreviewSize(Camera camera, Camera.Size size){
        /**
         * This may look retarded and it is but you really have to create a new instance of Parameters
         * and set properties in there and then set those on the camera.
         *
         * The following does not work
         *
         *  camera.getParameters().setPreviewSize(size.width, size.height);
         *
         * or
         *
         *  camera.getParameters().setPreviewSize(size.width, size.height);
         *  camera.setParameters(camera.getParameters());
         *
         *  It has to be done as below.
         */
        Camera.Parameters params = camera.getParameters();
        params.setPreviewSize(size.width, size.height);
        camera.setParameters(params);
    }

    private static class StartPreviewCallback implements SurfaceHolder.Callback {

        private Camera camera;

        private StartPreviewCallback(Camera camera) {
            this.camera = camera;
        }

        public void surfaceCreated(SurfaceHolder holder) {
            // no-op -- wait until surfaceChanged()
        }

        /**
         * We need to wait for surface changed here as it is the only safe place to assume that the
         * surface holder is in a valid sate to be able to be set as the preview display and start the
         * preview.
         */
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            try {
                camera.setPreviewDisplay(holder);
            } catch (IOException e) {
                throw new RuntimeException("Failed to set preview display: " + e.getMessage());
            }
            camera.startPreview();
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            // no-op
        }
    }

    public enum CameraFacing{
        FRONT(Camera.CameraInfo.CAMERA_FACING_FRONT), BACK(Camera.CameraInfo.CAMERA_FACING_BACK);

        private int facing;
        CameraFacing(int facing) {
            this.facing = facing;
        }

        private int value() {
            return facing;
        }
    }


}
