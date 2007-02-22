/**
 * Copyright (c) 2000-2007 Liferay, Inc. All rights reserved.
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

package com.liferay.portlet.tags.service.persistence;

import com.liferay.portal.SystemException;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.spring.hibernate.CustomSQLUtil;
import com.liferay.portal.spring.hibernate.HibernateUtil;
import com.liferay.portlet.tags.model.impl.TagsAssetImpl;
import com.liferay.util.StringUtil;
import com.liferay.util.dao.hibernate.QueryPos;
import com.liferay.util.dao.hibernate.QueryUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.SQLQuery;
import org.hibernate.Session;

/**
 * <a href="TagsAssetFinder.java.html"><b><i>View Source</i></b></a>
 *
 * @author Brian Wing Shun Chan
 *
 */
public class TagsAssetFinder {

	public static String COUNT_BY_AND_ENTRY_IDS =
		TagsAssetFinder.class.getName() + ".countByAndEntryIds";

	public static String COUNT_BY_OR_ENTRY_IDS =
		TagsAssetFinder.class.getName() + ".countByOrEntryIds";

	public static String FIND_BY_AND_ENTRY_IDS =
		TagsAssetFinder.class.getName() + ".findByAndEntryIds";

	public static String FIND_BY_OR_ENTRY_IDS =
		TagsAssetFinder.class.getName() + ".findByOrEntryIds";

	public static int countByAndEntryIds(long[] entryIds)
		throws SystemException {

		if (entryIds.length == 0) {
			return 0;
		}

		Session session = null;

		try {
			session = HibernateUtil.openSession();

			StringBuffer sb = new StringBuffer();

			sb.append("SELECT COUNT(DISTINCT assetId) AS COUNT_VALUE ");
			sb.append("FROM TagsAssets_TagsEntries WHERE entryId = ?");

			if (entryIds.length > 1) {
				sb.append(" AND assetId IN (");
			}

			for (int i = 1; i < entryIds.length; i++) {
				sb.append(CustomSQLUtil.get(FIND_BY_AND_ENTRY_IDS));

				if ((i + 1) < entryIds.length) {
					sb.append(" AND assetId IN (");
				}
			}

			for (int i = 1; i < entryIds.length; i++) {
				if ((i + 1) < entryIds.length) {
					sb.append(StringPool.CLOSE_PARENTHESIS);
				}
			}

			if (entryIds.length > 1) {
				sb.append(StringPool.CLOSE_PARENTHESIS);
			}

			String sql = sb.toString();

			SQLQuery q = session.createSQLQuery(sql);

			q.setCacheable(false);

			q.addScalar(HibernateUtil.getCountColumnName(), Hibernate.LONG);

			QueryPos qPos = QueryPos.getInstance(q);

			_setEntryIds(qPos, entryIds);

			Iterator itr = q.list().iterator();

			if (itr.hasNext()) {
				Long count = (Long)itr.next();

				if (count != null) {
					return count.intValue();
				}
			}

			return 0;
		}
		catch (Exception e) {
			throw new SystemException(e);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	public static int countByOrEntryIds(long[] entryIds)
		throws SystemException {

		if (entryIds.length == 0) {
			return 0;
		}

		Session session = null;

		try {
			session = HibernateUtil.openSession();

			String sql = CustomSQLUtil.get(COUNT_BY_OR_ENTRY_IDS);

			sql = StringUtil.replace(
				sql, "[$ENTRY_ID$]", _getEntryIds(entryIds));

			SQLQuery q = session.createSQLQuery(sql);

			q.setCacheable(false);

			q.addScalar(HibernateUtil.getCountColumnName(), Hibernate.LONG);

			QueryPos qPos = QueryPos.getInstance(q);

			_setEntryIds(qPos, entryIds);

			Iterator itr = q.list().iterator();

			if (itr.hasNext()) {
				Long count = (Long)itr.next();

				if (count != null) {
					return count.intValue();
				}
			}

			return 0;
		}
		catch (Exception e) {
			throw new SystemException(e);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	public static List findByAndEntryIds(long[] entryIds, int begin, int end)
		throws SystemException {

		if (entryIds.length == 0) {
			return new ArrayList();
		}

		Session session = null;

		try {
			session = HibernateUtil.openSession();

			StringBuffer sb = new StringBuffer();

			sb.append("SELECT DISTINCT {TagsAsset.*} ");
			sb.append("FROM TagsAsset WHERE assetId IN (");

			for (int i = 0; i < entryIds.length; i++) {
				sb.append(CustomSQLUtil.get(FIND_BY_AND_ENTRY_IDS));

				if ((i + 1) < entryIds.length) {
					sb.append(" AND assetId IN (");
				}
			}

			for (int i = 0; i < entryIds.length; i++) {
				if ((i + 1) < entryIds.length) {
					sb.append(StringPool.CLOSE_PARENTHESIS);
				}
			}

			sb.append(") ORDER BY TagsAsset.modifiedDate DESC");

			String sql = sb.toString();

			SQLQuery q = session.createSQLQuery(sql);

			q.setCacheable(false);

			q.addEntity("TagsAsset", TagsAssetImpl.class);

			QueryPos qPos = QueryPos.getInstance(q);

			_setEntryIds(qPos, entryIds);

			return QueryUtil.list(q, HibernateUtil.getDialect(), begin, end);
		}
		catch (Exception e) {
			throw new SystemException(e);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	public static List findByOrEntryIds(long[] entryIds)
		throws SystemException {

		return findByOrEntryIds(entryIds, QueryUtil.ALL_POS, QueryUtil.ALL_POS);
	}

	public static List findByOrEntryIds(long[] entryIds, int begin, int end)
		throws SystemException {

		if (entryIds.length == 0) {
			return new ArrayList();
		}

		Session session = null;

		try {
			session = HibernateUtil.openSession();

			String sql = CustomSQLUtil.get(FIND_BY_OR_ENTRY_IDS);

			sql = StringUtil.replace(
				sql, "[$ENTRY_ID$]", _getEntryIds(entryIds));

			SQLQuery q = session.createSQLQuery(sql);

			q.setCacheable(false);

			q.addEntity("TagsAsset", TagsAssetImpl.class);

			QueryPos qPos = QueryPos.getInstance(q);

			_setEntryIds(qPos, entryIds);

			return QueryUtil.list(q, HibernateUtil.getDialect(), begin, end);
		}
		catch (Exception e) {
			throw new SystemException(e);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	private static String _getEntryIds(long[] entryIds) {
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < entryIds.length; i++) {
			sb.append("TagsEntry.entryId = ? ");

			if ((i + 1) != entryIds.length) {
				sb.append("OR ");
			}
		}

		return sb.toString();
	}

	private static void _setEntryIds(QueryPos qPos, long[] entryIds) {
		for (int i = 0; i < entryIds.length; i++) {
			qPos.add(entryIds[i]);
		}
	}

}