package org.xbib.elasticsearch.index.mapper.reference.zipcodegeo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.index.IndexableField;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.compress.CompressedXContent;
import org.elasticsearch.index.mapper.DocumentMapper;
import org.elasticsearch.index.mapper.DocumentMapperParser;
import org.elasticsearch.index.mapper.ParseContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xbib.elasticsearch.NodeTestUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.elasticsearch.common.io.Streams.copyToString;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.xbib.elasticsearch.MapperTestUtils.newDocumentMapperParser;

/**
 *
 */
public class GeoReferenceMappingTests extends NodeTestUtils {

    private static final Logger logger = LogManager.getLogger(GeoReferenceMappingTests.class.getName());

    @Before
    public void setupReferences() throws IOException {
        startCluster();
        try {
            client().admin().indices().prepareDelete("ref").execute().actionGet();
        } catch (Exception e) {
            logger.warn("unable to delete 'ref' index");
        }
        client().prepareIndex("ref", "ref", "11229")
                .setSource(jsonBuilder().startObject()
                        .startObject("point")
                        .field("lat", 40.6011)
                        .field("lon", -73.9475)
                        .endObject()
                        .endObject())
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
                .execute().actionGet();
    }

    @After
    public void cleanup() throws IOException {
        stopCluster();
    }

    @Test
    public void testGeoRef() throws IOException {
        String mapping = copyToStringFromClasspath("geo-mapping.json");
        DocumentMapperParser mapperParser = newDocumentMapperParser("doc");
        DocumentMapper docMapper = mapperParser.parse("doc", new CompressedXContent(mapping));
        BytesReference json = jsonBuilder().startObject()
                .field("zipcode", "11229")
                .endObject().bytes();
        ParseContext.Document doc = docMapper.parse("doc", "doc", "1", json).rootDoc();
        assertNotNull(doc);
        for (IndexableField field : doc.getFields()) {
            logger.info("testGeoRefMappings {} = {}", field.name(), field.stringValue());
        }
        assertNotNull(docMapper.mappers().smartNameFieldMapper("point"));
        // strange format, but we don't care. Needs adaption.
        assertNull(doc.getFields("point")[0].stringValue());
        assertEquals("4160878338827240632", doc.getFields("point")[1].stringValue());
    }

    private String copyToStringFromClasspath(String path) throws IOException {
        return copyToString(new InputStreamReader(getClass().getResource(path).openStream(), StandardCharsets.UTF_8));
    }
}
