package org.xbib.elasticsearch.module.reference;

import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
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
import org.elasticsearch.index.similarity.SimilarityLookupService;

import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
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
        client.prepareIndex("test", "authorities", "1").setSource(json).execute().actionGet();

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
        Settings indexSettings = ImmutableSettings.settingsBuilder()
                .put("ref_index", "test")
                .put("ref_type", "test")
                .put("ref_fields", "myfield").build();
        mapperParser.putTypeParser(ReferenceMapper.CONTENT_TYPE,
                new ReferenceMapper.TypeParser(client, indexSettings));

        String mapping = copyToStringFromClasspath("/ref-mapping.json");
        DocumentMapper docMapper = mapperParser.parse(mapping);
        BytesReference json = jsonBuilder().startObject().field("_id", 1).field("someField", "1234").endObject().bytes();
        ParseContext.Document doc = docMapper.parse(json).rootDoc();
        assertNotNull(doc);
        assertNotNull(docMapper.mappers().smartName("someField"));
        assertEquals(doc.get(docMapper.mappers().smartName("someField").mapper().names().indexName()), "1234");
        assertEquals(doc.getFields("someField.ref").length, 3);
        assertEquals(doc.getFields("someField.ref")[0].stringValue(), "a");
        assertEquals(doc.getFields("someField.ref")[1].stringValue(), "b");
        assertEquals(doc.getFields("someField.ref")[2].stringValue(), "c");

        // re-parse it
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
        Settings indexSettings = ImmutableSettings.EMPTY;
        mapperParser.putTypeParser(ReferenceMapper.CONTENT_TYPE,
                new ReferenceMapper.TypeParser(client, indexSettings));

        String mapping = copyToStringFromClasspath("/ref-mapping-authorities.json");
        DocumentMapper docMapper = mapperParser.parse(mapping);
        BytesReference json = jsonBuilder().startObject()
                .field("_id", 1)
                .field("title", "A title")
                .startObject("author")
                    .field("index", "test")
                    .field("type", "authorities")
                    .field("id", "1")
                   .field("fields", "author")
                .endObject()
                .endObject().bytes();
        ParseContext.Document doc = docMapper.parse(json).rootDoc();
        assertEquals(doc.getFields("author.ref").length, 1);
        assertEquals(doc.getFields("author.ref")[0].stringValue(), "John Doe");
    }

    @Test
    public void testRefFromID() throws Exception {
        Settings indexSettings = ImmutableSettings.settingsBuilder()
                .put("ref_index", "test")
                .put("ref_type", "authorities")
                .put("ref_fields", "author").build();
        mapperParser.putTypeParser(ReferenceMapper.CONTENT_TYPE,
                new ReferenceMapper.TypeParser(client, indexSettings));

        System.err.println("testRefFromID");
        String mapping = copyToStringFromClasspath("/ref-mapping-from-id.json");
        DocumentMapper docMapper = mapperParser.parse(mapping);
        BytesReference json = jsonBuilder().startObject()
                .field("_id", 1)
                .field("title", "A title")
                .field("author", "1")
                .endObject().bytes();
        ParseContext.Document doc = docMapper.parse(json).rootDoc();
        assertEquals(doc.getFields("author.ref").length, 1);
        assertEquals(doc.getFields("author.ref")[0].stringValue(), "John Doe");
    }

}
