[![0](https://upload.wikimedia.org/wikipedia/commons/thumb/a/a3/SanDiegoCityCollegeLearningResource_-_bookshelf.jpg/320px-SanDiegoCityCollegeLearningResource_-_bookshelf.jpg)](https://upload.wikimedia.org/wikipedia/commons/thumb/a/a3/SanDiegoCityCollegeLearningResource_-_bookshelf.jpg/320px-SanDiegoCityCollegeLearningResource_-_bookshelf.jpg)

Image taken from Wikipedia https://commons.wikimedia.org/wiki/File:SanDiegoCityCollegeLearningResource_-_bookshelf.jpg
licensed under the Creative Commons Attribution 2.0 Generic license.

# Reference plugin for Elasticsearch

This plugin for [Elasticsearch](http://github.com/elasticsearch/elasticsearch) uses
a reference mechanism for indexing content from other documents. The lookup takes place during
the indexing (analysis field mapping) phase, not at search time.

For example, if you have two indexes whre you want to link from one index to another by using
document IDs, you can use this plugin to reference the content behind the ID.

This process is also known as denormalization. Denormalization in this sense can be defined as 
the copying of the same data into multiple documents in order to simplify querying.

See the example below how to create a library catalog entry for the book "Goethe's Faust" and
making it searchable by referencing variant forms of the author's name.

The analyzer is capable of fetching content from other fields in other documents because
Elasticsearch documents have a near-real-time (NRT) read property. 
The mapping phase of this analyzer can safely execute a get request and transfer field 
values from already indexed documents into a new document.

Since the documents are denormalized, it is required to repeat creating the whole index when 
the referenced data source has changed. This can be expensive and must be balanced against
the additional query load on a normalized data model.

Note, in a search response, the `_source` field will not reflect the included content 
because the `_source` field is immutable. It is important for Elasticsearch internal functions 
that `_source` field value represents exactly what the user pushed over the API into 
the index.

## Compatibility matrix

| Elasticsearch  | Plugin        | Release date |
| -------------- | ------------- | ------------ |
| 2.2.1          | 2.2.1.0       | Jun 30, 2016 |
| 2.1.1          | 2.1.1.0       | Dec 24, 2015 |
| 2.0.0-beta2    | 2.0.0-beta2.0 | Sep 20, 2015 |
| 1.3.2          | 1.3.0.3       | Aug 24, 2014 |
| 1.3.1          | 1.3.0.0       | Aug  5, 2014 |
| 1.2.1          | 1.2.1.0       | Jul  1, 2014 |

## Installation 2.x

    ./bin/plugin install http://xbib.org/repository/org/xbib/elasticsearch/plugin/elasticsearch-analysis-reference/2.2.1.0/elasticsearch-analysis-reference-2.2.1.0-plugin.zip

## Installation 1.x

    ./bin/plugin -install analysis-reference -url http://xbib.org/repository/org/xbib/elasticsearch/plugin/elasticsearch-analysis-reference/1.3.0.3/elasticsearch-analysis-reference-1.3.0.3-plugin.zip

Do not forget to restart the node after installing.

## Project docs

The Maven project site is available at [Github](http://jprante.github.io/elasticsearch-analysis-reference)

## Issues

All feedback is welcome! If you find issues, please post them at [Github](https://github.com/jprante/elasticsearch-analysis-reference/issues)

# Documentation

This plugin introduces a new mapping field type named `ref`. 

If a field with name `fieldname` is of type `ref`, the values in a document must contain a coordinate of the form

    "fieldname" : {
        "type" : "ref",
        "ref_index" : ...
        "ref_type" : ...
        "ref_fields" : ...
        "to" : [
           "my_field_1",
           "my_field_2"
        ]
    }
    
where `ref_index`, `ref_type`, `ref_fields` is the coordinate of field content in the cluster, 
and `to` denotes the index fields where one or more fields from where the values are indexed into. 

If there is more than one value, all values are appended into an array (multivalued field).

If the coordinate does not point to a valid document, the analyzer does not bail out with failure, 
but the referencing is skipped.

# Example: Goethe's many names

![Goethe](https://upload.wikimedia.org/wikipedia/commons/thumb/0/0e/Goethe_%28Stieler_1828%29.jpg/195px-Goethe_%28Stieler_1828%29.jpg)

Image taken from Wikipedia https://commons.wikimedia.org/wiki/File:Goethe_(Stieler_1828).jpg

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
         "properties" : { 
           "title" : { "type" : "string" },
           "author" : {
             "properties" : {
               "preferredName" : { "type" : "string" },
               "variantName" : { 
                     "type" : "ref",                    
                     "ref_index" : "test",
                     "ref_type" : "authorities",
                     "ref_fields" : [ "variants" ],
                     "to" : [ "variant", "_all" ]
               }
             }
           }
         }
     }
     '
     
     curl -XGET 'localhost:9200/test/books/_mapping?pretty'
     
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
     
     curl -XPUT 'localhost:9200/test/books/1' -d '
     {
       "title" : "Faust",
       "author" : {
           "preferredName" : "Johann Wolfgang Goethe",
           "variantName" : "Johann Wolfgang Goethe"                        
       }
     }
     '
     
     # here we do refresh for the books index
     
     curl -XGET 'localhost:9200/test/_refresh'
     
     curl -XPOST 'localhost:9200/test/books/_search?pretty' -d '
     {
        "query" : {
            "match" : {
                 "variant" : "Gūta"
            }
        }
     }
     '
     
     curl -XPOST 'localhost:9200/test/books/_search?pretty' -d '
     {
        "query" : {
            "match" : {
                 "_all" : "Gūta"
            }
        }
     }
     '
     
Result of both searches for `Gūta` is `Johann Wolfgang Goethe` !

    {
      "took" : 68,
      "timed_out" : false,
      "_shards" : {
        "total" : 5,
        "successful" : 5,
        "failed" : 0
      },
      "hits" : {
        "total" : 1,
        "max_score" : 0.03321779,
        "hits" : [ {
          "_index" : "test",
          "_type" : "books",
          "_id" : "1",
          "_score" : 0.03321779,
          "_source":
        {
          "title" : "Faust",
          "author" : {
              "preferredName" : "Johann Wolfgang Goethe",
              "variantName" : "Johann Wolfgang Goethe"                        
          }
        }
        
        } ]
      }
    }
    {
      "took" : 10,
      "timed_out" : false,
      "_shards" : {
        "total" : 5,
        "successful" : 5,
        "failed" : 0
      },
      "hits" : {
        "total" : 1,
        "max_score" : 0.028767452,
        "hits" : [ {
          "_index" : "test",
          "_type" : "books",
          "_id" : "1",
          "_score" : 0.028767452,
          "_source":
        {
          "title" : "Faust",
          "author" : {
              "preferredName" : "Johann Wolfgang Goethe",
              "variantName" : "Johann Wolfgang Goethe"                        
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