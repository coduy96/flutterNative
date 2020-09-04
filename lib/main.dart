import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() => runApp(MaterialApp(home: _MyHomePageState()));

class _MyHomePageState extends StatefulWidget {
  _MyHomePageState({Key key}) : super(key: key);

  @override
  __MyHomePageStateState createState() => __MyHomePageStateState();
}

class __MyHomePageStateState extends State<_MyHomePageState> {
  static const platform = const MethodChannel('samples.flutter.dev/battery');

  // Get battery level.
  String _batteryLevel = 'Unknown battery level.';

  Future<void> onScan() async {
    platform.invokeMethod('onScan');
  }

  Future<void> setConnect() async {
    platform.invokeMethod('setConnect');
  }

  Future<void> startConnect() async {
    String batteryLevel;

    try {
      final int result = await platform.invokeMethod('startConnect');
      batteryLevel = 'Battery level at $result % .';
    } catch (err) {}
    platform.invokeMethod('startConnect');

    setState(() {
      _batteryLevel = batteryLevel;
    });
  }

  Future<void> sendInit() async {
    platform.invokeMethod('sendInit');
  }

  Future<void> sendTime() async {
    platform.invokeMethod('sendTime');
  }

  @override
  Widget build(BuildContext context) {
    return Material(
      child: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
          children: [
            RaisedButton(
              child: Text('Scan Device'),
              onPressed: onScan,
            ),
            RaisedButton(
              child: Text('Get Connect'),
              onPressed: setConnect,
            ),
            RaisedButton(
              child: Text('Start connect'),
              onPressed: startConnect,
            ),
            RaisedButton(
              child: Text('Send init data'),
              onPressed: sendInit,
            ),
            RaisedButton(
              child: Text('Send time'),
              onPressed: sendTime,
            ),
            Text(_batteryLevel),
          ],
        ),
      ),
    );
  }
}
