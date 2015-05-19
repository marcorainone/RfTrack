/* Copyright 2014 Marco Rainone, for ICTP Wireless Laboratory.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 */
// see the documentation:
// http://developer.android.com/reference/android/widget/package-summary.html

package com.ictp.mrainone.rftrack;

// java.io: Provides for system input and output through data streams, serialization and the file system.
import java.io.*;

// java.text: Provides classes and interfaces for handling text, dates, numbers, and messages in a manner independent of natural languages.
import java.text.*;
import java.text.SimpleDateFormat;

// java.util: Contains the collections framework, legacy collection classes, event model, date and time facilities, internationalization,
// and miscellaneous utility classes (a string tokenizer, a random-number generator, and a bit array).
import java.util.Calendar;
import java.util.Locale;
// java.util.concurrent: Utility classes commonly useful in concurrent programming.
import java.util.concurrent.ExecutorService;		// ExecutorService: An Executor that provides methods to manage termination and methods that can produce a Future for tracking progress of one or more asynchronous tasks.
import java.util.concurrent.Executors;

// android.app:Contains high-level classes encapsulating the overall Android application model.
import android.app.Activity;			// Activity: An activity is a single, focused thing that the user can do.
import android.app.AlertDialog;			// AlertDialog: A subclass of Dialog that can display one, two or three buttons.

// android.content: Contains classes for accessing and publishing data on a device.
import android.content.Context;				// Context:	Interface to global information about an application environment.
import android.content.DialogInterface;

// android.graphics: Provides low level graphics tools such as canvases, color filters, points, and rectangles that let you handle drawing to the screen directly. 
import android.graphics.Color;

// android.view: Provides classes that expose basic user interface classes that handle screen layout and interaction with the user.
import android.view.LayoutInflater;		// LayoutInflater: Instantiates a layout XML file into its corresponding View objects.
import android.view.View;				// View: This class represents the basic building block for user interface components.
import android.view.View.OnClickListener;
import android.view.Gravity;

// for menus:
import android.view.Menu;
import android.view.MenuItem;

// android.widget: The widget package contains (mostly visual) UI elements to use on Application screen.
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

// android.os: Provides basic operating system services, message passing, and inter-process communication on the device.
import android.os.Bundle;				// Bundle: A mapping from String values to various Parcelable types.
import android.os.Environment;			// Environment: Provides access to environment variables.

// android.hardware.usb: Provides support to communicate with USB hardware peripherals that are connected to Android-powered devices.
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

// android.media: provides classes that manage various media interfaces in audio and video. 
// used for the generation of a tone. See the example:
// http://matrix-examplecode.blogspot.it/2011/08/dtmf-tone-generator.html
import android.media.AudioManager;
import android.media.ToneGenerator;

// android.location: Contains the framework API classes that define Android location-based and related services (for gps)
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;

// android.util: Provides common utility methods such as date/time manipulation, base64 encoders and decoders, string and number conversion methods, and XML utilities.
import android.util.Log;

// See project home page: http://code.google.com/p/usb-serial-for-android/
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import com.ictp.mrainone.rftrack.R;

// import au.com.bytecode.opencsv.*;
import au.com.bytecode.opencsv.CSVWriter;

/**
 * Activity for the measures acquisition of the Spectrum Analyzer RFExplorer
 * www.rf-explorer.com
 *
 * @author Marco Rainone. 
 * Program developed for the ICTP Wireless laboratory
 */

// To create an activity, you must create a subclass of Activity (or an existing subclass of it).
public class RfTrackActivity extends Activity
{
	//---------------------------------------
	// menu item
	Menu myMenu;
	
	// menu items
	// change 15/07: enabled only three items
    final int MENU_FONT_SIZE = Menu.FIRST;
    final int MENU_CLEAN_SCREEN = Menu.FIRST + 1;
    final int MENU_HELP = Menu.FIRST + 2;   
	
	//---------------------------------------
	// to read gps
    private LocationManager locationManager;
    String GpsStatus;
	double Longitude;
	double Latitude;
	
	//---------------------------------------
	// for the generation of the tones
	ToneGenerator tone;
	
	// ====================================
	// @@@ used for debugging
	CharSequence contentCharSequence; 		// contain entire text content
	boolean bContentFormatHex = false;
	int contentFontSize = 16;
	boolean bWriteEcho = true;
    final String[] contentFormatItems = {"Character","Hexadecimal"};
    final String[] fontSizeItems = {"5","6","7","8","10","12","14","16","18","20"};
    final String[] echoSettingItems = {"On","Off"};

    // Interface to global information about an application environment.
	public Context global_context;
    final Context context = this;
	
	// ====================================
	// graphical objects    
	ScrollView scrollView;
	TextView readText;
	EditText writeText;

    // EditText is a thin veneer over TextView that configures itself to be editable.
    private EditText editTextMainScreen;

    // Displays text to the user and optionally allows them to edit it. A TextView is a complete text editor,
    // however the basic class is configured to not allow editing
    private TextView mTitleTextView;
    private TextView mDumpTextView;

    // Layout container for a view hierarchy that can be scrolled by the user,
    // allowing it to be larger than the physical display
    private ScrollView mScrollView;
	
    private Button button;
    // Returns the simple name of the class represented by this Class as defined in the source code.
    private final String TAG = RfTrackActivity.class.getSimpleName();

	// ====================================
    /**
     * The device currently in use, or {@code null}.
     */
    private UsbSerialDriver mSerialDevice;

