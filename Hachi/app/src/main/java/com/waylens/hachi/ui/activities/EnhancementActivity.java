package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.waylens.hachi.R;
import com.waylens.hachi.vdb.ClipSetManager;

/**
 * Created by Richard on 2/22/16.
 */
public class EnhancementActivity extends BaseActivity {





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_fragment);
        setTitle(R.string.enhance);
        setHomeAsUpIndicator(R.drawable.navbar_close);
        Intent intent = getIntent();

        int clipSetIndex = intent.getIntExtra("clipSetIndex", ClipSetManager.CLIP_SET_TYPE_ENHANCE);
//        getFragmentManager()
//                .beginTransaction()
//                .replace(R.id.fragment_content, EnhancementActivity2.newInstance(clipSetIndex)).commit();
        //EnhancementActivity2.launch();
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
