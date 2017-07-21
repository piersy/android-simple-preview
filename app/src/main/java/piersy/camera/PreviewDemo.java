package piersy.camera;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.SurfaceView;

public class PreviewDemo extends Activity {
    private SimplePreview simplePreview;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

    @Override
    public void onResume() {
        super.onResume();
        SurfaceView preview = (SurfaceView) findViewById(R.id.preview);
        simplePreview = new SimplePreview(preview, 640, 480, previewCallback);
    }

    @Override
    public void onPause() {
        simplePreview.stopPreview();
        super.onPause();
    }

    Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        public void onPreviewFrame(byte[] data, Camera camera) {
            camera.addCallbackBuffer(data);
        }
    };

}