    // android.hardware.usb.UsbManager:
    // This class allows you to access the state of USB and communicate with USB devices.
    // Currently only host mode is supported in the public API.
    // The system's USB service.
    private UsbManager mUsbManager;

    //-----------------------------------------------------------
    // mr:
    // External Storage
    // http://www.androidaspect.com/2014/02/android-external-storage.html
    // Checking External Storage Availability

    final String AcqDataDirectory = "/RFTrk_data";       // Directory that contains the files of acquisitions

    // change 03/11/2014:
    // designed two types of files:
    // .csv contain data acquisitions
    // .log contains information about the data acquisition
	
    // SD Card Storage
    File sdCard;				// position of sd card
    File directory;				// directory containing the file
	String FileName;			// file name 
	String FileExt;				// extension of the file containing the data
	String FileExtLog;          // extension of the file containing the log info
	
	// file that stores the data
    File file;					// File: An "abstract" representation of a file system entity identified by a pathname.
    FileOutputStream fos;		// output stream that writes bytes to the data file
    OutputStreamWriter osw;		// bridge from character streams to byte streams: Characters written to it are encoded into bytes using a specified charset.
    
	// file that stores log info
    File fileLog;               // "abstract" representation of a file system entity identified by a pathname.
    FileOutputStream fosLog;    // output stream that writes bytes to the log file
    OutputStreamWriter oswLog;  // bridge from character streams to byte streams

	char DecimalSeparator;		// Decimal separator char
	String CsvSeparator;		// Separator in the line of the csv file
	DecimalFormatSymbols CsvDecimalFormatSym;		// Used to set the value of the numerical formatting
	
	// Configuration of the floating point format in the .csv file
	// DecimalFormat: subclass of NumberFormat that formats decimal numbers.
	DecimalFormat FormatterFreq;	// value formatter for frequency
	DecimalFormat FormatterDBval;	// value formatter for DB
	DecimalFormat FormatterDBmax;	// value formatter for max DB
	
	//---------------------------------------
	long Csv_start_time;		// Time in milliseconds to start creating the csv file
	long Idx_Letture;			// Number of readings actually made
	
	// max values
	float MaxDB;				// Maximum signal value in DB in floating point
	byte MaxDBbyte;				// Byte value of the Maximum signal value in DB 
	float MaxDBFreq;			// Value of the frequency corresponding to the maximum value of the signal
	
	// Writing a CSV file with Java using OpenCSV
	// http://snipplr.com/view/58019/writing-a-csv-file-with-java-using-opencsv/
	BufferedWriter outCsv;
	CSVWriter writerCsv;	
	
	boolean fOpen;				// True when the data file is open to save the measures

	//---------------------------------------
	// serial parameters 
	//---------------------------------------
	// the RF Explorer RF spectrum analyzer uses 2 baudrates: 500000 (default) and 2400.
	// Verify and set the correct baudrate using the menu instrument.
	// Note: transmission with baud rate from 500k has been tested even 
	// in the virtual machine virtualbox used for debugging
	//
	final int baudRate = 500000;			// two baudrates: 2400 or 500000
//	final int baudRate = 2400;				// two baudrates: 2400 or 500000
	
	
//============================================================
// Auxiliary functions for graphics
//============================================================

	// Set the size of the font used to display
	void SetTextFontSize()
	{
		mDumpTextView.setTextSize(contentFontSize);
		mTitleTextView.setTextSize(contentFontSize);
		editTextMainScreen.setTextSize(contentFontSize);
	}
	
//============================================================
// START OF MENU FUNCTIONS
//

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		myMenu = menu;

		// change 15/07: enabled only three items
		myMenu.add(0, MENU_FONT_SIZE, 0, "Font Size");
		myMenu.add(0, MENU_CLEAN_SCREEN, 0, "Clean Screen");
		myMenu.add(0, MENU_HELP, 0, "Info");
		
		return super.onCreateOptionsMenu(myMenu);
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId())
        {
			
        case MENU_FONT_SIZE:
        	//-----------------------------------------------------------
			new AlertDialog.Builder(global_context).setTitle("Font Size")
			.setItems(fontSizeItems, new DialogInterface.OnClickListener() 
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{	
					contentFontSize = Integer.parseInt(fontSizeItems[which]);
					// sets the font size to display data
					SetTextFontSize();
					
				}
			}).show();
			
            break;

        case MENU_CLEAN_SCREEN:
        	//-----------------------------------------------------------
			// clear the TextView of the data logging
			mDumpTextView.setText("");
            break;

        case MENU_HELP:
        	//-----------------------------------------------------------
			// show the program version info
			{
			midToast( "RfTrack Logger - Ver. 0.2 - November 3, 2014\n\n" +
						"Author: Marco Rainone -\n"
				, Toast.LENGTH_LONG);
			break;
			}   

		/***
		//-----------------------------------------
		// 15/07: START MENU ITEMS DISABLED
		//
        case MENU_SETTING:
			// toggleMenuSetting();
        	break;
		
        case MENU_CONTENT_FORMAT:
        	new AlertDialog.Builder(global_context).setTitle("Content Format")
			.setItems(contentFormatItems, new DialogInterface.OnClickListener() 
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{	
					if(0 == which)
					{
						if(true ==  bContentFormatHex)
						{
							toggleContentHexFormat(false);
						}
					}
					else if(1 == which)
					{
						if(false ==  bContentFormatHex)
						{
							toggleContentHexFormat(true);
						}
					}
				}
			}).show();
            break;
 		
        case MENU_SAVE_CONTENT_DATA:
			{	
				if(true == bSendButtonClick || true == bLogButtonClick)
				{
					midToast("Can't save content data to file during sending file and saving data.",Toast.LENGTH_LONG);    		
				}
				else
				{
					bUartModeTaskSet = false;
					tempTransferMode = MODE_SAVE_CONTENT_DATA;
					
					// select file
					final String[] actItems = {"Create New File","Save to File"};
					new AlertDialog.Builder(global_context).setTitle("File Destination")
						.setItems(actItems, new DialogInterface.OnClickListener() 
						{
							@Override
							public void onClick(DialogInterface dialog, int which) {
								if(0 == which)
								{
									fileDialog.setSelectDirectoryOption(true);
									fileDialog.setActionCode(ACT_SELECT_SAVED_FILE_FOLDER);
									fileDialog.showDialog();
								}
								else if(1 == which)
								{
									fileDialog.setSelectDirectoryOption(false);
									fileDialog.setActionCode(ACT_SELECT_SAVED_FILE_NAME);
									fileDialog.showDialog();	
								}
							}
						}).show();
				}
			}
        	break;

        case MENU_ECHO:
        	new AlertDialog.Builder(global_context).setTitle("Echo")
			.setItems(echoSettingItems, new DialogInterface.OnClickListener() 
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{	
					MenuItem item = myMenu.findItem(MENU_ECHO);
					if(0 == which)
					{
						bWriteEcho = true;
					    item.setTitle("Echo - On");
					}
					else if(1 == which)
					{
						bWriteEcho = false;
						item.setTitle("Echo - Off");
					}
				}
			}).show();           	
        	break;

		//
		// 15/07: 	END MENU ITEMS DISABLED
		//-----------------------------------------
		***/
		
        default:
        	break;
        }
 
        return super.onOptionsItemSelected(item);
    }
	
