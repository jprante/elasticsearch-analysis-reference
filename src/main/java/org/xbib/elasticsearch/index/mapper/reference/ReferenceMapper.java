package org.xbib.elasticsearch.index.mapper.reference;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.get.GetField;
import org.elasticsearch.index.mapper.FieldMapperListener;
import org.elasticsearch.index.mapper.Mapper;
import org.elasticsearch.index.mapper.MapperParsingException;
import org.elasticsearch.index.mapper.MergeContext;
import org.elasticsearch.index.mapper.MergeMappingException;
import org.elasticsearch.index.mapper.ObjectMapperListener;
import org.elasticsearch.index.mapper.ParseContext;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.common.collect.Lists.newLinkedList;
import static org.elasticsearch.index.mapper.MapperBuilders.stringField;

public class ReferenceMapper implements Mapper {

    public static final String CONTENT_TYPE = "ref";

    @SuppressWarnings({"rawtypes"})
    public static class Builder extends Mapper.Builder<Builder, ReferenceMapper> {

        private Mapper.Builder contentBuilder;

        private Mapper.Builder refBuilder;

        private Client client;

        private Settings clientSettings;

        public Builder(String name, Client client, Settings clientSettings) {
            super(name);
            this.builder = this;
            this.client = client;
            this.clientSettings = clientSettings;
            this.contentBuilder = stringField(name);
            this.refBuilder = stringField("ref");
        }

        public Builder content(Mapper.Builder content) {
            this.contentBuilder = content;
            return this;
        }

        public Builder ref(Mapper.Builder ref) {
            this.refBuilder = ref;
            return this;
        }

        @Override
        public ReferenceMapper build(BuilderContext context) {
            context.path().add(name);
            Mapper contentMapper = contentBuilder.build(context);
            Mapper refMapper = refBuilder.build(context);
            context.path().remove();
            return new ReferenceMapper(name, contentMapper, refMapper, client, clientSettings);
        }
    }

    @SuppressWarnings({"unchecked","rawtypes"})
    public static class TypeParser implements Mapper.TypeParser {

        private final Client client;

        private final Settings settings;

        public TypeParser(Client client, Settings settings) {
            this.client = client;
            this.settings = settings;
        }

        @Override
        public Mapper.Builder parse(String name, Map<String, Object> node, ParserContext parserContext)
                throws MapperParsingException {
            ReferenceMapper.Builder builder = new Builder(name, client, settings);
            for (Map.Entry<String, Object> entry : node.entrySet()) {
                String fieldName = entry.getKey();
                Object fieldNode = entry.getValue();
                if (fieldName.equals("ref")) {
                    builder.ref(parserContext.typeParser("string").parse(name, (Map<String, Object>) fieldNode, parserContext));
                }
            }
            return builder;
        }
    }

    private final String name;

    private final Mapper contentMapper;

    private final Mapper refMapper;

    private final Client client;

    private final Settings settings;

    private String index;

    private String type;

    private String[] fields;

    public ReferenceMapper(String name,
                           Mapper contentMapper,
                           Mapper refMapper,
                           Client client, Settings settings) {
        this.name = name;
        this.contentMapper = contentMapper;
        this.refMapper = refMapper;
        this.client = client;
        this.settings= settings;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void parse(final ParseContext context) throws IOException {
        String content = null;
        XContentParser parser = context.parser();
        XContentParser.Token token = parser.currentToken();
        if (token == XContentParser.Token.VALUE_STRING) {
            content = parser.text();
        } else {
            String currentFieldName = null;
            while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                if (token == XContentParser.Token.FIELD_NAME) {
                    currentFieldName = parser.currentName();
                } else if (token == XContentParser.Token.VALUE_STRING) {
                    switch (currentFieldName) {
                        case "id":
                            content = parser.text();
                            break;
                        case "index":
                            index = parser.text();
                            break;
                        case "type":
                            type = parser.text();
                            break;
                        case "fields":
                            fields = new String[]{parser.text()};
                            break;
                    }
                } else if (token == XContentParser.Token.START_ARRAY) {
                    List<String> values = newLinkedList();
                    while (parser.nextToken() != XContentParser.Token.END_ARRAY) {
                        if (parser.text() != null) {
                            values.add(parser.text());
                        }
                    }
                    fields = values.toArray(new String[values.size()]);
                }
            }
        }
        if (content == null) {
            // do not throw exception - silently ignore
            return;
        }
        context.externalValue(content);
        contentMapper.parse(context);
        final String id = content;
        if (index == null) {
            // parse content for index/type/fields pattern
            index = parseIndex(content);
            type = parseType(content);
            fields = parseFields(content);
        }
        if (index == null) {
            // no parsed content, try settings
            index = settings.get("ref_index");
            type = settings.get("ref_type");
            fields = settings.getAsArray("ref_fields");
        }
        if (index != null && type != null && fields != null) {
            // get document from other index
            GetResponse response = client.prepareGet()
                    .setIndex(index)
                    .setType(type)
                    .setId(id)
                    .setFields(fields)
                    .get(TimeValue.timeValueSeconds(5));
            if (response != null) {
                for (String field : fields) {
                    GetField getField = response.getField(field);
                    if (getField != null) {
                        for (Object object : getField.getValues()) {
                            context.externalValue(object);
                            refMapper.parse(context);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void merge(Mapper mergeWith, MergeContext mergeContext) throws MergeMappingException {
    }

    @Override
    public void traverse(FieldMapperListener fieldMapperListener) {
        contentMapper.traverse(fieldMapperListener);
        refMapper.traverse(fieldMapperListener);
    }

    @Override
    public void traverse(ObjectMapperListener objectMapperListener) {
    }

    @Override
    public void close() {
        contentMapper.close();
        refMapper.close();
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(name);
        builder.field("type", CONTENT_TYPE);
        builder.startObject("fields");
        contentMapper.toXContent(builder, params);
        refMapper.toXContent(builder, params);
        builder.endObject();
        builder.endObject();
        return builder;
    }

    private String parseIndex(String content) {
        String[] s = content.split("/");
        return s.length > 2 ? s[0] : null;
    }

    private String parseType(String content) {
        String[] s = content.split("/");
        return s.length > 2 ? s[1] : null;
    }

    private String[] parseFields(String content) {
        String[] s = content.split("/");
        return s.length > 2 ? s[2].split(",") : null;
    }
}