/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.designcompose.testapp.helloworld;

import android.animation.Animator;
import android.content.Context;
import android.util.Log;
import android.util.DisplayMetrics;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.io.InputStream;

import com.android.designcompose.ScalableUiDoc;
import com.android.designcompose.ScalableUiDocKt;
import com.android.car.scalableui.model.Alpha;
import com.android.car.scalableui.model.Bounds;
import com.android.car.scalableui.model.KeyFrameVariant;
import com.android.car.scalableui.model.Layer;
import com.android.car.scalableui.model.PanelState;
import com.android.car.scalableui.model.Role;
import com.android.car.scalableui.model.Transition;
import com.android.car.scalableui.model.Variant;
import com.android.car.scalableui.model.Visibility;
// TODO update name to ScalableUiComponentSet?
import com.android.designcompose.common.DesignDocId;
import com.android.designcompose.definition.element.Event;
import com.android.designcompose.definition.element.ScalableUIComponentSet;
import com.android.designcompose.definition.element.ScalableUiVariant;
import com.android.designcompose.definition.element.ScalableDimension;
import com.android.designcompose.definition.element.KeyframeVariant;
import com.android.designcompose.definition.element.Keyframe;

public class PanelStateDocLoader {
    public static final long DEFAULT_TRANSITION_DURATION = 300;
    private static final String TAG = PanelStateDocLoader.class.getSimpleName();

    private Context mContext;

    public PanelStateDocLoader(Context context) {
        mContext = context;
    }

    public List<PanelState> loadPanelStates(InputStream fileStream, String docId) throws DocLoadException {
        ScalableUiDoc doc = ScalableUiDocKt.loadScalableUiDoc(fileStream, docId);
        return loadPanelStates(doc);
    }

    public List<PanelState> loadPanelStates(String dcfFilePath) throws DocLoadException {
        ScalableUiDoc doc = ScalableUiDocKt.loadScalableUiDoc(dcfFilePath, mContext);
        return loadPanelStates(doc);
    }

    private List<PanelState> loadPanelStates(ScalableUiDoc doc) throws DocLoadException {
        if (doc == null) {
            Log.e(TAG, "### Error loading scalable dcf doc");
            return null;
        }

        List<ScalableUIComponentSet> panels = doc.getPanels();
        if (panels == null || panels.isEmpty())
            throw new DocLoadException("### No panels found");

        Log.i(TAG, "### panels: " + panels.size());
        ArrayList<PanelState> states = new ArrayList(panels.size());
        int index = 0;
        for(ScalableUIComponentSet setData : panels) {
            PanelState panel = createPanelStateFromDoc(setData, doc);
            states.add(panel);
            ++index;
        }
        return states;
    }


    private PanelState createPanelStateFromDoc(ScalableUIComponentSet setData, ScalableUiDoc doc) throws DocLoadException {
        String roleName = setData.getRole();
        int role = mContext.getResources().getIdentifier(roleName, "string", mContext.getPackageName());
        PanelState result = new PanelState(setData.getName(), new Role(role));

        // Add variants
        for (String variantId : setData.getVariantIdsList()) {
            ScalableUiVariant scalableUiVariant = doc.getVariantById(variantId);
            if (scalableUiVariant == null)
                throw new DocLoadException("No variant found with id " + variantId);
            Variant variant = createVariantFromDoc(scalableUiVariant);
            result.addVariant(variant);
        }

        // Add keyframe variants
        for (KeyframeVariant kfv : setData.getKeyframeVariantsList()) {
            KeyFrameVariant keyFrameVariant = createKeyFrameVariantFromDoc(kfv, result);
            //keyFrameVariant.setPanelId(setData.getId());
            result.addVariant(keyFrameVariant);
        }

        // Add transitions
        long transitionDuration = DEFAULT_TRANSITION_DURATION; // TODO get this from doc
        Interpolator interpolator = new AccelerateDecelerateInterpolator(); // TODO get this from doc
        Map<String, com.android.designcompose.definition.element.Event> eventMap = setData.getEventMapMap();
        for (String transitionName : eventMap.keySet()) {
            com.android.designcompose.definition.element.Event event = eventMap.get(transitionName);
            if (event == null)
                throw new DocLoadException("No event found from transition " + transitionName);

            Transition transition = createTransitionFromDoc(event, result, transitionDuration, interpolator);
            result.addTransition(transition);
        }

        result.setVariant(setData.getDefaultVariantName());
        return result;
    }

    private Variant createVariantFromDoc(ScalableUiVariant variant) throws DocLoadException {
        Variant result = new Variant.Builder(variant.getName())
                .setLayer(new Layer.Builder().setLayer(variant.getLayer()).build().getLayer())
                .setVisibility(new Visibility.Builder().setIsVisible(variant.getIsVisible()).build().isVisible())
                .setAlpha(new Alpha.Builder().setAlpha(variant.getAlpha()).build().getAlpha())
                .setBounds(createBoundsFromDoc(variant).getRect())
                .build();
        return result;
    }

    private Bounds createBoundsFromDoc(ScalableUiVariant scalableUiVariant) {
        com.android.designcompose.definition.element.Bounds b = scalableUiVariant.getBounds();
        ScalableDimension dimLeft = b.getLeft();
        ScalableDimension dimTop = b.getTop();
        ScalableDimension dimWidth = b.getWidth();
        ScalableDimension dimHeight = b.getHeight();

        int left = getDimensionPixelSize(true, dimLeft);
        int top = getDimensionPixelSize(false, dimTop);
        int width = getDimensionPixelSize(true, dimWidth);
        int height = getDimensionPixelSize(false, dimHeight);

        return new Bounds.Builder()
                .setLeft(left)
                .setTop(top)
                .setWidth(width)
                .setHeight(height)
                .build();
    }

    private int getDimensionPixelSize(boolean isHorizontal, ScalableDimension dim) {
        if (dim.hasPoints()) {
            return (int) (dim.getPoints() * mContext.getResources().getDisplayMetrics().density);
        } else {
            DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
            if (isHorizontal) {
                return (int) (dim.getPercent() * displayMetrics.widthPixels / 100);
            } else {
                return (int) (dim.getPercent() * displayMetrics.heightPixels / 100);
            }
        }
    }

    private KeyFrameVariant createKeyFrameVariantFromDoc(KeyframeVariant kfv, PanelState panelState) throws DocLoadException {
        // No ID for KFV, so use name
        KeyFrameVariant.Builder result = new KeyFrameVariant.Builder(kfv.getName());
        for (Keyframe kf : kfv.getKeyframesList()) {
            int frame = kf.getFrame();
            String variantName = kf.getVariantName();
            Variant panelVariant = panelState.getVariant(variantName);
            if (panelVariant == null)
                throw new DocLoadException("Could not find variant with name " + variantName);
            result.addKeyFrame(new KeyFrameVariant.KeyFrame(frame, panelVariant));
        }
        return result.build();
    }

    private Transition createTransitionFromDoc(Event event, PanelState panelState, long duration, Interpolator interpolator) throws DocLoadException {
        Variant fromVariant = null; // TODO get from doc
        Variant toVariant = panelState.getVariant(event.getVariantName());
        if (toVariant == null) {
            throw new DocLoadException("Could not find variant with name " + event.getVariantName());
        }
        Animator animator = null; // TODO get from doc
        return new Transition.Builder(fromVariant, toVariant)
                .setAnimator(animator)
                .setDefaultDuration(duration)
                .setDefaultInterpolator(interpolator)
                .setOnEvent(event.getEventName(), null)
                .build();
    }

}

