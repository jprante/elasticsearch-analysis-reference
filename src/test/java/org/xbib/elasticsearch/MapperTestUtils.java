package org.xbib.elasticsearch;

import org.elasticsearch.Version;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.AnalysisRegistry;
import org.elasticsearch.index.analysis.AnalyzerProvider;
import org.elasticsearch.index.analysis.CharFilterFactory;
import org.elasticsearch.index.analysis.IndexAnalyzers;
import org.elasticsearch.index.analysis.TokenFilterFactory;
import org.elasticsearch.index.analysis.TokenizerFactory;
import org.elasticsearch.index.mapper.DocumentMapperParser;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.index.similarity.SimilarityService;
import org.elasticsearch.indices.IndicesModule;
import org.elasticsearch.indices.analysis.AnalysisModule;
import org.elasticsearch.indices.mapper.MapperRegistry;
import org.xbib.elasticsearch.plugin.reference.ReferencePlugin;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 *
 */
public class MapperTestUtils {

    public static DocumentMapperParser newDocumentMapperParser(String index) throws IOException {
        return newDocumentMapperParser(Settings.EMPTY, index);
    }

    public static DocumentMapperParser newDocumentMapperParser(Settings customSettings, String index) throws IOException {
        Settings settings = Settings.builder()
                .put("path.home", System.getProperty("path.home", System.getProperty("user.dir")))
                .put(IndexMetaData.SETTING_VERSION_CREATED, Version.CURRENT)
                .put(customSettings)
                .build();
        Environment environment = new Environment(settings);
        ReferencePlugin referencePlugin = new ReferencePlugin();
        AnalysisModule analysisModule = new AnalysisModule(environment, Collections.emptyList());
        IndicesModule indicesModule = new IndicesModule(Collections.singletonList(referencePlugin));
        MapperRegistry mapperRegistry = indicesModule.getMapperRegistry();
        AnalysisRegistry analysisRegistry = analysisModule.getAnalysisRegistry();
        IndexMetaData indexMetaData = IndexMetaData.builder(index)
                .settings(settings)
                .numberOfShards(1)
                .numberOfReplicas(1)
                .build();
        IndexSettings indexSettings = new IndexSettings(indexMetaData, settings);
        SimilarityService similarityService = new SimilarityService(indexSettings, SimilarityService.BUILT_IN);
        Map<String, CharFilterFactory> charFilterFactoryMap = analysisRegistry.buildCharFilterFactories(indexSettings);
        Map<String, TokenFilterFactory> tokenFilterFactoryMap = analysisRegistry.buildTokenFilterFactories(indexSettings);
        Map<String, TokenizerFactory> tokenizerFactoryMap = analysisRegistry.buildTokenizerFactories(indexSettings);
        Map<String, AnalyzerProvider<?>> analyzerProviderMap = analysisRegistry.buildAnalyzerFactories(indexSettings);
        Map<String, AnalyzerProvider<?>> normalizerProviderMap = analysisRegistry.buildNormalizerFactories(indexSettings);
        IndexAnalyzers indexAnalyzers = analysisRegistry.build(indexSettings,
                analyzerProviderMap,
                normalizerProviderMap,
                tokenizerFactoryMap,
                charFilterFactoryMap,
                tokenFilterFactoryMap);
        MapperService mapperService = new MapperService(indexSettings, indexAnalyzers, NamedXContentRegistry.EMPTY,
                similarityService, mapperRegistry, null);
        return new DocumentMapperParser(indexSettings, mapperService, indexAnalyzers, NamedXContentRegistry.EMPTY,
                similarityService, mapperRegistry, null);
    }
}
