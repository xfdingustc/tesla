package com.waylens.hachi.ui.activities;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.MenuItem;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.fragments.ClipListFragment;
import com.waylens.hachi.ui.fragments.EnhancementFragment;
import com.waylens.hachi.vdb.Clip;

import java.util.ArrayList;

/**
 * Created by Richard on 3/2/16.
 */
public class ClipChooserActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_fragment);
        setTitle(R.string.add_more_clips);
        setHomeAsUpIndicator(R.drawable.navbar_close);
        Intent intent = getIntent();
        //Clip clip = intent.getParcelableExtra("clip");
        ArrayList<Clip> clips = intent.getParcelableArrayListExtra("clips");
        Fragment fragment = ClipListFragment.newInstance(Clip.TYPE_MARKED, true);

        getFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_content, fragment).commit();
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