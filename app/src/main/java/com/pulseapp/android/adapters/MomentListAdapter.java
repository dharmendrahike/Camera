package com.pulseapp.android.adapters;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pulseapp.android.R;
import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.pulseapp.android.modelView.CustomMomentModel;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.RoundedCornerTransformation;
import com.squareup.picasso.Picasso;

import java.util.LinkedHashMap;

/**
 * Created by user on 4/22/2016.
 */
public class MomentListAdapter extends HeaderRecyclerViewAdapterV2 implements FireBaseKEYIDS {


    // View Type for Regular rows
    private static final int ITEM_VIEW_TYPE_REGULAR = 1;
    private static final String TAG = "MomentListAdapter";
    //    private boolean comingFromPublicContribution;
    //    private boolean isCheckDoneOnPublicStream;
//    private boolean isAutoCheckDoneOnInstitutionStream;
    private Context mContext;
    private LinkedHashMap<String, CustomMomentModel> modelMap;
    private ViewControlsCallback viewControlsCallback;

    private class MomentListViewHolder extends RecyclerView.ViewHolder {

        private ImageView momentImage;
        private TextView momentName;
        private AppCompatCheckBox compatCheckBox;
        private View itemView;
        private LinearLayout settingView;
        private TextView viewer;

        MomentListViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            momentImage = (ImageView) itemView.findViewById(R.id.momentImage);
            settingView = (LinearLayout) itemView.findViewById(R.id.settingView);
            momentName = (TextView) itemView.findViewById(R.id.momentName);
            viewer = (TextView) itemView.findViewById(R.id.viewers);
            compatCheckBox = (AppCompatCheckBox) itemView.findViewById(R.id.itemRadio);
        }
    }

    private CustomMomentModel getMomentByIndex(int index) {
        String[] keySet = modelMap.keySet().toArray(new String[modelMap.size()]);
        return modelMap.get(keySet[index]);
    }

    public MomentListAdapter(Fragment fragment, Context context, LinkedHashMap<String, CustomMomentModel> modelMap/*, boolean comingFromPublicContribution*/) {
        mContext = context;
        this.modelMap = modelMap;
        viewControlsCallback = (ViewControlsCallback) fragment;
//        this.comingFromPublicContribution = comingFromPublicContribution;
    }

    public LinkedHashMap<String, CustomMomentModel> getModelMap() {
        return modelMap;
    }

    @Override
    public boolean useHeader() {
        return false;
    }

    @Override
    public RecyclerView.ViewHolder onCreateHeaderViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindHeaderView(RecyclerView.ViewHolder holder, int position) {

    }

    @Override
    public boolean useFooter() {
        return false;
    }

    @Override
    public RecyclerView.ViewHolder onCreateFooterViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindFooterView(RecyclerView.ViewHolder holder, int position) {

    }

    @Override
    public RecyclerView.ViewHolder onCreateBasicItemViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.moment_list_item, parent, false);
        return new MomentListViewHolder(view);
    }

    @Override
    public void onBindBasicItemView(final RecyclerView.ViewHolder holder, final int position) {
        final CustomMomentModel customMomentModel = getMomentByIndex(position);
        ((MomentListViewHolder) holder).momentName.setText(customMomentModel.name);
        if (AppLibrary.checkStringObject(customMomentModel.thumbnailUrl) != null) {
            Picasso.with(mContext).load(customMomentModel.thumbnailUrl)
                    .fit().centerCrop().transform(new RoundedCornerTransformation(customMomentModel.momentId, 5))
                    .into(((MomentListViewHolder) holder).momentImage);
        }
        if (customMomentModel.totalViews != 0) {
            ((MomentListViewHolder) holder).viewer.setText(String.valueOf(customMomentModel.totalViews) + " Views");
            ((MomentListViewHolder) holder).settingView.setVisibility(View.VISIBLE);
        } else {
            ((MomentListViewHolder) holder).settingView.setVisibility(View.GONE);
            ((RelativeLayout.LayoutParams) ((MomentListViewHolder) holder).momentName.getLayoutParams()).addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        }
        ((MomentListViewHolder) holder).itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                onMomentSelected(holder, customMomentModel);
                customMomentModel.isChecked = !customMomentModel.isChecked;
                ((MomentListViewHolder) holder).compatCheckBox.setChecked(customMomentModel.isChecked);
                viewControlsCallback.onMomentToggled();
//                notifyDataSetChanged();
//                notifyItemChanged();
//                notifyItemChanged(position);
            }
        });

        ((MomentListViewHolder) holder).compatCheckBox.setChecked(customMomentModel.isChecked);
        if (customMomentModel.isChecked){
            viewControlsCallback.onMomentToggled();

        }
//        viewControlsCallback.onMomentToggled();

//        if (comingFromPublicContribution && !checked) {
//            checked = true;
//            onMomentSelected(holder, customMomentModel);
//        } else {
//            customMomentModel.isThisMyInstitution = BaseFragment.getBaseFragmentInstance().isThisMyInstitutionId(customMomentModel.momentId);
//
//            if (!checked && customMomentModel.isThisMyInstitution) {
//                onMomentSelected(holder, customMomentModel);
//                checked = true;
//            }
//        }
    }

//    private boolean checked;

//    private void onMomentSelected(RecyclerView.ViewHolder holder, CustomMomentModel customMomentModel) {
//        if (((MomentListViewHolder) holder).compatCheckBox.isChecked()) {
//            ((MomentListViewHolder) holder).compatCheckBox.setChecked(false);
//        } else {
//            ((MomentListViewHolder) holder).compatCheckBox.setChecked(true);
//        }

//        boolean isSelected = !((MomentListViewHolder) holder).compatCheckBox.isChecked();
//        ((MomentListViewHolder) holder).compatCheckBox.setChecked(isSelected);
//        viewControlsCallback.onMomentSelected(customMomentModel.momentId, CUSTOM_MOMENT, isSelected);
//        ((MomentListViewHolder) holder).compatCheckBox.setChecked(customMomentModel.isChecked);
//        viewControlsCallback.onMomentToggled();
//    }

    @Override
    public int getBasicItemCount() {
        return modelMap.size();
    }

    @Override
    public int getBasicItemType(int position) {
        return ITEM_VIEW_TYPE_REGULAR;
    }

    public interface ViewControlsCallback {
//        void onMomentSelected(String momentId, int source, boolean selected);

        void onMomentToggled();

        void onSettingsSelected(View view);
    }

}
