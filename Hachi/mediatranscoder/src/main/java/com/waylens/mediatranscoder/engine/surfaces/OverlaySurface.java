package com.waylens.mediatranscoder.engine.surfaces;


import com.waylens.mediatranscoder.engine.OverlayProvider;

/**
 * Created by Xiaofei on 2015/11/4.
 */
public class OverlaySurface {
    private static final String TAG = OverlaySurface.class.getSimpleName();

    private FullFrameRect mFullScreenOverlay;
    private int mOverlayTextureId;

    private OverlayProvider mOverlayProvider;

    public OverlaySurface(OverlayProvider overlayProvider) {
        setup();
        mOverlayProvider = overlayProvider;
    }


    public void release() {
        mFullScreenOverlay.release();
    }

    private void setup() {
        mFullScreenOverlay = new FullFrameRect(new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_2D));
        //mOverlayTextureId = GlUtil.createTextureFromImage(context, R.drawable.sailor);
        //mOverlayTextureId = GlUtil.createTextureWithTextContent("xfding");
    }

    public void drawImage(long pts, float[] texMatrix) {
        mOverlayTextureId = GlUtil.createTextureFromBitmap(mOverlayProvider.updateTexImage(pts));
        mFullScreenOverlay.drawFrame(mOverlayTextureId, texMatrix);
    }
}
