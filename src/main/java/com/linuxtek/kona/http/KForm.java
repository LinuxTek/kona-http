/*
 * Copyright (C) 2012 LINUXTEK, Inc.  All Rights Reserved.
 */
package com.linuxtek.kona.http;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import com.linuxtek.kona.util.KDateUtil;

/**
 * KForm reads all of an HTML form's parameters into an internal Hashtable and
 * makes them available through several convenience methods. The class
 * automatically detects if the form is enctype="multipart/form-data" and
 * returns all attached files.
 */

public class KForm {
	private static Logger logger = Logger.getLogger(KForm.class);

	/** Default max post size for multipart/form-data forms. */
	public static final int MAX_POST_SIZE = 10 * 1024 * 1024;

	private HttpServletRequest req = null;

	protected LinkedHashMap<String, Object> param = null;
	protected LinkedHashMap<String, String> invalidFields = null;
	protected LinkedHashMap<String, String> fieldLabels = null;

	private int maxPostSize = 5 * 1024 * 1024;
	
	/*
	private boolean isMultipart = false;
	private List<FileItem> fileItemList = null;
	private Map<String, FileItem> fileItemMap = null;
	private String tempDirPath = null;
	*/

	/**
	 * Constructs a KForm.
	 */
	public KForm(HttpServletRequest req, int maxPostSize) {
		this.req = req;
		this.maxPostSize = maxPostSize;

		param = new LinkedHashMap<String, Object>();
		invalidFields = new LinkedHashMap<String, String>();
		fieldLabels = new LinkedHashMap<String, String>();
		init();
	}

	public KForm(HttpServletRequest req) {
		this(req, MAX_POST_SIZE);
	}

	public void setMaxPostSize(int maxPostSize) {
		this.maxPostSize = maxPostSize;
	}

	public Boolean isSubmitted() {
		if (param.size() > 0)
			return (true);
		return (false);
	}

	public int getParamCount() {
		return (param.size());
	}

	public Map<String, Object> getParamMap() {
		return (param);
	}
    
	public boolean isDefined(String name) {
		for (String s : param.keySet()) {
			if (s.equals(name)) {
				return true;
			}
		}
        return false;
	}

	public void setFieldLabel(String fieldName, String label) {
		fieldLabels.put(fieldName, label);
	}

	public String getFieldLabel(String fieldName) {
		return (fieldLabels.get(fieldName));
	}

	public Map<String, String> getInvalidFields() {
		return (invalidFields);
	}

	public Boolean isFieldValid(String paramName) {
		String error = invalidFields.get(paramName);

		if (error == null)
			return (true);

		return (false);
	}

	public void addInvalidField(String paramName, String error) {
		invalidFields.put(paramName, error);
	}

	public void addInvalidField(String paramName) {
		invalidFields.put(paramName, "");
	}

	public void removeInvalidField(String paramName) {
		invalidFields.remove(paramName);
	}

	public void clearInvalidFields() {
		invalidFields.clear();
	}

	/**
	 * Returns the max size of data that can be submitted in a multipart form.
	 * 
	 * @return the max size of data that can be submitted in a multipart form.
	 */
	public int getMaxPostSize() {
		return (maxPostSize);
	}

	private File createTempDir() {
		final String baseTempPath = System.getProperty("java.io.tmpdir");

		Random rand = new Random();
		Date now = new Date();
		long randomInt = now.getTime() + (long) rand.nextInt();

		File tempDir = new File(baseTempPath + File.separator + "servlet-form-"
				+ randomInt);

		if (tempDir.exists() == false)
			tempDir.mkdir();

		tempDir.deleteOnExit();

		return (tempDir);
	}

