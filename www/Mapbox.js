var exec = require("cordova/exec");

module.exports = {
    showMap: function (options, successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "Mapbox", "showMap", [options]);
    },
    
    addMarkers: function (options, successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "Mapbox", "addMarkers", [options]);
    },
    
    saveTile: function (successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "Mapbox", "saveTile", []);
    },
    
    addMarkerToCenter: function (successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "Mapbox", "addMarkerToCenter", []);
    },
    
    addMapTouchCallback: function (callback) {
        cordova.exec(callback, null, "Mapbox", "addMapTouchCallback", []);
    },
    
    removeAllMarkers: function (successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "Mapbox", "removeAllMarkers", []);
    }
    
    
};
