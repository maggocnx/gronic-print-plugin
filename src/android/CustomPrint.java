package com.gronic.plugin;

import it.custom.printer.api.android.*;

import android.app.Dialog;
import android.hardware.usb.*;

import org.apache.cordova.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import android.content.*;
import android.graphics.*;
import android.os.Debug;
import android.os.Handler;
import android.util.Base64;

import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;



public class CustomPrint extends CordovaPlugin {

    static UsbDevice[] usbDeviceList = null;
    static CustomPrinter prnDevice = null;
    static int lastDeviceSelected = -1;
    static int deviceSelected = 0;
    static Context context = null;
    private String lock="lockAccess";

    private CallbackContext callback = null;
    private PluginResult pluginResult = null;
    private String initLog = "";

    @Override
    protected void pluginInitialize() {
        context = cordova.getActivity().getApplicationContext();
        InitEverything();
    }


    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {
   
        if (action.equals("print")) {
            showAlertMsg("PRINT", "PRINT BABAB");

             String text = data.getString(0);
            callbackContext.success("prePrint");

            PrintText(context, text);

            return true;

        }
        if (action.equals("printImage")) {
            showAlertMsg("PRINT", "PRINT IMAGE");
            String imgData = data.getString(0);
            PrintImage(imgData, callbackContext);
            callbackContext.success("Image Printed");

            return true;
        }
        if (action.equals("cut")) {
            int feed = data.getInt(0);
            boolean cutPartial = data.getBoolean(1);
            showAlertMsg("CUT", "CUT " + feed + " " +cutPartial);
            this.Cut(feed, cutPartial);
            callbackContext.success("foo");

            return true;
        }
        if (action.equals("getStatus")){
            String status = getStatus();
            callbackContext.success(status);
            return true;
        }
        else{
            return false;
        }
    }

    void showAlertMsg(String title,String msg)
    {
        if(callback != null){

            if(initLog != "")
                msg = initLog + msg;

            pluginResult.setKeepCallback(true);
            PluginResult result = new PluginResult(PluginResult.Status.OK, msg);
            callback.sendPluginResult(result);


        }

        Toast.makeText(cordova.getActivity().getApplicationContext(),
                msg, Toast.LENGTH_SHORT).show();        // return;

    }

    private void InitEverything()
    {
        showAlertMsg("Gronic...", "Initializing Printer");

        try
        {
            //Get the list of devices
            usbDeviceList = CustomAndroidAPI.EnumUsbDevices(context);


            if ((usbDeviceList == null) || (usbDeviceList.length == 0))
            {
                //Show Error
                showAlertMsg("Error...", "No Devices Connected...");
                return;
             }
         }
         catch(CustomException e )
         {
             //Show Error
             showAlertMsg("Error...", e.getMessage());
             return;
         }
         catch(Exception e )
         {
             //Show Error
             showAlertMsg("Error...", "Enum devices error...");
             return;
         }

    }


    public void Cut(int feed,boolean partial){



        int cutType = (partial) ? CustomPrinter.CUT_PARTIAL : CustomPrinter.CUT_TOTAL;

        try {
            prnDevice.feed(feed);
            prnDevice.cut(cutType);

        } catch (CustomException e) {
            e.printStackTrace();
        }
    }

