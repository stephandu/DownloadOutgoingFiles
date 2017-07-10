

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.util.ClientUtils;

public class SolrQueryBuilder {
  public static final String DEFAULT_TIMEZONE_CODE = "GMT";
  public static final String SOLR_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
  public static final String SOLR_DATE_FORMAT_IN_SECOND = "yyyy-MM-dd'T'HH:mm:ss'Z'";
  
  public static TimeZone getTimeZone(String timeZoneID) {
    if (StringUtils.isEmpty(timeZoneID)) {
      return TimeZone.getTimeZone(DEFAULT_TIMEZONE_CODE);
    }
    String localTimeZoneID = timeZoneID.trim();
    int beginIndex = localTimeZoneID.lastIndexOf(' ');
    if (beginIndex > 0) {
      return TimeZone.getTimeZone(localTimeZoneID.substring(beginIndex + 1));
    }
    return TimeZone.getTimeZone(localTimeZoneID);
  }
  
  public static Date convertToTimeZone(Date date, TimeZone fromTimeZone, TimeZone toTimeZone) {
    if (null == date) {
      return null;
    }
    Calendar calendarDate = Calendar.getInstance();
    calendarDate.setTimeZone(fromTimeZone);
    calendarDate.setTime(date);
    long milliTime = calendarDate.getTimeInMillis();
    int oldOffset = calendarDate.getTimeZone().getOffset(milliTime);
    calendarDate.setTimeZone(toTimeZone);
    milliTime = calendarDate.getTimeInMillis();
    int newOffset = calendarDate.getTimeZone().getOffset(milliTime);
    int diff = newOffset - oldOffset;
    milliTime += diff;
    calendarDate.setTimeInMillis(milliTime);
    return calendarDate.getTime();
  }
  
  public static String formatDate(Date dateGMT, TimeZone timeZone, String dateFormat) {
    if (null == dateGMT) {
      return "";
    }
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
    return simpleDateFormat.format(timeZone != null ? convertToTimeZone(dateGMT, getTimeZone(DEFAULT_TIMEZONE_CODE), timeZone) : dateGMT);
  }
  
  public static String formatDate(String dateFormat, Date dateGMT) {
    if (null == dateGMT) {
      return "";
    }
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
    return simpleDateFormat.format(dateGMT);
  }
  
  public static String lessEqualsOrNotExists(String fieldName, Date date) {
    StringBuilder sb = new StringBuilder();
    String dateString = formatDate(date, getTimeZone("UTC"), SOLR_DATE_FORMAT);
    String escapedDateString = ClientUtils.escapeQueryChars(dateString);
    sb.append("-").append(fieldName).append(":[").append(escapedDateString).append(" TO *]");
    return sb.toString();
  }

  public static String lessOrNotExists(String fieldName, Date date) {
    StringBuilder sb = new StringBuilder();
    String dateString = formatDate(date, getTimeZone("UTC"), SOLR_DATE_FORMAT);
    String escapedDateString = ClientUtils.escapeQueryChars(dateString);
    sb.append("-").append(fieldName).append(":{").append(escapedDateString).append(" TO *}");
    return sb.toString();
  }

  public static String greatEqualsOrNotExists(String fieldName, Date date) {
    StringBuilder sb = new StringBuilder();
    String dateString = formatDate(SOLR_DATE_FORMAT, date);
    String escapedDateString = ClientUtils.escapeQueryChars(dateString);
    sb.append("-").append(fieldName).append(":[*  TO ").append(escapedDateString).append("]");
    return sb.toString();
  }

  public static String less(String fieldName, Date date) {
    StringBuilder sb = new StringBuilder();
    String dateString = formatDate(date, getTimeZone("UTC"), SOLR_DATE_FORMAT);
    String escapedDateString = ClientUtils.escapeQueryChars(dateString);
    sb.append(fieldName).append(":{* TO ").append(escapedDateString).append("}");
    return sb.toString();
  }
  
  public static String less(String fieldName, long value) {
    StringBuilder sb = new StringBuilder();    
    sb.append(fieldName).append(":{* TO ").append(value + "").append("}");
    return sb.toString();
  }

  public static String greatEquals(String fieldName, Date date) {
    StringBuilder sb = new StringBuilder();
    String dateString = formatDate(SOLR_DATE_FORMAT, date);
    String escapedDateString = ClientUtils.escapeQueryChars(dateString);
    sb.append(fieldName).append(":[").append(escapedDateString).append("  TO *]");
    return sb.toString();
  }
  
  public static String equalsOrNotExists(String fieldName, String value) {
    StringBuilder sb = new StringBuilder();
    sb.append("-").append(fieldName).append(":(");
    String escapedValue = ClientUtils.escapeQueryChars(value);
    sb.append("{* TO ").append(escapedValue).append("} OR {").append(escapedValue).append(" TO *}");
    sb.append(")");
    return sb.toString();
  }

  public static String notExists(String fieldName, String value) {
    StringBuilder sb = new StringBuilder();
    sb.append("-").append(fieldName).append(":(");
    sb.append(ClientUtils.escapeQueryChars(value));
    sb.append(")");
    return sb.toString();
  }

  public static String in(String fieldName, String[] values) {
    StringBuilder sb = new StringBuilder();
    sb.append(fieldName).append(":(");
    for (int i = 0; i < values.length; i++) {
      if (i > 0) {
        sb.append(" OR ");
      }
      sb.append(ClientUtils.escapeQueryChars(values[i]));
    }
    sb.append(")");
    return sb.toString();
  }

  public static String equals(String fieldName, String value) {
    if (StringUtils.isEmpty(value)) {
      return "";
    }
    return fieldName + ":" + ClientUtils.escapeQueryChars(value);
  }
  
  public static String like(String fieldName, String value) {
    return fieldName + ":" + ClientUtils.escapeQueryChars(value).replace("\\*", "*");
  }

  public static String and(String query1, String query2) {
    return "(" + query1 + " AND " + query2 + ")";
  }

  public static String and(String... queries) {
    if (queries == null || queries.length <= 0) {
      return "";
    }
    String separator = " AND ";
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < queries.length; i++) {
      if (StringUtils.isNotEmpty(queries[i])) {
        if (sb.length() > 0) {
          sb.append(separator);
        }
        sb.append(queries[i]);
      }
    }
    return "(" + sb.toString() + ")";
  }

  public static String or(String query1, String query2) {
    return "(" + query1 + " OR " + query2 + ")";
  }
  
  public static String convertDateToStringWithFormat(Date date, String fromat) {
    if (date == null) {
      return "";
    }
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(fromat);
    return simpleDateFormat.format(date);
  }
  
  public static String range(String fieldName,Object fromValue, Object toValue) {
    
    if ((fromValue instanceof Date) && (toValue instanceof Date)) {
      fromValue = convertDateToStringWithFormat((Date) fromValue, SOLR_DATE_FORMAT_IN_SECOND);
      toValue = convertDateToStringWithFormat((Date) toValue, SOLR_DATE_FORMAT_IN_SECOND);
    }
    
    return fieldName+":["+fromValue+" TO "+toValue+"]";
  }

  public static String escapeQueryChars(String q) {
    return ClientUtils.escapeQueryChars(q).replace("\\*", "*").replace("\\:", ":");
  }
}
