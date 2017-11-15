package com.pulseapp.android.adapters;

/**
 * Created by deepankur on 14/4/16.
 */
//this interface doesn't take just clicks but is used for general commmunication
public interface RecyclerViewClickInterface {
    //we might/might not pass position through the first parameter of interface
    void onItemClick(int extras, Object data);

}