//
// END OF MENU FUNCTIONS
//============================================================

	//=================================================
	// SD CARD UTILITIES
	//=================================================
	
	//------------------------------------------------------------------------
	// NOTE:
    // The external storage may be unavailable.
	// Thatbecause we can mount it as USB storage and in some cases remove it from the device.
    // Therefore we should always check its availability before using it.
    // We can simply check external storage availability using the getExternalStorageState() method.

    // Checks if SD Card is available for read and write
    public boolean isSDCardWritable()
    {
        String status = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(status))
        {
            return true;
        }
        return false;
    }

    // Checks if SD Card is available to at least read
    public boolean isSDCardReadable()
    {
        String status = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(status)
                || Environment.MEDIA_MOUNTED_READ_ONLY.equals(status))
        {
            return true;
        }
        return false;
    }

	// Create the file name containing the date
	// See these info:
	// http://www.tutorialspoint.com/java/java_date_time.htm
	// http://stackoverflow.com/questions/2271131/display-the-current-time-and-date-in-an-android-application
	//
	public void SetFileName()
	{
        Calendar c = Calendar.getInstance();
        // System.out.println("Current time => "+c.getTime());
        SimpleDateFormat df = new SimpleDateFormat("yyMMdd_HHmmss");
        // FileName have current date/time
        FileName = df.format(c.getTime());
		
		// type of extensions used:
		FileExt =  "csv";             // Filename extension for the file with measures
		FileExtLog = "log";           // Filename extension for the file with log infos
	}
	
    public void OpenFile()
    {
        if (isSDCardWritable())
        {
            try
            {
                // SD Card Storage
                sdCard = Environment.getExternalStorageDirectory();
                directory = new File(sdCard.getAbsolutePath() + AcqDataDirectory);
                directory.mkdirs();
                file = new File(directory, "text.txt");
                fos = new FileOutputStream(file);
                osw = new OutputStreamWriter(fos);
                
                fOpen = true;			// success: file opened

            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            fOpen = false;				// SD Card Not Available
        }
    }

	// write the string to the file
    public void WriteData(String dataToSave)
    {
        if(fOpen)
        {
            try
            {
                osw.write(dataToSave);
                osw.flush();
            }
            catch(IOException ie)
            {
                ie.printStackTrace();
            }
        }
    }

    public void CloseFile()
    {
        try
        {
            osw.close();
        }
        catch(IOException ie)
        {
            ie.printStackTrace();
        }
    }

    //-----------------------------------------------------------
	// Use OpenCsv to create a file to be saved

	// Create a line with the values of of frequency
	public String dumpCsvFreq(byte[] array) 
	{
        // int length = array.length;
		String result = "";
		byte Nval;
		float freq;

		// http://www.coderanch.com/t/385190/java/java/Setting-decimalFormatSymbols
        // DecimalFormat FormatterFreq = new DecimalFormat("####.000", CsvDecimalFormatSym);
        // DecimalFormat FormatterFreq = new DecimalFormat("* ##0.000", CsvDecimalFormatSym);
		
		int offset = 0;
		Nval = array[2];			// contiene il numero di valori seguenti

		result= result + String.format("% 4d",(int)Nval);
		result= result + CsvSeparator;
		// for (int i = offset; i < offset + length; i++) 
		for (int i = 0; i < Nval; i++) 
		{
			freq = RfCfg.GetFrequency(i, Nval);
			if(i>offset)
			{
				result= result + CsvSeparator;
			}
			result= result + FormatterFreq.format(freq);
		}

		return result;
	}
	
	public boolean ChkMaxDB(byte[] array, boolean init)
	{
        int length = array.length;
		int Nval;
		float val;
		boolean result = false;			// false if the DB values is not updated
        int offset = 0;

		if(init)
		{
			// initialize the values
			MaxDB = -255.0f;
			MaxDBFreq = 0.0f;
		}
		
		Nval = array[2];				// has the following values
		if((offset + length)<(Nval +1))
		{
			// Means that the length of the message is less than the expected
			return result;
		}
		for (int i = 3; i < 3 + Nval; i++) 
		{
			// Attention to the unsigned values !!!!!
			// b = array[i] & 0xFF;		// b is integer
			// val = ((float)b)/2.0f;	// According to the protocol specification, the value is divided by 2
			// val = -val;				// the value is negative
			
			val = RfCfg.GetDBvalue(array[i]);
			
			// Check if the frequency is within the limits
            if(val<RfCfg.Amp_Bottom)
                continue;
            if(val>RfCfg.Amp_Top)
                continue;
			
			// Check if the frequency is greater of the actual value
			if(MaxDB <= val)
			{
				MaxDB = val;
				MaxDBbyte = array[i];			// byte max db
				MaxDBFreq = RfCfg.GetFrequency(i-3, Nval);
				result = true;					// frequency value updated.
			}
		}

		return result;
		// return(true);
	}	
	
	// Encode a row in csv format with the values contained in a byte array
	public String dumpCsvString(byte[] array) 
	{
        int length = array.length;
		String result = "";
		
		// see:
		// http://www.coderanch.com/t/385190/java/java/Setting-decimalFormatSymbols
        // DecimalFormat FloatFormatter = new DecimalFormat("####.0", CsvDecimalFormatSym);
        // DecimalFormat FormatterDBval = new DecimalFormat("* #####0.0", CsvDecimalFormatSym);
		
		// Warning:
		// in Java, all integer values are signed.
		// see http://www.javamex.com/java_equivalents/unsigned.shtml
		// the c++ code:
		// unsigned byte b = ...; b += 100;
		// In java is equivalent:
		// int b = ...; b = (b + 100) & 0xff;
		// int b;
		
		int Nval;
		float val;
		
		int offset = 0;
		Nval = array[2];			// holds the number of the following values
		if((offset + length)<(Nval +1))
		{
			// The message length is below the expected
			return result;
		}
		// Insert the number of values
		result= result + String.format("% 4d",(int)Nval);
		for (int i = 3; i < 3 + Nval; i++) 
		{
			// Insert the separator
			if(i>offset)
			{
				result= result + CsvSeparator;
			}
			// Insert the value: Beware of the unsigned value !!!
			// b = array[i] & 0xFF;	// b e' un intero
			// val = ((float)b)/2.0f;	// in base alle specifiche protocollo, il valore e' diviso per 2
			// val = -val;				// il valore e' negativo

			val = RfCfg.GetDBvalue(array[i]);
			
			// result= result + String.format("% 5.1f", val);
            // result= result + FormatterDBval.format(val).replaceAll("\\G0", " ");
            result= result + FormatterDBval.format(val);
		}

		return result;
	}
	
	// Calculates the difference between the present time and an initial time
	public long DeltaMsec(long start_time)
	{
		long end_time = System.currentTimeMillis();
		return(end_time-start_time);
	}
    
	public void OpenCsvFile()
    {
        if (!isSDCardWritable())
        {
            // SD Card Not Available
            fOpen = false;
			return;
        }
		try
		{
			// SD Card Storage
			sdCard = Environment.getExternalStorageDirectory();
            directory = new File(sdCard.getAbsolutePath() + AcqDataDirectory);
			if (!directory.exists())
				directory.mkdirs();
			
			SetFileName();								// Create a file name containing current date and time
			
			String fname = FileName + "." + FileExt;
			file = new File(directory, fname);
			fos = new FileOutputStream(file);
            osw = new OutputStreamWriter(fos);       	// osw: file with csv data
            
            
            String fnameLog = FileName + "." + FileExtLog;
            fileLog = new File(directory, fnameLog);
            fosLog = new FileOutputStream(fileLog);
            oswLog = new OutputStreamWriter(fosLog);    // oswLog: file with log dataa

			//-------------------------------
			// set file csv parameters
			DecimalSeparator = ',';			// use comma for decimal separator
			CsvSeparator = ";";				// csv fields separated by ';'
			
			// Set formatter for numeric format. See
			// http://www.coderanch.com/t/385190/java/java/Setting-decimalFormatSymbols
			CsvDecimalFormatSym = new DecimalFormatSymbols(Locale.US);  
			CsvDecimalFormatSym.setDecimalSeparator(DecimalSeparator);
			
			//-------------------------------
			// http://www.coderanch.com/t/385190/java/java/Setting-decimalFormatSymbols
			FormatterFreq = new DecimalFormat("* ##0.000", CsvDecimalFormatSym);
			FormatterDBval = new DecimalFormat("* #####0.0", CsvDecimalFormatSym);
			FormatterDBmax = new DecimalFormat("* #####0.0", CsvDecimalFormatSym);
 			
			// initial time in msec
			// http://stackoverflow.com/questions/9707938/calculating-time-difference-in-milliseconds
			Csv_start_time = System.currentTimeMillis();
			
			Idx_Letture = 0L;
			// success message
			fOpen = true;
			
			// Write the initial values in the log file
			WriteCsvInitialInfo();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
    }

	// Write the initial values in the log file
    public void WriteCsvInitialInfo()
    {
        if(!fOpen)
        {
			return;
		}
		// write the string to the file
		try
		{
			String str;
			str = String.format("Start Frequency:") +
				CsvSeparator+
				String.format("% 8d", RfCfg.Start_Freq) +
				String.format("\n");
            oswLog.write(str);
			str = String.format("End Frequency:") +
				CsvSeparator+
				String.format("% 8d", RfCfg.End_Freq) +
				String.format("\n");
			oswLog.write(str);
			str = String.format("Amplitude Highest value dBm:") +
				CsvSeparator+
				String.format("% 8d", RfCfg.Amp_Top) +
				String.format("\n");
			oswLog.write(str);
			str = String.format("Amplitude Lowest value dBm:") +
				CsvSeparator+
				String.format("% 8d", RfCfg.Amp_Bottom) +
				String.format("\n\n");
			oswLog.write(str);
			oswLog.flush();
		}
		catch(IOException ie)
		{
			 ie.printStackTrace();
		}
    }
	
    public void WriteCsvData(String values)
    {
        if(!fOpen)
        {
			return;
		}
		// write the string to the file
		try
		{
            DecimalFormat dtimef = new DecimalFormat("* ###0.0");
            String tm = dtimef.format(((float)DeltaMsec(Csv_start_time))/1000.0f);
            osw.write(tm);
            osw.write(CsvSeparator);
			//--------------------------------
			// posizione
			DecimalFormat df = new DecimalFormat("* ########0.000000");
            osw.write(df.format(Latitude));
            osw.write(CsvSeparator);
            osw.write(df.format(Longitude));
            osw.write(CsvSeparator);
			//--------------------------------
            osw.write(values);
            osw.write("\n");
            // osw.write(values + "\n");
			osw.flush();
		}
		catch(IOException ie)
		{
			 ie.printStackTrace();
		}
    }

    public void CloseCsvFile()
    {
        try
        {
            osw.close();                // close the csv file
            oswLog.close();             // close the log file
        }
        catch(IOException ie)
        {
            ie.printStackTrace();
        }
    }

	
    //-----------------------------------------------------------
	// LOCATION MANAGEMENT
    //-----------------------------------------------------------

    private final LocationListener gpsLocationListener = new LocationListener() {

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            switch (status) {
            case LocationProvider.AVAILABLE:
                GpsStatus = String.format("GPS available again");
                break;
            case LocationProvider.OUT_OF_SERVICE:
                GpsStatus = String.format("PS out of service");
                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                GpsStatus = String.format("GPS temporarily unavailable");
                break;
            }
			editTextMainScreen.setText(GpsStatus);
        }

        @Override
        public void onProviderEnabled(String provider) {
            GpsStatus = String.format("GPS Provider Enabled");
			editTextMainScreen.setText(GpsStatus);
        }

        @Override
        public void onProviderDisabled(String provider) {
            GpsStatus = String.format("GPS Provider Disabled\n");
			editTextMainScreen.setText(GpsStatus);
        }

        @Override
        public void onLocationChanged(Location location) {
            // locationManager.removeUpdates(networkLocationListener);

            GpsStatus = String.format("GPS location: "
                    + String.format("% 9.6f - ", location.getLatitude()) + ", "
                    + String.format("% 9.6f", location.getLongitude()) + "\n");
			editTextMainScreen.setText(GpsStatus);

			Longitude = location.getLongitude();
			Latitude = location.getLatitude();
        }
    };				// end of private final LocationListener gpsLocationListener

    //-----------------------------------------------------------
	// Configuration parameters of the measures
    //
    private class RFEConfiguration
    {
        public int Start_Freq;		// 7 ASCII digits, decimal, KHZ, Value of frequency span start (lower)
        public int End_Freq;		// 7 ASCII digits, decimal, KHZ, Value of frequency span end (higher)
        public int Amp_Top;			// 4 ASCII digits, decimal, dBm, Highest value of amplitude for GUI
        public int Amp_Bottom;		// 4 ASCII digits, decimal, dBm, Lowest value of amplitude for GUI
        
		// default configuration parameters
        public void SetDefault()
        {
            Start_Freq = 400000;	// 7 ASCII digits, decimal, KHZ, Value of frequency span start (lower)
            End_Freq = 500000;		// 7 ASCII digits, decimal, KHZ, Value of frequency span end (higher)
			//---- dbg
            // Start_Freq = 2400000;	// 7 ASCII digits, decimal, KHZ, Value of frequency span start (lower)
            // End_Freq = 2500000;		// 7 ASCII digits, decimal, KHZ, Value of frequency span end (higher)
			//-----
            Amp_Top = -50;			// 4 ASCII digits, decimal, dBm, Highest value of amplitude for GUI
            Amp_Bottom = -120;		// 4 ASCII digits, decimal, dBm, Lowest value of amplitude for GUI
        }

        // class costructor
        public RFEConfiguration()
        {
            SetDefault();
        }

		// Returns the frequency corresponding to the index idx
		float GetFrequency(int idx, int sweep_steps)
		{
			float ris = (float)Start_Freq;
			if(idx>0)
			{
				ris = ris + idx * ((float)(End_Freq - Start_Freq)/(float)sweep_steps);
			}
			ris = (ris / 1000.0f);            // freq. in MHz
			return(ris);
		}

		// Converts the value of a byte in DB
		float GetDBvalue(byte DBbyte)
		{
			float fVal;
			int b;
			
			// calculate the value of dBm: Beware of the unsigned value !!!
			b = DBbyte & 0xFF; 			// b is an integer
			b = -b;
			fVal = ((float)b)/2.0f;		// According to the protocol specification, the value is divided by 2
			
			return(fVal);
		}
		
        // Form the string to be displayed on the set parameters.
		// Display the start/end frequency values in MHz
        public String strInfoParam()
		{
			String result = "Ranges:    ";

			// show frequency
			result= result + String.format("Frequency (MHz): " +
										"[" +
										"% 4d ... % 4d" +
										"]",
										Start_Freq / 1000,			// freq. in Mhz
										End_Freq / 1000);			// freq. in Mhz
			// show DB signal amplitude
			result= result + "   ";
			result= result + String.format("Amplitude (dBm): " +
										"[" +
										"% 3d ... % 3d" +
										"]",
										(Amp_Bottom),
										(Amp_Top) );
			
			return result;
        }
		
		
        // Prepare the string to display the parameters.
		// Values of start and end frequency in MHz
        public String strShowParam()
        {
            int len = 7 + 7 + 4 + 4 + 3; 		// Total length of the string, including separators
            StringBuffer dest = new StringBuffer(len);
			
			// For clarity, first appears Amp_Bottom then Amp_Top
            dest.append(String.format("% 4d, % 4d, % 3d, % 3d",
                                      Start_Freq / 1000,			// freq. in Mhz
									  End_Freq / 1000,				// freq. in Mhz
									  (Amp_Bottom),
									  (Amp_Top)
									  )
									  );
            return dest.toString();
        }
		
        
		// Form the parameter string to send the message through the USB port
        public String strGetParam()
        {
            int len = 7 + 7 + 4 + 4 + 3;		// Total length of the string, including separators
            StringBuffer dest = new StringBuffer(len);
            dest.append(String.format("%07d,%07d,-%03d,-%03d",
                                      Start_Freq, End_Freq, Math.abs(Amp_Top), Math.abs(Amp_Bottom)));
            return dest.toString();
        }

        // Recognize the configuration parameters RrTrack provided in the string.
		// modify 10/07: the freq. limits are in Mhz
		// MOdify 08/02/2015: the frequency limit is 6500 (4850-6100 frequency range for RFExplorer 6G model)
        public void strSetParam(String s)
        {
            // Integer: MAX, MIN VALUE
            // int maximum value (2^31-1), int minimum value -2^31.
            int tmp;
            int fld = 0;			// field index

			// Array containing the string subdivided in parts
			String[] items = s.split(",");
			
			for(fld = 0; fld<items.length;fld++)
            {
                // Convert the word to a numeric value
			    // Note: trim clear the spaces before and after the numerical part.
			    // ParseInt wants a string formed only by numeric characters
                tmp = Integer.parseInt(items[fld].trim());
                switch (fld)
                {
                case 0:             			// Start_Freq
                    // Incorrect frequency value is negative
                    if (tmp < 0)
                        tmp = -tmp;
                    // Do not accept the value if the frequency is greater than or equal to 6500 (in MHz),
                    if (tmp >= 6500)
                        break;
                    Start_Freq = tmp * 1000;	// Save the freq. in KHz
                    break;
                case 1:             // End_Freq
                    // Incorrect frequency value is negative
                    if (tmp < 0)
                        tmp = -tmp;
                    // Do not accept the value if the frequency is greater than or equal to 6500 (in MHz),
                    if (tmp >= 6500)
                        break;
                    End_Freq = tmp * 1000;		// Save the freq. in KHz
                    break;
					// modify 15/07:
					// For clarity, has changed the order of recognition,
					// Is read before Amp_Bottom (par. 3) then Amp_top (par. 2).
					// Remember that in the message serial order is reversed
                case 2:             			// Amp_Bottom
                    // Do not accept an absolute value that is greater or equal to 1000
                    if(Math.abs(tmp)>=1000)
                        break;
                    // If the value of the amplification is positive, changes sign
                    if (tmp > 0)
                        tmp = -tmp;
                    Amp_Bottom = tmp;
                    break;
                case 3:             // Amp_Top
                    // Do not accept an absolute value that is greater or equal to 1000
                    if(Math.abs(tmp)>=1000)
                        break;
                    // If the value of the amplification is positive, changes sign
                    if (tmp > 0)
                        tmp = -tmp;
                    Amp_Top = tmp;
                    break;
                }
            }
            // If End_Freq <Start_Freq, exchange values
            if (End_Freq < Start_Freq)
            {
                tmp = End_Freq;
                End_Freq = Start_Freq;
                Start_Freq = tmp;
            }
            // If Amp_Top <Amp_Bottom, exchange values
            if (Amp_Top < Amp_Bottom)
            {
                tmp = Amp_Top;
                Amp_Top = Amp_Bottom;
                Amp_Bottom = tmp;
            }
        }
		
    }					// end of private class RFEConfiguration

    RFEConfiguration RfCfg = new RFEConfiguration();      // configurazione strumento

    // gestione messaggio lettura dati
    RxRfMsg RxMsg = new RxRfMsg();

    //-----------------------------------------------------------

    // An Executor that provides methods to manage termination and methods that can produce a Future for tracking progress of one or more asynchronous tasks
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private SerialInputOutputManager mSerialIoManager;

    private final SerialInputOutputManager.Listener mListener =
        new SerialInputOutputManager.Listener()
    {

        @Override
        public void onRunError(Exception e)
        {
            Log.d(TAG, "Runner stopped.");
        }

        @Override
        public void onNewData(final byte[] data)
        {
            // Runs the specified action on the UI thread.
            // If the current thread is the UI thread, then the action is executed immediately.
            // If the current thread is not the UI thread, the action is posted to the event queue of the UI thread.
            // action: the action to run on the UI thread
            RfTrackActivity.this.runOnUiThread
            (
                new Runnable()
            {
                @Override
                public void run()
                {
                    RfTrackActivity.this.updateReceivedData(data);
                }
            }
            );
        }		// end public void onNewData
    };			// end new SerialInputOutputManager.Listener

    // The entire lifetime of an activity happens between the first call to onCreate(Bundle) through to a single final call to onDestroy().
    // An activity will do all setup of "global" state in onCreate(), and release all remaining resources in onDestroy().
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        // super e' un puntatore implicito alla classe madre. Punta sempre alla classe estesa da quella che stai usando.
        // the function onCreate(Bundle savedInstanceState). is called when the activity is first created, it should contain all the initialization and UI setup.
        super.onCreate(savedInstanceState);			// super is used inside a sub-class method definition to call a method defined in the super class

        // R is a class containing the definitions for all resources of a particular application package.
        // setContentView(R.layout.main); is method which dsiplays the layout definition in main.xml file in res/layout directory
        // by accessing a reference to it from the R class.
        //This method is required to display the user interface, if the activity does not include this function then it would display a blank screen
        // Activity class takes care of creating a window for you in which you can place your UI with setContentView(View)
        setContentView(R.layout.main);

		// @@@ mr
		global_context = this;
		
		//---------------------------------------------------------
        // per gestione gps
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		//---------------------------------------------------------
		
		
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        // These elements are defined in main.xml
        // The basic unit of an Android application is an Activity.
        // An Activity displays the user interface of your application,
        // which may contain widgets like buttons, labels, text boxes, etc.
        // Typically, you define your UI using an XML file (for example, the main.xml file located in the res/layout folder),
        mTitleTextView = (TextView) findViewById(R.id.rfexploTitle);
        mDumpTextView = (TextView) findViewById(R.id.rfexploText);
        mScrollView = (ScrollView) findViewById(R.id.rfexploScroller);

        // components from main.xml
        button = (Button) findViewById(R.id.button);
        editTextMainScreen = (EditText) findViewById(R.id.editTextResult);
		
		// show parameters info
        String msg = (RfCfg.strInfoParam());
        editTextMainScreen.setText(msg);

		// mr: set the font used to display data
		SetTextFontSize();
		
		//-------------------------------------
		// 18/11/2014: init tone generator. 
		// A tone is generated for each group of measurements acquired by the instrument
        tone = new ToneGenerator(AudioManager.STREAM_DTMF, 100);
		//-------------------------------------
		
        button.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                // get prompts.xml view
                LayoutInflater layoutInflater = LayoutInflater.from(context);

                View promptView = layoutInflater.inflate(R.layout.prompts, null);

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

                // set prompts.xml to be the layout file of the alertdialog builder
                alertDialogBuilder.setView(promptView);

                final EditText input = (EditText) promptView.findViewById(R.id.userInput);

                input.setText(RfCfg.strShowParam());

                // setup a dialog window
                alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                                   new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        // get user input and set it to result
                        editTextMainScreen.setText(input.getText());
                        try
                        {
                            //---------------------
							// Recognize the string configuration parameters of the program RfTrack
                            RfCfg.strSetParam(editTextMainScreen.getText().toString());
														
                            // send the message

							// Byte array containing the configuration message
							byte[] msgSendCfg = 
								RxMsg.MsgConfigurationData(
									RfCfg.Start_Freq,		// KHZ, Value of frequency span start (lower)
									RfCfg.End_Freq,			// KHZ, Value of frequency span end (higher)
									RfCfg.Amp_Top,			// dBm, Highest value of amplitude for GUI
									RfCfg.Amp_Bottom );		// dBm, Lowest value of amplitude for GUI

							// Convert the string message to display it
                            String msg = new String(msgSendCfg);			// conversione corretta byte[] to String
											
							// ok, original
                            // editTextMainScreen.setText("Tx Message: [" + msg + "]");		// show the message
							editTextMainScreen.setText(RfCfg.strInfoParam());		// show the message
							// aggiorna il log
                            Log.i("mr", "Msg protocollo:>" + msg);

							// cancella la textview del data logging
							mDumpTextView.setText("");
							
							// ========================== SET COMMUNICATION PARAMETERS
                            mSerialDevice.setParameters(baudRate, 8, 1, 0);
                            // mSerialDevice.setParameters(500000, 8, 1, 0);
							
                            mSerialDevice.write(msgSendCfg, 1000);			// send the message

							// chiusura del file CSV precedente e apertura di uno nuovo
							// ci pensa il garbage collector a rilasciare la memoria
							CloseCsvFile();
							OpenCsvFile();			// crea un nuovo file CSV
							
                            //---------------------
                        }
                        catch (IOException e2)
                        {
                            // Ignore.
                        }
                    }
                })
                .setNegativeButton("Cancel",
                                   new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        dialog.cancel();
                    }
                });

                // create an alert dialog
                AlertDialog alertD = alertDialogBuilder.create();
                alertD.show();
            }
        });
    }				// end onCreate

	
    // onPause() is where you deal with the user leaving your activity.
    // Most importantly, any changes made by the user should at this point be committed
    // (usually to the ContentProvider holding the data).
    @Override
    protected void onPause()
    {
        super.onPause();			// super is used inside a sub-class method definition to call a method defined in the super class
        stopIoManager();
		
		//------------------------------------------
		// per gps
        locationManager.removeUpdates(gpsLocationListener);
		//------------------------------------------
		
        if (mSerialDevice != null)
        {
            try
            {
                mSerialDevice.close();
            }
            catch (IOException e)
            {
                // Ignore.
            }
            mSerialDevice = null;
        }
    }				// end onPause

    // The foreground lifetime of an activity happens between a call to onResume() until a corresponding call to onPause().
    // During this time the activity is in front of all other activities and interacting with the user.
    @Override
    protected void onResume()
    {
        super.onResume();			// super is used inside a sub-class method definition to call a method defined in the super class
        mSerialDevice = UsbSerialProber.acquire(mUsbManager);
		
		//----------------------------------------------------------------
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 0, gpsLocationListener);
		//----------------------------------------------------------------
		
        Log.d(TAG, "Resumed, mSerialDevice=" + mSerialDevice);
        if (mSerialDevice == null)
        {
            mTitleTextView.setText("No serial device.");
        }
        else
        {
            // if(!(editTextMainScreen.getText().toString().isEmpty()))
            {
                try
                {
                    mSerialDevice.open();

                    //----------------------------------------------------
                    // apri il file per la scrittura
                    OpenCsvFile();
                    // OpenFile();
                    //----------------------------------------------------

                    try
                    {
                        //---------------------
                        // mr: invio del messaggio
                        // String msg = "# C2-F:0400000,0500000,-050,-120";
                        // String msg = "# C2-F:" + (editTextMainScreen.getText().toString());

						// vettore di byte contenente il messaggio di configurazione
						byte[] msgSendCfg = 
							RxMsg.MsgConfigurationData(
								RfCfg.Start_Freq,		// KHZ, Value of frequency span start (lower)
								RfCfg.End_Freq,			// KHZ, Value of frequency span end (higher)
								RfCfg.Amp_Top,			// dBm, Highest value of amplitude for GUI
								RfCfg.Amp_Bottom );		// dBm, Lowest value of amplitude for GUI
                        
						// ========================== SET COMMUNICATION PARAMETERS
						mSerialDevice.setParameters(baudRate, 8, 1, 0);
						// mSerialDevice.setParameters(500000, 8, 1, 0);
						
						mSerialDevice.write(msgSendCfg, 500);					// send the message
                    }
                    catch (IOException e2)
                    {
                        // Ignore.
                    }
                }
                catch (IOException e)
                {
                    Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
                    mTitleTextView.setText("Error opening device: " + e.getMessage());
                    try
                    {
                        mSerialDevice.close();
                    }
                    catch (IOException e2)
                    {
                        // Ignore.
                    }
                    mSerialDevice = null;
                    return;
                }
            }
            mTitleTextView.setText("Serial device: " + mSerialDevice);
        }
        onDeviceStateChange();
    }			// end onResume

    private void stopIoManager()
    {
        if (mSerialIoManager != null)
        {
            Log.i(TAG, "Stopping io manager ..");
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }

    private void startIoManager()
    {
        if (mSerialDevice != null)
        {
            Log.i(TAG, "Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(mSerialDevice, mListener);
            mExecutor.submit(mSerialIoManager);
        }
    }

    private void onDeviceStateChange()
    {
        stopIoManager();
        startIoManager();
    }

    private void updateReceivedData(byte[] data)
    {
		boolean initMaxDB = false;

		// for debugging:
        // final String message = "Read " + data.length + " bytes: \n" + HexDump.dumpHexString(data) + "\n\n";
		
		// ok:
		String message;
							   
		if(RxMsg.updateRx(data))
        {
			// The message has been completely received

            //-----------------------------------------------------
            // write the data in the CSV file
			if(Idx_Letture == 0L)
			{
				initMaxDB = true;
				WriteCsvData(dumpCsvFreq(RxMsg.GetMsg()));
			}
            WriteCsvData(dumpCsvString(RxMsg.GetMsg()));
			Idx_Letture = Idx_Letture + 1;
            //-----------------------------------------------------

			//-----------------------------------------
			// sound signaling storing a group of measures
			// a beep is generated each 32 readings
			if((Idx_Letture % 10) == 0)
			{
				// TONE_SUP_INTERCEPT_ABBREV=30
				tone.startTone(30,100);
			}
			//-----------------------------------------
			
			// update the info on screen
			
			/***
			//----------------------
			// dbg: update the info in hex
			// mDumpTextView.append(message);
            mDumpTextView.append(HexDump.dumpHexString(RxMsg.GetMsg()));
            RxMsg.clean();
            mScrollView.smoothScrollTo(0, mDumpTextView.getBottom());
			//----------------------
			***/
			
			// ok:
			// show the maximum values
			if(ChkMaxDB(RxMsg.GetMsg(), initMaxDB) == true)
			{
				DecimalFormat df = new DecimalFormat("* ########0.000000");
				
				message = "[Latitude: " + df.format(Latitude) +
							" - Longitude: " + df.format(Longitude) +
							"]    Freq.: " + FormatterFreq.format(MaxDBFreq) + 
							" . Max dBm: " + FormatterDBval.format(MaxDB) + "\n";
							// for testing
							// " . Max dBm: " + FormatterDBmax.format(MaxDB) + "[" + String.format("%04X", MaxDBbyte) + "]" + "\n";
	            
				// // update the screen
	            mDumpTextView.append(message);
				mScrollView.smoothScrollTo(0, mDumpTextView.getBottom());
			};
            RxMsg.clean();
            // mScrollView.smoothScrollTo(0, mDumpTextView.getBottom());
        }
    }			// updateReceivedData
	
	//------------------------------------------------
	// utility
	//
	// j2xx.hyperterm
	//
	// call this API to show message
    void midToast(String str, int showTime)
    {
		Toast toast = Toast.makeText(global_context, str, showTime);			
		toast.setGravity(Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL , 0, 0);
		
		TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
		v.setTextColor(Color.YELLOW);
		v.setTextSize(contentFontSize);
		toast.show();	
    }

}		// end class RfTrackActivity