    public void PrintText(Context view, String strTextToPrint)
    {
        PrinterFont fntPrinterNormal = new PrinterFont();
        PrinterFont fntPrinterBold2X = new PrinterFont();

        //open device
        if (OpenDevice() == false)
            return;


        try
        {
            //Fill class: NORMAL
            fntPrinterNormal.setCharHeight(PrinterFont.FONT_SIZE_X1);					//Height x1
            fntPrinterNormal.setCharWidth(PrinterFont.FONT_SIZE_X1);					//Width x1
            fntPrinterNormal.setEmphasized(false);										//No Bold
            fntPrinterNormal.setItalic(false);											//No Italic
            fntPrinterNormal.setUnderline(false);										//No Underline
            fntPrinterNormal.setJustification(PrinterFont.FONT_JUSTIFICATION_CENTER);	//Center
            fntPrinterNormal.setInternationalCharSet(PrinterFont.FONT_CS_DEFAULT);		//Default International Chars

            //Fill class: BOLD size 2X
            fntPrinterBold2X.setCharHeight(PrinterFont.FONT_SIZE_X2);					//Height x2
            fntPrinterBold2X.setCharWidth(PrinterFont.FONT_SIZE_X2);					//Width x2
            fntPrinterBold2X.setEmphasized(true);										//Bold
            fntPrinterBold2X.setItalic(false);											//No Italic
            fntPrinterBold2X.setUnderline(false);										//No Underline
            fntPrinterBold2X.setJustification(PrinterFont.FONT_JUSTIFICATION_CENTER);	//Center
            fntPrinterBold2X.setInternationalCharSet(PrinterFont.FONT_CS_DEFAULT);		//Default International Chars
        }
        catch(CustomException e )
        {

            //Show Error
            showAlertMsg("Error...", e.getMessage());
        }
        catch(Exception e )
        {
            showAlertMsg("Error...", "Set font properties error...");
        }

        //***************************************************************************
        // PRINT TEXT
        //***************************************************************************

        synchronized (lock)
        {
            try
            {
                //Print Text (NORMAL)
                prnDevice.printText(strTextToPrint, fntPrinterNormal);
                prnDevice.printTextLF(strTextToPrint, fntPrinterNormal);
                //Print Text (BOLD size 2X)
                prnDevice.printTextLF(strTextToPrint, fntPrinterBold2X);
            }
            catch(CustomException e )
            {
                //Show Error
                showAlertMsg("Error...", e.getMessage());
            }
            catch(Exception e )
            {
                showAlertMsg("Error...", "Print Text Error...");
            }
        }
    }


    public void showOverlay(Bitmap image){
        final Dialog dialog = new Dialog(cordova.getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout popUp = new LinearLayout(cordova.getActivity());
        popUp.setBackgroundColor(Color.BLUE);
        popUp.setOrientation(1);


        ImageView imageView = new ImageView(cordova.getActivity());
        imageView.setImageBitmap(image);

        LinearLayout btnLayout = new LinearLayout(cordova.getActivity());
        btnLayout.setBackgroundColor(Color.LTGRAY);
        btnLayout.setOrientation(0);
        btnLayout.setGravity(Gravity.CENTER);

        Button btnOk = new  Button(cordova.getActivity());
        btnOk.setText("Ok");

        btnLayout.addView(btnOk);
        popUp.addView(imageView);
        popUp.addView(btnLayout);

        dialog.setContentView(popUp);



        btnOk.setOnClickListener(new  Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                dialog.dismiss();
            }
        });


        dialog.show();

    }


    public void PrintImage(String completeImageData, CallbackContext callbackContext)
    {

        byte[] decodedByte = Base64.decode(completeImageData, Base64.DEFAULT);
        Bitmap image =  BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);

