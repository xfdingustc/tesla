package com.transee.common;



import com.waylens.hachi.vdb.RawDataBlock;

import java.util.ArrayList;

public class GPSPath {

	static public class Segment {
		public final RawDataBlock.RawDataBlockHeader mHeader;
		public final double[] mLatArray;
		public final double[] mLngArray;
		public final byte[] mSepArray;
		public final int mNumPoints;
		public final long mStartTimeMs;

		public Segment(RawDataBlock.RawDataBlockHeader header, double[] latArray, double[] lngArray, byte[] sepArray,
				long startTimeMs) {
			mHeader = header;
			mLatArray = latArray;
			mLngArray = lngArray;
			mSepArray = sepArray;
			mNumPoints = latArray.length;
			mStartTimeMs = startTimeMs;
		}
	}

	final ArrayList<double[]> mLatList = new ArrayList<double[]>();
	final ArrayList<double[]> mLngList = new ArrayList<double[]>();

}
