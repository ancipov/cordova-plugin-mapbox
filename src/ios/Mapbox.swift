//import Cordova
import MapboxMaps

@objc(Mapbox) class Mapbox : CDVPlugin {
    
    var mapView: MapView!
    var tileStore: TileStore?
    var saveButton: UIButton!
    
    private lazy var offlineManager: OfflineManager = {
        return OfflineManager(resourceOptions: mapInitOptions.resourceOptions)
    }()
    
    private lazy var mapInitOptions: MapInitOptions = {
        MapInitOptions(cameraOptions: CameraOptions(center: tokyoCoord, zoom: maxZoom),
                       styleURI: .outdoors)
    }()
    
    private let tokyoCoord = CLLocationCoordinate2D(latitude: 34.043492, longitude: -118.258644)
    private let maxZoom: CGFloat = 12
    
    // MARK: - Map
    
    @objc(addMarkerToCenter:)
    func addMarkerToCenter(command: CDVInvokedUrlCommand) {
        let center = mapView.mapboxMap.cameraState.center
        let params = ["lat": center.latitude, "lon": center.longitude]
        putMarkersOnTheMap(annotations: [params])
        let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
        self.commandDelegate!.send(pluginResult, callbackId: command.callbackId)
    }
    
    @objc(showMap:)
    func showMap(command: CDVInvokedUrlCommand) {
        tileStore =  TileStore.default
        let myMapInitOptions = MapInitOptions(resourceOptions:mapInitOptions.resourceOptions, styleURI: StyleURI.streets)
        let args = command.arguments[0] as! Dictionary<String, Any>
        let margins = args["margins"]  as! Dictionary<String, Any>
        let left = margins["left"] as? Int ?? 0
        let right = margins["right"] as? Int ?? 0
        let top = margins["top"] as? Int ?? 0
        let bottom =  margins["bottom"] as? Int ?? 0
        
        let webViewFrame = self.webView.frame
        let mapFrame = CGRect(x: left, y: top, width: Int(webViewFrame.size.width) - left - right, height: Int(webViewFrame.size.height) - top - bottom)
        
        mapView = MapView(frame:mapFrame, mapInitOptions: myMapInitOptions)
        mapView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        mapView.tintColor = .gray
        self.webView.addSubview(mapView)
        let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
        self.commandDelegate!.send(pluginResult, callbackId: command.callbackId)
    }
    
    @objc(addMarkers:)
    func addMarkers(command: CDVInvokedUrlCommand) {
        guard let annotations: Array = command.arguments[0] as? Array<Any> else {
            return
        }
        putMarkersOnTheMap(annotations: annotations)
        let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
        self.commandDelegate!.send(pluginResult, callbackId: command.callbackId)
        
    }
    
    private func putMarkersOnTheMap(annotations: [Any]) {
        self.commandDelegate.run {
            var pointAnnotations = [PointAnnotation]()
            annotations.forEach { item in
                let marker = item  as! Dictionary<String, Any>
                if let lat = marker["lat"] as? Double, let long = marker["lon"] as? Double {
                    var annotation = PointAnnotation(coordinate: CLLocationCoordinate2D(latitude: lat, longitude: long))
                    if #available(iOS 13.0, *) {
                        annotation.image = .init(image: UIImage(systemName: "mappin") ?? UIImage(), name: "mappin")
                    } else {
                        // Fallback on earlier versions
                    }
                    
                    pointAnnotations.append(annotation)
                }
            }
            let pointAnnotationManager = self.mapView.annotations.makePointAnnotationManager()
            pointAnnotationManager.annotations = pointAnnotations
        }
    }
    
    @objc(saveTile:)
    func saveTile(command: CDVInvokedUrlCommand) {
        let options = TilesetDescriptorOptions(styleURI: .streets,
                                               zoomRange: 0...UInt8(maxZoom))
        let descriptor = offlineManager.createTilesetDescriptor(for: options)
        let center = mapView.mapboxMap.cameraState.center
        let regionId = "\(Int(center.latitude))-\(Int(center.longitude))"
        var pluginResult: CDVPluginResult!
        
        
        let tileRegionLoadOptions = TileRegionLoadOptions(
            geometry: .point(Point(center)),
            descriptors: [descriptor],
            metadata: ["tag": "regionId-\(regionId)"],
            acceptExpired: true)!
        
        let _ = tileStore?.loadTileRegion(forId: regionId,
                                          loadOptions: tileRegionLoadOptions,
                                          progress: { progress in
            print("\(Float(progress.completedResourceCount) / Float(progress.requiredResourceCount))")
            let progress = Double(progress.completedResourceCount) / Double(progress.requiredResourceCount)
            print("progress -> \(progress)")
            pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: progress)
            pluginResult.setKeepCallbackAs(true)
        }, completion: { result in
            switch result {
            case .success(_):
                pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
            case .failure(_):
                pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR)
            }
            pluginResult.setKeepCallbackAs(false)
            self.commandDelegate!.send(pluginResult, callbackId: command.callbackId)
        })
        
    }
    
}
