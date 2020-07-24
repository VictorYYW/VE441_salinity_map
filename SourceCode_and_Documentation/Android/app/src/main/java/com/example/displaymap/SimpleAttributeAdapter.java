package com.example.displaymap;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.esri.arcgisruntime.mapping.popup.PopupField;
import com.esri.arcgisruntime.mapping.popup.PopupManager;

/** 1.1 ver
 * Custom adapter for attributes displayed in a popup view.
 */
public class SimpleAttributeAdapter extends RecyclerView.Adapter<SimpleAttributeAdapter.ViewHolder> {

    private static PopupManager mPopupManager;

    public SimpleAttributeAdapter(PopupManager popupManager) {
        mPopupManager = popupManager;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.simple_attribute_row, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // Validate
        if (mPopupManager == null) {
            return;
        }

        // Current popup field
        PopupField field = mPopupManager.getDisplayedFields().get(position);
        // Show formatted value
        holder.getValueView().setText(mPopupManager.getFormattedValue(field));

        // Field label
        if ((mPopupManager.getFieldLabel(field) == null)
                || (mPopupManager.getFieldLabel(field).length() == 0)) {
            holder.getLableView().setText(field.getFieldName());
        } else {
            holder.getLableView().setText(mPopupManager.getFieldLabel(field));
        }
    }

    @Override
    public int getItemCount() {
        int count = 0;

        if (mPopupManager != null) {
            count = mPopupManager.getDisplayedFields().size();
        }

        return count;
    }

    @Override public int getItemViewType(int position) {
        return position;
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {

        private TextView mLabelView;
        private TextView mValueView;

        public ViewHolder(View itemView) {
            super(itemView);

            // Field label
            mLabelView = (TextView) itemView.findViewById(R.id.popup_attributeview_fieldlabel);
            // Field value
            mValueView = (TextView) itemView.findViewById(R.id.popup_attributeview_fieldvalue);
        }

        public TextView getLableView() {
            return mLabelView;
        }

        public TextView getValueView() {
            return mValueView;
        }

    }
}