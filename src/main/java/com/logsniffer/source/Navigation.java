package com.logsniffer.source;

import java.io.IOException;
import java.util.Date;

import com.logsniffer.source.LogPointerFactory.NavigationFuture;

/**
 * Represents a strategy for navigating in a log.
 * 
 * @author mbok
 *
 * @param <M>
 *            metric type for absolute navigation
 */
public interface Navigation<M> {

	/**
	 * Navigation types.
	 * 
	 * @author mbok
	 *
	 */
	public static enum NavigationType {
		BYTE, DATE;
	}

	/**
	 * Marker interface to navigate in byte offset oriented log.
	 * 
	 * @author mbok
	 *
	 */
	public static interface ByteOffsetNavigation extends Navigation<Long> {

	}

	/**
	 * Marker interface to navigate in the log using timestamps.
	 * 
	 * @author mbok
	 *
	 */
	public static interface DateOffsetNavigation extends Navigation<Date> {

	}

	/**
	 * Navigates absolutely to the desired position in the log.
	 * 
	 * @param offset
	 *            the offset to navigate to
	 * @return the target pointer
	 * @throws IOException
	 */
	NavigationFuture absolute(M offset) throws IOException;
}