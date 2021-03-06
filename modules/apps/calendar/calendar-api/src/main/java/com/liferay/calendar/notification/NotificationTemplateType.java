/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.calendar.notification;

/**
 * @author Eduardo Lundgren
 * @author Pier Paolo Ramon
 */
public enum NotificationTemplateType {

	DECLINE("decline"), INVITE("invite"), INSTANCE_DELETED("instance-deleted"),
	MOVED_TO_TRASH("moved-to-trash"), REMINDER("reminder"), UPDATE("update");

	public static NotificationTemplateType parse(String value) {
		if (DECLINE.getValue().equals(value)) {
			return DECLINE;
		}
		else if (INVITE.getValue().equals(value)) {
			return INVITE;
		}
		else if (INSTANCE_DELETED.getValue().equals(value)) {
			return INSTANCE_DELETED;
		}
		else if (MOVED_TO_TRASH.getValue().equals(value)) {
			return MOVED_TO_TRASH;
		}
		else if (REMINDER.getValue().equals(value)) {
			return REMINDER;
		}
		else if (UPDATE.getValue().equals(value)) {
			return UPDATE;
		}

		throw new IllegalArgumentException("Invalid value " + value);
	}

	public String getValue() {
		return _value;
	}

	@Override
	public String toString() {
		return _value;
	}

	private NotificationTemplateType(String value) {
		_value = value;
	}

	private final String _value;

}