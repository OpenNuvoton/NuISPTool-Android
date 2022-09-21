# NuISPTool-Android
 
 The “NuISPTool APP” (Nuvoton In-System Programming tool APP) allows the embedded Flash memory to be
reprogrammed under software control through the firmware using on-chip connectivity interface,
such as UART and USB, without removing any microcontroller from the system.
For NuMicro®

 Family microcontroller (MCU) products, the on-chip Flash memory is partitioned into
three blocks: APROM, Data Flash and LDROM. The APROM saves the user application program
developed for a specific application; the Data Flash provides storage for nonvolatile application
data; and the LDROM saves the ISP code for MCU to update its APROM/Data Flash/CONFIG.

 User can update the MCU’s APROM, Data Flash, and User Configuration bits with Nuvoton
standard ISP code programmed in LDROM easily by using the ISP function.
 
Download [NuISPTool-Android](https://drive.google.com/file/d/1Uaj_9Q__ORS6HAyXr4eApse8MZeZjmny/view?usp=sharing "link")

# How to start 
 
 1. Make sure the USB cable has [USB OTG](https://en.wikipedia.org/wiki/USB_On-The-Go "link")
 2. If you want to ues SPI/I2C/CAN/RS485 Interface in NuISPTool,need to use Nu-link2-pro
 
 ![圖片參考名稱](https://truth.bahamut.com.tw/s01/202209/561314b5f332e0c40e611a9e2cdca45b.PNG "Logo")
 
 3. If you want to use Bluetooth Low Energy(BLE) function in NuISPTool. 
    *  Need to use BLE transport module and connect on target board.
    *  Make sure the module’s UUID be as the same as follow.
    *  BLE service UUID 0xabf0
    *  BLE Characteristic UUID 0xABF1 (write)
    *  BLE Characteristic UUID 0xABF2 (notify)
    
 4. If you want to use WiFi Interface in NuISPTool.
    *  Need to use Wifi transport module and connect UART on target board.
    *  Make sure the module’s TCP Server IP be as the same as follow.
    *  TCP://192.168.4.1:520
   
# How to Run
1. Agree USB permission 
2. Click the `FIND DEVICE` link and find NuMaker
3. Click `OPEN` to enter NuISPTool home page
 
![圖片參考名稱](https://truth.bahamut.com.tw/s01/202209/f41ad707eb0ffa7722a36639329474cc.JPG "Logo")

4. Click `config bit` to enter settings
5. Modify the setting
6. Execute and burn into the device
 
![圖片參考名稱](https://truth.bahamut.com.tw/s01/202209/ac4928b037b430b624759020b96b6648.JPG "Logo")

7. Click `file icon`, and select the bin file you want to burn in.
8. Select the action and start buring
 
![圖片參考名稱](https://truth.bahamut.com.tw/s01/202209/607cc301f9498108604ad46c4272b77f.JPG "Logo")
