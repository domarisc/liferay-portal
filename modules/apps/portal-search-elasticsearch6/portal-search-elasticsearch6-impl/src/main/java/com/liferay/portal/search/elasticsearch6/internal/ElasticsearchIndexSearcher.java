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

package com.liferay.portal.search.elasticsearch6.internal;

import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.dao.search.SearchPaginationUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.search.BaseIndexSearcher;
import com.liferay.portal.kernel.search.BooleanClause;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.GeoDistanceSort;
import com.liferay.portal.kernel.search.GroupBy;
import com.liferay.portal.kernel.search.Hits;
import com.liferay.portal.kernel.search.HitsImpl;
import com.liferay.portal.kernel.search.IndexSearcher;
import com.liferay.portal.kernel.search.Query;
import com.liferay.portal.kernel.search.QueryConfig;
import com.liferay.portal.kernel.search.SearchContext;
import com.liferay.portal.kernel.search.SearchException;
import com.liferay.portal.kernel.search.Sort;
import com.liferay.portal.kernel.search.Stats;
import com.liferay.portal.kernel.search.StatsResults;
import com.liferay.portal.kernel.search.facet.Facet;
import com.liferay.portal.kernel.search.facet.collector.FacetCollector;
import com.liferay.portal.kernel.search.filter.BooleanFilter;
import com.liferay.portal.kernel.search.filter.Filter;
import com.liferay.portal.kernel.search.filter.FilterTranslator;
import com.liferay.portal.kernel.search.geolocation.GeoLocationPoint;
import com.liferay.portal.kernel.search.highlight.HighlightUtil;
import com.liferay.portal.kernel.search.query.QueryTranslator;
import com.liferay.portal.kernel.search.suggest.QuerySuggester;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.MapUtil;
import com.liferay.portal.kernel.util.Props;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.search.constants.SearchContextAttributes;
import com.liferay.portal.search.elasticsearch6.configuration.ElasticsearchConfiguration;
import com.liferay.portal.search.elasticsearch6.constants.ElasticsearchSearchContextAttributes;
import com.liferay.portal.search.elasticsearch6.internal.connection.ElasticsearchConnectionManager;
import com.liferay.portal.search.elasticsearch6.internal.facet.AggregationFilteringFacetProcessorContext;
import com.liferay.portal.search.elasticsearch6.internal.facet.CompositeFacetProcessor;
import com.liferay.portal.search.elasticsearch6.internal.facet.FacetCollectorFactory;
import com.liferay.portal.search.elasticsearch6.internal.facet.FacetProcessor;
import com.liferay.portal.search.elasticsearch6.internal.facet.FacetProcessorContext;
import com.liferay.portal.search.elasticsearch6.internal.facet.FacetUtil;
import com.liferay.portal.search.elasticsearch6.internal.groupby.GroupByTranslator;
import com.liferay.portal.search.elasticsearch6.internal.index.IndexNameBuilder;
import com.liferay.portal.search.elasticsearch6.internal.stats.StatsTranslator;
import com.liferay.portal.search.elasticsearch6.internal.util.DocumentTypes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang.time.StopWatch;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.geo.GeoDistance;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.GeoDistanceSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Michael C. Han
 * @author Milen Dyankov
 */
@Component(
	configurationPid = "com.liferay.portal.search.elasticsearch6.configuration.ElasticsearchConfiguration",
	immediate = true, property = "search.engine.impl=Elasticsearch",
	service = IndexSearcher.class
)
public class ElasticsearchIndexSearcher extends BaseIndexSearcher {

	@Override
	public String getQueryString(SearchContext searchContext, Query query) {
		QueryBuilder queryBuilder = queryTranslator.translate(
			query, searchContext);

		return queryBuilder.toString();
	}

