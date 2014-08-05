# Elasticsearch Analysis Reference Plugin

This plugin for [Elasticsearch](http://github.com/elasticsearch/elasticsearch) uses
a reference mechanism for including content from other documents in the cluster during
the analysis field mapping phase.

This process is also known as denormalization.

Denormalization can be defined as the copying of the same data into multiple documents 
in order to simplify query processing or to fit the user’s data into a particular data model.

See the example below how to create a library catalog entry for the book "Goethe's Faust" and
making it searchable by referencing variant forms of the author's name at indexing time.

The analyzer is capable of fetching content from other fields in other documents because
Elasticsearch documents have a near-real-time (NRT) read property. The mapping phase of this analyzer
can safely execute a get request and transfer field values from already indexed documents
into a new document.

Since the documents are denormalized, it is required to reindex the whole index when 
the referenced data source has changed.

Note, in a search response, the `_source` field will not reflect the included content 
because the `_source` field is immutable. It is important for Elasticsearch internal functions 
that `_source` field value represents exactly what the user pushed over the API into 
the index. But by following the coordinates of the referred document it is easy 
to lookup the included document content afterwards.

## Versions

| Elasticsearch  | Plugin       | Release date |
| -------------- | ------------ | ------------ |
| 1.3.1          | 1.3.0.0      | Aug  5, 2014 |
| 1.2.1          | 1.2.1.0      | Jul  1, 2014 |

## Installation

    ./bin/plugin -install analysis-reference -url http://xbib.org/repository/org/xbib/elasticsearch/plugin/elasticsearch-analysis-reference/1.3.0.0/elasticsearch-analysis-reference-1.3.0.0-plugin.zip

Do not forget to restart the node after installing.

## Checksum

| File                                                  | SHA1                                     |
| ----------------------------------------------------- | -----------------------------------------|
| elasticsearch-analysis-reference-1.3.0.0-plugin.zip   | 1151801eb4de2ecb8d6dcae41a999c8b6b0a6579 |
| elasticsearch-analysis-reference-1.2.1.0-plugin.zip   | 1bdaf7d1b0cc8c8a08e1b5487ab39d351c9365d7 |


## Project docs

The Maven project site is available at [Github](http://jprante.github.io/elasticsearch-analysis-reference)

## Issues

All feedback is welcome! If you find issues, please post them at [Github](https://github.com/jprante/elasticsearch-analysis-reference/issues)

# Documentation

This plugin introduces a new mapping field type named `ref`. If a field with name `fieldname` 
is of type `ref`, the values in a document must contain a coordinate of the form

    "fieldname" : {
        "index" : ...
        "type" : ...
        "id" : ...
        "fields" : ...
    }
    
where `index/type/id` is the coordinate of a document in the ELasticsearch cluster, and `fields` denotes
one or more fields from where the values are fetched. If there is more than one value, all values are
appended into an array (multivalued field).

If the coordinate does not point to a valid document, the analyzer does not bail out with failure, 
but the referencing is skipped.

# Example


    curl -XDELETE 'localhost:9200/test'
    
    curl -XPUT 'localhost:9200/test'
    
    curl -XPOST 'localhost:9200/test/authorities/_mapping' -d '
    {
      "authorities" : {
        "properties" : {
           "variants" : { "type" : "string" }
        }
      }
    }
    '
    
    curl -XPOST 'localhost:9200/test/books/_mapping' -d '
    { 
      "books" : {
        "properties" : { 
          "title" : { "type" : "string" },
          "author" : {
            "properties" : {
              "preferredName" : { "type" : "string" },
              "variantName" : { "type" : "ref" }
            }
          }
        }
      } 
    }
    '
    
    # http://d-nb.info/gnd/118540238
    
    curl -XPUT 'localhost:9200/test/authorities/Johann%20Wolfgang%20Goethe' -d '
    { 
      "variants" : [
            "Goethe, Johann Wolfgang v.",
            "Goethe, Johann Wolfgang",
            "Goethe, Johann W. von",
            "Goethe, Johann W.",
            "Goethe, Johan Wolfgang von",
            "Goethe, Joh. Wolfg. v.",
            "Goethe, J. Wolfgang",
            "Goethe, J. W. von",
            "Goethe, J. W. v.",
            "Goethe, J. W.",
            "Goethe, Ioannes W.",
            "Goethe, Iohan Wolphgang",
            "Goethe, Jan Wolfgang",
            "Goethe, Jean Wolfgang von",
            "Goethe, João Wolfgang von",
            "Goethe, Juan W.",
            "Goethe, Juan Wolfgang von",
            "Goethe, Volfango",
            "Goethe, Volfgango",
            "Goethe, Wolfgang von",
            "Goethe, Wolfgang",
            "Goethe, Wolfango",
            "Goethe, Wolfgango",
            "Goethe, ...",
            "Goethius, ...",
            "Göthe, Johann Wolfgang von",
            "Göthe, J. W. von",
            "Göthe, Giov. Volfango",
            "Göte, Iogann V.",
            "Göte, ...",
            "Gede, ...",
            "Gēte, ...",
            "Gě%27ṭe, ...",
            "Gete, ...",
            "Gete, Iogann W.",
            "Gete, Iogann Vol%27fgang",
            "Gete, J. V.",
            "Ge͏̈te, Iogan",
            "Gete, Iohan Volfgang",
            "Gete, I. V.",
            "Gete, Johan Volfgang",
            "Géte, Johans Volfgangs",
            "Gete, Johann Vol%27fgang",
            "Gete, Jogann Vol%27fgang fon",
            "Gete, Vol%27fgang",
            "Gete, Yogann Vol%27fgang",
            "Gete, Yôhân Wôlfgang fôn",
            "Gête, Yôhan Wolfgang",
            "Gete, Yohann Volfqanq",
            "Gêtê, Y. W.",
            "Geteh, Yohan Ṿolfgang fon",
            "Gkaite, ...",
            "Gkaite, Giochan Bolphnkannk phon",
            "Gkaite, Giochan B. phon",
            "Gót, ...",
            "G%27ote, ...",
            "G%27ote, Jochan Volfgang",
            "Goet%27e, ...",
            "Goet%27e, Iohan Volp%27gang",
            "Gūta, Yūhān Wulfgāng fun",
            "Gūta, Yūhān Wulfgāng fūn",
            "Gūta, ...",
            "Ġūtih, Yūhān Vūlfġanġ fūn",
            "Gyot%27e, Yohan Wolfgang",
            "He͏̈te, E͏̈han ",
            "Hete, Johann-Vol%27fhanh",
            "Koet%27e, ...",
            "Koet%27e, Yohan Polp%27ŭgang p%27on",
            "Gėtė, Johanas Volfgangas",
            "Höte, Iohann Volfqanq",
            "von Goethe, Johann Wolfgang",
            "Ge de",
            "Gede",
            "Gede, ...",
            "괴테, 요한 볼프강 폰",
            "歌德",
            "約翰・沃爾夫岡・馮・歌德",
            "约翰・沃尔夫冈・冯・歌德 ",
            "ゲーテ, ヨハン・ヴォルフガング・フォン",
             "גתה, יוהן וולפגנג פון"
      ]
    }
    '
    # no refresh needed here!
    
    curl -XPUT 'localhost:9200/test/books/1' -d '
    {
      "title" : "Faust",
      "author" : {
          "preferredName" : "Johann Wolfgang Goethe",
          "variantName" : {
              "index" : "test",
              "type" : "authorities",
              "id" : "Johann Wolfgang Goethe",
              "fields" : "variants"
          }
      }
    }
    '
    
    # here we do refresh for the books index
    
    curl -XGET 'localhost:9200/test/_refresh'
    
    curl -XPOST 'localhost:9200/test/books/_search?pretty' -d '
    {
       "query" : {
           "match_phrase" : {
                "author.variantName.ref" : "Gūta, Yūhān Wulfgāng fun"
           }
       }
    }
    '
    
    curl -XPOST 'localhost:9200/test/books/_search?pretty' -d '
    {
       "query" : {
           "match_phrase" : {
                "_all" : "Gūta, Yūhān Wulfgāng fun"
           }
       }
    }
    '
    
Result of both searches for `Gūta, Yūhān Wulfgāng fun` is `Johann Wolfgang Goethe` !
    
    {
      "took" : 1,
      "timed_out" : false,
      "_shards" : {
        "total" : 5,
        "successful" : 5,
        "failed" : 0
      },
      "hits" : {
        "total" : 1,
        "max_score" : 0.067124054,
        "hits" : [ {
          "_index" : "test",
          "_type" : "books",
          "_id" : "1",
          "_score" : 0.067124054,
          "_source":
    {
      "title" : "Faust",
      "author" : {
          "preferredName" : "Johann Wolfgang Goethe",
          "variantName" : {
              "index" : "test",
              "type" : "authorities",
              "id" : "Johann Wolfgang Goethe",
              "fields" : "variants"
          }
      }
    }
    
        } ]
      }
    }
    

# License

Elasticsearch Reference Plugin

Copyright (C) 2014 Jörg Prante

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
you may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.