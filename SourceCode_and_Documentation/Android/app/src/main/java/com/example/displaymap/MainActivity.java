package com.example.displaymap;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.IdentifyLayerResult;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.portal.Portal;
import com.esri.arcgisruntime.portal.PortalItem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
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
            mMapView.setOnTouchListener(new DefaultMapViewOnTouchListener(MainActivity.this, mMapView) {
                @Override public boolean onSingleTapConfirmed(MotionEvent e) {
                    android.graphics.Point screenPoint = new android.graphics.Point(Math.round(e.getX()),
                            Math.round(e.getY()));
                    identifyResult(screenPoint);
                    return true;
                }
            });
        }
    }

    /**
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

    /**
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

    /**
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

    /**
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