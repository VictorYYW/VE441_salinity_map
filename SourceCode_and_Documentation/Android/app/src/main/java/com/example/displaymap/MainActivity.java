package com.example.displaymap;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.ArcGISFeature;
import com.esri.arcgisruntime.data.FeatureEditResult;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.GeoElement;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.popup.Popup;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.IdentifyLayerResult;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.portal.Portal;
import com.esri.arcgisruntime.portal.PortalItem;

import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    private ArcGISFeature selectedFeature;
    private ServiceFeatureTable featureTable;

    /** 1.1 ver
     *  A custom OnTouchListener on MapView to identify geo-elements when users single tap
     *  on a map view.
     */
    private class MyTouchListener extends DefaultMapViewOnTouchListener{
        private MapView mapView;
        private Context context;

        /*
         * Constructs a DefaultMapViewOnTouchListener with the given Context and MapView.
         * @param context the context from which this is being created
         * @param mapView the Mapview with which to interact
         * @param since 100.0.0
         */
        public MyTouchListener(Context context, MapView mapView) {
            super(context, mapView);

            this.context = context;
            this.mapView = mapView;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            // Validation
            if (mapView == null){
                return super.onSingleTapConfirmed(e);
            }

            // Obtain GeoElements, and create popup
            android.graphics.Point screenPoint = new android.graphics.Point((int) e.getX(), (int) e.getY());
            doIdentify(mapView, screenPoint);

            return true;
        }

        /**
         * Creates popup for the first GeoElement obtained from identify.
         *
         * @param mapView
         * @param screenPoint
         */
        private void doIdentify(final MapView mapView, final android.graphics.Point screenPoint) {
            // Identify all layers in the map view
            final ListenableFuture<List<IdentifyLayerResult>> future = mapView.identifyLayersAsync(
                    screenPoint, 10.0, false, 3
            );
            future.addDoneListener(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Obtain identify results
                        List<IdentifyLayerResult> results = future.get();
                        if ((results == null) || (results.isEmpty())){
                            Log.i(TAG, "Null or empty result from identify. ");
                            return;
                        }

                        List<GeoElement> geoElements = results.get(0).getElements();
                        if ((geoElements == null) || (geoElements.isEmpty())) {
                            Log.i(TAG, "Null or empty element from identify. ");
                        }

                        if (geoElements.get(0) instanceof ArcGISFeature) {
                            selectedFeature = (ArcGISFeature) geoElements.get(0);
                            // show callout with the value for the attribute "typdamage" of the selected feature
                            Map<String, Object> attributes = selectedFeature.getAttributes();
                        }
                        // Create popup
                        Intent myIntent = new Intent(MainActivity.this, PopupActivity.class);
                        startActivityForResult(myIntent, 100);
                    } catch (Exception ex){
                        Log.i(TAG, "exception in identify: " + ex.getMessage());
                    }
                }
            });
        }
    }

    /**
     * Function to read the result from newly created activity
     */
    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 100) {
            // display progress dialog while updating attribute callout
            //mProgressDialog.show();
            updateAttributes(data.getStringExtra("salinity"));
        }
    }
    /**
     * Applies changes to the feature, Service Feature Table, and server.
     */
    private void updateAttributes(final String salinity) {

        // load the selected feature
        selectedFeature.loadAsync();

        // update the selected feature
        selectedFeature.addDoneLoadingListener(new Runnable() {
            @Override public void run() {
                if (selectedFeature.getLoadStatus() == LoadStatus.FAILED_TO_LOAD) {
                    Log.e(TAG, "Error while loading feature");
                }

                // update the Attributes map with the new selected value for "typdamage"
                selectedFeature.getAttributes().put("salinity", salinity);

                try {
                    // update feature in the feature table
                    ListenableFuture<Void> mapViewResult = featureTable.updateFeatureAsync(selectedFeature);
                    /*mServiceFeatureTable.updateFeatureAsync(mSelectedArcGISFeature).addDoneListener(new Runnable() {*/
                    mapViewResult.addDoneListener(new Runnable() {
                        @Override
                        public void run() {
                            // apply change to the server
                            final ListenableFuture<List<FeatureEditResult>> serverResult = featureTable.applyEditsAsync();

                            serverResult.addDoneListener(new Runnable() {
                                @Override
                                public void run() {
                                    try {

                                        // check if server result successful
                                        List<FeatureEditResult> edits = serverResult.get();
                                        if (!edits.isEmpty()) {
                                            if (!edits.get(0).hasCompletedWithErrors()) {
                                                Log.e(TAG, "Feature successfully updated");
                                                //mSnackbarSuccess.show();
                                                //mFeatureUpdated = true;
                                            }
                                        } else {
                                            Log.e(TAG, "The attribute type was not changed");
                                            //mSnackbarFailure.show();
                                            //mFeatureUpdated = false;
                                        }
                                        /*
                                        if (mProgressDialog.isShowing()) {
                                            mProgressDialog.dismiss();
                                            // display the callout with the updated value
                                            showCallout((String) mSelectedArcGISFeature.getAttributes().get("typdamage"));
                                        }

                                         */
                                    } catch (Exception e) {
                                        Log.e(TAG, "applying changes to the server failed: " + e.getMessage());
                                    }
                                }
                            });
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "updating feature in the feature table failed: " + e.getMessage());
                }
            }
        });
    }
    /**
     * Creates a popup view for a given popup.
     * @param popup
     */
    private void createPopupView(Popup popup){
        // Create a popup view
        SimplePopupFragment fragment = SimplePopupFragment.newInstance();
        fragment.setPopupManager(MainActivity.this, popup);
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, fragment, "popup fragment")
                .addToBackStack("popup fragment")
                .commit();
    }


    private final String TAG = MainActivity.class.getSimpleName();

    private MapView mMapView;

//    private void addTrailheadsLayer() {
//        // String url = "https://services9.arcgis.com/RiYZ8nZnTVvmpV8H/ArcGIS/rest/services/trailheads/FeatureServer/0";
//        String url = "https://services9.arcgis.com/RiYZ8nZnTVvmpV8H/arcgis/rest/services/sample_data/FeatureServer/0";
//        ServiceFeatureTable serviceFeatureTable = new ServiceFeatureTable(url);
//        FeatureLayer featureLayer = new FeatureLayer(serviceFeatureTable);
//        ArcGISMap map = mMapView.getMap();
//        map.getOperationalLayers().add(featureLayer);
//    }

    private void setupMap() {
        if (mMapView != null) {
//            Basemap.Type basemapType = Basemap.Type.IMAGERY_WITH_LABELS;
//            double latitude = 34.0270;
//            double longitude = -118.8050;
//            int levelOfDetail = 5;
//            ArcGISMap map = new ArcGISMap(basemapType, latitude, longitude, levelOfDetail);
//            mMapView.setMap(map);
            String itemId = "c99d7de508614ee98eb9ed21759d4064";
            Portal portal = new Portal("https://www.arcgis.com", false);
            PortalItem portalItem = new PortalItem(portal, itemId);
            ArcGISMap map = new ArcGISMap(portalItem);
            // set initial viewpoint to a specific region
            map.setInitialViewpoint(
                    new Viewpoint(new Point(-10977012.785807, 4514257.550369, SpatialReference.create(3857)), 68015210));
            mMapView.setMap(map);

            // add a listener to detect taps on the map view
            /* 1.0 ver
            */
//            mMapView.setOnTouchListener(new DefaultMapViewOnTouchListener(MainActivity.this, mMapView) {
//                @Override public boolean onSingleTapConfirmed(MotionEvent e) {
//                    android.graphics.Point screenPoint = new android.graphics.Point(Math.round(e.getX()),
//                            Math.round(e.getY()));
//                    identifyResult(screenPoint);
//                    return true;
//                }
//            });

            /* 1.1 ver
             */
            mMapView.setOnTouchListener(new MyTouchListener(this, mMapView));
        }
    }

    /** 1.0 ver
     * Performs an identify on layers at the given screenpoint and calls handleIdentifyResults(...) to process them.
     *
     * @param screenPoint in Android graphic coordinates.
     */
    private void identifyResult(android.graphics.Point screenPoint) {

        final ListenableFuture<List<IdentifyLayerResult>> identifyLayerResultsFuture = mMapView
                .identifyLayersAsync(screenPoint, 12, false, 10);

        identifyLayerResultsFuture.addDoneListener(new Runnable() {
            @Override public void run() {
                try {
                    List<IdentifyLayerResult> identifyLayerResults = identifyLayerResultsFuture.get();
                    handleIdentifyResults(identifyLayerResults);
                } catch (InterruptedException | ExecutionException e) {
                    Log.e(TAG, "Error identifying results: " + e.getMessage());
                }
            }
        });
    }

    /** 1.0 ver
     * Processes identify results into a string which is passed to showAlertDialog(...).
     *
     * @param identifyLayerResults a list of identify results generated in identifyResult().
     */
    private void handleIdentifyResults(List<IdentifyLayerResult> identifyLayerResults) {
        StringBuilder message = new StringBuilder();
        int totalCount = 0;
        for (IdentifyLayerResult identifyLayerResult : identifyLayerResults) {
            int count = geoElementsCountFromResult(identifyLayerResult);
            String layerName = identifyLayerResult.getLayerContent().getName();
            message.append(layerName).append(": ").append(count);

            // add new line character if not the final element in array
            if (!identifyLayerResult.equals(identifyLayerResults.get(identifyLayerResults.size() - 1))) {
                message.append("\n");
            }
            totalCount += count;
        }

        // if any elements were found show the results, else notify user that no elements were found
        if (totalCount > 0) {
            showAlertDialog(message);
        } else {
            Toast.makeText(this, "No element found", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "No element found.");
        }
    }

    /** 1.0 ver
     * Gets a count of the GeoElements in the passed result layer.
     *
     * @param result from a single layer.
     * @return the total count of GeoElements.
     */
    private int geoElementsCountFromResult(IdentifyLayerResult result) {
        // create temp array
        List<IdentifyLayerResult> tempResults = new ArrayList<>();
        tempResults.add(result);

        // using Depth First Search approach to handle recursion
        int count = 0;
        int index = 0;

        while (index < tempResults.size()) {
            // get the result object from the array
            IdentifyLayerResult identifyResult = tempResults.get(index);

            // update count with geoElements from the result
            count += identifyResult.getElements().size();

            // if sublayer has any results, add result objects in the tempResults array after the current result
            if (identifyResult.getSublayerResults().size() > 0) {
                tempResults.add(identifyResult.getSublayerResults().get(index));
            }

            // update the count and repeat
            index += 1;
        }
        return count;
    }

    /** 1.0 ver
     * Shows message in an AlertDialog.
     *
     * @param message contains identify results processed into a string.
     */
    private void showAlertDialog(StringBuilder message) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set title
        alertDialogBuilder.setTitle("Number of elements found");

        // set dialog message
        alertDialogBuilder
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int id) {
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show the alert dialog
        alertDialog.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMapView = findViewById(R.id.mapView);
        ArcGISRuntimeEnvironment.setLicense(getResources().getString(R.string.arcgis_license_key));
        setupMap();
        featureTable = new ServiceFeatureTable(getResources().getString(R.string.sample_service_url));

        // addTrailheadsLayer();
    }

    @Override
    protected void onPause(){
        if (mMapView != null) {
            mMapView.pause();
        }
        super.onPause();
    }

    @Override
    protected void onResume(){
        super.onResume();
        if (mMapView != null) {
            mMapView.resume();
        }
    }

    @Override
    protected void onDestroy() {
        if (mMapView != null) {
            mMapView.dispose();
        }
        super.onDestroy();
    }
}