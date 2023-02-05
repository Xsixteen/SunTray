package com.ericulicny.suntray;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;


import javax.swing.Timer;

import com.ericulicny.domain.Weather;
import com.ericulicny.weather.WeatherService;
import sun.Location;
import com.ericulicny.sun.Sun;

public class SunTray {
	
	//private static Sun s;
	private java.util.Date sunsetTime;
	private java.util.Date sunriseTime;

	private Sun currentSun;
	private Sun futureSun;
	private MenuItem sunset;
	private MenuItem sunrise;
	private MenuItem timeTo;
	private MenuItem tomDelt;
	private MenuItem dayLen;
	private MenuItem ttSummer;
	private MenuItem ttWinter;
	private MenuItem ttFall;
	private MenuItem ttSpring;
	private MenuItem maxDay;
	private MenuItem minDay;
	private MenuItem currentTemp;
	private MenuItem maxTemp;
	private MenuItem minTemp;
	private MenuItem curretConditions;


	private Image sunIcon 	= Toolkit.getDefaultToolkit().getImage(getClass().getClassLoader().getResource("sun-icon-md.png"));
	private Image moonIcon 	= Toolkit.getDefaultToolkit().getImage(getClass().getClassLoader().getResource("full-moon-icon-md.png"));

	
	private TrayIcon trayIcon;
	private static Timer pulse;
	private Location currentLocation;
	private static String version              = "v0.13";
	private static Integer CONST_TIMER_MS      = 5*60000;

	private WeatherService weatherService 	   = new WeatherService();

	private String weatherAPIKey			   = "647f8fa8c4890ee185692dc94807a4a0";

