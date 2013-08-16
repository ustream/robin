package com.jayway.android.robotium.remotecontrol;

import java.io.Serializable;
import java.lang.reflect.Method;

import com.jayway.android.robotium.remotecontrol.solo.Message;

/** 
 * From release4.0, Robotium begins to support some APIs who use com.jayway.android.robotium.solo.By as<br>
 * parameter. To support this kind of APIs, this class was created, it implements interface Serializable<br>
 * so that it can be transported as an object thru our TCP protocol.<br>
 * Example of usage:<br>
 * At controller side, user can simply call {@link By#xpath(String)} etc. to get a By instance 'by'.<br>
 * and send this object thru the wire.<br>
 * At the device side, we receive that object 'by' and we call {@link By#toSoloBy()} <br>
 * to get a Solo's By object, which can be used by Robotium's APIs.<br><br>
 * 
 * This class copy the content of com.jayway.android.robotium.solo.By, if that class is modified, we needs<br>
 * to update this class also.<br>
 * 
 * @author Lei Wang, SAS Institute, Inc
 * @since  May 14, 2013
 *
 */
public abstract class By implements Serializable{
	private static final long serialVersionUID = 8011551405071393553L;

	/**
	 * Select a WebElement by its id.
	 * 
	 * @param id the id of the web element	
	 * @return the Id object
	 */
	public static By id(final String id) {
		return new Id(id); 

	}

	/**
	 * Select a WebElement by its xpath.
	 * 
	 * @param xpath the xpath of the web element
	 * @return the Xpath object
	 */
	public static By xpath(final String xpath) {
		return new Xpath(xpath); 

	}

	/**
	 * Select a WebElement by its css selector.
	 * 
	 * @param selectors the css selector of the web element
	 * @return the CssSelector object
	 */
	public static By cssSelector(final String selectors) {
		return new CssSelector(selectors); 

	}

	/**
	 * Select a WebElement by its name.
	 * 
	 * @param name the name of the web element
	 * @return the Name object
	 */
	public static By name(final String name) {
		return new Name(name); 

	}

	/**
	 * Select a WebElement by its class name.
	 * 
	 * @param className the class name of the web element
	 * @return the ClassName object
	 */
	public static By className(final String className) {
		return new ClassName(className); 

	}

	/**
	 * Select a WebElement by its text content.
	 * 
	 * @param textContent the text content of the web element
	 * @return the TextContent object
	 */
	public static By textContent(final String textContent) {
		return new Text(textContent); 

	}
	
	/**
	 * Select a WebElement by its tag name.
	 * 
	 * @param tagName the tag name of the web element
	 * @return the TagName object
	 */
	public static By tagName(final String tagName) {
		return new TagName(tagName); 

	}

	/**
	 * Returns the value. 
	 * 
	 * @return the value
	 */
	public String getValue(){
		return "";
	}

	/**
	 * Return the name of static method for creating a By instance. 
	 * 
	 * @return the name of static method for creating a By instance.
	 */
	public abstract String getStaticMethodName();

	/**
	 * Convert this class to com.jayway.android.robotium.solo.By with help of reflection.<br>
	 * It is not suggested to call this method frequently. User should store it in<br>
	 * a local variable and use that variable.<br>
	 * 
	 * @return Object, an object of com.jayway.android.robotium.solo.By
	 */
	public Object toSoloBy(){
		try {
			Method method = Class.forName("com.jayway.android.robotium.solo.By").getMethod(getStaticMethodName(), String.class);
			return method.invoke(null, new Object[]{getValue()});
		} catch (Throwable ignore) {}
		
		return null;
	}

	static class Id extends By {
		private final String id;

		public Id(String id) {
			this.id = id;
		}

		public String getValue(){
			return id;
		}

		public String getStaticMethodName() {
			return Message.method_by_id;
		}
	}

	static class Xpath extends By {
		private final String xpath;

		public Xpath(String xpath) {
			this.xpath = xpath;
		}

		public String getValue(){
			return xpath;
		}
		
		public String getStaticMethodName() {
			return Message.method_by_xpath;
		}
	}

	static class CssSelector extends By {
		private final String selector;

		public CssSelector(String selector) {
			this.selector = selector;
		}


		public String getValue(){
			return selector;
		}
		
		public String getStaticMethodName() {
			return Message.method_by_cssSelector;
		}
	}

	static class Name extends By {
		private final String name;

		public Name(String name) {
			this.name = name;
		}


		public String getValue(){
			return name;
		}
		
		public String getStaticMethodName() {
			return Message.method_by_name;
		}
	}
	
	static class ClassName extends By {
		private final String className;

		public ClassName(String className) {
			this.className = className;
		}


		public String getValue(){
			return className;
		}
		
		public String getStaticMethodName() {
			return Message.method_by_className;
		}
	}
	
	static class Text extends By {
		private final String textContent;

		public Text(String textContent) {
			this.textContent = textContent;
		}


		public String getValue(){
			return textContent;
		}
		
		public String getStaticMethodName() {
			return Message.method_by_textContent;
		}
	}
	
	static class TagName extends By {
		private final String tagName;
		
		public TagName(String tagName){
			this.tagName = tagName;
		}
		

		public String getValue(){
			return tagName;
		}
		
		public String getStaticMethodName() {
			return Message.method_by_tagName;
		}
	}
}
