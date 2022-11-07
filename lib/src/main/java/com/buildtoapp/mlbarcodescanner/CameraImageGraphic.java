package com.buildtoapp.mlbarcodescanner;

import android.graphics.Bitmap;
import android.graphics.Canvas;

/** Draw camera image to background. */
class CameraImageGraphic extends GraphicOverlay.Graphic {

  private final Bitmap bitmap;

  public CameraImageGraphic(GraphicOverlay overlay, Bitmap bitmap) {
    super(overlay);
    this.bitmap = bitmap;
  }

  @Override
  public void draw(Canvas canvas) {
    canvas.drawBitmap(bitmap, getTransformationMatrix(), null);
  }
}
