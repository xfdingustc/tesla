package com.transee.viditcam.app;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.transee.ccam.Camera;
import com.transee.ccam.CameraManager;
import com.transee.ccam.CameraManager.WifiItem;
import com.transee.ccam.CameraState;
import com.transee.ccam.WifiState;
import com.transee.common.OverlayImageView;
import com.transee.common.Timer;
import com.waylens.hachi.R;

import java.util.List;

abstract public class CameraListAdapter extends BaseAdapter {

	abstract public void onCameraConnected(Camera camera);

	abstract public void onCameraDisconnected(Camera camera);

	abstract public void onClickDropDown(View view, int position);

	abstract public void onClickFolder(View view, int position);

	static final boolean DEBUG = false;
	static final String TAG = "CameraListAdapter";

	private Context mContext;
	private ListView mListView;
	private CameraManager mCameraManager;
	private CameraManager.Callback mCameraManagerCallback;
	private LayoutInflater mLayoutInflater;
	private Timer mShowRecordTimer;

	public CameraListAdapter(Context context, CameraManager cameraManager, ListView listView) {
		mContext = context;
		mLayoutInflater = LayoutInflater.from(context);
		mListView = listView;
		mCameraManager = cameraManager;
		mCameraManagerCallback = new CameraManagerCallback();
		mCameraManager.addCallback(mCameraManagerCallback);
	}

	// API
	public void stopAndClear() {
		if (mCameraManager != null) {
			mCameraManager.removeCallback(mCameraManagerCallback);
			mCameraManager.clearAll();
			mCameraManager = null;
		}
		if (mShowRecordTimer != null) {
			mShowRecordTimer.cancel();
			mShowRecordTimer = null;
		}
	}

	// API
	public void connectCamera(Camera.ServiceInfo serviceInfo) {
		if (mCameraManager != null) {
			mCameraManager.connectCamera(serviceInfo);
		}
	}

	// API
	public void filterScanResult(List<ScanResult> list) {
		if (mCameraManager != null) {
			mCameraManager.filterScanResult(list);
		}
	}

	// API
	public int getConnectedCameras() {
		return mCameraManager == null ? 0 : mCameraManager.getConnectedCameras().size();
	}

	// API
	public Camera getCamera(int position) {
		if (mCameraManager == null)
			return null;

		List<Camera> list1 = mCameraManager.getConnectedCameras();
		List<Camera> list2 = mCameraManager.getConnectingCameras();

		int index = position;
		if (index < 0)
			return null;

		if (index < list1.size())
			return list1.get(index);

		index -= list1.size();
		if (index < list2.size())
			return list2.get(index);

		return null;
	}

	// API
	public Camera findConnectedCamera(String ssid, String hostString) {
		if (mCameraManager == null)
			return null;
		return mCameraManager.findCameraById(ssid, hostString);
	}

	// API
	public Camera isCameraConnected(Camera camera) {
		return findConnectedCamera(camera.getSSID(), camera.getHostString());
	}

	// API
	public boolean removeConnectedCamera(Camera camera) {
		if (mCameraManager != null) {
			if (mCameraManager.removeConnectedCamera(camera)) {
				notifyDataSetChanged();
				return true;
			}
		}
		return false;
	}

	// API
	public CameraManager.WifiItem getWifiItem(int position) {
		if (mCameraManager == null)
			return null;

		List<Camera> list1 = mCameraManager.getConnectedCameras();
		List<Camera> list2 = mCameraManager.getConnectingCameras();
		List<WifiItem> list3 = mCameraManager.getWifiList();

		int numCameras = list1.size() + list2.size();
		int index = position;
		if (index < numCameras) {
			return null;
		}

		index -= numCameras;
		if (index >= list3.size())
			return null;

		return list3.get(index);
	}

	@Override
	public int getCount() {
		return mCameraManager == null ? 0 : mCameraManager.getTotalItems();
	}

