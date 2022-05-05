package io.appery.plugin.mapbox;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;

import com.mapbox.bindgen.Expected;
import com.mapbox.bindgen.Value;
import com.mapbox.common.NetworkRestriction;
import com.mapbox.common.TileDataDomain;
import com.mapbox.common.TileRegion;
import com.mapbox.common.TileRegionError;
import com.mapbox.common.TileRegionLoadOptions;
import com.mapbox.common.TileRegionLoadProgress;
import com.mapbox.common.TileStore;
import com.mapbox.common.TileStoreObserver;
import com.mapbox.common.TileStoreOptions;
import com.mapbox.common.TilesetDescriptor;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.ConstrainMode;
import com.mapbox.maps.GlyphsRasterizationMode;
import com.mapbox.maps.GlyphsRasterizationOptions;
import com.mapbox.maps.MapInitOptions;
import com.mapbox.maps.MapOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.OfflineManager;
import com.mapbox.maps.ResourceOptions;
import com.mapbox.maps.Style;
import com.mapbox.maps.TileStoreUsageMode;
import com.mapbox.maps.TilesetDescriptorOptions;
import com.mapbox.maps.plugin.annotation.AnnotationConfig;
import com.mapbox.maps.plugin.annotation.AnnotationPlugin;
import com.mapbox.maps.plugin.annotation.AnnotationPluginImplKt;
import com.mapbox.maps.plugin.annotation.AnnotationType;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions;


import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * This class echoes a string called from JavaScript.
 */
public class Mapbox extends CordovaPlugin {

    private static final String TAG = Mapbox.class.getSimpleName();
    private MapView mapView;
    private TileStore tileStore;
    private PointAnnotationManager pointAnnotationManager;
    ResourceOptions resourceOptions;
    private Point point;

    private static final double MIN_ZOOM = 0;
    private static final double MAX_ZOOM = 12;

    private MapInitOptions mapInitOptions;
    private static float retinaFactor;


    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        Log.d("Mapbox", "initialize");

