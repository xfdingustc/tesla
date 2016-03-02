package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.fragments.EnhancementFragment;
import com.waylens.hachi.vdb.Clip;

import java.util.ArrayList;

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
        setContentView(R.layout.activity_single_fragment);
        setTitle(R.string.enhance);
        setHomeAsUpIndicator(R.drawable.navbar_close);
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
