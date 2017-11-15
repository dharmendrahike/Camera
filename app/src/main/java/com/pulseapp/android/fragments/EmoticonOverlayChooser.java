package com.pulseapp.android.fragments;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.widget.GridView;

//import com.instalively.android.adapters.GridViewAdapter;
import com.pulseapp.android.util.EmoticonTypeScroller;


public class EmoticonOverlayChooser extends Fragment implements EmoticonTypeScroller {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private GridView mGridView;

    private OnFragmentInteractionListener mListener;

    private int mPosition;

//    static int [] mPositionIdMap = {R.array.smileys, R.array.lifestyle, R.array.travel};

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment EmoticonOverlay.
     */
/*    // TODO: Rename and change types and number of parameters
    public static EmoticonOverlayChooser newInstance(String param1, String param2) {
        EmoticonOverlayChooser fragment = new EmoticonOverlayChooser();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }*/

    public EmoticonOverlayChooser() {
        mPosition = 0;
    }

//    public EmoticonOverlayChooser(int position) {
//        mPosition = position;
//    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
/*        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }*/
    }

//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        // Inflate the layout for this fragment
//        View rootView = inflater.inflate(R.layout.fragment_overlay_emoticon, container, false);
//        GridView grid = (GridView) rootView.findViewById(R.id.gridView);
//        grid.setAdapter(new GridViewAdapter(getActivity(), EmoticonOverlayChooser.mPositionIdMap[mPosition]));
//        return rootView;
//    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

}