	/**
	 * Initialize the form object. The method first determines if the form is
	 * multipart then retrieves all of its parameters into the param hashtable.
	 */
	@SuppressWarnings("unchecked")
	private void init() {
		/*
		isMultipart = ServletFileUpload.isMultipartContent(req);
		if (isMultipart) {
			try {
				File tempDir = createTempDir();
				tempDirPath = tempDir.getPath();
				FileItemFactory factory = new DiskFileItemFactory();
				ServletFileUpload upload = new ServletFileUpload(factory);
				fileItemList = upload.parseRequest(req);
				fileItemMap = new HashMap<String, FileItem>();

				logger.debug("form is multipart/form-data");
			} catch (FileUploadException e) {
				logger.error("Error parsing multipart form", e);
				isMultipart = false;
			}
		}
		*/

		// Get the non-multiReq params (e.g. in a query string)
		Enumeration<?> e = req.getParameterNames();

		if (e.hasMoreElements()) {
			String reqUrl = "[" + KServletUtil.getFullRequestURL(req) + "]";
			logger.debug("******* Form Fields " + reqUrl + " *******");
		}

		while (e.hasMoreElements()) {
			String name = (String) e.nextElement();
			String[] values = req.getParameterValues(name);

			if (values.length == 1) {
				String value = values[0];
				param.put(name, value);
				logger.debug(name + "==>" + value);
			} else {
				param.put(name, values);
				for (int i = 0; i < values.length; i++)
					logger.debug("array: " + name + "==>" + values[i]);
			}
		}

		/*
		if (isMultipart) {
			for (FileItem item : fileItemList) {
				if (item.isFormField()) {
					// FIXME: how do we handle arrays??

					String name = item.getFieldName();
					String value = item.getString();
					param.put(name, value);

					logger.debug("[" + name + "] ==>" + value);
				} else {
					logger.debug("******* Form File *******");
					String name = item.getFieldName();
					Date now = new Date();

					// FIXME: add extension
					String fileName = KStringUtil.toHex(now.getTime(), 12);
					File value = new File(tempDirPath + "/" + fileName);

					try {
						item.write(value);
						param.put(name, value);
						fileItemMap.put(name, item);

						logger.debug("[" + name + "] ==>" + value);
						logger.debug("FILE: " + value.getName());
					} catch (Exception e1) {
						logger.error("Error writing multipart file", e1);
					}
				}
			}
		}
		*/
	}

	/**
	 * Returns the content type of the named parameter in a multipart form.
	 * 
	 * \param name the form parameter name \return the content type (mime type)
	 * of the named parameter or \c null if the form is not multipart.
	 */
	public String getContentType(String name) {
		String contentType = null;

		/*
		if (isMultipart) {
			FileItem item = fileItemMap.get(name);
			if (item != null)
				contentType = item.getContentType();
		}
		*/

		return (contentType);
	}

	/**
	 * Returns a File object of the named parameter in a multipart form.
	 * 
	 * \param name the form parameter name \return a File object of the named
	 * parameter or \c null if the form is not multipart.
	 */
	/*
	public File getFile(String name) {
		File file = null;

		logger.debug("requesting File for: [" + name + "]");
		logger.debug("isMultipart: [" + isMultipart + "]");

		if (isMultipart)
			file = (File) param.get(name);

		if (file == null)
			logger.debug("File is null for: [" + name + "]");
		else
			logger.debug("File found for: [" + name + "]");

		return (file);
	}
	*/

	/*
	public Long getFileSize(String name) {
		Long size = null;

		if (isMultipart) {
			FileItem item = fileItemMap.get(name);
			size = item.getSize();
		}

		return (size);
	}
	*/

	/**
	 * Returns a List of the file names contained in a multipart form.
	 * 
	 * \return a List of the file names contained in a multipart form or \c null
	 * if the form is not multipart.
	 */
	/*
	public List<String> getFileNames() {
		if (isMultipart) {
			List<String> names = new ArrayList<String>();
			Collection<FileItem> items = fileItemMap.values();
			for (FileItem item : items) {
				names.add(item.getName());
			}
			return (names);
		}
		return (null);
	}
	*/

