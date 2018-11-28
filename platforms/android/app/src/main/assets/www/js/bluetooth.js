var dom = document.getElementById('status_show');
var address = '';

//扫描设备。大概需要三四秒的时间
function scan() {
    var dom = document.getElementById('status_show');
    navigator.HJ580.chackPermissions(
        function (res) {
            navigator.HJ580.initialize(
                function (res) {
                    dom.innerText = '初始化成功，正在扫描...';
                    navigator.HJ580.startScan(
                        function (device) {
                            var msg = 'Name: ';
                            msg += device.name; //设备名称
                            msg += '\nrssi: ';
                            msg += device.rssi; //设备rssi
                            msg += '\nAddress: ';
                            msg += device.address; //设备地址
                            dom.innerText = msg;
                            address = device.address;
                        }
                    )
                },
                function (err) {
                    alert(err);
                }
            );
        },
        function (err) {
            alert(err);
        }
    );
}
/*function read(long,lat,time,id) {
    var dom = document.getElementById('status_show');
    navigator.HJ580.readData(
        function (data) {
            if (data.msg.length) {
                dom.innerText += '\n';
                //dom.innerText += data.msg; //收到的信息
                long = parseFloat(data.msg.match(/a(.*)a/)[1]);
                lat = parseFloat(data.msg.match(/b(.*)b/)[1]);
                time = parseFloat(data.msg.match(/c(.*)c/)[1]);
                id = parseFloat(data.msg.match(/i(.*)i/)[1]);
                if (long && lat && time && id)
                {
                    dom.innerText +=long;
                    dom.innerText +=lat;
                    dom.innerText +=time;
                    dom.innerText +=id;
                }
            }
        })}*/

function connect(delay_time) {
    var dom = document.getElementById('status_show');
    if (address === '')
        return;
    navigator.HJ580.connect(address, //连接设备时需要指定设备地址
        function (res) {
            //连接成功
            dom.innerText = "connect successfully";
            // 以一秒为周期不断读取蓝牙数据。
            // 注意：蓝牙模块最大传输长度为20，大于此长度会自动切分。
            // 所以读取数据时有可能一次读取并不能获得完整的数据，需要根据数据内容确定一次传输是否接收完成
            // 比如，在消息中天界结束符号或者判断消息中后大括号“}”的数量
            setInterval(
                function () {
                    navigator.HJ580.readData(
                        function (data) {
                            if (data.msg.length) {
                                dom.innerText += '\n';
                                //dom.innerText += data.msg; //收到的信息
                                long = parseFloat(data.msg.match(/a(.*)a/)[1]);
                                lat = parseFloat(data.msg.match(/b(.*)b/)[1]);
                                time = parseFloat(data.msg.match(/c(.*)c/)[1]);
                                id = parseFloat(data.msg.match(/i(.*)i/)[1]);
                                if (long && lat && time && id)
                                {
                                    dom.innerText +=long;
                                    dom.innerText +=lat;
                                    dom.innerText +=time;
                                    dom.innerText +=id;
                                    point.position=Cesium.Cartesian3.fromDegrees(long,lat,0);
                                }
                                if (dom.innerText.length>100) dom.innerText ="";
                            }
                        }
                    )
                }, delay_time
            );
        },
        function (err) {
            alert(err);
        }
    )
}

function send() {
    // 发送数据
    navigator.HJ580.sendData(
        'Send data. this is sent from hj_plugin',
        function () {},
        function () {}
    );
}

function disconnect() {
    // 与蓝牙断开连接
    navigator.HJ580.disconnect(
        function (res) {
            alert('已断开连接');
        }
    )
}