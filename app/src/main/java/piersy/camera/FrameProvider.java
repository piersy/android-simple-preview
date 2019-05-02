package piersy.camera;

import android.hardware.Camera;
import android.os.Handler;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Presents a simple way to retrieve frames from a camera preview
 */
public class FrameProvider implements Camera.PreviewCallback {

    /**
     * The currently available buffer from the preview.
     */
    private byte[] currentFrame;
    //private final Stack<byte[]> framesToReturn = new Stack<byte[]>();
    final Lock lock = new ReentrantLock();
    final Condition newFrame = lock.newCondition();
    final Camera mCamera;
    private final Handler handler;

    public FrameProvider(Camera mCamera, Handler handler) {
        this.mCamera = mCamera;
        this.handler = handler;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        lock.lock();
        try {
            // If frame unused then recycle it and replace with new frame.
            // otherwise just set the frame
            if (currentFrame != null) {
                mCamera.addCallbackBuffer(currentFrame);
            }
            currentFrame = data;
            newFrame.signal();
        } finally {
            lock.unlock();
        }
    }

    public byte[] getFrame() throws InterruptedException {
        lock.lock();
        try {
            if (currentFrame == null) {
                newFrame.await();
            }
            byte[] toReturn = currentFrame;
            currentFrame = null;
            return toReturn;
        } finally {
            lock.unlock();
        }
    }

    public void returnFrame(final byte[] frame) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                mCamera.addCallbackBuffer(frame);
            }
        });
    }
}
