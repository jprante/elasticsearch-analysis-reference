package org.xbib.elasticsearch.module.reference;

import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.index.IndexableField;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.analysis.AnalysisService;
import org.elasticsearch.index.analysis.AnalyzerProviderFactory;
import org.elasticsearch.index.analysis.AnalyzerScope;
import org.elasticsearch.index.analysis.PreBuiltAnalyzerProviderFactory;
import org.elasticsearch.index.codec.docvaluesformat.DocValuesFormatService;
import org.elasticsearch.index.codec.postingsformat.PostingsFormatService;
import org.elasticsearch.index.mapper.DocumentMapper;
import org.elasticsearch.index.mapper.DocumentMapperParser;
import org.elasticsearch.index.mapper.ParseContext;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.similarity.SimilarityLookupService;

import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.search.SearchHit;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.xbib.elasticsearch.index.mapper.reference.ReferenceMapper;

import java.io.IOException;
import java.util.Map;

import static org.elasticsearch.common.io.Streams.copyToStringFromClasspath;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class ReferenceMappingTest extends Assert {

    private final static ESLogger logger = ESLoggerFactory.getLogger(ReferenceMappingTest.class.getName());

    private DocumentMapperParser mapperParser;

    private Node node;

    private Client client;

    @BeforeClass
    public void setupMapperParser() throws IOException {
        Settings nodeSettings = ImmutableSettings.settingsBuilder()
                .put("gateway.type", "none")
                .put("index.store.type", "memory")
                .put("index.number_of_shards", 1)
                .put("index.number_of_replica", 0)
                .put("cluster.routing.schedule", "50ms")
                .build();
        node = NodeBuilder.nodeBuilder().settings(nodeSettings).local(true).build().start();
        client = node.client();

        BytesReference json = jsonBuilder().startObject().array("myfield", "a","b","c").endObject().bytes();
        client.prepareIndex("test", "test", "1234").setSource(json).execute().actionGet();

        json = jsonBuilder().startObject().field("author", "John Doe").endObject().bytes();
        client.prepareIndex("authorities", "persons", "1").setSource(json).execute().actionGet();

        Index index = new Index("test");
        Map<String, AnalyzerProviderFactory> analyzerFactoryFactories = Maps.newHashMap();
        analyzerFactoryFactories.put("keyword",
                new PreBuiltAnalyzerProviderFactory("keyword", AnalyzerScope.INDEX, new KeywordAnalyzer()));
        Settings settings = ImmutableSettings.Builder.EMPTY_SETTINGS;
        AnalysisService analysisService = new AnalysisService(index, settings, null, analyzerFactoryFactories, null, null, null);
        this.mapperParser = new DocumentMapperParser(index, settings,
                analysisService,
                new PostingsFormatService(index),
                new DocValuesFormatService(index),
                new SimilarityLookupService(index, settings),
                null);
    }

    @AfterClass
    public void shutdown() {
        client.close();
        node.close();
    }

    @Test
    public void testRefMappings() throws Exception {
        mapperParser.putTypeParser(ReferenceMapper.REF, new ReferenceMapper.TypeParser(client));

        String mapping = copyToStringFromClasspath("/ref-mapping.json");
        DocumentMapper docMapper = mapperParser.parse(mapping);
        BytesReference json = jsonBuilder().startObject()
                .field("_id", 1)
                .field("someField", "1234")
                .endObject().bytes();
        ParseContext.Document doc = docMapper.parse(json).rootDoc();
        assertNotNull(doc);
        assertNotNull(docMapper.mappers().smartName("someField"));
        assertEquals(doc.get(docMapper.mappers().smartName("someField").mapper().names().indexName()), "1234");
        assertEquals(doc.getFields("someField.ref").length, 3);
        assertEquals(doc.getFields("someField.ref")[0].stringValue(), "a");
        assertEquals(doc.getFields("someField.ref")[1].stringValue(), "b");
        assertEquals(doc.getFields("someField.ref")[2].stringValue(), "c");

        // re-parse from mapping
        String builtMapping = docMapper.mappingSource().string();
        docMapper = mapperParser.parse(builtMapping);

        json = jsonBuilder().startObject().field("_id", 1).field("someField", "1234").endObject().bytes();
        doc = docMapper.parse(json).rootDoc();

        assertEquals(doc.get(docMapper.mappers().smartName("someField").mapper().names().indexName()), "1234");
        assertEquals(doc.getFields("someField.ref").length, 3);
        assertEquals(doc.getFields("someField.ref")[0].stringValue(), "a");
        assertEquals(doc.getFields("someField.ref")[1].stringValue(), "b");
        assertEquals(doc.getFields("someField.ref")[2].stringValue(), "c");
    }

    @Test
    public void testRefInDoc() throws Exception {
        mapperParser.putTypeParser(ReferenceMapper.REF, new ReferenceMapper.TypeParser(client));

        String mapping = copyToStringFromClasspath("/ref-mapping-authorities.json");
        DocumentMapper docMapper = mapperParser.parse(mapping);
        BytesReference json = jsonBuilder().startObject()
                .field("_id", 1)
                .field("title", "A title")
                .startObject("authorID")
                    .field("ref_id", "1")
                    .field("ref_index", "authorities")
                    .field("ref_type", "persons")
                    .field("ref_fields", "author")
                .endObject()
                .endObject().bytes();
        ParseContext.Document doc = docMapper.parse(json).rootDoc();
        for (IndexableField field : doc.getFields()) {
            logger.info("{} = {}", field.name(), field.stringValue());
        }
        assertEquals(doc.getFields("dc.creator").length, 1);
        assertEquals(doc.getFields("dc.creator")[0].stringValue(), "John Doe");
    }

    @Test
    public void testRefFromID() throws Exception {
        mapperParser.putTypeParser(ReferenceMapper.REF, new ReferenceMapper.TypeParser(client));
        String mapping = copyToStringFromClasspath("/ref-mapping-from-id.json");
        DocumentMapper docMapper = mapperParser.parse(mapping);
        BytesReference json = jsonBuilder().startObject()
                .field("_id", 1)
                .field("title", "A title")
                .field("authorID", "1")
                .endObject().bytes();
        ParseContext.Document doc = docMapper.parse(json).rootDoc();
        assertEquals(doc.getFields("ref").length, 1);
        assertEquals(doc.getFields("ref")[0].stringValue(), "John Doe");
    }

    @Test
    public void testSearch() throws Exception {
        String json = copyToStringFromClasspath("/ref-doc-book.json");
        String mapping = copyToStringFromClasspath("/ref-mapping-books-test.json");
        client.admin().indices().prepareCreate("books")
                .setIndex("books")
                .addMapping("test", mapping)
                .execute().actionGet();
        client.prepareIndex("books", "test", "1").setSource(json).setRefresh(true).execute().actionGet();

        // get mappings
        GetMappingsResponse getMappingsResponse= client.admin().indices().getMappings(new GetMappingsRequest()
                .indices("books")
                .types("test"))
                .actionGet();
        MappingMetaData md = getMappingsResponse.getMappings().get("books").get("test");
        logger.info("mappings={}", md.getSourceAsMap());

        // search in field 1
        SearchResponse searchResponse = client.search(new SearchRequest()
                .indices("books")
                .types("test")
                .extraSource("{\"query\":{\"match\":{\"dc.creator\":\"John Doe\"}}}"))
                .actionGet();
        logger.info("hits = {}", searchResponse.getHits().getTotalHits());
        for (SearchHit hit : searchResponse.getHits().getHits()) {
            logger.info("{}", hit.getSource());
        }
        assertEquals(searchResponse.getHits().getTotalHits(), 1);

        // search in field 2
        searchResponse = client.search(new SearchRequest()
                .indices("books")
                .types("test")
                .extraSource("{\"query\":{\"match\":{\"dc.contributor\":\"John Doe\"}}}"))
                .actionGet();
        logger.info("hits = {}", searchResponse.getHits().getTotalHits());
        for (SearchHit hit : searchResponse.getHits().getHits()) {
            logger.info("{}", hit.getSource());
        }
        assertEquals(searchResponse.getHits().getTotalHits(), 1);

    }

}
