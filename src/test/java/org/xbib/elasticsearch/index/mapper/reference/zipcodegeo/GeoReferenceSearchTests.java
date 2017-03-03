package org.xbib.elasticsearch.index.mapper.reference.zipcodegeo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.junit.Test;
import org.xbib.elasticsearch.NodeTestUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.elasticsearch.common.io.Streams.copyToString;
import static org.junit.Assert.assertEquals;

/**
 *
 */
public class GeoReferenceSearchTests extends NodeTestUtils {

    private static final Logger logger = LogManager.getLogger(GeoReferenceSearchTests.class.getName());

    @Test
    public void testGeoRefSearch() throws IOException {
        startCluster();
        try {
            try {
                client().admin().indices().prepareDelete("ref").execute().actionGet();
            } catch (Exception e) {
                logger.warn("can not delete index ref");
            }
            // index zip code geo document
            client().admin().indices().prepareCreate("ref")
                    .setSettings(copyToStringFromClasspath("ref-geo-settings.json"))
                    .addMapping("ref", copyToStringFromClasspath("ref-geo-mapping.json"))
                    .execute().actionGet();
            client().prepareIndex("ref", "ref", "11229")
                    .setSource(copyToStringFromClasspath("ref-geo-document.json"))
                    .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE).execute().actionGet();

            try {
                client().admin().indices().prepareDelete("doc").execute().actionGet();
            } catch (Exception e) {
                logger.warn("can not delete index doc");
            }
            // index zip code
            client().admin().indices().prepareCreate("doc")
                    .setSettings(copyToStringFromClasspath("geo-settings.json"))
                    .addMapping("doc", copyToStringFromClasspath("geo-mapping.json"))
                    .execute().actionGet();
            client().prepareIndex("doc", "doc", "1")
                    .setSource(copyToStringFromClasspath("geo-document.json"))
                    .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE).execute().actionGet();

            // search for geo and get back the zip code
            QueryBuilder queryBuilder = QueryBuilders.geoDistanceQuery("point")
                    .distance(1, DistanceUnit.KILOMETERS)
                    .point(40.6011, -73.9475);

            SearchResponse searchResponse = client().prepareSearch("doc")
                    .setQuery(queryBuilder)
                    .execute()
                    .actionGet();
            logger.info("geo query, hits = {}", searchResponse.getHits().getTotalHits());
            for (SearchHit hit : searchResponse.getHits().getHits()) {
                logger.info("{}", hit.getSource());
            }
            assertEquals(1, searchResponse.getHits().getTotalHits());
        } finally {
            stopCluster();
        }
    }

    private String copyToStringFromClasspath(String path) throws IOException {
        return copyToString(new InputStreamReader(getClass().getResource(path).openStream(), StandardCharsets.UTF_8));
    }
}
