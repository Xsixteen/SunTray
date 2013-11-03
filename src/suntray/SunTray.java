package suntray;

import java.awt.AWTException;
import java.awt.CheckboxMenuItem;
import java.awt.Image;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;


import javax.swing.ImageIcon;
import javax.swing.Timer;

import sun.Sun;

public class SunTray {
	
	private static Sun s;
	private java.util.Date sunsetTime;
	private java.util.Date sunriseTime;

	private MenuItem sunset;
	private MenuItem sunrise;
	private MenuItem timeTo;
	private MenuItem tomDelt;
	
	
	private Image sun = Toolkit.getDefaultToolkit().getImage(getClass().getResource("sun-icon-md.png"));
	private Image moon = Toolkit.getDefaultToolkit().getImage(getClass().getResource("full-moon-icon-md.png"));

	
	private TrayIcon trayIcon;
	private static Timer pulse;


	public static void main(String[] args) throws AWTException, ParseException {

		// TODO Auto-generated method stub
		s = new Sun(42.5869, -82.9200);
	//	s.setDate(13, 10, 2013);
		
		final SunTray h = new SunTray();
		h.doUpdate();
	
        
        pulse = new Timer(60000, new ActionListener() {
        	   public void actionPerformed(ActionEvent evt) {
        		   	try {
						h.doUpdate();
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
        	   }
        });
        pulse.start();

	
	}
	
	public SunTray() {
		
	      //Check the SystemTray is supported
        if (!SystemTray.isSupported()) {
            System.out.println("SystemTray is not supported");
            return;
        }
        final PopupMenu popup = new PopupMenu();
        Image i  = Toolkit.getDefaultToolkit().createImage("src/suntray/sun-icon-md.png");
        trayIcon = new TrayIcon(sun,"tray icon");
        final SystemTray tray = SystemTray.getSystemTray();
       
        // Create a pop-up menu components
        sunset = new MenuItem("Sunset: ");
        sunset.setEnabled(false);
        sunrise = new MenuItem("Sunrise: ");
        sunrise.setEnabled(false);
        timeTo = new MenuItem("Time until Sunset: ");
        timeTo.setEnabled(false);
        tomDelt = new MenuItem("Tomorrow change: ");
        tomDelt.setEnabled(false);
       
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent arg0) {
                // TODO Auto-generated method stub
                System.exit(0);
            }
        });
        
        //Add components to pop-up menu
        popup.add(sunset);
        popup.add(sunrise);
        popup.addSeparator();
        popup.add(timeTo);
        popup.add(tomDelt);
        popup.addSeparator();
        popup.add(exitItem);
       
        trayIcon.setPopupMenu(popup);
       
        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            System.out.println("TrayIcon could not be added.");
        }
        

	}
	
	@SuppressWarnings("deprecation")
	public void curSunDate() throws ParseException {
		Date cur = new Date();
		
		s.setDate(cur.getDate(), cur.getMonth() + 1, cur.getYear());
		s.calculate();

	}
	
	public void doUpdate() throws ParseException {
		curSunDate();
		setTime(s.getSunset(), s.getSunrise());
		deltTime();
		tomDeltTime();
        chkIcon();
	}
	
	@SuppressWarnings("deprecation")
	public void setTime(java.util.Date d, java.util.Date r) throws ParseException {
		Date cur = new Date();
		DateFormat df = new SimpleDateFormat("hh:mma MMM dd yyyy");
		
		sunsetTime = d;
		sunriseTime = r;
		
		sunsetTime.setDate(cur.getDate());
		sunsetTime.setMonth(cur.getMonth());
		sunsetTime.setYear(cur.getYear());
		
		sunriseTime.setDate(cur.getDate());
		sunriseTime.setMonth(cur.getMonth());
		sunriseTime.setYear(cur.getYear());
		
		if(hasSunSet()) {
			sunset.setLabel("Sunset occured at: " + df.format(d));
			tomSunRise();
		} else {
			sunrise.setLabel("Sunrise: " + df.format(r));
			sunset.setLabel("Sunset: " + df.format(d));
		}
	}
	
	public void tomDeltTime() throws ParseException {
		Sun tomsun = new Sun(42.5869, -82.9200);

		Date cur = new Date();
		Date todaySunset, tomSunset, todaySunrise, tomSunrise;
		long millisSunset, millisSunrise, millis;
		Calendar c = Calendar.getInstance(); 
		
		c.setTime(cur); 
		c.add(Calendar.DATE, 1);
		cur = c.getTime();
		
		tomsun.setDate(cur.getDate(), cur.getMonth() + 1, cur.getYear());
		tomsun.calculate();
	
		todaySunset = s.getSunset();
		tomSunset = tomsun.getSunset();
		todaySunrise = s.getSunrise();
		tomSunrise = tomsun.getSunrise();
		
		millisSunset  = (tomSunset.getTime() - todaySunset.getTime());
		millisSunrise = (todaySunrise.getTime() - tomSunrise.getTime());
		millis = millisSunset + millisSunrise;
		
		tomDelt.setLabel("Tomorrow Time Delta: " + (TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis))) + " min and " + (TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))) + " sec");
	}
	
	public void tomSunRise() throws ParseException {
		DateFormat df = new SimpleDateFormat("hh:mma");
		Sun temp = new Sun(42.5869, -82.9200);
		Date cur = new Date();
		Calendar c = Calendar.getInstance(); 
		
		c.setTime(cur); 
		c.add(Calendar.DATE, 1);
		cur = c.getTime();
	 
		temp.setDate(cur.getDate(), cur.getMonth() + 1, cur.getYear());
		temp.calculate();
		
		sunrise.setLabel("Tomorrow's Sunrise: " + df.format(temp.getSunrise()));
	}
	
	public void deltTime() {
		DateFormat df = new SimpleDateFormat("hh:mm");
		long millisToSet = (sunsetTime.getTime()-System.currentTimeMillis());
		if(hasSunSet()) {
			timeTo.setLabel("Time since Sunset: " + TimeUnit.MILLISECONDS.toHours(millisToSet) + " hour and " + (TimeUnit.MILLISECONDS.toMinutes(millisToSet) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millisToSet))) + " minutes");
		} else {
			timeTo.setLabel("Time until Sunset: " + TimeUnit.MILLISECONDS.toHours(millisToSet) + " hour and " + (TimeUnit.MILLISECONDS.toMinutes(millisToSet) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millisToSet))) + " minutes");
		} 
	}
	
	public boolean hasSunSet() {
		long millisToSet  = (sunsetTime.getTime()-System.currentTimeMillis());
		long millisToRise = (System.currentTimeMillis()-sunriseTime.getTime());
		
		//Both values are positive if we're between sunrise and sunset.
		if(millisToSet > -1) {
			return false; 
		}
		
		return true;
	}
	
	public boolean hasSunRisen() {
		long millisToRise = (System.currentTimeMillis()-sunriseTime.getTime());
		
		//MillisToRise is positive if time is after sunrise. 
		if(millisToRise < -1) {
			return false; 
		}
		
		return true;
	}

	private void chkIcon() {
		if(hasSunSet() || !hasSunRisen()) {
			trayIcon.setImage(moon);
		} else {
			trayIcon.setImage(sun);
		}
	
	}
	

}
