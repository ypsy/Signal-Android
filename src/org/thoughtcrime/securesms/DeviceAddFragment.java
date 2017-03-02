package org.thoughtcrime.securesms;

import android.animation.Animator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import org.thoughtcrime.securesms.components.ShapeScrim;
import org.thoughtcrime.securesms.components.camera.CameraView;
import org.thoughtcrime.securesms.components.camera.CameraView.PreviewCallback;
import org.thoughtcrime.securesms.components.camera.CameraView.PreviewFrame;
import org.thoughtcrime.securesms.qr.ScanListener;
import org.thoughtcrime.securesms.qr.ScanningThread;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.ViewUtil;

public class DeviceAddFragment extends Fragment {

  private ViewGroup      container;
  private LinearLayout   overlay;
  private ImageView      devicesImage;
  private CameraView     scannerView;
  private ScanningThread scanningThread;
  private ScanListener   scanListener;
  private EditText       qrcodeEditText;
  private Button         qrcodeTextButton;
  private ShapeScrim     cameraFrame;  
  
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle bundle) {
    this.container    = ViewUtil.inflate(inflater, viewGroup, R.layout.device_add_fragment);
    this.overlay      = ViewUtil.findById(this.container, R.id.overlay);
    this.scannerView  = ViewUtil.findById(this.container, R.id.scanner);
    this.devicesImage = ViewUtil.findById(this.container, R.id.devices);
    this.cameraFrame = ViewUtil.findById(this.container, R.id.camera_frame);
    this.qrcodeEditText = ViewUtil.findById(this.container, R.id.qr_code_edit_text);
    this.qrcodeTextButton = ViewUtil.findById(this.container, R.id.qr_code_text_button);
    
    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
      this.overlay.setOrientation(LinearLayout.HORIZONTAL);
    } else {
      this.overlay.setOrientation(LinearLayout.VERTICAL);
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      this.container.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                   int oldLeft, int oldTop, int oldRight, int oldBottom)
        {
          v.removeOnLayoutChangeListener(this);

          Animator reveal = ViewAnimationUtils.createCircularReveal(v, right, bottom, 0, (int) Math.hypot(right, bottom));
          reveal.setInterpolator(new DecelerateInterpolator(2f));
          reveal.setDuration(800);
          reveal.start();
        }
      });
    }
    
    qrcodeTextButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        String url = qrcodeEditText.getText().toString();
        if (TextUtils.isEmpty(url)) {
          qrcodeEditText.setError("The QR Code Field should not be empty.");
        }
        //TODO check for correctly formed input here before spamming the server with garbage

        // Uri uri = Uri.parse(url); // Apparently the older scanListener method didn't read strings but Uri instead
        scanListener.onQrDataFound(url);
      }
    });

    qrcodeEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
      @Override
      public void onFocusChange(View v, boolean hasFocus) {
        if(hasFocus){
          scannerView.setVisibility(View.GONE);
          cameraFrame.setVisibility(View.GONE);
          scannerView.onPause();
          scanningThread.stopScanning();
        }
      }
    });
    
    return this.container;
  }

  @Override
  public void onResume() {
    super.onResume();
    this.scanningThread = new ScanningThread();
    this.scanningThread.setScanListener(scanListener);
    this.scannerView.onResume();
    this.scannerView.setPreviewCallback(scanningThread);
    this.scanningThread.start();
    scannerView.setVisibility(View.VISIBLE);
    cameraFrame.setVisibility(View.VISIBLE);
  }

  @Override
  public void onPause() {
    super.onPause();
    this.scannerView.onPause();
    this.scanningThread.stopScanning();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfiguration) {
    super.onConfigurationChanged(newConfiguration);

    this.scannerView.onPause();

    if (newConfiguration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
      overlay.setOrientation(LinearLayout.HORIZONTAL);
    } else {
      overlay.setOrientation(LinearLayout.VERTICAL);
    }

    this.scannerView.onResume();
    this.scannerView.setPreviewCallback(scanningThread);
  }


  public ImageView getDevicesImage() {
    return devicesImage;
  }

  public void setScanListener(ScanListener scanListener) {
    this.scanListener = scanListener;

    if (this.scanningThread != null) {
      this.scanningThread.setScanListener(scanListener);
    }
  }


}