	@Override
	public Hits search(SearchContext searchContext, Query query)
		throws SearchException {

		StopWatch stopWatch = new StopWatch();

		stopWatch.start();

		try {
			int start = searchContext.getStart();
			int end = searchContext.getEnd();

			if (start == QueryUtil.ALL_POS) {
				start = 0;
			}
			else if (start < 0) {
				throw new IllegalArgumentException("Invalid start " + start);
			}

			if (end == QueryUtil.ALL_POS) {
				end = GetterUtil.getInteger(
					props.get(PropsKeys.INDEX_SEARCH_LIMIT));
			}
			else if (end < 0) {
				throw new IllegalArgumentException("Invalid end " + end);
			}

			Hits hits = null;

			while (true) {
				hits = doSearchHits(searchContext, query, start, end);

				Document[] documents = hits.getDocs();

				if ((documents.length != 0) || (start == 0)) {
					break;
				}

				int[] startAndEnd = SearchPaginationUtil.calculateStartAndEnd(
					start, end, hits.getLength());

				start = startAndEnd[0];
				end = startAndEnd[1];
			}

			hits.setStart(stopWatch.getStartTime());

			return hits;
		}
		catch (Exception e) {
			if (_log.isWarnEnabled()) {
				_log.warn(e, e);
			}

			if (!_logExceptionsOnly) {
				throw new SearchException(e.getMessage(), e);
			}

			return new HitsImpl();
		}
		finally {
			if (_log.isInfoEnabled()) {
				stopWatch.stop();

				_log.info(
					StringBundler.concat(
						"Searching ", query.toString(), " took ",
						stopWatch.getTime(), " ms"));
			}
		}
	}

	@Override
	public long searchCount(SearchContext searchContext, Query query)
		throws SearchException {

		StopWatch stopWatch = new StopWatch();

		stopWatch.start();

		try {
			return doSearchCount(searchContext, query);
		}
		catch (Exception e) {
			if (_log.isWarnEnabled()) {
				_log.warn(e, e);
			}

			if (!_logExceptionsOnly) {
				throw new SearchException(e.getMessage(), e);
			}

			return 0;
		}
		finally {
			if (_log.isInfoEnabled()) {
				stopWatch.stop();

				_log.info(
					StringBundler.concat(
						"Searching ", query.toString(), " took ",
						stopWatch.getTime(), " ms"));
			}
		}
	}

	@Override
	@Reference(target = "(search.engine.impl=Elasticsearch)", unbind = "-")
	public void setQuerySuggester(QuerySuggester querySuggester) {
		super.setQuerySuggester(querySuggester);
	}

	@Activate
	@Modified
	protected void activate(Map<String, Object> properties) {
		_elasticsearchConfiguration = ConfigurableUtil.createConfigurable(
			ElasticsearchConfiguration.class, properties);

		_logExceptionsOnly = _elasticsearchConfiguration.logExceptionsOnly();
	}

	protected void addFacets(
		SearchRequestBuilder searchRequestBuilder,
		List<QueryBuilder> queryBuilders, SearchContext searchContext) {

		Map<String, Facet> facetsMap = searchContext.getFacets();

		Collection<Facet> facets = facetsMap.values();

		FacetProcessorContext facetProcessorContext = getFacetProcessorContext(
			facets, searchContext);

		for (Facet facet : facets) {
			if (facet.isStatic()) {
				continue;
			}

			Optional<AggregationBuilder> optional = facetProcessor.processFacet(
				facet);

			optional.map(
				aggregationBuilder -> postProcessAggregationBuilder(
					aggregationBuilder, facetProcessorContext)
			).ifPresent(
				searchRequestBuilder::addAggregation
			);

			addFilterQuery(queryBuilders, facet, searchContext);
		}
	}

	protected void addFilterQuery(
		List<QueryBuilder> queryBuilders, Facet facet,
		SearchContext searchContext) {

		BooleanClause<Filter> booleanClause =
			facet.getFacetFilterBooleanClause();

		if (booleanClause == null) {
			return;
		}

		QueryBuilder queryBuilder = translate(booleanClause, searchContext);

		queryBuilders.add(queryBuilder);
	}

	protected void addGroupBy(
		SearchRequestBuilder searchRequestBuilder, SearchContext searchContext,
		int start, int end) {

		GroupBy groupBy = searchContext.getGroupBy();

		if (groupBy == null) {
			return;
		}

		groupByTranslator.translate(
			searchRequestBuilder, searchContext, start, end);
	}

	protected void addHighlightedField(
		HighlightBuilder highlightBuilder, QueryConfig queryConfig,
		String fieldName) {

		highlightBuilder.field(
			fieldName, queryConfig.getHighlightFragmentSize(),
			queryConfig.getHighlightSnippetSize());

		String localizedFieldName = Field.getLocalizedName(
			queryConfig.getLocale(), fieldName);

		highlightBuilder.field(
			localizedFieldName, queryConfig.getHighlightFragmentSize(),
			queryConfig.getHighlightSnippetSize());
	}

