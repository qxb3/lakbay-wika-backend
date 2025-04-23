package org.lakbaywika.capstone.phrases;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class PhrasesLoader {
    public static HashMap<String, HashMap<String, String>> load() throws ParserConfigurationException, IOException, SAXException {
        HashMap phrasesMap = new HashMap<String, ArrayList<Translation>>();

        InputStream is = PhrasesLoader.class.getClassLoader().getResourceAsStream("phrases.xml");
        DocumentBuilder builderFac = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = builderFac.parse(is);

        document.getDocumentElement().normalize();

        NodeList phrases = document.getElementsByTagName("phrase");

        for (int i = 0; i < phrases.getLength(); i++) {
            Element phraseElement = (Element) phrases.item(i);
            String english = phraseElement.getAttribute("english");

            NodeList translations = phraseElement.getElementsByTagName("translation");
            HashMap<String, String> translationsList = new HashMap();

            for (int j = 0; j < translations.getLength(); j++) {
                Element translationElement = (Element) translations.item(j);
                String translationLanguage = translationElement.getAttribute("lang");
                String translationText = translationElement.getTextContent();

                translationsList.put(translationLanguage, translationText);
            }

            phrasesMap.put(english, translationsList);
        }

        return phrasesMap;
    }

    public static HashMap<String, String> translate(HashMap<String, HashMap<String, String>> phrasesMap, String text) throws IOException, ParseException {
        Directory dir = new ByteBuffersDirectory();
        Analyzer analyzer = new KeywordAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter writer = new IndexWriter(dir, config);

        Object[] keys = phrasesMap.keySet().toArray();
        for (Object key : keys) {
            org.apache.lucene.document.Document doc = new org.apache.lucene.document.Document();
            doc.add(new TextField("phrase", key.toString(), Field.Store.YES));
            writer.addDocument(doc);
        }

        writer.close();

        IndexReader reader = DirectoryReader.open(dir);
        IndexSearcher searcher = new IndexSearcher(reader);

        FuzzyQuery query = new FuzzyQuery(new Term("phrase", text));

        TopDocs results = searcher.search(query, 5);
        System.out.println(results.totalHits);
        ScoreDoc topResult = results.scoreDocs[0];

        org.apache.lucene.document.Document doc = searcher.storedFields().document(topResult.doc);
        String phrase = doc.get("phrase");

        return phrasesMap.get(phrase);
    }
}
