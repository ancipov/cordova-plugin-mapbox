package io.appery.plugin.mapbox.Mapbox;

import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Button;
import android.widget.FrameLayout;

import com.mapbox.common.TileStore;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.CameraState;
import com.mapbox.maps.MapInitOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.ResourceOptions;
import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class echoes a string called from JavaScript.
 */
public class Mapbox extends CordovaPlugin {

    private static final String ACCESS_TOKEN = "sk.eyJ1IjoiYWR6eWdhIiwiYSI6ImNremVsbXBmODF4Yjgyb28xM2hhMHdqczgifQ.T6CdcYg70gmKfbQrzaoNhA";

    private MapView mapView;
    private TileStore tileStore;
    private Button saveButton;
    private String mapTouchCallbackId;
    private PointAnnotationManager pointAnnotationManager;

    private Point defaultCoordinates = Point.fromLngLat(-118.258644, 34.043492);
    private double maxZoom = 12;

    private MapInitOptions mapInitOptions;
    private static float retinaFactor;


    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        Log.e("Mapbox", "initialize");

        ResourceOptions.Builder builder = new ResourceOptions.Builder();

        mapInitOptions = new MapInitOptions(cordova.getContext(), builder.build());

        DisplayMetrics metrics = new DisplayMetrics();
        cordova.getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        retinaFactor = metrics.density;

    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        Log.e("Mapbox", "execute");

        if (action.equals("showMap")) {
            showMap(args, callbackContext);
            return true;
        } else if (action.equals("addMarkerToCenter")) {
            addMarkerToCenter();
            return true;
        } else if (action.equals("removeAllMarkers")) {
            removeAllMarkers(callbackContext);
            return true;
        } else if (action.equals("removeMarkerById")) {
            removeMarkerById(args, callbackContext);
        } else if (action.equals("addMarkers")) {
            addMarkers(args, callbackContext);
        }
        return false;
    }

    private static String getStyle(final String requested) {
        if ("light".equalsIgnoreCase(requested)) {
            return Style.LIGHT;
        } else if ("dark".equalsIgnoreCase(requested)) {
            return Style.DARK;
        } else if ("satellite".equalsIgnoreCase(requested)) {
            return Style.SATELLITE;
        } else if ("streets".equalsIgnoreCase(requested)) {
            return Style.MAPBOX_STREETS;
        } else {
            return requested;
        }
    }

    private static int applyRetinaFactor(int i) {
        return (int) (i * retinaFactor);
    }

    public void showMap(JSONArray args, CallbackContext callbackContext) {
        tileStore =  TileStore.create();
        MapInitOptions myMapInitOptions = new MapInitOptions(cordova.getContext());

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mapView = new MapView(webView.getContext());
                final FrameLayout layout = (FrameLayout) webView.getView().getParent();

                try {
                final JSONObject options = args.getJSONObject(0);
                final String style = getStyle(options.optString("style"));

                final JSONObject margins = options.isNull("margins") ? null : options.getJSONObject("margins");
                final int left = applyRetinaFactor(margins == null || margins.isNull("left") ? 0 : margins.getInt("left"));
                final int right = applyRetinaFactor(margins == null || margins.isNull("right") ? 0 : margins.getInt("right"));
                final int top = applyRetinaFactor(margins == null || margins.isNull("top") ? 0 : margins.getInt("top"));
                final int bottom = applyRetinaFactor(margins == null || margins.isNull("bottom") ? 0 : margins.getInt("bottom"));

                final JSONObject center = options.isNull("center") ? null : options.getJSONObject("center");

                    Double zoom = options.isNull("zoomLevel") ? 10 : options.getDouble("zoomLevel");
                    float zoomLevel = zoom.floatValue();
                    if (center != null) {
                        final double lat = center.getDouble("lat");
                        final double lng = center.getDouble("lng");
                     } else {
                        if (zoomLevel > 18.0) {
                            zoomLevel = 18.0f;
                        }
                    }

                // position the mapView overlay
                int webViewWidth = webView.getView().getWidth();
                int webViewHeight = webView.getView().getHeight();
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(webViewWidth - left - right, webViewHeight - top - bottom);
                params.setMargins(left, top, right, bottom);
                mapView.setLayoutParams(params);

            } catch (JSONException e) {
                callbackContext.error(e.getMessage());
                return;
            }

                layout.addView(mapView);
                callbackContext.success();
            }
        });
    }

    public void addMarkerToCenter() {
        Point center = mapView.getMapboxMap().getCameraState().getCenter();
        //putMarkersOnTheMap(annotations: [params], id: command.callbackId)
    }

    public void removeAllMarkers(CallbackContext callbackContext){
        pointAnnotationManager.getAnnotations().clear();
        callbackContext.success();
    }

    public void removeMarkerById(JSONArray args, CallbackContext callbackContext){
        int index = 0;

        try {
            final JSONObject options = args.getJSONObject(0);
            int id = options.getInt("id");
            //TODO get index from args
        } catch (JSONException e){
            callbackContext.error(e.getMessage());
            return;
        }

        pointAnnotationManager.getAnnotations().remove(index);
        callbackContext.success();
    }


    private void addMarkers(JSONArray args, CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONArray markers = args.getJSONArray(0);
                    for (int i=0; i<markers.length(); i++) {
                        final JSONObject marker = markers.getJSONObject(i);
                        //pointAnnotationManager.getAnnotations().add(new PointAnnotation());
                    }

                    callbackContext.success();
                } catch (JSONException e) {
                    callbackContext.error(e.getMessage());
                }
            }
        });

    }

}


