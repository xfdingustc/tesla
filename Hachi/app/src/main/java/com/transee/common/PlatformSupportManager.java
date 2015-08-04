package com.transee.common;

import android.os.Build;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

public abstract class PlatformSupportManager<T> {

	private static final String TAG = PlatformSupportManager.class.getSimpleName();

	private final Class<T> mManagedInterface;
	private final T mDefaultImplementation;
	private final SortedMap<Integer, String> mImplementations;

	protected PlatformSupportManager(Class<T> managedInterface, T defaultImplementation) {
		if (!managedInterface.isInterface()) {
			throw new IllegalArgumentException();
		}
		if (!managedInterface.isInstance(defaultImplementation)) {
			throw new IllegalArgumentException();
		}
		this.mManagedInterface = managedInterface;
		this.mDefaultImplementation = defaultImplementation;
		this.mImplementations = new TreeMap<Integer, String>(Collections.reverseOrder());
	}

	protected final void addImplementationClass(int minVersion, String className) {
		mImplementations.put(minVersion, className);
	}

	public final T build() {
		for (Integer minVersion : mImplementations.keySet()) {
			if (Build.VERSION.SDK_INT >= minVersion) {
				String className = mImplementations.get(minVersion);
				try {
					Class<? extends T> clazz = Class.forName(className).asSubclass(mManagedInterface);
					// Log.i(TAG, "Using implmentation " + clazz + " of " + mManagedInterface + " for SDK " +
					// minVersion);
					return clazz.getConstructor().newInstance();
				} catch (ClassNotFoundException cnfe) {
					Log.w(TAG, cnfe);
				} catch (IllegalAccessException iae) {
					Log.w(TAG, iae);
				} catch (InstantiationException ie) {
					Log.w(TAG, ie);
				} catch (NoSuchMethodException nsme) {
					Log.w(TAG, nsme);
				} catch (InvocationTargetException ite) {
					Log.w(TAG, ite);
				}
			}
		}
		Log.i(TAG, "Using default implementation " + mDefaultImplementation.getClass() + " of " + mManagedInterface);
		return mDefaultImplementation;
	}
}
