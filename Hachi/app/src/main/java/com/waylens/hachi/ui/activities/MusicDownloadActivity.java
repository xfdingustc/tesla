package com.waylens.hachi.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.MenuItem;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.fragments.MusicFragment;

/**
 * Created by Richard on 3/7/16.
 */
public class MusicDownloadActivity extends BaseActivity {

    public static void launch(Context context) {
        Intent intent = new Intent(context, MusicDownloadActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_fragment);
        setTitle(R.string.musics);

        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_content, new MusicFragment()).commit();
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
