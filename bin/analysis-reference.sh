#!/bin/bash

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