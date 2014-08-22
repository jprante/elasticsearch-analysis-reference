package org.xbib.elasticsearch.module.reference;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.AbstractIndexComponent;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.index.settings.IndexSettings;
import org.xbib.elasticsearch.index.mapper.reference.ReferenceMapper;

public class RegisterReferenceType extends AbstractIndexComponent {

    @Inject
    public RegisterReferenceType(Index index, @IndexSettings Settings indexSettings,
                                 MapperService mapperService, Client client) {
        super(index, indexSettings);
        mapperService.documentMapperParser().putTypeParser("ref", new ReferenceMapper.TypeParser(client));
    }
}
