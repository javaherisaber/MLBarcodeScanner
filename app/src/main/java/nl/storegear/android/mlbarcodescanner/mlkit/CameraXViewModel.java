package nl.storegear.android.mlbarcodescanner.mlkit;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

/** View model for interacting with CameraX. */
public final class CameraXViewModel extends AndroidViewModel {

  private static final String TAG = "CameraXViewModel";
  private MutableLiveData<ProcessCameraProvider> cameraProviderLiveData;

  /**
   * Create an instance which interacts with the camera service via the given application context.
   */
  public CameraXViewModel(@NonNull Application application) {
    super(application);
  }

  public LiveData<ProcessCameraProvider> getProcessCameraProvider() {
    if (cameraProviderLiveData == null) {
      cameraProviderLiveData = new MutableLiveData<>();

      ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
          ProcessCameraProvider.getInstance(getApplication());
      cameraProviderFuture.addListener(
          () -> {
            try {
              cameraProviderLiveData.setValue(cameraProviderFuture.get());
            } catch (ExecutionException | InterruptedException e) {
              Log.e(TAG, "Unhandled exception", e);
            }
          },
          ContextCompat.getMainExecutor(getApplication()));
    }

    return cameraProviderLiveData;
  }
}