	public static void main(String[] args) throws AWTException, ParseException {
		Location clintonTwp = new Location(42.5869, -82.9200);
		
		final SunTray h = new SunTray(clintonTwp);
		h.doUpdate();
	
        
        pulse = new Timer(CONST_TIMER_MS, new ActionListener() {
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
	
	public SunTray(Location location) {
	    this.currentLocation = location;
		this.currentSun      = new Sun(this.currentLocation);
		this.futureSun       = new Sun(this.currentLocation);
				
	      //Check the SystemTray is supported
        if (!SystemTray.isSupported()) {
            System.out.println("SystemTray is not supported");
            return;
        }
        final PopupMenu popup = new PopupMenu();
        Image i  = Toolkit.getDefaultToolkit().createImage("src/suntray/sun-icon-md.png");
        trayIcon = new TrayIcon(sunIcon,"tray icon");
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
        dayLen = new MenuItem("Today's Day Length: ");
        dayLen.setEnabled(false);
        ttWinter = new MenuItem("Days until Summer Solstice: ");
        ttWinter.setEnabled(false);
        ttSummer = new MenuItem("Days until Winter Solstice: ");
        ttSummer.setEnabled(false);
        ttFall = new MenuItem("Days until Fall Equinox: ");
        ttFall.setEnabled(false);
        ttSpring = new MenuItem("Days until Spring Equinox: ");
        ttSpring.setEnabled(false);
        maxDay = new MenuItem("Maximum Day Length: ");
        maxDay.setEnabled(false);
        minDay = new MenuItem("Minimum Day Length: ");
        minDay.setEnabled(false);

		curretConditions = new MenuItem("Current Weather: ");
		curretConditions.setEnabled(false);
		currentTemp = new MenuItem("Current Temp: ");
		currentTemp.setEnabled(false);
		maxTemp = new MenuItem("High Temp: ");
		maxTemp.setEnabled(false);
		minTemp = new MenuItem("Low Temp: ");
		minTemp.setEnabled(false);
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
        popup.add(dayLen);
        popup.addSeparator();
        popup.add(timeTo);
        popup.add(tomDelt);
        popup.addSeparator();
        popup.add(ttWinter);
        popup.add(ttSpring);
        popup.add(ttSummer);
        popup.add(ttFall);
        popup.addSeparator();
        popup.add(maxDay);
        popup.add(minDay);
        popup.addSeparator();
        popup.add(curretConditions);
		popup.add(currentTemp);
		popup.add(maxTemp);
		popup.add(minTemp);
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
	public void currentSunDate() throws ParseException {
		Date cur = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(cur);
		
		currentSun.setDate(cur.getDate(), cur.getMonth() + 1, cal.get(Calendar.YEAR));
		currentSun.calculate();
		
	}
	
	public void calculateMaxMins() throws ParseException {	
		
		Calendar calSunset	 = Calendar.getInstance(); 
		Calendar calSunrise  = Calendar.getInstance(); 
	
		
		Date cur = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(cur);
		futureSun.setDate(21, 6, cal.get(Calendar.YEAR));
		futureSun.calculate();
		
		calSunset.setTime(futureSun.getSunset()); 
		calSunset.set(Calendar.YEAR, 2016);
		calSunset.set(Calendar.DAY_OF_MONTH, 21);
		calSunset.set(Calendar.MONTH, 21);
		calSunrise.setTime(futureSun.getSunrise());
		calSunrise.set(Calendar.YEAR, 2016);
		calSunrise.set(Calendar.DAY_OF_MONTH, 21);
		calSunrise.set(Calendar.MONTH, 21);
		
		genericDayLength(calSunset.getTime(), calSunrise.getTime(), maxDay, "Maximum Day Length: " );
		
		futureSun.setDate(21, 12, cal.get(Calendar.YEAR));
		futureSun.calculate();
		genericDayLength(futureSun.getSunset(), futureSun.getSunrise(), minDay, "Minimum Day Length: " );
		
	}
	
	public void doUpdate() throws ParseException {
	    this.currentSun = new Sun(this.currentLocation);
	    this.futureSun  = new Sun(this.currentLocation);
	    
		currentSunDate();
		setTime(currentSun.getSunset(), currentSun.getSunrise());
		deltaTime();
		tommorrowDeltaTime();
		dayLength(currentSun.getSunset(), currentSun.getSunrise());
        checkIcon();
        daysUntilEvent();
        calculateMaxMins();

        Weather weather = weatherService.getTodaysWeather("berkley", weatherAPIKey);
		setWeather(weather);
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
			tomorrowSunRise();
		} else {
			sunrise.setLabel("Sunrise: " + df.format(r));
			sunset.setLabel("Sunset: " + df.format(d));
		}
	}
	
	public void tommorrowDeltaTime() throws ParseException {
		Sun tomsun = new Sun(42.5869, -82.9200);

		Date cur = new Date();
		Date todaySunset, tomSunset, todaySunrise, tomSunrise;
		long millisSunset, millisSunrise, millis;
		Calendar c = Calendar.getInstance(); 
		
		c.setTime(cur); 
		c.add(Calendar.DATE, 1);
		cur = c.getTime();
		
		tomsun.setDate(cur.getDate(), cur.getMonth()+1, c.get(Calendar.YEAR));
		tomsun.calculate();
	
		todaySunset = currentSun.getSunsetCalendar().getTime();
		tomSunset = tomsun.getSunsetCalendar().getTime();
		
		todaySunrise = currentSun.getSunriseCalendar().getTime();
		tomSunrise = tomsun.getSunriseCalendar().getTime();
	
		long todayDuration    = todaySunset.getTime() - todaySunrise.getTime();
		long tomorrowDuration = tomSunset.getTime() - tomSunrise.getTime();

		long delta = tomorrowDuration - todayDuration;
	    tomDelt.setLabel("Tomorrow Time Delta: " +TimeUnit.MILLISECONDS.toMinutes(delta) + " min and " + (TimeUnit.MILLISECONDS.toSeconds(delta) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(delta))) + " sec");
	}
	
	public void tomorrowSunRise() throws ParseException {
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
	
	public void deltaTime() {
		DateFormat df = new SimpleDateFormat("hh:mm");
		long millisToSet = (sunsetTime.getTime()-System.currentTimeMillis());
		if(hasSunSet()) {
			timeTo.setLabel("Time since Sunset: " + TimeUnit.MILLISECONDS.toHours(millisToSet) + " hour and " + (TimeUnit.MILLISECONDS.toMinutes(millisToSet) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millisToSet))) + " minutes");
		} else {
			timeTo.setLabel("Time until Sunset: " + TimeUnit.MILLISECONDS.toHours(millisToSet) + " hour and " + (TimeUnit.MILLISECONDS.toMinutes(millisToSet) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millisToSet))) + " minutes");
		} 
	}
	
	public void dayLength(java.util.Date sunset, java.util.Date sunrise) {
		long milliseconds = (sunset.getTime() - sunrise.getTime());
		dayLen.setLabel("Today's Day Length: " + TimeUnit.MILLISECONDS.toHours(milliseconds) + " hours and " + (TimeUnit.MILLISECONDS.toMinutes(milliseconds) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milliseconds))) + " minutes");
	}
	
	public void genericDayLength(java.util.Date sunset, java.util.Date sunrise, MenuItem menuItem, String label) {
	
		
		long milliseconds = (sunset.getTime() - sunrise.getTime());
		 
		menuItem.setLabel(label + TimeUnit.MILLISECONDS.toHours(milliseconds) + " hours and " + (TimeUnit.MILLISECONDS.toMinutes(milliseconds) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milliseconds))) + " minutes");
	}
	
	
	public void daysUntilEvent() {
		Calendar cal		= Calendar.getInstance();
		final String summer = "21-06-";
		final String winter = "21-12-";
		final String spring = "21-03-";
		final String fall	= "21-09-";

		String year	 		= new SimpleDateFormat("yyyy").format(cal.getTime());
		cal.add(Calendar.YEAR, 1); 
		String yearPlusOne  = new SimpleDateFormat("yyyy").format(cal.getTime()); 


		String today  		= new SimpleDateFormat("dd-MM-yyyy").format(Calendar.getInstance().getTime());

		
	    try {
	    	if(this.convertDateToTimeFormat(today) < this.convertDateToTimeFormat(spring+year))
	    		ttSpring.setLabel("Days until Spring Equinox: " + this.numberOfDaysBetween(today, spring+year) + " Days");
	    	else
	    		ttSpring.setLabel("Days until Spring Equinox: " + this.numberOfDaysBetween(today, spring+yearPlusOne) + " Days");
	    	
	    	if(this.convertDateToTimeFormat(today) < this.convertDateToTimeFormat(summer+year))
	    		ttSummer.setLabel("Days until Summer Solstice: " + this.numberOfDaysBetween(today, summer+year) + " Days");
	    	else
	    		ttSummer.setLabel("Days until Summer Solstice: " + this.numberOfDaysBetween(today, summer+yearPlusOne) + " Days");
	    	
	    	if(this.convertDateToTimeFormat(today) < this.convertDateToTimeFormat(fall+year))
	    		ttFall.setLabel("Days until Fall Equinox: " + this.numberOfDaysBetween(today, fall+year) + " Days");
	    	else
	    		ttFall.setLabel("Days until Fall Equinox: " + this.numberOfDaysBetween(today, fall+yearPlusOne) + " Days");
	    	
	    	
	    	if(this.convertDateToTimeFormat(today) < this.convertDateToTimeFormat(winter+year))
	    		ttWinter.setLabel("Days until Winter Solstice: " + this.numberOfDaysBetween(today, winter+year) + " Days");
	    	else
	    		ttWinter.setLabel("Days until Winter Solstice: " + this.numberOfDaysBetween(today, winter+yearPlusOne) + " Days");

		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

	public void setWeather(Weather weather) {
		curretConditions.setLabel("Current Weather: " + weather.getCurrentWeather());
		currentTemp.setLabel("Current Temp: " + weather.getCurrentTempF() + '\u00B0' + "F");
		maxTemp.setLabel("Max Temp: " + weather.getMaxTempF() + '\u00B0' + "F");
		minTemp.setLabel("Min Temp: " + weather.getMinTempF() + '\u00B0' + "F");

	}

//=========PRIVATE Methods
	private long numberOfDaysBetween(String inputDate1, String inputDate2) throws ParseException {
		SimpleDateFormat myFormat = new SimpleDateFormat("dd-MM-yyyy");
	    Date date1 = myFormat.parse(inputDate1);
	    Date date2 = myFormat.parse(inputDate2);
	    long diff  = date2.getTime() - date1.getTime();
	    long days  = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
	    
	    return days;
	}
	
	private long convertDateToTimeFormat(String inputDate1) throws ParseException {
		SimpleDateFormat myFormat = new SimpleDateFormat("dd-MM-yyyy");
	    Date date1 = myFormat.parse(inputDate1);
	    long millis  = date1.getTime();
	    
	    return millis;
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

	private void checkIcon() {
		if(hasSunSet() || !hasSunRisen()) {
			trayIcon.setImage(moonIcon);
		} else {
			trayIcon.setImage(sunIcon);
		}
	
	}
	

}
