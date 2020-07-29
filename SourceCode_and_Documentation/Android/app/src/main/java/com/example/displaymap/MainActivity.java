package com.example.displaymap;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.SigningInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.ArcGISFeature;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureEditResult;
import com.esri.arcgisruntime.data.FeatureQueryResult;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.data.StatisticsQueryParameters;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.geometry.SpatialReferences;
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
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.portal.Portal;
import com.esri.arcgisruntime.portal.PortalItem;
import com.esri.arcgisruntime.security.AuthenticationManager;
import com.esri.arcgisruntime.security.DefaultAuthenticationChallengeHandler;
import com.esri.arcgisruntime.security.OAuthConfiguration;
import com.esri.arcgisruntime.security.UserCredential;
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.tasks.networkanalysis.ClosestFacilityParameters;
import com.esri.arcgisruntime.tasks.networkanalysis.ClosestFacilityResult;
import com.esri.arcgisruntime.tasks.networkanalysis.ClosestFacilityRoute;
import com.esri.arcgisruntime.tasks.networkanalysis.ClosestFacilityTask;
import com.esri.arcgisruntime.tasks.networkanalysis.DirectionManeuver;
import com.esri.arcgisruntime.tasks.networkanalysis.Facility;
import com.esri.arcgisruntime.tasks.networkanalysis.Incident;
import com.esri.arcgisruntime.tasks.networkanalysis.Route;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteParameters;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteResult;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteTask;
import com.esri.arcgisruntime.tasks.networkanalysis.Stop;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.net.MalformedURLException;
import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.Arrays;
import java.util.Collections;

import static com.esri.arcgisruntime.data.QueryParameters.SortOrder.ASCENDING;
import static com.esri.arcgisruntime.data.QueryParameters.SortOrder.DESCENDING;

public class MainActivity extends AppCompatActivity {
    private ArcGISFeature selectedFeature;
    private ServiceFeatureTable featureTable;
    /* ** ADD - myLocation ** */
    private LocationDisplay mLocationDisplay;

    private final SpatialReference mWebMercator = SpatialReferences.getWebMercator();
    private GraphicsOverlay mFacilityGraphicsOverlay;
    private GraphicsOverlay mIncidentGraphicsOverlay;
    private SimpleLineSymbol mRouteSymbol;
    private List<Facility> mFacilities;
    private ClosestFacilityTask mClosestFacilityTask;
    private ClosestFacilityParameters mClosestFacilityParameters;
    private Point mIncidentPoint;
    private ProgressDialog mProgressDialog;
    private RouteTask mRouteTask;
    private RouteParameters mRouteParams;
    private Route mRoute;
    private GraphicsOverlay mGraphicsOverlay;

    // variables for route
    private Graphic added_route = null;


