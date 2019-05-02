package piersy.camera;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceView;

public class PreviewDemo extends Activity {
    private SimplePreview simplePreview;

    private long startNanos = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

    @Override
    public void onResume() {
        super.onResume();
        Camera camera = SimplePreview.findCamera(SimplePreview.CameraFacing.FRONT, ImageFormat.NV21);
        SurfaceView preview = (SurfaceView) findViewById(R.id.preview);
        final FrameProvider fp = new FrameProvider(camera, new Handler());
        simplePreview = new SimplePreview(camera, preview, 1280, 720, fp);
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    try {
                        fp.returnFrame(fp.getFrame());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if(startNanos > 0){
                        Log.d("timing", ""+((System.nanoTime() - startNanos)/1000000d));
                    }
                    startNanos = System.nanoTime();

                }
            }
        }).start();
    }

    @Override
    public void onPause() {
        simplePreview.stopPreview();
        super.onPause();
    }

    Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        public void onPreviewFrame(byte[] data, Camera camera) {
            camera.addCallbackBuffer(data);
            if(startNanos > 0){
                Log.d("timing", ""+((System.nanoTime() - startNanos)/100000d));
            }
            startNanos = System.nanoTime();
        }
    };

}