	protected void addHighlights(
		SearchRequestBuilder searchRequestBuilder, SearchContext searchContext,
		QueryConfig queryConfig) {

		if (!queryConfig.isHighlightEnabled()) {
			return;
		}

		HighlightBuilder highlightBuilder = new HighlightBuilder();

		for (String highlightFieldName : queryConfig.getHighlightFieldNames()) {
			addHighlightedField(
				highlightBuilder, queryConfig, highlightFieldName);
		}

		highlightBuilder.postTags(HighlightUtil.HIGHLIGHT_TAG_CLOSE);
		highlightBuilder.preTags(HighlightUtil.HIGHLIGHT_TAG_OPEN);

		boolean highlighterRequireFieldMatch =
			queryConfig.isHighlightRequireFieldMatch();

		boolean luceneSyntax = GetterUtil.getBoolean(
			searchContext.getAttribute(
				SearchContextAttributes.ATTRIBUTE_KEY_LUCENE_SYNTAX));

		if (luceneSyntax) {
			highlighterRequireFieldMatch = false;
		}

		highlightBuilder.requireFieldMatch(highlighterRequireFieldMatch);

		searchRequestBuilder.highlighter(highlightBuilder);
	}

	protected void addPagination(
		SearchRequestBuilder searchRequestBuilder, int start, int end) {

		searchRequestBuilder.setFrom(start);
		searchRequestBuilder.setSize(end - start);
	}

	protected void addPreference(
		SearchRequestBuilder searchRequestBuilder,
		SearchContext searchContext) {

		String preference = (String)searchContext.getAttribute(
			ElasticsearchSearchContextAttributes.
				ATTRIBUTE_KEY_SEARCH_REQUEST_PREFERENCE);

		if (!Validator.isBlank(preference)) {
			searchRequestBuilder.setPreference(preference);
		}
	}

	protected void addSelectedFields(
		SearchRequestBuilder searchRequestBuilder, QueryConfig queryConfig) {

		String[] selectedFieldNames = queryConfig.getSelectedFieldNames();

		if (ArrayUtil.isEmpty(selectedFieldNames)) {
			searchRequestBuilder.addStoredField(StringPool.STAR);
		}
		else {
			searchRequestBuilder.storedFields(selectedFieldNames);
		}
	}

	protected void addSnippets(
		Document document, Map<String, HighlightField> highlightFields,
		String fieldName, Locale locale) {

		String snippetFieldName = Field.getLocalizedName(locale, fieldName);

		HighlightField highlightField = highlightFields.get(snippetFieldName);

		if (highlightField == null) {
			highlightField = highlightFields.get(fieldName);

			snippetFieldName = fieldName;
		}

		if (highlightField == null) {
			return;
		}

		Object[] array = highlightField.fragments();

		document.addText(
			Field.SNIPPET.concat(StringPool.UNDERLINE).concat(snippetFieldName),
			StringUtil.merge(array, StringPool.TRIPLE_PERIOD));
	}

	protected void addSnippets(
		SearchHit hit, Document document, QueryConfig queryConfig) {

		Map<String, HighlightField> highlightFields = hit.getHighlightFields();

		if (MapUtil.isEmpty(highlightFields)) {
			return;
		}

		for (String highlightFieldName : queryConfig.getHighlightFieldNames()) {
			addSnippets(
				document, highlightFields, highlightFieldName,
				queryConfig.getLocale());
		}
	}

