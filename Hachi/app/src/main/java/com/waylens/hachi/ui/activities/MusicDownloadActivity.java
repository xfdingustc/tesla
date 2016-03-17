package com.waylens.hachi.ui.activities;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.view.View;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.fragments.MusicFragment;

/**
 * Created by Richard on 3/7/16.
 */
public class MusicDownloadActivity extends BaseActivity {

    public static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 100;

    MusicFragment mMusicFragment;

    public static void launchForResult(Activity activity, int requestCode) {
        Intent intent = new Intent(activity, MusicDownloadActivity.class);
        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_fragment);
        setSupportActionBar(mToolbar);
        setTitle(R.string.musics);
        setHomeAsUpIndicator(R.drawable.navbar_close);

        View view = findViewById(R.id.tabs);
        if (view != null) {
            view.setVisibility(View.GONE);
        }
        mMusicFragment = new MusicFragment();
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_content, mMusicFragment).commit();
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
                int i = 0;
                for (String permission : permissions) {
                    if (Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permission)
                            && grantResults[i] == PackageManager.PERMISSION_GRANTED
                            && mMusicFragment != null) {
                        mMusicFragment.continueDownloadMusic();
                        break;
                    }
                    i++;
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
