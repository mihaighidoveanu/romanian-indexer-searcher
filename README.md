# Romanian Information Retrieval System

## Dependencies

- Java 1.8
- Lucene 8.0.0
- Tika 1.20

##Methodology

Documents are :
1. tokenized
2. lowercased
3. diacritics are removed
We do this before stemming to prevent the following *understemming* scenario :
```
functioneaza -> functioneaz
funcţionează -> funcţio
```
4. stopwords are removed
5. tokens are stemmed (three times)
We encourage *overstemming* because the SnowballStemmers seems to *understem*. 
```
mamei -> mame -> mam
mama -> mam
```
In our case, *overstemming* means retrieving all the relevant documents and including some non-relevant ones.
While *understemming* means retrieving only relevant documents, with the risk of missing some of them. 
So I choose to prefer *overstemming* to *understemming*, so we won't miss any relevant documents. 

## Input 

- Input paths are hard-coded in the source code.
- By default, **Indexer** retrieves documents from *"docs/"* directory.
- By default, **Searcher** retrieves the inverted index from *"index/"* directory. 
- Queries are hard-coded in an array in *Searcher.java -> main()*. 

## Output 
- By default, **Indexer** writes the inverted index under *"index/"* directory.
- By default, **Indexer** builds a *"stopwords.txt"* file with the necessary romanian stopwords without diacritics

## Running 

```java
javac -cp ".:*:lucene-8.0.0/core/*:lucene-8.0.0/analysis/common/*:lucene-8.0.0/queryparser/*" Indexer.java
java -cp ".:*:lucene-8.0.0/core/*:lucene-8.0.0/analysis/common/*:lucene-8.0.0/queryparser/*" Indexer
javac -cp ".:*:lucene-8.0.0/core/*:lucene-8.0.0/analysis/common/*:lucene-8.0.0/queryparser/*" Searcher.java
java -cp ".:*:lucene-8.0.0/core/*:lucene-8.0.0/analysis/common/*:lucene-8.0.0/queryparser/*" Searcher
```

### When running searcher

- add *Indexer.java* and *stopwords.txt* in the classpath
- **Searcher** uses **Indexer**'s method *getAnalyzer()*, to be sure it uses the same analyzer,
so *Indexer.java* needs to be added in the classpath
- **Searcher** uses *stopwords.txt* as a parameter when building a *CustomAnalyzer*, so the file needs to be in the classpath


