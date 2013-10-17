package sun;

import java.awt.AWTException;
import java.io.Console;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

//Based off of: http://williams.best.vwh.net/sunrise_sunset_algorithm.htm
//Validated against: http://williams.best.vwh.net/sunrise_sunset_example.htm

public class Sun {
	
	private boolean debug = false;
	private int sunsetHour;
	private int sunsetMinute;
	private int sunsetSecond;
	private int sunriseHour;
	private int sunriseMinute;
	private int sunriseSecond;
	private Date sunset_time;
	private Date sunrise_time;
	private double zenith = 90.833;
	private int offset = - 4;
	private int day, month, year;
	private double longitude, latitude;
	
	public static void main(String[] args) throws ParseException  {
		Sun s = new Sun(42.5869, -82.9200);
			s.setDate(9,6,2013);
			s.calculate();
			s.printSunset();
			s.printSunrise();
			System.out.println();
	}
	
	public Sun(double lat, double lon) {
		latitude = lat;
		longitude = lon;
	}
	
	public void setDate(int d, int m, int y) {
		day = d;
		month = m;
		year = y;
		
	}
	
	public void calculate() throws ParseException {
		int dayNum;
		double meanA, latToHour, trueLong, RA, localHour, meanTime, utc, sunset;
		double SRmeanA, SRlatToHour, SRtrueLong, SRRA, SRlocalHour, SRmeanTime, SRutc, sunrise;
		DateFormat df, display;
		
		//CTP Lat/long
	    //latitude = 42.5869;
	    //longitude = -82.9200;
		
		//Test Case Lat/Long
		//latitude = 40.9;
		//longitude = -74.3;
		
		dayNum = dayOfTheYear(month, day, year);
		
		latToHour   = convLatoHour (dayNum);
		SRlatToHour = SRconvLatoHour(dayNum);
		
		meanA = meanAnomaly(latToHour);
		SRmeanA = meanAnomaly(SRlatToHour);
		
		trueLong   = calcTrueLong(meanA);
		SRtrueLong = calcTrueLong(SRmeanA);
		
		RA   = calcRightAscension(trueLong);
		SRRA = calcRightAscension(SRtrueLong);
		
		localHour   = sunsLocalHour(trueLong);
		SRlocalHour = SRsunsLocalHour(SRtrueLong);

		meanTime   = meanTimeofSetting(RA, localHour,latToHour);
		SRmeanTime = meanTimeofSetting(SRRA, SRlocalHour, SRlatToHour);

		utc   = convToUTC(meanTime);
		SRutc = convToUTC(SRmeanTime);
		
		sunset  = utc + offset;
		sunrise = SRutc + offset;
		
		sunsetHour   = (int) sunset;
		sunsetMinute = (int) ((sunset - sunsetHour) * 60);
		sunsetSecond = (int) (((sunset - sunsetHour) * 3600) - sunsetMinute * 60);

		sunriseHour   = (int) sunrise;
		sunriseMinute = (int) ((sunrise - sunriseHour) * 60);
		sunriseSecond = (int) (((sunrise - sunriseHour) * 3600) - sunriseMinute * 60);
		
		df = new SimpleDateFormat("HH:mm:ss");
		display = new SimpleDateFormat("h:mma");
		
		try {
			sunset_time  = df.parse(sunsetHour+":"+sunsetMinute+":"+sunsetSecond);
			sunrise_time = df.parse(sunriseHour+":"+sunriseMinute+":"+sunriseSecond);

		} catch (ParseException e) {
			// TODO Auto-generated catch block
			System.out.println ("BAD DATE DATA");
			e.printStackTrace();
		}
		
		
		if(debug) {
			System.out.println("Day of the year: " + dayNum);
	
			System.out.println("Latitude to hour(t): " + latToHour);
	
			System.out.println("Sun's Mean Anomaly: " + meanA);
	
			System.out.println("Sun's True Longitude (L): " + trueLong);
	
			System.out.println("Right Ascension: " + RA);
	
			System.out.println("Local Hour(H): " + localHour);
	
			System.out.println("Local mean time (T): " + meanTime);
	
			System.out.println("UTC Sunset: " + utc);
			
			System.out.println("Sunset: " + sunset);
			
			System.out.println("Sunset is at: " + sunsetHour + ":" + sunsetMinute);
			
			System.out.println(display.format(sunset_time));
			System.out.println(display.format(sunrise_time));
		}
		
	}

	public Date getSunset() {
		return sunset_time;
	}
	
	public Date getSunrise() {
		return sunrise_time;
	}
	