	@Override
	public Object getItem(int position) {
		return mCameraManager == null ? null : mCameraManager.getItem(position);
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	private final View.OnClickListener mOnClickDropDown = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			ViewHolder holder = (ViewHolder)v.getTag();
			onClickDropDown(v, holder.position);
		}
	};

	private final View.OnClickListener mOnClickFolder = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			ViewHolder holder = (ViewHolder)v.getTag();
			onClickFolder(v, holder.position);
		}
	};

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null;
		if (convertView != null) {
			holder = (ViewHolder)convertView.getTag();
		} else {
			convertView = mLayoutInflater.inflate(R.layout.item_camera_list, parent, false);
			holder = new ViewHolder();
			holder.imageView1 = (OverlayImageView)convertView.findViewById(R.id.imageView1);
			holder.imageView2 = (ViditImageButton)convertView.findViewById(R.id.imageView2);
			holder.imageView4 = (ViditImageButton)convertView.findViewById(R.id.imageView4);
			holder.imageView2.setTag(holder);
			holder.imageView2.setOnClickListener(mOnClickDropDown);
			holder.imageView4.setOnClickListener(mOnClickFolder);
			holder.imageView4.setTag(holder);
			holder.textView1 = (TextView)convertView.findViewById(R.id.dialogTitle);
			holder.textView2 = (TextView)convertView.findViewById(R.id.textCameraName);
			convertView.setTag(holder);
		}
		initView(convertView, holder, position);
		return convertView;
	}

	private void initView(View view, ViewHolder holder, int position) {
		boolean bPcServer = false;
		holder.position = position;
		if (mCameraManager == null) {
			initEmptyView(holder, position);
		} else {
			List<Camera> list1 = mCameraManager.getConnectedCameras();
			List<Camera> list2 = mCameraManager.getConnectingCameras();
			List<WifiItem> list3 = mCameraManager.getWifiList();
			int index = position;
			if (index < 0) {
				initEmptyView(holder, position);
			} else if (index < list1.size()) {
				// connected camera
				Camera camera = list1.get(index);
				if (camera.isPcServer()) {
					holder.imageView1.setImageResource(R.drawable.viditpcc);
					holder.textView1.setText(camera.getServerName());
					holder.textView2.setText(camera.getSSID());
					bPcServer = true;
				} else {
					holder.imageView1.setImageResource(R.drawable.camera_active);
					if (Camera.getCameraStates(camera).mRecordState == CameraState.State_Record_Recording) {
						holder.imageView1.enableOverlay(true);
						holder.imageView1.setOverlayAlpha(255);
						setUpdateTimer();
					} else {
						holder.imageView1.disableOverlay();
					}

					holder.imageView2.setVisibility(View.VISIBLE);
					holder.imageView4.setVisibility(View.VISIBLE);

					String name = Camera.getCameraStates(camera).mCameraName;
					if (name.length() == 0) {
						name = mContext.getResources().getString(R.string.lable_camera_noname);
					}
					holder.textView1.setText(name);
					holder.textView2.setText(camera.getSSID());
					String mode = null;
					switch (Camera.getWifiStates(camera).mWifiMode) {
					case WifiState.WIFI_Mode_AP:
						mode = "AP";
						// holder.textView2.setCompoundDrawablesWithIntrinsicBounds(R.drawable.wifi_ap,
						// 0, 0, 0);
						break;
					case WifiState.WIFI_Mode_Client:
						// mode = "Client";
						// holder.textView2.setCompoundDrawablesWithIntrinsicBounds(R.drawable.wifi_client,
						// 0, 0, 0);
						break;
					default:
						// holder.textView2.setCompoundDrawablesWithIntrinsicBounds(0,
						// 0, 0, 0);
						break;
					}
					if (mode != null) {
						holder.textView2.setText("(" + mode + ") " + holder.textView2.getText());
					}
				}
			} else {
				holder.textView2.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
				index -= list1.size();
				if (index < list2.size()) {
					// connecting camera
					Camera camera = list2.get(index);
					holder.imageView1.setImageResource(R.drawable.camera_inactive);
					holder.imageView1.disableOverlay();
					holder.imageView2.setVisibility(View.VISIBLE);
					holder.imageView4.setVisibility(View.GONE);
					holder.textView1.setText(R.string.lable_camera_connecting);
					holder.textView2.setText(camera.getSSID());
				} else {
					index -= list2.size();
					if (index < list3.size()) {
						CameraManager.WifiItem item = list3.get(index);
						holder.imageView1.setImageResource(R.drawable.camera_inactive);
						holder.imageView1.disableOverlay();
						holder.imageView2.setVisibility(View.VISIBLE);
						holder.imageView4.setVisibility(View.GONE);
						holder.textView1.setText(R.string.lable_camera_disconnected);
						holder.textView2.setText(item.mSSID);
					} else {
						initEmptyView(holder, position);
					}
				}
			}
		}
		if (bPcServer) {
			holder.imageView2.setVisibility(View.INVISIBLE);
			holder.imageView4.setVisibility(View.INVISIBLE);
		} else {
			holder.imageView2.setVisibility(View.VISIBLE);
			holder.imageView4.setVisibility(View.VISIBLE);
		}
	}

	private void initEmptyView(ViewHolder holder, int position) {
		// TODO
	}

	private void setUpdateTimer() {
		if (mShowRecordTimer == null) {
			mShowRecordTimer = new Timer() {
				@Override
				public void onTimer(Timer timer) {
					if (timer == mShowRecordTimer) {
						onUpdateTimer();
					}
				}
			};
			mShowRecordTimer.run(1000);
		}
	}

	private final View getViewOfIndex(int index) {
		return mListView.getChildAt(index - mListView.getFirstVisiblePosition());
	}

	private void onUpdateTimer() {
		int first = mListView.getFirstVisiblePosition();
		int last = mListView.getLastVisiblePosition();
		int n = 0;
		for (int index = first; index <= last; index++) {
			View view = getViewOfIndex(index);
			if (view != null) {
				ViewHolder holder = (ViewHolder)view.getTag();
				OverlayImageView iv = holder.imageView1;
				if (iv.isOverlayEnabled()) {
					if (iv.getOverlayAlpha() == 255) {
						if (++iv.mCounter > 5) {
							iv.mCounter = 0;
							iv.setOverlayAlpha(0);
						}
					} else {
						iv.setOverlayAlpha(255);
					}
					n++;
				}
			}
		}
		if (n > 0) {
			mShowRecordTimer.run(200);
		} else {
			mShowRecordTimer.cancel();
			mShowRecordTimer = null;
		}
	}

	private void onCameraStateChanged(Camera camera) {
		int index = mCameraManager.findCameraIndex(camera);
		if (index >= 0) {
			View view = getViewOfIndex(index);
			if (view != null) {
				initView(view, (ViewHolder)view.getTag(), index);
			}
		}
	}

	final static class ViewHolder {
		int position;
		OverlayImageView imageView1;
		ViditImageButton imageView2;
		ViditImageButton imageView4;
		TextView textView1;
		TextView textView2;
	}

	class CameraManagerCallback implements CameraManager.Callback {

		@Override
		public void onCameraConnecting(CameraManager manager, Camera camera) {
			if (DEBUG) {
				Log.d(TAG, "connecting camera");
			}
			if (manager == mCameraManager) {
				notifyDataSetChanged();
			}
		}

		@Override
		public void onCameraConnected(CameraManager manager, Camera camera) {
			if (DEBUG) {
				Log.d(TAG, "camera connected");
			}
			if (manager == mCameraManager) {
				notifyDataSetChanged(); // TODO : optimize
				CameraListAdapter.this.onCameraConnected(camera);
			}
		}

		@Override
		public void onCameraDisconnected(CameraManager manager, Camera camera) {
			if (DEBUG) {
				Log.d(TAG, "camera disconnected");
			}
			if (manager == mCameraManager) {
				notifyDataSetChanged();
			}
		}

		@Override
		public void onCameraStateChanged(CameraManager manager, Camera camera) {
			if (DEBUG) {
				Log.d(TAG, "onCameraStateChanged");
			}
			if (manager == mCameraManager) {
				CameraListAdapter.this.onCameraStateChanged(camera);
			}
		}

		@Override
		public void onWifiListChanged(CameraManager manager) {
			if (DEBUG) {
				Log.d(TAG, "wifi list changed");
			}
			if (manager == mCameraManager) {
				notifyDataSetChanged();
			}
		}

	}
}