	protected void addSort(
		SearchRequestBuilder searchRequestBuilder, Sort[] sorts) {

		if (ArrayUtil.isEmpty(sorts)) {
			return;
		}

		Set<String> sortFieldNames = new HashSet<>(sorts.length);

		for (Sort sort : sorts) {
			if (sort == null) {
				continue;
			}

			String sortFieldName = getSortFieldName(sort, "_score");

			if (sortFieldNames.contains(sortFieldName)) {
				continue;
			}

			sortFieldNames.add(sortFieldName);

			SortOrder sortOrder = SortOrder.ASC;

			if (sort.isReverse() || sortFieldName.equals("_score")) {
				sortOrder = SortOrder.DESC;
			}

			SortBuilder<?> sortBuilder = null;

			if (sortFieldName.equals("_score")) {
				sortBuilder = SortBuilders.scoreSort();
			}
			else if (sort.getType() == Sort.GEO_DISTANCE_TYPE) {
				GeoDistanceSort geoDistanceSort = (GeoDistanceSort)sort;

				List<GeoPoint> geoPoints = new ArrayList<>();

				for (GeoLocationPoint geoLocationPoint :
						geoDistanceSort.getGeoLocationPoints()) {

					geoPoints.add(
						new GeoPoint(
							geoLocationPoint.getLatitude(),
							geoLocationPoint.getLongitude()));
				}

				GeoDistanceSortBuilder geoDistanceSortBuilder =
					SortBuilders.geoDistanceSort(
						sortFieldName,
						geoPoints.toArray(new GeoPoint[geoPoints.size()]));

				geoDistanceSortBuilder.geoDistance(GeoDistance.ARC);

				Collection<String> geoHashes = geoDistanceSort.getGeoHashes();

				if (!geoHashes.isEmpty()) {
					geoDistanceSort.addGeoHash(
						geoHashes.toArray(new String[geoHashes.size()]));
				}

				sortBuilder = geoDistanceSortBuilder;
			}
			else {
				FieldSortBuilder fieldSortBuilder = SortBuilders.fieldSort(
					sortFieldName);

				fieldSortBuilder.unmappedType("string");

				sortBuilder = fieldSortBuilder;
			}

			sortBuilder.order(sortOrder);

			searchRequestBuilder.addSort(sortBuilder);
		}
	}

	protected void addStats(
		SearchRequestBuilder searchRequestBuilder,
		SearchContext searchContext) {

		Map<String, Stats> statsMap = searchContext.getStats();

		for (Stats stats : statsMap.values()) {
			statsTranslator.translate(searchRequestBuilder, stats);
		}
	}

	protected SearchResponse doSearch(
			SearchContext searchContext, Query query, int start, int end,
			boolean count)
		throws Exception {

		Client client = elasticsearchConnectionManager.getClient();

		QueryConfig queryConfig = query.getQueryConfig();

		SearchRequestBuilder searchRequestBuilder = client.prepareSearch(
			getSelectedIndexNames(queryConfig, searchContext));

		searchRequestBuilder.setTypes(getSelectedTypes(queryConfig));

		addStats(searchRequestBuilder, searchContext);

		List<QueryBuilder> queryBuilders = new ArrayList<>();

		if (!count) {
			addFacets(searchRequestBuilder, queryBuilders, searchContext);
			addGroupBy(searchRequestBuilder, searchContext, start, end);
			addHighlights(searchRequestBuilder, searchContext, queryConfig);
			addPagination(searchRequestBuilder, start, end);
			addPreference(searchRequestBuilder, searchContext);
			addSelectedFields(searchRequestBuilder, queryConfig);
			addSort(searchRequestBuilder, searchContext.getSorts());

			searchRequestBuilder.setTrackScores(queryConfig.isScoreEnabled());
		}
		else {
			searchRequestBuilder.setSize(0);
		}

		if (query.getPostFilter() != null) {
			queryBuilders.add(
				filterTranslator.translate(
					query.getPostFilter(), searchContext));
		}

		if (!ListUtil.isEmpty(queryBuilders)) {
			searchRequestBuilder.setPostFilter(getPostFilter(queryBuilders));
		}

		QueryBuilder queryBuilder = queryTranslator.translate(
			query, searchContext);

		if (query.getPreBooleanFilter() != null) {
			QueryBuilder preFilterQueryBuilder = filterTranslator.translate(
				query.getPreBooleanFilter(), searchContext);

			BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

			boolQueryBuilder.filter(preFilterQueryBuilder);
			boolQueryBuilder.must(queryBuilder);

			queryBuilder = boolQueryBuilder;
		}

		searchRequestBuilder.setQuery(queryBuilder);

		String searchRequestBuilderString = searchRequestBuilder.toString();

		searchContext.setAttribute("queryString", searchRequestBuilderString);

		if (_log.isDebugEnabled()) {
			_log.debug("Search query " + searchRequestBuilderString);
		}

		SearchResponse searchResponse = searchRequestBuilder.get();

		if (_log.isInfoEnabled()) {
			_log.info(
				StringBundler.concat(
					"The search engine processed ", searchRequestBuilderString,
					" in ", searchResponse.getTook()));
		}

		return searchResponse;
	}