	public void printSunset() {
		DateFormat display = new SimpleDateFormat("h:mm:ssa");
		System.out.println(display.format(sunset_time));
	}
	
	public void printSunrise() {
		DateFormat display = new SimpleDateFormat("h:mm:ssa");
		System.out.println(display.format(sunrise_time));
	}
	//Private Methods
	
	
	private int dayOfTheYear(int month, int day, int year) {
		int N1, N2, N3;
		N1 = (int) Math.floor(275 * month / 9);
		N2 = (int) Math.floor((month + 9) / 12);
		N3 = (int) (1 + Math.floor((year - 4 * Math.floor(year / 4) + 2) / 3));
		return (N1 - (N2 * N3) + day - 30);
	}

	private double convLatoHour(int dayofyear) {		
		//Formula for converting latitude to local time for sunset.
		return ( dayofyear + ((18 - (longitude / 15)) / 24));
		//for sunrise
		//return(dayofyear + ((6 - (longitude / 15))/24));
	}
	
	private double SRconvLatoHour(int dayofyear) {		
		//Formula for converting latitude to local time for sunset.
		//for sunrise
		return(dayofyear + ((6 - (longitude / 15))/24));
	}
	private double meanAnomaly(double latToHour) {
		return  ((0.9856 * latToHour) - 3.289);
	}

	//Step 4
	private double calcTrueLong(double meanAnomaly) {
		//Note all Degs have been converted to Radians by 180/3.14
		double L = (meanAnomaly + (1.916 * Math.sin((Math.PI/180)*meanAnomaly)) + (0.020 * Math.sin((Math.PI/180)*2 * meanAnomaly)) + 282.634);

		if (L > 360.0) {
			L = L - 360;
		} else if (L < 0) {
			L = L + 360;
		}


		return L;
	}

	private double calcRightAscension(double trueLong) {
		double rightAscension, lQuadrant, raQuadrant;

		rightAscension = (180/Math.PI) * Math.atan(0.91764 * Math.tan((Math.PI/180) * trueLong));
		
		if (rightAscension > 360.0) {
			rightAscension  = rightAscension  - 360;
		} else if (rightAscension  < 0) {
			rightAscension  = rightAscension  + 360;
		}
	
		
		lQuadrant  = Math.floor( trueLong/90) * 90;
		raQuadrant = Math.floor(rightAscension/90) * 90;
		rightAscension = rightAscension + (lQuadrant - raQuadrant);
		
		rightAscension = rightAscension / 15;
		return rightAscension;
	}

	private double sunsLocalHour(double trueLong) {
		double localHour;
		double sinDec = 0.39782 * Math.sin((Math.PI/180) * trueLong);
		double cosDec = Math.cos(Math.asin(sinDec));
		double cosH = (Math.cos((Math.PI/180) * zenith) - (sinDec * Math.sin((Math.PI/180) * latitude))) / (cosDec * Math.cos((Math.PI/180) * latitude));
	
		if (debug) {
			System.out.println("Sin Dec: " + sinDec);
	
			System.out.println("Cos Dec: " + cosDec);
	
			System.out.println("CosH: " + cosH);
		}
		localHour = (180/Math.PI) * Math.acos(cosH);
		//sunrise
		//localHour = 360 - (180/Math.PI) * Math.acos(cosH);
		return localHour/15;
	}
	
	private double SRsunsLocalHour(double trueLong) {
		double localHour;
		double sinDec = 0.39782 * Math.sin((Math.PI/180) * trueLong);
		double cosDec = Math.cos(Math.asin(sinDec));
		double cosH = (Math.cos((Math.PI/180) * zenith) - (sinDec * Math.sin((Math.PI/180) * latitude))) / (cosDec * Math.cos((Math.PI/180) * latitude));
	
		if (debug) {
			System.out.println("Sin Dec: " + sinDec);
	
			System.out.println("Cos Dec: " + cosDec);
	
			System.out.println("CosH: " + cosH);
		}
		//sunrise
		localHour = 360 - (180/Math.PI) * Math.acos(cosH);
		return localHour/15;
	}


	private double meanTimeofSetting(double rightAscension, double localHour, double latToHour) {
		 return (localHour + rightAscension - (0.06571 * latToHour) - 6.622);

	}
	
	private double convToUTC(double meanTimeofSetting) {
		double utc;
		utc = meanTimeofSetting - (longitude / 15);
		if (utc > 24) {
			utc = utc - 24;
		} else if (utc < 0) {
			utc = utc + 24;
		}
		
		return utc;
		
	}
}