package com.waylens.hachi.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.MenuItem;
import android.view.View;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.fragments.ShareFragment;
import com.waylens.hachi.vdb.ClipSetManager;

/**
 * Created by Richard on 3/9/16.
 */

public class ShareActivity extends BaseActivity {
    private static final String EXTRA_CLIP_SET_INDEX = "extra.clip.set.index";
    private static final String EXTRA_GAUGE_SETTINGS = "extra.gauge.settings";
    private static final String EXTRA_AUDIO_ID = "extra.audio.id";

    private static final String EXTRA_IS_FROM_ENHANCE = "extra.is.from.enhance";

    public static void launch(Context context, int clipSetIndex) {
        Intent intent = new Intent(context, ShareActivity.class);
        intent.putExtra(EXTRA_CLIP_SET_INDEX, clipSetIndex);
        intent.putExtra(EXTRA_GAUGE_SETTINGS, "");
        intent.putExtra(EXTRA_AUDIO_ID, -1);
        intent.putExtra(EXTRA_IS_FROM_ENHANCE, false);
        context.startActivity(intent);
    }

    public static void launch(Context context, int clipSetIndex, String gaugeSettings, int audioID, boolean isFromEnhance) {
        Intent intent = new Intent(context, ShareActivity.class);
        intent.putExtra(EXTRA_CLIP_SET_INDEX, clipSetIndex);
        intent.putExtra(EXTRA_GAUGE_SETTINGS, gaugeSettings);
        intent.putExtra(EXTRA_AUDIO_ID, audioID);
        intent.putExtra(EXTRA_IS_FROM_ENHANCE, isFromEnhance);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_fragment);
        setSupportActionBar(mToolbar);
        setTitle(R.string.share);
        setHomeAsUpIndicator(R.drawable.navbar_close);

        View view = findViewById(R.id.tabs);
        if (view != null) {
            view.setVisibility(View.GONE);
        }

        Intent intent = getIntent();
        int clipSetIndex = ClipSetManager.CLIP_SET_TYPE_ENHANCE;
        String gaugeSettings = "";
        int audioID = -1;
        boolean isFromEnhance = false;
        if (intent != null) {
            clipSetIndex = intent.getIntExtra(EXTRA_CLIP_SET_INDEX, ClipSetManager.CLIP_SET_TYPE_ENHANCE);
            gaugeSettings = intent.getStringExtra(EXTRA_GAUGE_SETTINGS);
            if (gaugeSettings == null) {
                gaugeSettings = "";
            }
            audioID = intent.getIntExtra(EXTRA_AUDIO_ID, -1);
            isFromEnhance = intent.getBooleanExtra(EXTRA_IS_FROM_ENHANCE, false);
        }
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_content,
                        ShareFragment.newInstance(clipSetIndex, gaugeSettings, audioID, isFromEnhance)).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