//        showOverlay(image);

        if (OpenDevice() == false){
            callbackContext.error("No Printer Connected");
            return;
        }


        synchronized (lock) 
        {
            //***************************************************************************
            // PRINT PICTURE
            //***************************************************************************
            
            try
            {
                //Print (Left Align and Fit to printer width)
                prnDevice.printImage(image,CustomPrinter.IMAGE_ALIGN_TO_LEFT, CustomPrinter.IMAGE_SCALE_TO_FIT, 0);
            }
            catch(CustomException e )
            {               
                //Show Error
                showAlertMsg("Error...", e.getMessage());
            }
            catch(Exception e )
            {
                showAlertMsg("Error...", "Print Picture Error...");
            }
            
            //***************************************************************************
            // FEEDS and CUT
            //***************************************************************************
            
            try
            {
                //Feeds (3)
                prnDevice.feed(3);
                //Cut (Total)
                prnDevice.cut(CustomPrinter.CUT_TOTAL);
            }
            catch(CustomException e )
            {    
                //Only if isn't unsupported
                if (e.GetErrorCode() != CustomException.ERR_UNSUPPORTEDFUNCTION)
                {
                    //Show Error
                    showAlertMsg("Error...", e.getMessage());
                }
            }
            catch(Exception e )
            {
                showAlertMsg("Error...", "Print Picture Error...");
            }
            
            //***************************************************************************
            // PRESENT
            //***************************************************************************
            
            try
            {               
                //Present (40mm)
                prnDevice.present(40);
            }
            catch(CustomException e )
            {    
                //Only if isn't unsupported
                if (e.GetErrorCode() != CustomException.ERR_UNSUPPORTEDFUNCTION)
                {
                    //Show Error
                    showAlertMsg("Error...", e.getMessage());
                }
            }
            catch(Exception e )
            {
                showAlertMsg("Error...", "Print Picture Error...");
            }
        }
    } 


     //Open the device if it isn't already opened
    public boolean OpenDevice()
    {
        //Device not selected
        if (deviceSelected == -1)
        {
            showAlertMsg("Error...", "No Printer Device Selected...");
            return false;
        }
        
        //If i changed the device
        if (lastDeviceSelected != -1)
        {
            if (deviceSelected != lastDeviceSelected)
            {
                try
                {
                    //Force close
                    prnDevice.close();
                }
                catch(CustomException e )
                {
                    
                    //Show Error
                    showAlertMsg("Error...", e.getMessage());
                    return false;
                }
                catch(Exception e )
                {
                    //Show error
                    return false;
                }
                prnDevice = null;
            }
        }
        
        //If i never open it
        if (prnDevice == null)
        {
            try
            {                               
                //Open and connect it
                prnDevice = new CustomAndroidAPI().getPrinterDriverUSB(usbDeviceList[deviceSelected], context);
                //Save last device selected
                lastDeviceSelected = deviceSelected;
                return true;
            }
            catch(CustomException e )
            {
                
                //Show Error
                showAlertMsg("Error...", e.getMessage());
                return false;
            }
            catch(Exception e )
            {
                showAlertMsg("Error...", "Open Print Error...");
                //open error
                return false;
            }
        }
        //Already opened
        return true;
        
    }

    public String getStatus(){
        synchronized (lock)
        {
            try
            {
                PrinterStatus prnSts = prnDevice.getPrinterFullStatus();
                String jsonResult ="{";
                jsonResult += "\"nopaper \" :" +  String.valueOf(prnSts.stsNOPAPER) + ",";
                jsonResult += "\"nearendpap \" :" +  String.valueOf(prnSts.stsNEARENDPAP) + ",";
                jsonResult += "\"ticketout \" :" +  String.valueOf(prnSts.stsTICKETOUT) + ",";
                jsonResult += "\"nohead \" :" +  String.valueOf(prnSts.stsNOHEAD) + ",";
                jsonResult += "\"nocover \" :" +  String.valueOf(prnSts.stsNOCOVER) + ",";
                jsonResult += "\"spooling \" :" +  String.valueOf(prnSts.stsSPOOLING) + ",";
                jsonResult += "\"paperrolling \" :" +  String.valueOf(prnSts.stsPAPERROLLING) + ",";
                jsonResult += "\"lfpressed \" :" +  String.valueOf(prnSts.stsLFPRESSED) + ",";
                jsonResult += "\"ffpressed \" :" +  String.valueOf(prnSts.stsFFPRESSED) + ",";
                jsonResult += "\"overtemp \" :" +  String.valueOf(prnSts.stsOVERTEMP) + ",";
                jsonResult += "\"hlvolt \" :" +  String.valueOf(prnSts.stsHLVOLT) + ",";
                jsonResult += "\"paperjam \" :" +  String.valueOf(prnSts.stsPAPERJAM) + ",";
                jsonResult += "\"cuterror \" :" +  String.valueOf(prnSts.stsCUTERROR) + ",";
                jsonResult += "\"ramerror \" :" +  String.valueOf(prnSts.stsRAMERROR) + ",";
                jsonResult += "\"eepromerror \" :" +  String.valueOf(prnSts.stsEEPROMERROR);
                jsonResult += "}";

                return jsonResult;

            }
            catch(CustomException e )
            {
                return "{ err : " +  e.getMessage() + " }" ;
            }
            catch(Exception e)
            {
                return "{ err : " +  e.getMessage() + " }" ;
            }
        }
    }
}

