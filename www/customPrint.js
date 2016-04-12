cordova.define("com.example.plugin.CustomPrint.customPrint", function(require, exports, module) {
/*global cordova, module*/

    module.exports = {
        print: function (data, successCallback, errorCallback) {
            cordova.exec(successCallback, errorCallback, "CustomPrint", "print", [data]);
        },
        printImage: function (canvas, successCallback, errorCallback) {
            var dataUrl = canvas.toDataURL("image/png");
            var prData = dataUrl.replace(/^data:image\/\w+;base64,/, "");
            cordova.exec(successCallback, errorCallback, "CustomPrint", "printImage", [prData]);
        },
        cut : function (data, successCallback, errorCallback) {
            cordova.exec(successCallback, errorCallback, "CustomPrint", "cut", [data]);
        },
        feed : function(){

        },
        getStatus : function(callback){
            cordova.exec(function(status){
                try{
                    return callback(JSON.parse(status));
                }
                catch(e){
                    return callback(null);
                }
            },
            function(){
                return callback(null);
            },
            "CustomPrint", "getStatus", []);
        }
    };

});
