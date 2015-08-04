package com.transee.viditcam.actions;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.TextView;

import com.transee.common.MusicScanner;
import com.transee.common.MusicScanner.Music;
import com.transee.common.Utils;
import com.waylens.hachi.R;
import com.transee.viditcam.app.BaseActivity;

import java.util.ArrayList;

abstract public class SelectAudio {

	abstract public void onMusicSelected(String url);

	private final Activity mActivity;
	private View mLayout;
	private PopupWindow mWindow;

	private ListView mMusicList;
	private MusicListAdapter mAdapter;
	private MusicScanner mScanner;
	private int mSelectIndex = -1;
	private MediaPlayer mPlayer;
	private Button mOK;

	@SuppressLint("InflateParams")
	public SelectAudio(BaseActivity activity) {
		LayoutInflater inflater = LayoutInflater.from(activity);

		mActivity = activity;
		mLayout = inflater.inflate(R.layout.menu_select_audio, null);
		mWindow = activity.createPopupWindow(mLayout);

		mWindow.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss() {
				if (mScanner != null) {
					mScanner.stopWork();
					mScanner = null;
				}
				stopPlay();
			}
		});

		mMusicList = (ListView)mLayout.findViewById(R.id.listView1);
		mAdapter = new MusicListAdapter(activity.getResources().getColor(R.color.menuSelBackground), inflater,
				mMusicList);
		mMusicList.setAdapter(mAdapter);

		mMusicList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				selectAudio(view, position);
			}
		});

		Button button = (Button)mLayout.findViewById(R.id.button1);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish(false);
			}
		});

		mOK = (Button)mLayout.findViewById(R.id.button2);
		mOK.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish(true);
			}
		});

		enableOK();
	}

	private void enableOK() {
		mOK.setEnabled(mSelectIndex >= 0);
	}

	private void finish(boolean bOK) {
		mScanner.stopWork();
		stopPlay();
		mWindow.dismiss();
		if (bOK && mSelectIndex >= 0) {
			Music music = mAdapter.getMusic(mSelectIndex);
			if (music != null) {
				onMusicSelected(music.url);
			}
		}
	}

	public void show(View v) {
		DisplayMetrics dm = Utils.getDisplayMetrics(mActivity);
		mWindow.setWidth(dm.widthPixels);
		mWindow.setHeight(dm.heightPixels / 2);
		mWindow.showAtLocation(v, Gravity.NO_GRAVITY, 0, dm.heightPixels / 2);

		mScanner = new MusicScanner(mActivity.getContentResolver()) {
			@Override
			public void onScanResult(ArrayList<Music> list) {
				mAdapter.setMusicList(list);
			}
		};
		mScanner.startWork();
	}

	private void selectAudio(View view, int position) {
		boolean bNew = position != mSelectIndex;
		if (mSelectIndex >= 0) {
			stopPlay();
			mAdapter.selectItem(mSelectIndex, false);
			mSelectIndex = -1;
		}
		if (position >= 0 && bNew) {
			Music music = mAdapter.getMusic(position);
			if (music != null) {
				startPlay(music);
			}
			mAdapter.selectItem(position, true);
			mSelectIndex = position;
		}
		enableOK();
	}

	private void stopPlay() {
		if (mPlayer != null) {
			mPlayer.stop();
			mPlayer.release();
			mPlayer = null;
		}
	}

	private void startPlay(Music music) {
		if (mPlayer == null) {
			mPlayer = new MediaPlayer();
			mPlayer.setOnPreparedListener(new OnPreparedListener() {
				@Override
				public void onPrepared(MediaPlayer mp) {
					if (mPlayer != null) {
						mPlayer.start();
					}
				}
			});
		}
		try {
			mPlayer.setDataSource(music.url);
			mPlayer.prepareAsync();
		} catch (Exception ex) {

		}
	}

	static class MusicListAdapter extends BaseAdapter {

		private final int mSelColor;
		private final LayoutInflater mInflater;
		private final ListView mListView;
		private ArrayList<Music> mList;

		public MusicListAdapter(int selColor, LayoutInflater inflater, ListView listView) {
			mSelColor = selColor;
			mInflater = inflater;
			mListView = listView;
		}

		@Override
		public int getCount() {
			return mList == null ? 0 : mList.size();
		}

		@Override
		public Object getItem(int position) {
			return mList == null || position < 0 || position >= mList.size() ? null : mList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		private final View getViewOfIndex(int index) {
			return mListView.getChildAt(index - mListView.getFirstVisiblePosition());
		}

		public void selectItem(int position, boolean bSelected) {
			Music music = getMusic(position);
			if (music != null) {
				music.bSelected = bSelected;
				View view = getViewOfIndex(position);
				if (view != null) {
					ViewHolder holder = (ViewHolder)view.getTag();
					updateItemState(holder, music);
				}
			}
		}

		public Music getMusic(int position) {
			return position < 0 || position >= mList.size() ? null : mList.get(position);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;

			if (convertView != null) {
				holder = (ViewHolder)convertView.getTag();
			} else {
				convertView = mInflater.inflate(R.layout.item_music_menu, parent, false);
				holder = new ViewHolder(convertView, position);
				convertView.setTag(holder);
				holder.mTitleText = (TextView)convertView.findViewById(R.id.textView1);
				holder.mImageView = (ImageView)convertView.findViewById(R.id.imageView1);
			}

			Music music = mList.get(position);
			holder.mTitleText.setText(music.displayName);

			updateItemState(holder, music);

			return convertView;
		}

		private void updateItemState(ViewHolder holder, Music music) {
			if (music.bSelected) {
				holder.mView.setBackgroundColor(mSelColor);
				holder.mImageView.setVisibility(View.VISIBLE);
			} else {
				holder.mView.setBackgroundColor(Color.TRANSPARENT);
				holder.mImageView.setVisibility(View.GONE);
			}
		}

		public void setMusicList(ArrayList<Music> list) {
			mList = list;
			notifyDataSetChanged();
		}

	}

	static class ViewHolder {
		public final View mView;
		public final int mIndex;
		public TextView mTitleText;
		public ImageView mImageView;

		public ViewHolder(View view, int index) {
			mView = view;
			mIndex = index;
		}
	}
}
