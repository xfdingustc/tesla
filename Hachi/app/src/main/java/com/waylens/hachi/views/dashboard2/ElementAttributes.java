package com.waylens.hachi.views.dashboard2;

import android.util.AttributeSet;

import com.waylens.hachi.skin.Element;

/**
 * Created by Xiaofei on 2015/11/20.
 */
public class ElementAttributes implements AttributeSet {

    private final Element mElement;

    public ElementAttributes(Element element) {
        this.mElement = element;
    }

    @Override
    public int getAttributeCount() {
        return mElement.getAttributeCount();
    }

    @Override
    public String getAttributeName(int index) {
        return mElement.getAttributeName(index);
    }

    @Override
    public String getAttributeValue(int index) {
        return mElement.getAttributeValue(index);
    }

    @Override
    public String getAttributeValue(String namespace, String name) {
        return null;
    }

    @Override
    public String getPositionDescription() {
        return null;
    }

    @Override
    public int getAttributeNameResource(int index) {
        return 0;
    }

    @Override
    public int getAttributeListValue(String namespace, String attribute, String[] options, int defaultValue) {
        return 0;
    }

    @Override
    public boolean getAttributeBooleanValue(String namespace, String attribute, boolean defaultValue) {
        return false;
    }

    @Override
    public int getAttributeResourceValue(String namespace, String attribute, int defaultValue) {
        return 0;
    }

    @Override
    public int getAttributeIntValue(String namespace, String attribute, int defaultValue) {
        return 0;
    }

    @Override
    public int getAttributeUnsignedIntValue(String namespace, String attribute, int defaultValue) {
        return 0;
    }

    @Override
    public float getAttributeFloatValue(String namespace, String attribute, float defaultValue) {
        return 0;
    }

    @Override
    public int getAttributeListValue(int index, String[] options, int defaultValue) {
        return 0;
    }

    @Override
    public boolean getAttributeBooleanValue(int index, boolean defaultValue) {
        return false;
    }

    @Override
    public int getAttributeResourceValue(int index, int defaultValue) {
        return 0;
    }

    @Override
    public int getAttributeIntValue(int index, int defaultValue) {
        return 0;
    }

    @Override
    public int getAttributeUnsignedIntValue(int index, int defaultValue) {
        return 0;
    }

    @Override
    public float getAttributeFloatValue(int index, float defaultValue) {
        return 0;
    }

    @Override
    public String getIdAttribute() {
        return null;
    }

    @Override
    public String getClassAttribute() {
        return null;
    }

    @Override
    public int getIdAttributeResourceValue(int defaultValue) {
        return 0;
    }

    @Override
    public int getStyleAttribute() {
        return 0;
    }
}
