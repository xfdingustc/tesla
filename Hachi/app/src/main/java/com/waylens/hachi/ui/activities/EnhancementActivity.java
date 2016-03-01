package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.entities.SharableClip;
import com.waylens.hachi.ui.fragments.EnhancementFragment;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Richard on 2/22/16.
 */
public class EnhancementActivity extends BaseActivity {

    public static void launch(Activity activity, ArrayList<Clip> clipList) {
        Intent intent = new Intent(activity, EnhancementActivity.class);
        intent.putParcelableArrayListExtra("clips", clipList);
        activity.startActivity(intent);
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enhance);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.enhance);
            actionBar.setHomeAsUpIndicator(R.drawable.navbar_close);
        }

        Intent intent = getIntent();
        //Clip clip = intent.getParcelableExtra("clip");
        ArrayList<Clip> clips = intent.getParcelableArrayListExtra("clips");

        getFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_content, EnhancementFragment.newInstance(clips)).commit();
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
