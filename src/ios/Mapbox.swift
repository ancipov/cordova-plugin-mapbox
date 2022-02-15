import MapboxMaps

@objc(Mapbox) class Mapbox : CDVPlugin {
    
    var mapView: MapView!
    var tileStore: TileStore?
    var saveButton: UIButton!
    var mapTouchCallbackId: String?
    var pointAnnotationManager: PointAnnotationManager!
    
    private lazy var offlineManager: OfflineManager = {
        return OfflineManager(resourceOptions: mapInitOptions.resourceOptions)
    }()
    
    private lazy var mapInitOptions: MapInitOptions = {
        MapInitOptions(cameraOptions: CameraOptions(center: defaultCoordinates, zoom: maxZoom),
                       styleURI: .outdoors)
    }()
    
    private let defaultCoordinates = CLLocationCoordinate2D(latitude: 34.043492, longitude: -118.258644)
    private let maxZoom: CGFloat = 12
    
    // MARK: - Map
    
    @objc(addMarkerToCenter:)
    func addMarkerToCenter(command: CDVInvokedUrlCommand) {
        let center = mapView.mapboxMap.cameraState.center
        let params = ["lat": center.latitude, "lon": center.longitude]
        putMarkersOnTheMap(annotations: [params], id: command.callbackId)
    }
    
    @objc(addMapTouchCallback:)
    func addMapTouchCallback(command: CDVInvokedUrlCommand) {
        self.mapTouchCallbackId = command.callbackId
    }
    
    @objc func mapDidTouch(gesture: UIGestureRecognizer) {
        if mapTouchCallbackId != nil && gesture.state == .ended {
            let point = gesture.location(in: mapView)
            let coordinates = mapView.mapboxMap.coordinate(for: point)
            print(coordinates)
            let params = ["lat" : coordinates.latitude, "lon": coordinates.longitude]
            let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: params)
            pluginResult?.setKeepCallbackAs(true)
            self.commandDelegate!.send(pluginResult, callbackId: self.mapTouchCallbackId)
        }
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
        let height = margins["height"] as? Int
        
        let webViewFrame = self.webView.frame
        var mapFrame: CGRect!
        
        //  when set height ignoring bottom margin
        if let height = height {
            mapFrame = CGRect(x: left, y: top, width: Int(webViewFrame.size.width) - left - right, height: height)
        } else {
            mapFrame = CGRect(x: left, y: top, width: Int(webViewFrame.size.width) - left - right, height: Int(webViewFrame.size.height) - top - bottom)
        }
        
        mapView = MapView(frame:mapFrame, mapInitOptions: myMapInitOptions)
        mapView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        mapView.tintColor = .gray
        if let center = args["center"] as? Dictionary<String, Any>, let lat = center["lat"] as? Double, let lon = center["lon"] as? Double {
            let zoom = args["zoom"] as? CGFloat ?? maxZoom
            let centerCoordinate = CLLocationCoordinate2D(latitude: lat, longitude: lon)
            mapView.mapboxMap.setCamera(to: CameraOptions(center: centerCoordinate, padding: UIEdgeInsets.zero, anchor: nil, zoom: zoom, bearing: nil, pitch: nil))
        }
        
        self.webView.addSubview(mapView)
        mapView.gestures.singleTapGestureRecognizer.addTarget(self, action: #selector(mapDidTouch(gesture:)))
        pointAnnotationManager = self.mapView.annotations.makePointAnnotationManager()
        let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
        self.commandDelegate!.send(pluginResult, callbackId: command.callbackId)
    }
    
    @objc(removeAllMarkers:)
    func removeAllMarkers(command: CDVInvokedUrlCommand) {
        pointAnnotationManager.annotations.removeAll()
        let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
        self.commandDelegate!.send(pluginResult, callbackId: command.callbackId)
    }
    
    @objc(removeMarkerById:)
    func removeMarkerById(command: CDVInvokedUrlCommand) {
        guard let args = command.arguments[0] as? Dictionary<String, String> , let id = args["id"], let index = pointAnnotationManager.annotations.firstIndex(where: {$0.id == id}) else {
            let pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR)
            self.commandDelegate!.send(pluginResult, callbackId: command.callbackId)
            return
        }
        pointAnnotationManager.annotations.remove(at: index)
        let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
        self.commandDelegate!.send(pluginResult, callbackId: command.callbackId)
    }
    
    @objc(addMarkers:)
    func addMarkers(command: CDVInvokedUrlCommand) {
        guard let annotations: Array = command.arguments[0] as? Array<Any> else {
            return
        }
        putMarkersOnTheMap(annotations: annotations, id: command.callbackId)
        
    }
    
    private func putMarkersOnTheMap(annotations: [Any], id: String) {
        self.commandDelegate.run {
            let bundlePath = Bundle.main.path(forResource: "Mapbox", ofType: "bundle")!
            let imageName = Bundle(path: bundlePath)?.path(forResource: "default_marker", ofType: "png")
            let image = UIImage(contentsOfFile: imageName!)
            var pointAnnotations = [PointAnnotation]()
            annotations.forEach { item in
                let marker = item  as! Dictionary<String, Any>
                if let lat = marker["lat"] as? Double, let long = marker["lon"] as? Double {
                    var annotation = PointAnnotation(coordinate: CLLocationCoordinate2D(latitude: lat, longitude: long))
                    annotation.image = .init(image: image ?? UIImage(), name: "default_marker")
                    pointAnnotations.append(annotation)
                }
            }
            self.pointAnnotationManager.annotations.append(contentsOf: pointAnnotations)
            let annotationsIds = pointAnnotations.map {$0.id}
            let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: annotationsIds)
            self.commandDelegate!.send(pluginResult, callbackId:id)
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
            self.commandDelegate!.send(pluginResult, callbackId: command.callbackId)
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