	protected long doSearchCount(SearchContext searchContext, Query query)
		throws Exception {

		SearchResponse searchResponse = doSearch(
			searchContext, query, searchContext.getStart(),
			searchContext.getEnd(), true);

		SearchHits searchHits = searchResponse.getHits();

		return searchHits.getTotalHits();
	}

	protected Hits doSearchHits(
			SearchContext searchContext, Query query, int start, int end)
		throws Exception {

		SearchResponse searchResponse = doSearch(
			searchContext, query, start, end, false);

		return processResponse(searchResponse, searchContext, query);
	}

	protected FacetCollector getFacetCollector(
		Facet facet, Map<String, Aggregation> aggregationsMap) {

		FacetCollectorFactory facetCollectorFactory =
			new FacetCollectorFactory();

		return facetCollectorFactory.getFacetCollector(
			aggregationsMap.get(FacetUtil.getAggregationName(facet)));
	}

	protected FacetProcessorContext getFacetProcessorContext(
		Collection<Facet> facets, SearchContext searchContext) {

		boolean basicFacetSelection = GetterUtil.getBoolean(
			searchContext.getAttribute(
				SearchContextAttributes.ATTRIBUTE_KEY_BASIC_FACET_SELECTION));

		if (basicFacetSelection) {
			return null;
		}

		return AggregationFilteringFacetProcessorContext.newInstance(facets);
	}

	protected QueryBuilder getPostFilter(List<QueryBuilder> queryBuilders) {
		if (queryBuilders.size() == 1) {
			return queryBuilders.get(0);
		}

		BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

		for (QueryBuilder queryBuilder : queryBuilders) {
			boolQueryBuilder.must(queryBuilder);
		}

		return boolQueryBuilder;
	}

	protected String[] getSelectedIndexNames(
		QueryConfig queryConfig, SearchContext searchContext) {

		String[] selectedIndexNames = queryConfig.getSelectedIndexNames();

		if (ArrayUtil.isNotEmpty(selectedIndexNames)) {
			return selectedIndexNames;
		}

		String indexName = indexNameBuilder.getIndexName(
			searchContext.getCompanyId());

		return new String[] {indexName};
	}

	protected String[] getSelectedTypes(QueryConfig queryConfig) {
		String[] selectedTypes = queryConfig.getSelectedTypes();

		if (ArrayUtil.isNotEmpty(selectedTypes)) {
			return selectedTypes;
		}

		return new String[] {DocumentTypes.LIFERAY};
	}

	protected String getSortFieldName(Sort sort, String scoreFieldName) {
		String sortFieldName = sort.getFieldName();

		if (Objects.equals(sortFieldName, Field.PRIORITY)) {
			return sortFieldName;
		}

		return Field.getSortFieldName(sort, scoreFieldName);
	}

	protected AggregationBuilder postProcessAggregationBuilder(
		AggregationBuilder aggregationBuilder,
		FacetProcessorContext facetProcessorContext) {

		if (facetProcessorContext != null) {
			return facetProcessorContext.postProcessAggregationBuilder(
				aggregationBuilder);
		}

		return aggregationBuilder;
	}

	protected Hits processResponse(
		SearchResponse searchResponse, SearchContext searchContext,
		Query query) {

		SearchHits searchHits = searchResponse.getHits();

		Hits hits = new HitsImpl();

		updateFacetCollectors(searchContext, searchResponse);
		updateGroupedHits(searchResponse, searchContext, query, hits);
		updateStatsResults(searchContext, searchResponse, hits);

		TimeValue timeValue = searchResponse.getTook();

		hits.setSearchTime((float)timeValue.getSecondsFrac());

		return processSearchHits(searchHits, query, hits);
	}

	protected Document processSearchHit(
		SearchHit searchHit, QueryConfig queryConfig) {

		Document document = searchHitDocumentTranslator.translate(searchHit);

		populateUID(document, queryConfig);

		return document;
	}

