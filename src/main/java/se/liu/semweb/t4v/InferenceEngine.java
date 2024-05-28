package se.liu.semweb.t4v;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.ext.xerces.util.URI;
import org.apache.jena.ext.xerces.util.URI.MalformedURIException;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.exec.RowSet.Exception;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InferenceEngine {
    static private Logger logger = LoggerFactory.getLogger(InferenceEngine.class);

    /**
     * Returns a model with inference support from an input string. Imported
     * ontologies
     * are loaded automatically.
     * 
     * @param data
     * @return
     */
    static public Model load(String data) {
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        if (isURI(data)) {
            model.read(data);
        } else {

            InputStream is = new ByteArrayInputStream(data.getBytes());
            String[] syntaxes = new String[] { "TTL", "NT", "RDF/XML", "N3", "JSON-LD", "RDF/JSON" };
            for (String syntax : syntaxes) {
                try {
                    model.read(is, null, syntax);
                    logger.info("Parsed input as " + syntax);
                    break;
                } catch (Exception e) {
                    logger.info("Failed to parse input as " + syntax);
                }
            }
        }
        return model;
    }

    static public List<String> getInferredClasses(String data, String schema, String target) {
        Model model = load(data);
        Model schema_model = load(schema);
        model.add(schema_model);
        Resource resource = model.getResource(target);
        List<String> list = new ArrayList<>();
        model.listObjectsOfProperty(resource, RDF.type).forEach(v -> {
            if(v.isAnon()){
                return;
            }
            String key = v.toString();
            list.add(key);
        });
        return list;
    }

    static private boolean isURI(String s) {
        try {
            new URI(s);
            return true;
        } catch (MalformedURIException e) {
            return false;
        }
    }
}
