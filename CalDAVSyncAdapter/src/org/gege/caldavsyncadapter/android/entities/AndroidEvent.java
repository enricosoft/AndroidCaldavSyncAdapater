/**
 * Copyright (c) 2012-2013, Gerald Garcia, Timo Berger
 * 
 * This file is part of Andoid Caldav Sync Adapter Free.
 *
 * Andoid Caldav Sync Adapter Free is free software: you can redistribute 
 * it and/or modify it under the terms of the GNU General Public License 
 * as published by the Free Software Foundation, either version 3 of the 
 * License, or at your option any later version.
 *
 * Andoid Caldav Sync Adapter Free is distributed in the hope that 
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied 
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Andoid Caldav Sync Adapter Free.  
 * If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package org.gege.caldavsyncadapter.android.entities;

import java.net.SocketException;
import java.net.URISyntaxException;
import java.text.ParseException;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.parameter.PartStat;
import net.fortuna.ical4j.model.parameter.Role;
import net.fortuna.ical4j.model.parameter.Rsvp;
import net.fortuna.ical4j.model.property.Attendee;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Clazz;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.Duration;
import net.fortuna.ical4j.model.property.ExDate;
import net.fortuna.ical4j.model.property.ExRule;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.Organizer;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.RDate;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.Status;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.util.UidGenerator;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Events;

public class AndroidEvent {
	public static String ceTAG = Events.SYNC_DATA1;
	public static String cInternalTag = Events.SYNC_DATA2;
	public static String cRawData = Events.SYNC_DATA3;
	
	private Uri muri;
	private Uri mcalendarUri;
	public ContentValues ContentValues = new ContentValues();	
	private PropertyList mAttendees = new PropertyList();

	private Calendar mCalendar = null;
	
	public AndroidEvent(Uri uri, Uri calendarUri) {
		super();
		muri = uri;
		mcalendarUri = calendarUri;
	}
	
	public Uri getUri() {
		return muri;
	}

	public Uri getCalendarUri() {
		return mcalendarUri;
	}

	public String getETag() {
		String Result = "";
		if (this.ContentValues.containsKey(ceTAG))
			Result = this.ContentValues.getAsString(ceTAG);
		return Result;
	}

	public void setETag(String eTag) {
		this.ContentValues.put(ceTAG, eTag);
	}

	@Override
	public String toString() {
		return muri.toString();
	}

	public boolean readContentValues(Cursor cur) {
		this.setETag(cur.getString(cur.getColumnIndex(AndroidEvent.ceTAG)));

		this.ContentValues.put(Events.EVENT_TIMEZONE, cur.getString(cur.getColumnIndex(Events.EVENT_TIMEZONE)));
		this.ContentValues.put(Events.EVENT_END_TIMEZONE, cur.getString(cur.getColumnIndex(Events.EVENT_END_TIMEZONE)));
		this.ContentValues.put(Events.DTSTART, cur.getLong(cur.getColumnIndex(Events.DTSTART)));
		this.ContentValues.put(Events.DTEND, cur.getLong(cur.getColumnIndex(Events.DTEND)));
		this.ContentValues.put(Events.ALL_DAY, cur.getLong(cur.getColumnIndex(Events.ALL_DAY)));
		this.ContentValues.put(Events.TITLE, cur.getString(cur.getColumnIndex(Events.TITLE)));
		this.ContentValues.put(Events.CALENDAR_ID, cur.getString(cur.getColumnIndex(Events.CALENDAR_ID)));
		this.ContentValues.put(Events._SYNC_ID, cur.getString(cur.getColumnIndex(Events._SYNC_ID)));
		//this.ContentValues.put(Events.SYNC_DATA1, cur.getString(cur.getColumnIndex(Events.SYNC_DATA1))); //not needed here, eTag has already been read
		this.ContentValues.put(Events.DESCRIPTION, cur.getString(cur.getColumnIndex(Events.DESCRIPTION)));
		this.ContentValues.put(Events.EVENT_LOCATION, cur.getString(cur.getColumnIndex(Events.EVENT_LOCATION)));
		this.ContentValues.put(Events.ACCESS_LEVEL, cur.getInt(cur.getColumnIndex(Events.ACCESS_LEVEL)));
		
		this.ContentValues.put(Events.STATUS, cur.getInt(cur.getColumnIndex(Events.STATUS)));
		
		this.ContentValues.put(Events.LAST_DATE, cur.getInt(cur.getColumnIndex(Events.LAST_DATE)));
		this.ContentValues.put(Events.DURATION, cur.getString(cur.getColumnIndex(Events.DURATION)));

		this.ContentValues.put(Events.RDATE, cur.getString(cur.getColumnIndex(Events.RDATE)));
		this.ContentValues.put(Events.RRULE, cur.getString(cur.getColumnIndex(Events.RRULE)));
		this.ContentValues.put(Events.EXRULE, cur.getString(cur.getColumnIndex(Events.EXRULE)));
		this.ContentValues.put(Events.EXDATE, cur.getString(cur.getColumnIndex(Events.EXDATE)));		
		this.ContentValues.put(Events.DIRTY, cur.getInt(cur.getColumnIndex(Events.DIRTY)));
		
		return true;
	}
	
	public boolean readAttendees(Cursor cur) {
		Attendee attendee = null;
		Organizer organizer = null;
		ParameterList paraList = null;

		String Name = "";
		Cn cn = null;

		String Email = "";

		int Relationship = 0;
		
		
		int Status = 0;
		PartStat partstat = null;

		int Type = 0;
		Role role = null;
		
		try {
			while (cur.moveToNext()) {
				Name         = cur.getString(cur.getColumnIndex(Attendees.ATTENDEE_NAME));
				Email        = cur.getString(cur.getColumnIndex(Attendees.ATTENDEE_EMAIL));
				Relationship = cur.getInt(cur.getColumnIndex(Attendees.ATTENDEE_RELATIONSHIP));
				Type         = cur.getInt(cur.getColumnIndex(Attendees.ATTENDEE_TYPE));
				Status       = cur.getInt(cur.getColumnIndex(Attendees.ATTENDEE_STATUS));
				
				if (Relationship == Attendees.RELATIONSHIP_ORGANIZER) {
					organizer = new Organizer();
					organizer.setValue("mailto:" + Email);
					paraList = organizer.getParameters();
					mAttendees.add(organizer);
				} else {
					attendee = new Attendee();
					attendee.setValue("mailto:" + Email);
					paraList = attendee.getParameters();
					mAttendees.add(attendee);
				}
				
				Rsvp rsvp = new Rsvp(true);
				paraList.add(rsvp);

				cn = new Cn(Name);
				paraList.add(cn);
				
				if (Status == Attendees.ATTENDEE_STATUS_INVITED)
					partstat = new PartStat(PartStat.NEEDS_ACTION.getValue());
				else if (Status == Attendees.ATTENDEE_STATUS_ACCEPTED)
					partstat = new PartStat(PartStat.ACCEPTED.getValue());
				else 
					partstat = new PartStat(PartStat.NEEDS_ACTION.getValue());
				paraList.add(partstat);

				if (Type == Attendees.TYPE_OPTIONAL)
					role = new Role(Role.OPT_PARTICIPANT.getValue());
				else if (Type == Attendees.TYPE_NONE)
					role = new Role(Role.NON_PARTICIPANT.getValue()); //regular participants in android are non required?
				else if (Type == Attendees.TYPE_REQUIRED)
					role = new Role(Role.REQ_PARTICIPANT.getValue());
				else
					role = new Role(Role.NON_PARTICIPANT.getValue());
				paraList.add(role);
			}

		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return true;
	}
	
	public boolean readReminder(Cursor cur) {
		
		return true;
	}
	
	public java.util.ArrayList<String> getComparableItems() {
		java.util.ArrayList<String> Result = new java.util.ArrayList<String>();
		Result.add(Events.DTSTART);
		Result.add(Events.DTEND);
		Result.add(Events.EVENT_TIMEZONE);
		Result.add(Events.EVENT_END_TIMEZONE);
		Result.add(Events.ALL_DAY);
		Result.add(Events.DURATION);
		Result.add(Events.TITLE);
		Result.add(Events.CALENDAR_ID);
		Result.add(Events._SYNC_ID);
		//Result.add(Events.SYNC_DATA1);
		Result.add(AndroidEvent.ceTAG);
		Result.add(Events.DESCRIPTION);
		Result.add(Events.EVENT_LOCATION);
		Result.add(Events.ACCESS_LEVEL);
		Result.add(Events.STATUS);
		Result.add(Events.RDATE);
		Result.add(Events.RRULE);
		Result.add(Events.EXRULE);
		Result.add(Events.EXDATE);
		
		return Result;
	}
	
	public boolean createIcs() {
		boolean Result = false;
		TimeZone timezone = null;
		TimeZoneRegistry registry = TimeZoneRegistryFactory.getInstance().createRegistry();

		/* 
		 * dtstart=1365598800000
		 * dtend=1365602400000
		 * eventTimezone=Europe/Berlin
		 * eventEndTimezone=null
		 * duration=null
		 * allDay=0
		 * rrule=null
		 * rdate=null
		 * exrule=null
		 * exdate=null
		 * title=Einurlner Termin
		 * description=null
		 * eventLocation=null
		 * accessLevel=0
		 * eventStatus=0
		 * 
		 * calendar_id=4
		 * lastDate=-197200128
		 * sync_data1=null
		 * _sync_id=null
		 * dirty=1
		 */
		
		try {
			mCalendar = new Calendar();
			PropertyList propCalendar = mCalendar.getProperties();
			propCalendar.add(new ProdId("-//Ben Fortuna//iCal4j 1.0//EN"));
			propCalendar.add(Version.VERSION_2_0);
			propCalendar.add(CalScale.GREGORIAN);
			
			VEvent event = new VEvent();
			mCalendar.getComponents().add(event);
			PropertyList propEvent = event.getProperties();

			// DTSTART
			long lngStart = this.ContentValues.getAsLong(Events.DTSTART);
			String strTZStart = this.ContentValues.getAsString(Events.EVENT_TIMEZONE);
			if (lngStart > 0) {
				DateTime dateStart = new DateTime();
				dateStart.setTime(lngStart);
				DtStart dtStart = new DtStart();
				dtStart.setDate(dateStart);
				timezone = registry.getTimeZone(strTZStart);
				dtStart.setTimeZone(timezone);
				propEvent.add(dtStart);
				mCalendar.getComponents().add(timezone.getVTimeZone());
			}
			
			// DTEND
			long lngEnd       = this.ContentValues.getAsLong(Events.DTEND);
			String strTZEnd   = this.ContentValues.getAsString(Events.EVENT_END_TIMEZONE);
			if (lngEnd > 0) {
				DateTime dateEnd = new DateTime();
				dateEnd.setTime(lngEnd);
				DtEnd dtEnd = new DtEnd();
				dtEnd.setDate(dateEnd);
				if (strTZEnd != null)
					timezone = registry.getTimeZone(strTZEnd);
				dtEnd.setTimeZone(timezone);
				propEvent.add(dtEnd);
			}
			
			// DURATION
			if (this.ContentValues.containsKey(Events.DURATION)) {
				String strDuration = this.ContentValues.getAsString(Events.DURATION);
				if (strDuration != null) {
					Duration duration = new Duration();
					duration.setValue(strDuration);
					
					propEvent.add(duration);
				}
			}

			//RRULE
			if (this.ContentValues.containsKey(Events.RRULE)) {
				String strRrule = this.ContentValues.getAsString(Events.RRULE);
				if (strRrule != null) {
					RRule rrule = new RRule();
					rrule.setValue(strRrule);
					propEvent.add(rrule);
				}
			}
			
			//RDATE
			if (this.ContentValues.containsKey(Events.RDATE)) {
				String strRdate = this.ContentValues.getAsString(Events.RDATE);
				if (strRdate != null) {
					RDate rdate = new RDate();
					rdate.setValue(strRdate);
					propEvent.add(rdate);
				}
			}
			
			//EXRULE
			if (this.ContentValues.containsKey(Events.EXRULE)) {
				String strExrule = this.ContentValues.getAsString(Events.EXRULE);
				if (strExrule != null) {
					ExRule exrule = new ExRule();
					exrule.setValue(strExrule);
					propEvent.add(exrule);
				}
			}
			
			//EXDATE
			if (this.ContentValues.containsKey(Events.EXDATE)) {
				String strExdate = this.ContentValues.getAsString(Events.EXDATE);
				if (strExdate != null) {
					ExDate exdate = new ExDate();
					exdate.setValue(strExdate);
					propEvent.add(exdate);
				}
			}
			
			//SUMMARY
			if (this.ContentValues.containsKey(Events.TITLE)) {
				String strTitle = this.ContentValues.getAsString(Events.TITLE);
				if (strTitle != null) {
					Summary summary = new Summary(strTitle);
					propEvent.add(summary);
				}
			}
			
			//DESCIPTION
			if (this.ContentValues.containsKey(Events.DESCRIPTION)) {
				String strDescription = this.ContentValues.getAsString(Events.DESCRIPTION);
				if (strDescription != null) {
					Description description = new Description(strDescription);
					propEvent.add(description);
				}
			}
			
			//LOCATION
			if (this.ContentValues.containsKey(Events.EVENT_LOCATION)) {
				Location location = new Location(this.ContentValues.getAsString(Events.EVENT_LOCATION));
				propEvent.add(location);
			}
			
			//CLASS / ACCESS_LEVEL
			if (this.ContentValues.containsKey(Events.ACCESS_LEVEL)) {
				int accessLevel = this.ContentValues.getAsInteger(Events.ACCESS_LEVEL);
				Clazz clazz = new Clazz();
				if (accessLevel == Events.ACCESS_PUBLIC)
					clazz.setValue(Clazz.PUBLIC.getValue());
				else if (accessLevel == Events.ACCESS_PRIVATE)
					clazz.setValue(Clazz.PRIVATE.getValue());
				else if (accessLevel == Events.ACCESS_CONFIDENTIAL)
					clazz.setValue(Clazz.CONFIDENTIAL.getValue());
				else 
					clazz.setValue(Clazz.PUBLIC.getValue());
				
				propEvent.add(clazz);
			}
			
			//STATUS
			if (this.ContentValues.containsKey(Events.STATUS)) {
				int intStatus = this.ContentValues.getAsInteger(Events.STATUS);
				Status status = new Status();
				if (intStatus == Events.STATUS_CANCELED) 
					status.setValue(Status.VEVENT_CANCELLED.getValue());
				else if (intStatus == Events.STATUS_CONFIRMED)
					status.setValue(Status.VEVENT_CONFIRMED.getValue());
				else if (intStatus == Events.STATUS_TENTATIVE)
					status.setValue(Status.VEVENT_TENTATIVE.getValue());
				else
					status.setValue(Status.VEVENT_TENTATIVE.getValue());
				
				propEvent.add(status);
			}

			//UID
			UidGenerator ug = new UidGenerator("1");
			propEvent.add(ug.generateUid());

			if (mAttendees.size() > 0) {
				for (Object objProp: mAttendees) {
					Property prop = (Property) objProp;
					propEvent.add(prop);
				}
			}
			
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		return Result;
	}
}

