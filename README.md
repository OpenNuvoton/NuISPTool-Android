# NuISPTool-Android
 
If you want to use Bluetooth Low Energy(BLE) function in ISPTool.

Need to use BLE transport module and connect on target board.

Make sure the module’s UUID be as the same as follow.

BLE service UUID 0xabf0

BLE Characteristic UUID 0xABF1 (write)

BLE Characteristic UUID 0xABF2 (notify)

------------------------------------------------------------------

If you want to use WiFi InterFace in ISPTool.

Need to use Wifi transport module and connect UART on target board.

Make sure the module’s be as the same as follow.

TCP://192.168.4.1:520