	/**
	 * Returns the name of the file that was submitted in a multipart form.
	 * Note: the returned filesystem name is not a full path name and does not
	 * contain any directory information.
	 * 
	 * \param name the form parameter name \return the name of the file that or
	 * \c null if the form is not multipart.
	 */
	/*
	public String getFilesystemName(String name) {
		String filesystemName = null;

		if (isMultipart) {
			FileItem item = fileItemMap.get(name);
			if (item != null) {
				filesystemName = item.getName();
			}
		}
		return (filesystemName);
	}
	*/

	public void remove(Object name) {
		param.remove(name);
	}

	public void unset(Object name) {
		remove(name);
	}

	// used with webmacro: $form.id ==> form.getInt("id")
	public Object get(Object name) {
		return (getObject(name));
	}

	// used with webmacro: $form.id = 5 ==> form.setInt("id", "5")
	public void put(String name, Object value) {
		setObject(name, value);
	}

	public Boolean getBoolean(Object name) {
		return (getBoolean(name, null));
	}

	public Boolean getBoolean(Object name, Boolean defaultValue) {
		Object value = param.get(name);

		if (value == null)
			return (defaultValue);

		if (value.toString().equalsIgnoreCase("true")
				|| value.toString().equalsIgnoreCase("yes")
				|| value.toString().equalsIgnoreCase("y")
				|| value.toString().equalsIgnoreCase("1"))
			return (true);

		return (false);
	}

	public void setBoolean(String name, Boolean value) {
		setObject(name, value);
	}

	public String getString(String name, String defaultValue) {
		return getString(name, defaultValue, true);
	}
	
	public String getString(String name, String defaultValue, boolean scrub) {
		String result = getString(name, scrub);

		if (result == null) {
			return defaultValue;
		}
		
		return result;
	}

	public String getString(String name) {
		return getString(name, true);
	}
	
	public String getString(String name, boolean scrub) {
		Object value = param.get(name);

		if (value != null) {
			String s = value.toString().trim();
			if (s.length() == 0) {
				return (null);
			}
			String safe = s;
			if (scrub) {
				safe = Jsoup.clean(s, Whitelist.basic());
			}
			return safe;
		}
		return null;
	}
	
