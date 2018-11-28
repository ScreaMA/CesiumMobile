var exec = require('cordova/exec');

var HJ580 = {
    chackPermissions: function (success, error) {
        exec(success, error, 'HJ580', 'chackPermissions', []);
    },
    initialize: function (success, error) {
        exec(success, error, 'HJ580', 'initBLE', []);
    },
    /**开始扫描蓝牙设备
     * @param success 扫描到设备回调函数。返回参数均为string：status, name, rssi, address
     */
    startScan: function (success) {
        exec(success, success, 'HJ580', 'startScan', []);
    },
    stopScan: function (success, error) {
        exec(success, error, 'HJ580', 'stopScan', []);
    },
    /**连接设备
     * @param address 蓝牙设备的地址，扫描到设备时会得到
     * @param success 连接上蓝牙之后接收到信息的回调函数
     * @param error 连接失败的回调函数
     */
    connect: function (address, success, error) {
        exec(success, error, 'HJ580', 'connect', [address]);
    },
    disconnect: function (success) {
        exec(success, success, 'HJ580', 'disconnect', []);
    },
    /**发送数据
     * @param data 要发送的数据
     * @param success 发送成功
     * @param error 发送失败
     */
    sendData: function (data, success, error) {
        exec(success, error, 'HJ580', 'sendData', [data]);
    },
    readData: function (success) {
        exec(success, success, 'HJ580', 'readData', []);
    }
};

module.exports = HJ580;