        DisplayMetrics metrics = new DisplayMetrics();
        cordova.getActivity()
                .getWindowManager()
                .getDefaultDisplay()
                .getMetrics(metrics);
        retinaFactor = metrics.density;
    }

    @Override
    public boolean execute(@NonNull String action, JSONArray args, CallbackContext callbackContext) {
        Log.e("Mapbox", "execute");

        switch (action) {
            case "showMap":
                showMap(args, callbackContext);
                return true;
            case "addMarkerToCenter":
                addMarkerToCenter(callbackContext);
                return true;
            case "removeAllMarkers":
                removeAllMarkers(callbackContext);
                return true;
            case "removeMarkerById":
                removeMarkerById(args, callbackContext);
                return true;
            case "addMarkers":
                addMarkers(args, callbackContext);
                return true;
            case "saveTile":
                saveTile(args, callbackContext);
                return true;
        }
        return false;
    }

    private void initMapInitOptions(double zoom, double lng, double lat) {
        ResourceOptions.Builder builder = new ResourceOptions.Builder();
        TileStoreUsageMode tileStoreUsageMode;
//        tileStoreUsageMode = TileStoreUsageMode.READ_AND_UPDATE;
        tileStoreUsageMode = TileStoreUsageMode.READ_ONLY;
        resourceOptions = builder.accessToken(getAccessToken())
                .tileStoreUsageMode(tileStoreUsageMode)
                .build();

        MapOptions mapOptions = new MapOptions.Builder()
                .constrainMode(ConstrainMode.HEIGHT_ONLY)
                .glyphsRasterizationOptions(
                        new GlyphsRasterizationOptions.Builder()
                                .rasterizationMode(GlyphsRasterizationMode.IDEOGRAPHS_RASTERIZED_LOCALLY)
                                .fontFamily("sans-serif")
                                .build())
                .build();

//        List<Plugin> plugins = new ArrayList<>();
//        plugins.add(new Plugin.Mapbox(Plugin.MAPBOX_LOGO_PLUGIN_ID));
//        plugins.add(new Plugin.Mapbox(Plugin.MAPBOX_GESTURES_PLUGIN_ID));
//        plugins.add(new Plugin.Mapbox(Plugin.MAPBOX_CAMERA_PLUGIN_ID));
//        plugins.add(new Plugin.Mapbox(Plugin.MAPBOX_SCALEBAR_PLUGIN_ID));
//        plugins.add(new Plugin.Mapbox(Plugin.MAPBOX_CAMERA_PLUGIN_ID));
        point = Point.fromLngLat(lng, lat);
        CameraOptions cameraOptions = new CameraOptions.Builder()
                .center(point)
                .zoom(zoom)
                .bearing(120.0)
                .build();

        mapInitOptions = new MapInitOptions(cordova.getContext(), resourceOptions, mapOptions);
        mapInitOptions.setCameraOptions(cameraOptions);
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

        cordova.getActivity().runOnUiThread(() -> {
            try {
                final FrameLayout layout = (FrameLayout) webView.getView().getParent();
                final JSONObject options = args.getJSONObject(0);
                final String style = getStyle(options.optString("style"));

                final JSONObject margins = options.isNull("margins") ? null : options.getJSONObject("margins");
                final int left = applyRetinaFactor(margins == null || margins.isNull("left") ? 0 : margins.getInt("left"));
                final int right = applyRetinaFactor(margins == null || margins.isNull("right") ? 0 : margins.getInt("right"));
                final int top = applyRetinaFactor(margins == null || margins.isNull("top") ? 0 : margins.getInt("top"));
                final int bottom = applyRetinaFactor(margins == null || margins.isNull("bottom") ? 0 : margins.getInt("bottom"));

                final JSONObject center = options.isNull("center") ? null : options.getJSONObject("center");

                double zoom = options.isNull("zoomLevel") ? 10 : options.getDouble("zoomLevel");
                float zoomLevel = (float) zoom;
                double lng = 0;
                double lat = 0;
                if (center != null) {
                    lng = center.getDouble("lon");
                    lat = center.getDouble("lat");
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

                initMapInitOptions(zoom, lng, lat);
                mapView = new MapView(webView.getContext(), mapInitOptions);

                mapView.setLayoutParams(params);
                layout.addView(mapView);
            }
            catch (JSONException e) {
                callbackContext.error(e.getMessage());
                return;
            }

            callbackContext.success();
        });
    }

    public void saveTile(JSONArray args, CallbackContext callbackContext) {

//        String jsonGeometry = "   {\n" +
//                "     \"TYPE\": \"GeometryCollection\",\n" +
//                "     \"geometries\": [{\n" +
//                "       \"TYPE\": \"Point\",\n" +
//                "       \"coordinates\": [100.0, 0.0]\n" +
//                "     }, {\n" +
//                "       \"TYPE\": \"LineString\",\n" +
//                "       \"coordinates\": [\n" +
//                "         [101.0, 0.0],\n" +
//                "         [102.0, 1.0]\n" +
//                "       ]\n" +
//                "     }]\n" +
//                "   }";

        if (point == null) {
            callbackContext.error("coordinates are empty");
            return;
        }

        double lng = point.longitude();
        double lat = point.latitude();

        String pointJson = String.format("{\"type\": \"Point\", \"coordinates\": [%s, %s]}", lng, lat);

        Point point = Point.fromJson(pointJson);

//        GeometryCollection geometryCollection = GeometryCollection.fromJson(jsonGeometry);

        TilesetDescriptor tilesetDescriptor = new OfflineManager(resourceOptions)
                .createTilesetDescriptor(new TilesetDescriptorOptions.Builder()
                        .styleURI(Style.OUTDOORS)
                        .minZoom((byte) MIN_ZOOM)
                        .maxZoom((byte) MAX_ZOOM)
                        .build());
        List<TilesetDescriptor> descriptors = new ArrayList<>();
        descriptors.add(tilesetDescriptor);

        // You need to keep a reference of the created tileStore and keep it during the download process.
        TileRegionLoadOptions tileRegionLoadOptions = new TileRegionLoadOptions.Builder()
                .geometry(point)
                .acceptExpired(false)
                .descriptors(descriptors)
                .networkRestriction(NetworkRestriction.NONE)
                .build();

        TileStore tileStore = TileStore.create();
        tileStore.setOption(
                TileStoreOptions.MAPBOX_ACCESS_TOKEN,
                TileDataDomain.MAPS,
                new Value(getAccessToken())
        );

        tileStore.addObserver(new TileStoreObserver() {
            @Override
            public void onRegionLoadProgress(@NonNull String id,
                                             @NonNull TileRegionLoadProgress progress) {
                Log.d(TAG, "onRegionLoadProgress: id - " + id + " progress - " + progress);
            }

            @Override
            public void onRegionLoadFinished(@NonNull String id,
                                             @NonNull Expected<TileRegionError,
                                                     TileRegion> region) {
                if (region.isValue()) {
                    Log.d(TAG, "onRegionLoadFinished: id - " + id + ", region value - "
                            + Objects.requireNonNull(region.getValue()).toString());
                } else
                if (region.isError()) {
                    Log.e(TAG, "onRegionLoadFinished error: "
                            + Objects.requireNonNull(region.getError()).getMessage());
                }
            }

            @Override
            public void onRegionRemoved(@NonNull String id) {
                Log.d(TAG, "onRegionRemoved: id - " + id);
            }

            @Override
            public void onRegionGeometryChanged(@NonNull String id,
                                                @NonNull Geometry geometry) {
                Log.d(TAG, "onRegionGeometryChanged: id - " + id + ", geometry - "
                        + geometry.toString());
            }

            @Override
            public void onRegionMetadataChanged(@NonNull String id,
                                                @NonNull Value value) {
                Log.d(TAG, "onRegionMetadataChanged: id - " + id + ", value - "
                        + value.toJson());
            }
        });

        final String newId = UUID.randomUUID().toString();

        cordova.getThreadPool().execute(
                () -> tileStore.loadTileRegion(newId, tileRegionLoadOptions,
                        region -> Log.d(TAG, "TileRegionLoadProgress: " + region.toString()),
                        region -> {
                            if (region.isError()) {
                                callbackContext.error(Objects.requireNonNull(region.getError()).getMessage());
                                return;
                            }

                            if (region.isValue()) {
                                fetchAllTileRegions();
                                TileRegion tileRegion = region.getValue();
                                if (tileRegion != null) {
                                    callbackContext.success(tileRegion.toString());
                                } else {
                                    callbackContext.error("tileRegion is null");
                                }
                            }
                        }));
    }

    private void fetchAllTileRegions() {
        tileStore.getAllTileRegions(regions -> {
            Log.d("", "");
        });
    }

    public void addMarkerToCenter(CallbackContext callbackContext) {
        Point center = mapView.getMapboxMap().getCameraState().getCenter();
        addMarkerToMap(center);
//        callbackContext.success();
    }

    public void removeAllMarkers(@NonNull CallbackContext callbackContext){
        pointAnnotationManager.deleteAll();
        callbackContext.success();
    }

    public void removeMarkerById(@NonNull JSONArray args, CallbackContext callbackContext){
        long id = -1;

        try {
            final JSONObject options = args.getJSONObject(0);
            id = options.getInt("id");
        } catch (JSONException e){
            callbackContext.error(e.getMessage());
            return;
        }
        boolean removed = false;
        List<PointAnnotation> pointAnnotations = pointAnnotationManager.getAnnotations();
        for (PointAnnotation pointAnnotation: pointAnnotations) {
            if (pointAnnotation.getId() == id) {
                pointAnnotationManager.delete(pointAnnotation);
                removed = true;
                break;
            }
        }
        if (removed)
            callbackContext.success();
        else
            callbackContext.error("Marker with such id not exists");
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

    private void addMarkerToMap(Point point) {
        int resourceId = android.R.drawable.ic_menu_myplaces;

        Drawable constantState = AppCompatResources.getDrawable(cordova.getContext(), resourceId);
        Drawable drawable = constantState.getConstantState().newDrawable().mutate();
        Bitmap bitmap = Bitmap.createBitmap(
                drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888
        );
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        DrawableCompat.setTint(drawable, Color.BLUE);
        drawable.draw(canvas);
        if (pointAnnotationManager == null) {
            initPointAnnotationManager();
        }

        PointAnnotationOptions pointAnnotationOptions = new PointAnnotationOptions()
                .withPoint(point)
                .withIconImage(bitmap);

        pointAnnotationManager.create(pointAnnotationOptions);
    }

    private void initPointAnnotationManager() {
        AnnotationPlugin annotationApi = AnnotationPluginImplKt.getAnnotations(mapView);
        AnnotationConfig annotationConfig = new AnnotationConfig();
        pointAnnotationManager = (PointAnnotationManager) annotationApi
                .createAnnotationManager(AnnotationType.PointAnnotation, annotationConfig);
    }

    private String getAccessToken() {
        int resId = cordova.getActivity()
                .getResources()
                .getIdentifier("access_token",
                        "string",
                        cordova.getActivity().getPackageName()
                );
        return cordova.getContext().getString(resId);
    }
}