	protected Hits processSearchHits(
		SearchHits searchHits, Query query, Hits hits) {

		List<Document> documents = new ArrayList<>();
		List<Float> scores = new ArrayList<>();

		if (searchHits.getTotalHits() > 0) {
			SearchHit[] searchHitsArray = searchHits.getHits();

			for (SearchHit searchHit : searchHitsArray) {
				Document document = processSearchHit(
					searchHit, query.getQueryConfig());

				documents.add(document);

				scores.add(searchHit.getScore());

				addSnippets(searchHit, document, query.getQueryConfig());
			}
		}

		hits.setDocs(documents.toArray(new Document[documents.size()]));
		hits.setLength((int)searchHits.getTotalHits());
		hits.setQuery(query);
		hits.setQueryTerms(new String[0]);
		hits.setScores(ArrayUtil.toFloatArray(scores));

		return hits;
	}

	protected QueryBuilder translate(
		BooleanClause<Filter> booleanClause, SearchContext searchContext) {

		BooleanFilter booleanFilter = new BooleanFilter();

		booleanFilter.add(
			booleanClause.getClause(), booleanClause.getBooleanClauseOccur());

		return filterTranslator.translate(booleanFilter, searchContext);
	}

	protected void updateFacetCollectors(
		SearchContext searchContext, SearchResponse searchResponse) {

		Aggregations aggregations = searchResponse.getAggregations();

		if (aggregations == null) {
			return;
		}

		Map<String, Aggregation> aggregationsMap = aggregations.getAsMap();

		Map<String, Facet> facetsMap = searchContext.getFacets();

		for (Facet facet : facetsMap.values()) {
			if (!facet.isStatic()) {
				facet.setFacetCollector(
					getFacetCollector(facet, aggregationsMap));
			}
		}
	}

	protected void updateGroupedHits(
		SearchResponse searchResponse, SearchContext searchContext, Query query,
		Hits hits) {

		GroupBy groupBy = searchContext.getGroupBy();

		if (groupBy == null) {
			return;
		}

		Aggregations aggregations = searchResponse.getAggregations();

		Map<String, Aggregation> aggregationsMap = aggregations.getAsMap();

		Terms terms = (Terms)aggregationsMap.get(
			GroupByTranslator.GROUP_BY_AGGREGATION_PREFIX + groupBy.getField());

		List<? extends Terms.Bucket> buckets = terms.getBuckets();

		for (Terms.Bucket bucket : buckets) {
			Aggregations bucketAggregations = bucket.getAggregations();

			TopHits topHits = bucketAggregations.get(
				GroupByTranslator.TOP_HITS_AGGREGATION_NAME);

			SearchHits groupedSearchHits = topHits.getHits();

			Hits groupedHits = new HitsImpl();

			processSearchHits(groupedSearchHits, query, groupedHits);

			groupedHits.setLength((int)groupedSearchHits.getTotalHits());

			hits.addGroupedHits(bucket.getKeyAsString(), groupedHits);
		}
	}

	protected void updateStatsResults(
		SearchContext searchContext, SearchResponse searchResponse, Hits hits) {

		Map<String, Stats> statsMap = searchContext.getStats();

		if (statsMap.isEmpty()) {
			return;
		}

		Aggregations aggregations = searchResponse.getAggregations();

		if (aggregations == null) {
			return;
		}

		Map<String, Aggregation> aggregationsMap = aggregations.getAsMap();

		for (Stats stats : statsMap.values()) {
			if (!stats.isEnabled()) {
				continue;
			}

			StatsResults statsResults = statsTranslator.translate(
				aggregationsMap, stats);

			hits.addStatsResults(statsResults);
		}
	}

	@Reference
	protected ElasticsearchConnectionManager elasticsearchConnectionManager;

	@Reference(service = CompositeFacetProcessor.class)
	protected FacetProcessor<SearchRequestBuilder> facetProcessor;

	@Reference(target = "(search.engine.impl=Elasticsearch)")
	protected FilterTranslator<QueryBuilder> filterTranslator;

	@Reference
	protected GroupByTranslator groupByTranslator;

	@Reference
	protected IndexNameBuilder indexNameBuilder;

	@Reference
	protected Props props;

	@Reference(target = "(search.engine.impl=Elasticsearch)")
	protected QueryTranslator<QueryBuilder> queryTranslator;

	@Reference
	protected SearchHitDocumentTranslator searchHitDocumentTranslator;

	@Reference
	protected StatsTranslator statsTranslator;

	private static final Log _log = LogFactoryUtil.getLog(
		ElasticsearchIndexSearcher.class);

	private volatile ElasticsearchConfiguration _elasticsearchConfiguration;
	private boolean _logExceptionsOnly;

}