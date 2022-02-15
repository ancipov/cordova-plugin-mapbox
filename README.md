# Mapbox
## cordova-plugin-mapbox

### Mapbox.showMap
- {dictionary} params - parameters for start map state
- {function} success - callback function which takes parameter data that will be invoked on successs
- {function} error - callback function which takes a parameter error which will be invoked on failure

##### Supported Options


| property | value | description |
| ------ | ------ |------ |
| margins | {'left': 10, 'top': 20, 'bottom': 20, 'height': 600} | margins for map (if set 'height'- bottom will be ignoring )
| center | {'lat': 53.896454, 'lon': 27.548004} | Set map center on start
| zoom | 'zoom': 4 | set start zoom level for the map

Example: 
function (options, successCallback, errorCallback)
```
Mapbox.showMap({
                    margins: {
                        'left': 0,
                        'right': 0,
                        'top': 0,
                        'bottom': 100,
                        'height': 600
                    },
                    center: {
                        lat: 53.896454,
                        lon: 27.548004
                    },
                    zoom: 4
                },
                function (success) {},
                function (error) {},
                );
```
### Mapbox.addMarkers
- {dictionary} params - markers to add
- {function} success - callback function which takes parameter data that will be invoked on successs
- {function} error - callback function which takes a parameter error which will be invoked on failure

#### Supported Options
| property | value | description |
| ------ | ------ |------ |
| markers | {markers: [{"lat": 21.445555, "lon": 23.343434}, {"lat": 22.445555, "lon": 24.343434}]} | array of markers

Example: 
function (options, successCallback, errorCallback)
- successCallback - return array of added markers ids (["id-xxxxxxx, id-xxxxxxxxxx"])
```
Mapbox.addMarkers(
                 { markers: [{"lat": 21.445555, "lon": 23.343434}, 
                 {"lat": 22.445555, "lon": 24.343434}]
                 },
                 function (success) {},
                 function (error) {},
                 );
```
### Mapbox.saveTile
Save tile with coordinate in map center and zoom level from 0 to 12
- {function} success - callback function which takes parameter data that will be invoked on successs (will return progress value from 0.0 to 1.0, 1.0 mean downloading finished)
- {function} error - callback function which takes a parameter error which will be invoked on failure

Example: 
function (successCallback, errorCallback)
```
Mapbox.saveTile(
                function (result) {
                   ...
                },
                function (error) {
                    ...
                }
                );
```

### Mapbox.addMarkerToCenter
Add marker to center on the map
- {function} success - callback function which takes parameter data that will be invoked on successs
- {function} error - callback function which takes a parameter error which will be invoked on failure

Example: 
- successCallback - return array of added markers ids (["id-xxxxxxx])
- 
```
Mapbox.addMarkerToCenter(
                function (result) {
                    ...
                },
                function (error) {
                    ...
                }
                );
```
### Mapbox.addMapTouchCallback
Subscribe to map tap
- callback - will return coordinate of tap ({"lon" : 21.343434, "lat: 22.345434"})
Example: 
function (callback)
```
Mapbox.addMapTouchCallback(
                function (callback) {
                   ...
                }
                );
```
### Mapbox.removeAllMarkers
Remove all markers from the map
- {function} success - callback function which takes parameter data that will be invoked on successs
- {function} error - callback function which takes a parameter error which will be invoked on failure

Example: 
function (successCallback, errorCallback)
```
Mapbox.removeAllMarkers(
                function (result) {
                    ...
                },
                function (error) {
                    ...
                }
                );
```

### Mapbox.removeMarkerById
- {dictionary} params - marker to delete
- {function} success - callback function which takes parameter data that will be invoked on successs
- {function} error - callback function which takes a parameter error which will be invoked on failure

### Supported Options
| property | value | description |
| ------ | ------ |------ |
| id | "marker_id" | marker id for removing

Example: 
function (options, successCallback, errorCallback)
```
Mapbox.removeMarkerById(
                        {"id" : "marker_id"},
                        function (result) {
                            ...
                        },
                        function (error) {
                            ...
                        }
                        );
```