    private void setupOAuthManager() {
        String clientId = getResources().getString(R.string.client_id);
        String redirectUrl = getResources().getString(R.string.redirect_url);
        String clientSecret = getResources().getString(R.string.client_secret);
        try {
            OAuthConfiguration oAuthConfiguration = new OAuthConfiguration("https://www.arcgis.com", clientId, redirectUrl);
            DefaultAuthenticationChallengeHandler authenticationChallengeHandler = new DefaultAuthenticationChallengeHandler(this);
            AuthenticationManager.setAuthenticationChallengeHandler(authenticationChallengeHandler);
            AuthenticationManager.addOAuthConfiguration(oAuthConfiguration);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
    /** 1.1 ver
     *  A custom OnTouchListener on MapView to identify geo-elements when users single tap
     *  on a map view.
     */
    private class MyTouchListener extends DefaultMapViewOnTouchListener {
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
            if (mapView == null) {
                return super.onSingleTapConfirmed(e);
            }

            // Obtain GeoElements, and create popup
            android.graphics.Point screenPoint = new android.graphics.Point((int) e.getX(), (int) e.getY());
            // Debug: get the x and y
            Log.e(TAG, Float.toString(e.getX()));
            Log.e(TAG, Float.toString(e.getY()));

            doIdentify_singletap(mapView, screenPoint);

            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            // Validation
            if (mapView == null) {
                super.onLongPress(e);
            }

            // Obtain GeoElements, and create popup
            android.graphics.Point screenPoint = new android.graphics.Point((int) e.getX(), (int) e.getY());
            doIdentify_longpress(mapView, screenPoint);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            // Validation
            if (mapView == null) {
                return super.onDoubleTap(e);
            }

            // Obtain GeoElements, and create popup
            android.graphics.Point screenPoint = new android.graphics.Point((int) e.getX(), (int) e.getY());
            doIdentify_doubleTap(mapView, screenPoint, e);

            return true;
        }


        /**
         * Creates popup for navigation function
         *
         * @param mapView
         * @param screenPoint
         */
        private void doIdentify_doubleTap(final MapView mapView, final android.graphics.Point screenPoint, MotionEvent e) {
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
                        if ((results == null) || (results.isEmpty())) {
                            Log.i(TAG, "Null or empty result from identify. ");
                            MyTouchListener.super.onDoubleTap(e);
                            return;
                        }

                        // Get the first location of results
                        IdentifyLayerResult result = results.get(0);
                        Map<String, Object> attr = result.getElements().get(0).getAttributes();

                        // symbols that display incident (black cross) and route (blue line) to view
                        SimpleMarkerSymbol incidentSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CROSS, Color.BLACK, 20);
                        mRouteSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.BLUE, 2.0f);
                        mProgressDialog = new ProgressDialog(context);
                        mProgressDialog.setTitle("Finding route");
                        // create RouteTask instance
                        mProgressDialog.show();
                        mRouteTask = new RouteTask(getApplicationContext(), getString(R.string.routing_service));
                        final ListenableFuture<RouteParameters> listenableFuture = mRouteTask.createDefaultParametersAsync();
                        listenableFuture.addDoneListener(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    if (listenableFuture.isDone()) {
                                        int i = 0;
                                        mRouteParams = listenableFuture.get();

                                        Point curPoint = mLocationDisplay.getMapLocation();
                                        Log.e(TAG,curPoint.toString());

                                        Double x = (Double) attr.get("longitude");
                                        Double y = (Double) attr.get("latitude");
                                        Log.e(TAG, x.toString());
                                        Log.e(TAG, y.toString());

                                        // create stops
                                        Stop stop1 = new Stop(curPoint);
                                        Stop stop2 = new Stop(
                                                new Point(x,
                                                        y, SpatialReferences.getWgs84())
                                        );

                                        List<Stop> routeStops = new ArrayList<>();
                                        // add stops
                                        routeStops.add(stop1);
                                        routeStops.add(stop2);
                                        mRouteParams.setStops(routeStops);

                                        // set return directions as true to return turn-by-turn directions in the result of
                                        // getDirectionManeuvers().
                                        mRouteParams.setReturnDirections(true);

                                        // solve
                                        RouteResult result = mRouteTask.solveRouteAsync(mRouteParams).get();
                                        final List routes = result.getRoutes();
                                        mRoute = (Route) routes.get(0);
                                        // create a mRouteSymbol graphic
                                        Graphic routeGraphic = new Graphic(mRoute.getRouteGeometry(), mRouteSymbol);
                                        // check before add the route
                                        if (added_route != null){
                                            mGraphicsOverlay.getGraphics().remove(added_route);
                                        }
                                        // add mRouteSymbol graphic to the map
                                        mGraphicsOverlay.getGraphics().add(routeGraphic);
                                        added_route = routeGraphic;
                                        // get directions
                                        // NOTE: to get turn-by-turn directions Route Parameters should set returnDirection flag as true
                                        final List<DirectionManeuver> directions = mRoute.getDirectionManeuvers();

                                        String[] directionsArray = new String[directions.size()];

                                        for (DirectionManeuver dm : directions) {
                                            directionsArray[i++] = dm.getDirectionText();
                                        }
                                        Log.d(TAG, directions.get(0).getGeometry().getExtent().getXMin() + "");
                                        Log.d(TAG, directions.get(0).getGeometry().getExtent().getYMin() + "");



                                        if (mProgressDialog.isShowing()) {
                                            mProgressDialog.dismiss();
                                        }

                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, e.getMessage());
                                }
                            }
                        });
                    } catch (Exception ex) {
                        Log.i(TAG, "exception in identify: " + ex.getMessage());
                    }
                }
            });
        }


        /**
         * Creates popup for the information popup
         *
         * @param mapView
         * @param screenPoint
         */
        private void doIdentify_singletap(final MapView mapView, final android.graphics.Point screenPoint) {
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
                        if ((results == null) || (results.isEmpty())) {
                            Log.i(TAG, "Null or empty result from identify. ");
                            return;
                        }

                        // Get the first popup
                        IdentifyLayerResult result = results.get(0);
                        List<Popup> popups = result.getPopups();
                        if ((popups == null) || (popups.isEmpty())) {
                            Log.i(TAG, "Null or empty popup from identify. ");
                        }

                        Popup popup = popups.get(0);
                        // Create a popup view for the first popup
                        createPopupView(popup);
                    } catch (Exception ex) {
                        Log.i(TAG, "exception in identify: " + ex.getMessage());
                    }
                }
            });
        }

        /**
         * Creates popup for the update function.
         *
         * @param mapView
         * @param screenPoint
         */
        private void doIdentify_longpress(final MapView mapView, final android.graphics.Point screenPoint) {
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
                        if ((results == null) || (results.isEmpty())) {
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
                            Log.i(TAG, Integer.toString((Integer) attributes.get("salinity")));
                        }
                        // Create popup

                        // login
                        if (GlobalFlag.isLoggedin){
                            Intent myIntent = new Intent(MainActivity.this, PopupActivity.class);
                            startActivityForResult(myIntent, 100);
                        }
                        else{
                            Intent intent1 = new Intent(MainActivity.this, LoginActivity.class);
                            startActivityForResult(intent1, 200);
                        }
                    } catch (Exception ex) {
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
            try {
                // check sanity
                String raw_data = data.getStringExtra("salinity");
                if (raw_data == null)
                    return;

                int update_data = Integer.parseInt(raw_data);

                if (update_data < 0)
                    return;
                if (!Integer.toString(update_data).equals(raw_data))
                    return;

                updateAttributes(Integer.toString(update_data));
            } catch (Exception err){
                return;
            }
        }
        else if (resultCode == 200){
            // logged in
            GlobalFlag.isLoggedin = true;
            if (GlobalFlag.isLoggedin){
                Intent myIntent = new Intent(MainActivity.this, PopupActivity.class);
                startActivityForResult(myIntent, 100);
            }
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
                selectedFeature.getAttributes().put("salinity", Integer.parseInt(salinity));

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

    private void setupLocationDisplay() {
        mLocationDisplay = mMapView.getLocationDisplay();
        mLocationDisplay.addDataSourceStatusChangedListener(dataSourceStatusChangedEvent -> {
            if (dataSourceStatusChangedEvent.isStarted() || dataSourceStatusChangedEvent.getError() == null) {
                return;
            }

            int requestPermissionsCode = 2;
            String[] requestPermissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

            if (!(ContextCompat.checkSelfPermission(MainActivity.this, requestPermissions[0]) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(MainActivity.this, requestPermissions[1]) == PackageManager.PERMISSION_GRANTED)) {
                ActivityCompat.requestPermissions(MainActivity.this, requestPermissions, requestPermissionsCode);
            } else {
                String message = String.format("Error in DataSourceStatusChangedListener: %s",
                        dataSourceStatusChangedEvent.getSource().getLocationDataSource().getError().getMessage());
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
//        mLocationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.COMPASS_NAVIGATION);
        mLocationDisplay.startAsync();
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
            UserCredential credential = new UserCredential("yuan_yongwei", "victor_ve441");
            String itemId = "69536509520444b895a952d3865daf4d";
            final Portal portal = new Portal("https://www.arcgis.com");
            portal.setCredential(credential);
            PortalItem portalItem = new PortalItem(portal, itemId);
            ArcGISMap map = new ArcGISMap(portalItem);
            // set initial viewpoint to a specific region
            map.setInitialViewpoint(
                    new Viewpoint(new Point(-12977012.785807, 4514257.550369, SpatialReference.create(3857)), 38015210));
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mLocationDisplay.startAsync();
        } else {
            Toast.makeText(MainActivity.this, getResources().getString(R.string.location_permission_denied), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMapView = findViewById(R.id.mapView);
        ArcGISRuntimeEnvironment.setLicense(getResources().getString(R.string.arcgis_license_key));
        setupOAuthManager();
        setupMap();
        setupLocationDisplay();
        featureTable = new ServiceFeatureTable(getResources().getString(R.string.sample_service_url));
        // addTrailheadsLayer();

        // symbols that display incident (black cross) and route (blue line) to view
        SimpleMarkerSymbol incidentSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CROSS, Color.BLACK, 20);
        mRouteSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.BLUE, 2.0f);
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle("Finding route");
        mGraphicsOverlay = new GraphicsOverlay();
        //add the overlay to the map view
        mMapView.getGraphicsOverlays().add(mGraphicsOverlay);

        // create RouteTask instance
        mRouteTask = new RouteTask(getApplicationContext(), getString(R.string.routing_service));
        FloatingActionButton mDirectionFab = (FloatingActionButton) findViewById(R.id.directionFAB);
        mDirectionFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mProgressDialog.show();

                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    getSupportActionBar().setHomeButtonEnabled(true);
                    setTitle(getString(R.string.app_name));
                }

                // create RouteTask instance
                mRouteTask = new RouteTask(getApplicationContext(), getString(R.string.routing_service));

                final ListenableFuture<RouteParameters> listenableFuture = mRouteTask.createDefaultParametersAsync();
                listenableFuture.addDoneListener(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (listenableFuture.isDone()) {
                                int i = 0;
                                mRouteParams = listenableFuture.get();

                                Point curPoint = mLocationDisplay.getMapLocation();
                                Log.e(TAG,curPoint.toString());
                                // query water source with lowest water salinity near me
                                Log.e(TAG, Double.toString(curPoint.getX()));
                                Log.e(TAG, Double.toString(curPoint.getY()));
                                String whereClause = "longitude < " + (curPoint.getX() + 0.5) +
                                        " AND longitude > " + (curPoint.getX()-0.5) +
                                        " AND latitude < " + (curPoint.getY()+0.5) +
                                        " AND latitude > " + (curPoint.getY()-0.5);

                                QueryParameters queryParams = new QueryParameters();
                                //String whereClause = "country='US'";
                                queryParams.setWhereClause(whereClause);
                                QueryParameters.OrderBy orderBy = new QueryParameters.OrderBy("salinity", ASCENDING);
                                queryParams.getOrderByFields().add(orderBy);


                                ListenableFuture<FeatureQueryResult> queryResultFuture = featureTable.queryFeaturesAsync(queryParams, ServiceFeatureTable.QueryFeatureFields.LOAD_ALL);
                                Feature feature = null;
                                    try {
                                        Iterator<Feature> featureIt = queryResultFuture.get().iterator();
                                        if (!featureIt.hasNext())
                                            Log.e(TAG, "no next");
                                        feature = featureIt.next();
                                    } catch (ExecutionException e) {
                                        e.printStackTrace();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    Map<String, Object> attr = feature.getAttributes();
                                    if (attr == null)
                                        Log.e(TAG, "null attr");
                                    else
                                        Log.e(TAG, "attr valid");
                                    for (Map.Entry<String, Object> entry : attr.entrySet()) {
                                        System.out.println(entry.getKey() + ":" + entry.getValue().toString());
                                    }
                                    Double x = (Double) attr.get("longitude");
                                    Double y = (Double) attr.get("latitude");
                                    Log.e(TAG, x.toString());
                                    Log.e(TAG, y.toString());

                                // create stops
                                Stop stop1 = new Stop(curPoint);
                                Stop stop2 = new Stop(
                                        new Point(x,
                                                y, SpatialReferences.getWgs84())
                                );

                                List<Stop> routeStops = new ArrayList<>();
                                // add stops
                                routeStops.add(stop1);
                                routeStops.add(stop2);
                                mRouteParams.setStops(routeStops);

                                // set return directions as true to return turn-by-turn directions in the result of
                                // getDirectionManeuvers().
                                mRouteParams.setReturnDirections(true);

                                // solve
                                RouteResult result = mRouteTask.solveRouteAsync(mRouteParams).get();
                                final List routes = result.getRoutes();
                                mRoute = (Route) routes.get(0);
                                // create a mRouteSymbol graphic
                                Graphic routeGraphic = new Graphic(mRoute.getRouteGeometry(), mRouteSymbol);
                                // check before add the route
                                if (added_route != null){
                                    mGraphicsOverlay.getGraphics().remove(added_route);
                                }
                                // add mRouteSymbol graphic to the map
                                mGraphicsOverlay.getGraphics().add(routeGraphic);
                                added_route = routeGraphic;
                                // get directions
                                // NOTE: to get turn-by-turn directions Route Parameters should set returnDirection flag as true
                                final List<DirectionManeuver> directions = mRoute.getDirectionManeuvers();

                                String[] directionsArray = new String[directions.size()];

                                for (DirectionManeuver dm : directions) {
                                    directionsArray[i++] = dm.getDirectionText();
                                }
                                Log.d(TAG, directions.get(0).getGeometry().getExtent().getXMin() + "");
                                Log.d(TAG, directions.get(0).getGeometry().getExtent().getYMin() + "");



                                if (mProgressDialog.isShowing()) {
                                    mProgressDialog.dismiss();
                                }

                            }
                        } catch (Exception e) {
                            Log.e(TAG, e.getMessage());
                        }
                    }
                });
            }
        });
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