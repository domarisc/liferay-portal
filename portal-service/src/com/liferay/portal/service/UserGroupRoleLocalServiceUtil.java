/**
 * Copyright (c) 2000-2006 Liferay, Inc. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.portal.service;

/**
 * <a href="UserGroupRoleLocalServiceUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 *
 */
public class UserGroupRoleLocalServiceUtil {
	public static void addUserGroupRoles(java.lang.String userId, long groupId,
		java.lang.String[] roleIds)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		UserGroupRoleLocalService userGroupRoleLocalService = UserGroupRoleLocalServiceFactory.getService();
		userGroupRoleLocalService.addUserGroupRoles(userId, groupId, roleIds);
	}

	public static void addUserGroupRoles(java.lang.String[] userIds,
		long groupId, java.lang.String roleId)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		UserGroupRoleLocalService userGroupRoleLocalService = UserGroupRoleLocalServiceFactory.getService();
		userGroupRoleLocalService.addUserGroupRoles(userIds, groupId, roleId);
	}

	public static void deleteUserGroupRoles(java.lang.String userId,
		long groupId, java.lang.String[] roleIds)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		UserGroupRoleLocalService userGroupRoleLocalService = UserGroupRoleLocalServiceFactory.getService();
		userGroupRoleLocalService.deleteUserGroupRoles(userId, groupId, roleIds);
	}

	public static void deleteUserGroupRoles(java.lang.String[] userIds,
		long groupId, java.lang.String roleId)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		UserGroupRoleLocalService userGroupRoleLocalService = UserGroupRoleLocalServiceFactory.getService();
		userGroupRoleLocalService.deleteUserGroupRoles(userIds, groupId, roleId);
	}

	public static void deleteUserGroupRolesByGroupId(long groupId)
		throws com.liferay.portal.SystemException {
		UserGroupRoleLocalService userGroupRoleLocalService = UserGroupRoleLocalServiceFactory.getService();
		userGroupRoleLocalService.deleteUserGroupRolesByGroupId(groupId);
	}

	public static void deleteUserGroupRolesByRoleId(java.lang.String roleId)
		throws com.liferay.portal.SystemException {
		UserGroupRoleLocalService userGroupRoleLocalService = UserGroupRoleLocalServiceFactory.getService();
		userGroupRoleLocalService.deleteUserGroupRolesByRoleId(roleId);
	}

	public static void deleteUserGroupRolesByUserId(java.lang.String userId)
		throws com.liferay.portal.SystemException {
		UserGroupRoleLocalService userGroupRoleLocalService = UserGroupRoleLocalServiceFactory.getService();
		userGroupRoleLocalService.deleteUserGroupRolesByUserId(userId);
	}

	public static void deleteUserGroupRolesByU_G(java.lang.String[] userIds,
		long groupId) throws com.liferay.portal.SystemException {
		UserGroupRoleLocalService userGroupRoleLocalService = UserGroupRoleLocalServiceFactory.getService();
		userGroupRoleLocalService.deleteUserGroupRolesByU_G(userIds, groupId);
	}

	public static boolean hasUserGroupRole(java.lang.String userId,
		long groupId, java.lang.String roleId)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		UserGroupRoleLocalService userGroupRoleLocalService = UserGroupRoleLocalServiceFactory.getService();

		return userGroupRoleLocalService.hasUserGroupRole(userId, groupId,
			roleId);
	}
}