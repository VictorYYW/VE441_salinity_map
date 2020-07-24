package com.example.displaymap;


import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.esri.arcgisruntime.mapping.popup.Popup;
import com.esri.arcgisruntime.mapping.popup.PopupManager;

/** 1.1 ver
 * Fragment to provide a popup view that contains a title view, a scrollable
 * attribute/description view
 */
public class SimplePopupFragment extends Fragment {

    private PopupManager mPopupManager;

    private static final String TAG = SimplePopupFragment.class.getSimpleName();

    public static SimplePopupFragment newInstance() {
        SimplePopupFragment fragment = new SimplePopupFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (!checkPopupManager()) {
            return null;
        }
        View view = inflater.inflate(R.layout.simple_popup_view, container, false);
        initPopupView(view);

        return view;
    }

    /**
     * Sets PopupManager which is a must have property of SimplePopupFragment.
     * @param popup
     */
    public void setPopupManager(Context context, Popup popup) {
        mPopupManager = new PopupManager(context, popup);
    }

    /**
     * Checks if PopupManager has been set. Logs a message if it hasn't.
     *
     * return
     */
    private boolean checkPopupManager() {
        if (mPopupManager == null) {
            Log.w(TAG, "Need to set PopupManager before showing the popup view!");
            return false;
        }
        return true;
    }

    /**
     * Initializes the popup view
     *
     * @param popupView
     */
    private void initPopupView(View popupView) {
        if (mPopupManager == null) {
            return;
        }

        // Initializes each individual component view
        initTitleView(popupView);
        initAttributeView(popupView);
    }

    /**
     * Initializes title view including symbol image, popup title and
     * edit summary if there is one.
     *
     * @param popupView
     */
    private void initTitleView(View popupView) {
        // Popup title
        TextView titleView = (TextView) popupView.findViewById(R.id.simple_titleview_title);
        titleView.setText(mPopupManager.getTitle());
        // Edit summary
        TextView editInfoView = (TextView) popupView.findViewById(R.id.simple_titleview_editinfo);
        editInfoView.setText(mPopupManager.getEditSummary());
    }

    /**
     * Constructs SimpleAttributeAdapter and initializes attribute view.
     *
     * @param popupView
     */
    private void initAttributeView(View popupView) {
        RecyclerView attrView = (RecyclerView) popupView.findViewById(R.id.simple_attribute_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        attrView.setLayoutManager(linearLayoutManager);

        if ((mPopupManager.getCustomHtmlDescription() == null) || (mPopupManager.getCustomHtmlDescription().length()== 0)) {
            // Showing attributes
            WebView webView = (WebView) popupView.findViewById(R.id.simple_description_view);
            webView.setVisibility(View.GONE);
            attrView.setVisibility(View.VISIBLE);

            attrView.setHasFixedSize(true);
            SimpleAttributeAdapter adapter = new SimpleAttributeAdapter(mPopupManager);
            attrView.setAdapter(adapter);
        } else {
            // Showing custom description
            attrView.setVisibility(View.GONE);
            WebView webView = (WebView) popupView.findViewById(R.id.simple_description_view);
            webView.loadData(mPopupManager.getCustomHtmlDescription(), "text/html; charset=utf-8", null);
            webView.setVisibility(View.VISIBLE);
        }
    }


}