	public String getDecodedString(String name) {
		String s = getString(name);
		if (s != null) {
			try {
				s = URLDecoder.decode(s, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				logger.warn(e);
				s = null;
			}
		}
		return s;
	}

	// public int getInt(String name) throws NumberFormatException
	public Integer getInteger(String name) {
		return (getInteger(name, null));
	}

	public Integer getInteger(String name, Integer defaultValue) {
		Integer value = null;

		try {
			if (name != null && getString(name) != null)
				value = Integer.decode(getString(name));
		} catch (NumberFormatException ignored) {
			value = defaultValue;
		}

		if (value == null)
			value = defaultValue;

		return (value);
	}

	public Long getLong(String name) {
		return getLong(name, null);
	}

	public Long getLong(String name, Long defaultValue) {
		Integer dv = (defaultValue == null ? null : defaultValue.intValue());

		Integer value = getInteger(name, dv);
		if (value == null)
			return null;
		return value.longValue();
	}

	/*
	public Integer getInt(String name) {
		return getInt(name, null);
	}

	public Integer getInt(String name, Integer defaultValue) {
		int value = defaultValue;

		String tmp = getString(name);

		if (tmp == null)
			return (defaultValue);

		try {
			value = Integer.parseInt(tmp);
		} catch (NumberFormatException ignored) {
			value = defaultValue;
		}

		return (value);
	}
	*/

	public Double getDouble(String name) {
		return getDouble(name, null);
	}

	public Double getDouble(String name, Double defaultValue) {
		double value;

		String tmp = getString(name);

		if (tmp == null)
			return (defaultValue);

		try {
			value = Double.parseDouble(tmp);
		} catch (NumberFormatException ignored) {
			value = defaultValue;
		}

		return (value);
	}

	public BigDecimal getCurrency(String name) {
		return (getCurrency(name, (Locale) null));
	}

	public BigDecimal getCurrency(String name, Locale locale) {
		BigDecimal value = null;
		boolean symbolPre = true;
		int index = 0;
		String currencySymbol = "";
		String text = getString(name);

		if (text == null || text.trim().length() == 0)
			return (value);

		try {
			if (locale == null)
				locale = Locale.getDefault();

			NumberFormat nf = NumberFormat.getCurrencyInstance(locale);

			// determine if symbol is prefix or suffix
			if (nf instanceof DecimalFormat) {
				DecimalFormatSymbols dfs = ((DecimalFormat) nf)
						.getDecimalFormatSymbols();

				currencySymbol = dfs.getCurrencySymbol();
				String lp = ((DecimalFormat) nf).toLocalizedPattern();
				index = lp.indexOf('\u00A4'); // currency sign

				if (index > 0)
					symbolPre = false;
			}

			index = text.indexOf(currencySymbol);

			if (index == -1) {
				if (symbolPre)
					text = currencySymbol + text;
				else
					text = text + " " + currencySymbol;
			}

			Number num = nf.parse(text);
			// value = num.doubleValue();

			DecimalFormat twoDForm = new DecimalFormat("#########.##");
			// value = Double.valueOf(twoDForm.format(value));
			value = new BigDecimal(twoDForm.format(num));

			// paranoid check to make sure returned value is formatted right
			value = value.setScale(2, BigDecimal.ROUND_HALF_UP);

			logger.debug("format: " + text + " -> " + value);

		} catch (Exception e) {
			// throw new NumberFormatException(e.getMessage());
			logger.warn("Error parsing currency field.", e);
		}
		return (value);
	}

	public BigDecimal getCurrency(String name, BigDecimal defaultValue) {
		BigDecimal value;

		String tmp = getString(name);

		if (tmp == null || tmp.trim().length() == 0)
			return (defaultValue);

		try {
			value = getCurrency(name);
		} catch (Exception e) {
			logger.warn("Error parsing currency field.", e);
			value = defaultValue;
		}

		return (value);
	}

	/**
	 * Return the Logical OR of all the Integer values of this parameter.
	 * \return the logical AND of the values of this param or -1 if name is
	 * null.
	 */
	public int getIntOR(String name) {
		return (getIntOR(name, -1));
	}

	/**
	 * Return the Logical OR of all the Integer values of this parameter.
	 * \return the logical AND of the values of this param or defaultValue if
	 * name is null.
	 */
	public int getIntOR(String name, int defaultValue) {
		int value = 0;

		Object o = getObject(name);

		if (o == null)
			return (defaultValue);

		if ((o.getClass()).isArray()) {
			String[] tmp = (String[]) o;

			for (int i = 0; i < tmp.length; i++)
				value = value | Integer.parseInt(tmp[i]);
		} else
			value = Integer.parseInt(o.toString());

		logger.debug("Logical OR of " + name + " = " + value);

		return (value);
	}

	/**
	 * Return the Logical AND of all the Integer values of this parameter.
	 * \return the logical AND of the values of this param or 0 if name is null.
	 */
	public int getIntAND(String name) {
		return (getIntAND(name, 0));
	}

	/**
	 * Return the Logical AND of all the Integer values of this parameter.
	 * \return the logical AND of the values of this param or defaultValue if
	 * name is null.
	 */
	public int getIntAND(String name, int defaultValue) {
		int value = 0;

		Object o = getObject(name);

		if (o == null)
			return (defaultValue);

		if ((o.getClass()).isArray()) {
			String[] tmp = (String[]) o;

			for (int i = 0; i < tmp.length; i++)
				value = value | Integer.parseInt(tmp[i]);
		} else
			value = Integer.parseInt(o.toString());

		logger.debug("Logical AND of " + name + " values = " + value);

		return (value);
	}

	public Date getDate(String name) {
		Date date = null;

		int month = getInteger(name + "_month", -1);
		int day = getInteger(name + "_date", -1);
		int year = getInteger(name + "_year", -1);

		if (month > 0 && day > 0 && year > 0)
			date = KDateUtil.getDate(year, month, day);

		return (date);
	}

	public Date getDateTime(String name) {
		Date date = getDate(name);

		if (date == null)
			return (null);

		int hour = getInteger(name + "_hour", -1);
		int minute = getInteger(name + "_minute", -1);
		int second = getInteger(name + "_second", 0);

		if (hour < 0 || minute < 0)
			return (date);

		String meridian = null;
		meridian = getString(name + "_meridian");
		if (meridian != null) {
			if (meridian.equalsIgnoreCase("PM")) {
				hour += 12;
				if (hour == 24)
					hour = 0;
			} else {
				if (hour == 12)
					hour = 0;
			}
		}

		date = KDateUtil.set24Hour(date, hour);
		date = KDateUtil.setMinutes(date, minute);
		date = KDateUtil.setSeconds(date, second);

		return (date);
	}

	public Date getDate(String name, String format) {
		Locale locale = Locale.getDefault();
		return (getDate(name, format, locale));
	}

	public Date getDate(String name, String format, Locale locale) {
		Date date = null;
		String value = getString(name);

		try {
			if (value != null) {
				SimpleDateFormat df = new SimpleDateFormat(format, locale);
				// date = df.parse(value, new ParsePosition(0));
				date = df.parse(value);
			}
		} catch (ParseException e) {
			logger.error(e);
		}

		return (date);
	}

	public Object getObject(Object name) {
		Object value = param.get(name);

		return (value);
	}

	public Object[] getObjectArray(Object name) {
		Object tmp[] = null;

		Object value = getObject(name);

		if (value == null)
			return (null);

		if ((value.getClass()).isArray())
			tmp = (Object[]) value;
		else {
			tmp = new Object[1];
			tmp[0] = getObject(name);
		}

		return (tmp);
	}

	public String[] getStringArray(String name) {
		// return ((String[])getObjectArray(name));
		Object[] tmp = getObjectArray(name);

		if (tmp == null)
			return null;

		String[] tmp2 = new String[tmp.length];

		for (int i = 0; i < tmp.length; i++)
			tmp2[i] = tmp[i].toString();

		return (tmp2);
	}

	/*
	 * public String[] getStringArray(String name) { String tmp[] = null;
	 * 
	 * Object value = getObject(name);
	 * 
	 * if (value == null) return (null);
	 * 
	 * if ((value.getClass()).isArray()) tmp = (String[]) value; else { tmp =
	 * new String[1]; tmp[0] = getString(name); }
	 * 
	 * return (tmp); }
	 */

	public int[] getIntArray(String name) {
		Object[] tmp = getObjectArray(name);

		if (tmp == null)
			return (null);

		int[] tmp2 = new int[tmp.length];

		for (int i = 0; i < tmp.length; i++) {
			try {
				tmp2[i] = Integer.parseInt(tmp[i].toString());
			} catch (NumberFormatException e) {
				return (null);
			}
		}

		return (tmp2);
	}

	public void setList(String name, Collection<?> value) {
		if (value != null) {
			List<?> v = new ArrayList<Object>(value);
			setObject(name, v);
		}
	}

	public void setList(String name, Object[] array) {
		if (array != null) {
			List<Object> v = new ArrayList<Object>();

			for (int i = 0; i < array.length; i++)
				v.add(array[i]);

			setObject(name, v);
		}
	}

	public void setArray(String name, Object[] array) {
		setObject(name, array);
	}

	/*
	 * public String[] getParams(String name) { Object value = param.get(name);
	 * 
	 * if (value != null) return ((String[]) value);
	 * 
	 * return (null); }
	 */

	public void setString(String name, String value) {
		setObject(name, value);
	}

	/*
	 * public void setParam(String name, String value) { if (name == null ||
	 * value == null) log.debug(this,
	 * "Error setting parameter: name and/or value is null"); else
	 * param.put(name, value); }
	 */

	public void setObject(String name, Object value) {
		/*
		 * if (name == null || value == null) log.warn(this,
		 * "Error setting parameter: name and/or value is null"); else
		 * param.put(name, value);
		 */
		param.put(name, value);
	}

	/*
	 * public void setParam(String name, Object value) { if (name == null ||
	 * value == null) log.debug(this,
	 * "Error setting parameter: name and/or value is null"); else
	 * param.put(name, value); }
	 */

	public void setDouble(String name, double value) {
		setObject(name, new Double(value));
	}

	public void setCurrency(String name, Integer value) {
		// setCurrency(name, new Double(value));
		setCurrency(name, value.doubleValue());
	}

	public void setCurrency(String name, BigDecimal value) {
		setCurrency(name, value.doubleValue());
	}

	public void setCurrency(String name, double value) {
		if (value < 0.0)
			return;

		Locale locale = Locale.getDefault();

		String s = null;
		NumberFormat nf = NumberFormat.getCurrencyInstance(locale);
		s = nf.format(value);
		setObject(name, s);
	}

	public void setInteger(String name, Integer value) {
		setObject(name, value);
	}

	public void setInt(String name, Integer value) {
		setObject(name, value);
	}

	public void setInt(String name, int value) {
		setObject(name, new Integer(value));
	}

	public void setDate(String name, Date date) {
		if (date == null)
			return;

		setInt(name + "_month", KDateUtil.getMonth(date));
		setInt(name + "_date", KDateUtil.getDayOfMonth(date));
		setInt(name + "_year", KDateUtil.getYear(date));
	}

	public void setDateTime(String name, Date date) {
		if (date == null)
			return;

		setInt(name + "_month", KDateUtil.getMonth(date));
		setInt(name + "_date", KDateUtil.getDayOfMonth(date));
		setInt(name + "_year", KDateUtil.getYear(date));
		setInt(name + "_hour", KDateUtil.get24Hour(date));
		setInt(name + "_minute", KDateUtil.getMinutes(date));
		setInt(name + "_second", KDateUtil.getSeconds(date));
	}

	public void setDate(String name, Date date, String format) {
		Locale locale = Locale.getDefault();
		setDate(name, date, "dd MMMM yyyy", locale);
	}

	public void setDate(String name, Date date, String format, Locale locale) {
		String value = null;

		if (date != null) {
			SimpleDateFormat df = new SimpleDateFormat(format, locale);
			value = df.format(date);

			if (value != null)
				setObject(name, value);
		}
	}

	/*
	 * public void setParam(String name, int value) { setParam(name, new
	 * Integer(value)); }
	 * 
	 * public void setParam(Object name, Object value) { if (name == null ||
	 * value == null) log.debug(this,
	 * "Error setting parameter: name and/or value is null"); else
	 * param.put(name, value); }
	 */

	/*
	 * public void setParam(String name, String[] value) { if (name == null ||
	 * value == null) log.debug(this,
	 * "Error setting parameter: name and/or value is null"); else
	 * param.put(name, value); }
	 */

	/*
	 * Regular expression obtained from:
	 * 
	 * http://www.jguru.com/faq/view.jsp?EID=1088553 by praveen J
	 */
	public static Boolean isValidEmailAddress(String email) {
		if (email.matches(".*<.*>")) {
			int start = email.indexOf("<");
			int end = email.indexOf(">");
			email = email.substring(start + 1, end);
			// logger.debug("isValidEmailAddress: " + email);
		}
		// String filter = "/^([a-zA-Z0-9_\.\-])+" +
		// "\@(([a-zA-Z0-9\-])+\.)+([a-zA-Z0-9]{2,4})+$/";

		// String filter = "/^(([^<>()[\]\\.,;:\s@\"]+" +
		// "(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))" +
		// "@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\." +
		// "[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/";

		// String filter = "^[a-zA-Z][\w\.-]*[a-zA-Z0-9]@[a-zA-Z0-9]" +
		// "[\w\.-]*[a-zA-Z0-9]\.[a-zA-Z][a-zA-Z\.]*[a-zA-Z]$";

		String filter = "^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*"
				+ "@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[_A-Za-z0-9-]+)";

		if (email.matches(filter))
			return (true);

		return (false);
	}
}
