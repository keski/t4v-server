/**
 OWL2SHACL converts a set of classes in an OWL ontology into SHACL node and property shapes. Node shapes and property shapes are generated for any OWL class with at least one restriction in the ontology. Any restriction that applies to a class/property is applied to to its subclasses/subproperties using inference. The translation between OWL and SHACL is as follows:

- owl:Class -> sh:targetClass
- owl:onProperty -> sh:path
- owl:minCardinality -> sh:min
- owl:maxCardinality -> sh:max
- owl:cardinality -> sh:min and sh:max
- owl:qualifiedCardinality -> sh:min and sh:max
- owl:minQualifiedCardinality -> sh:min
- owl:maxQualifiedCardinality -> sh:max
- owl:hasValue -> sh:value

owl:someValuesFrom

- owl:onClass is mapped to a property shape with sh:class
- if owl:onClass is null the property's rdfs:range is mapped to to a property with shape sh:class
- (optional) owl:onClass can instead be mapped to a property shape with sh:in containing all named individuals of the classes // förtydliga oneOf!

- owl:onDataRange is mapped to a property shape sh:datatype
- if owl:onDataRange is null rdfs:range is mapped to a property shape with sh:datatype
- owl:someValuesFrom is mapped to a property shape with sh:min as value 1
- owl:unionOf is mapped mapped to a property shape with sh:or
- owl:intersectionOf is mapped to a using multiple property shapes // Dubbelkolla!
 */

// <some> is optional!

package se.liu.semweb.t4v.owl2shacl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.Restriction;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import se.liu.semweb.t4v.owl2shacl.utils.ShapeUtils;

public class OWL2SHACL {
    private static final Logger logger = Logger.getLogger(OWL2SHACL.class.getName());
    private static String shapeBase = "http://owl2shacl.liu.se/";

    public static void main(String[] args) {
        String url = "https://raw.githubusercontent.com/LiUSemWeb/T4V/dev/ontology/flatglass/0.2/flatglass.ttl";
        String s = owl2shacl(url);
        try {
            FileWriter fw = new FileWriter(new File("test.ttl"));
            fw.write(s);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static String owl2shacl(String url) {
        Model base = ModelFactory.createDefaultModel();
        base.read(url, "TTL");
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF, base);

        Model m = generate(model);

        StringWriter sq = new StringWriter();
        m.setNsPrefixes(base.getNsPrefixMap());
        m.setNsPrefix("sh", "http://www.w3.org/ns/shacl#");

        m = ShapeUtils.compressModel(m);
        m.add(base);
        ShapeUtils.addSHACLNames(m);
        ShapeUtils.orderModel(m);

        m.write(sq, "TTL");
        return sq.toString();
    }

    /**
     * Return a model containing the a set of SHACL shapes generated from an
     * ontology model.
     * 
     * @param model
     * @return
     */
    public static Model generate(OntModel model) {
        Model shapes = ModelFactory.createDefaultModel();

        Map<Resource, List<Restriction>> restrictionsMap = getRestrictionsMap(model);
        restrictionsMap.entrySet().forEach(entrySet -> {
            Resource key = entrySet.getKey();

            List<Restriction> restrictions = entrySet.getValue();

            Resource nodeShape = shapes.createResource(shapeBase + getShortName(key) + "Shape")
                    .addProperty(RDF.type, ShapeUtils.NodeShape)
                    .addProperty(ShapeUtils.targetClass, key);

            restrictions.forEach(restriction -> {
                Resource propShape = shapes.createResource()
                        .addProperty(ShapeUtils.path, restriction.getPropertyValue(OWL2.onProperty));
                nodeShape.addProperty(ShapeUtils.property, propShape);

                Literal max = getMaxCardinality(restriction);
                if (max != null) {
                    propShape.addLiteral(ShapeUtils.maxCount, max);
                }

                Literal minCount = getMinCardinality(restriction);
                if (minCount != null) {
                    propShape.addProperty(ShapeUtils.minCount, minCount);
                }

                RDFNode value = restriction.getPropertyValue(OWL2.hasValue);
                if (value != null) {
                    propShape.addProperty(ShapeUtils.hasValue, value);
                }

                // oneOf
                Set<Resource> possibleValues = getPossibleValues(restriction, false);
                if (possibleValues != null) {
                    propShape.addProperty(ShapeUtils.in,
                            shapes.createList(possibleValues.iterator()));
                }

                // onClass, someValuesFrom, domain
                List<RDFNode> possibleClasses = getPossibleClasses(restriction, model);
                if (possibleValues == null && possibleClasses != null) {
                    if (possibleClasses.size() == 1) {
                        propShape.addProperty(ShapeUtils.class_, possibleClasses.get(0));
                    } else {
                        List<Resource> list = new ArrayList<>();
                        possibleClasses.forEach(d -> {
                            list.add(shapes.createResource()
                                    .addProperty(ShapeUtils.class_, d));
                        });
                        propShape.addProperty(ShapeUtils.or, shapes.createList(list.iterator()));
                    }
                }

                List<RDFNode> possibleDatatypes = getPossibleDatatypes(restriction, model);
                if (possibleDatatypes != null) {
                    if (possibleDatatypes.size() == 1) {
                        propShape.addProperty(ShapeUtils.datatype, possibleDatatypes.get(0));
                    } else {
                        List<Resource> list = new ArrayList<>();
                        possibleDatatypes.forEach(d -> {
                            list.add(shapes.createResource()
                                    .addProperty(ShapeUtils.datatype, d));
                        });
                        propShape.addProperty(ShapeUtils.or, shapes.createList(list.iterator()));
                    }
                }
            });
        });

        restrictionsMap.entrySet().forEach(entrySet -> {
            Resource key = entrySet.getKey();
            Resource nodeShape = shapes.createResource(shapeBase + getShortName(key) + "Shape");

            model.listStatements(null, RDFS.domain, (RDFNode) null).forEach(stmt -> {
                Resource property = stmt.getSubject();
                Resource domain = stmt.getObject().asResource();

                // Skip if property is part of OWL, RDF or RDFS.
                String p = property.toString();
                if (p.startsWith(RDFS.uri) || p.startsWith(RDF.uri) || p.startsWith(OWL2.NS)) {
                    return;
                }

                // Skip if key not subclass of domain
                if (!model.contains(key, RDFS.subClassOf, domain)) {
                    return;
                }

                // Skip if already added to shape
                String queryString = String.format(
                        "PREFIX sh: <http://www.w3.org/ns/shacl#>\n" +
                        "ASK\n" +
                        "WHERE {\n" +
                        "   <%s> sh:property/sh:path <%s> .\n" +
                        "}",
                        nodeShape, property);

                if (executeSparqlAsk(queryString, shapes)) {
                    return;
                }

                Resource propShape = shapes.createResource()
                        .addProperty(ShapeUtils.path, property)
                        .addProperty(ShapeUtils.targetClass, key);

                model.listObjectsOfProperty(property, RDFS.range).forEach(range -> {
                    if (model.contains(range.asResource(), RDF.type, RDFS.Datatype)) {
                        propShape.addProperty(ShapeUtils.datatype, range);
                        nodeShape.addProperty(ShapeUtils.property, propShape);
                    } else {
                        if (model.contains(range.asResource(), RDF.type, RDFS.Class)) {
                            propShape.addProperty(ShapeUtils.class_, range);
                            nodeShape.addProperty(ShapeUtils.property, propShape);
                        }
                    }
                });

            });
        });

        return shapes;
    }

    /**
     * Return a map of restrictions with the target class as the key and the
     * restrictions that apply to it as as a list.
     * 
     * @param model
     * @return
     */
    public static Map<Resource, List<Restriction>> getRestrictionsMap(OntModel model) {
        final Map<Resource, List<Restriction>> restrictionMap = new HashMap<>();
        model.listRestrictions().forEach(restriction -> {
            model.listSubjectsWithProperty(RDFS.subClassOf, restriction)
                    .filterKeep(Resource::isURIResource)
                    .filterDrop(cls -> cls.equals(OWL2.Nothing))
                    .forEach(cls -> restrictionMap.computeIfAbsent(cls, k -> new ArrayList<>()).add(restriction));
        });
        return restrictionMap;
    }

    /**
     * Return a list of allowed classes for a restriction or null.
     * 
     * @param restriction
     * @return
     */
    public static List<RDFNode> getPossibleClasses(Restriction restriction, Model model) {
        Resource prop = restriction.getOnProperty();
        if (!prop.hasProperty(RDF.type, OWL2.ObjectProperty)) {
            return null;
        }

        // OWL2.someValuesFrom
        RDFNode value2 = restriction.getPropertyValue(OWL2.someValuesFrom);
        if (value2 != null && !value2.equals(OWL2.Thing)) {
            return unionOfOrValue(value2.asResource());
        }

        // OWL2.onClass
        RDFNode value = restriction.getPropertyValue(OWL2.onClass);
        if (value != null && !value.equals(OWL2.Thing)) {
            return unionOfOrValue(value.asResource());
        }

        // Class by range of property
        List<RDFNode> list = model.listObjectsOfProperty(prop, RDFS.range).toList();
        list.remove(OWL2.Thing);
        list.remove(RDFS.Resource);
        if (list.isEmpty()) {
            return null;
        } else if (list.size() > 1) {
            list.forEach(System.out::println);
            logger.warning("Intersection for range not yet supported: " + prop);
            return null;
        } else {
            return unionOfOrValue(list.get(0).asResource());
        }
    }

    /**
     * Return a list of allowed datatypes for a restriction or null.
     * 
     * @param restriction
     * @return
     */
    public static List<RDFNode> getPossibleDatatypes(Restriction restriction, Model model) {
        Resource prop = restriction.getOnProperty().asResource();
        if (!prop.hasProperty(RDF.type, OWL2.DatatypeProperty)) {
            return null;
        }
        // Qualified cardinality restriction defines range
        RDFNode value = restriction.getPropertyValue(OWL2.onDataRange);
        if (value != null && !value.equals(OWL2.Thing)) {
            return unionOfOrValue(value.asResource());
        }

        // Non-qualified cardinality restriction defines range
        RDFNode value2 = restriction.getPropertyValue(OWL2.someValuesFrom);
        if (value2 != null && !value2.equals(OWL2.Thing)) {
            return unionOfOrValue(value2.asResource());
        }

        // Data range of property
        List<RDFNode> list = model.listObjectsOfProperty(prop, RDFS.range).toList();
        list.remove(OWL2.Thing);
        list.remove(RDFS.Resource);
        if (list.isEmpty()) {
            return null;
        } else if (list.size() > 1) {
            list.forEach(System.out::println);
            logger.warning("Intersection for range not yet supported: " + prop);
            return null;
        } else {
            return unionOfOrValue(list.get(0).asResource());
        }

    }

    /**
     * Return a list representing the union values of a resource, or only the value
     * if the resource is not a union.
     * 
     * @param value
     * @return
     */
    public static List<RDFNode> unionOfOrValue(Resource value) {
        if (value.hasProperty(OWL2.unionOf)) {
            Resource list = value.getPropertyResourceValue(OWL2.unionOf);
            return value.getModel().getList(list).asJavaList();
        }
        return Arrays.asList(value);
    }

    /**
     * Return the possible values from a restriction.
     * 
     * @param restriction
     * @param traverseSuperClasses
     * @return
     */
    public static Set<Resource> getPossibleValues(Restriction restriction, boolean traverseSuperClasses) {
        Model model = restriction.getModel();
        Set<Resource> values = new HashSet<>();
        // if (restriction.hasProperty(OWL2.onClass)) { // qualified cardinality shape
        // Resource cls = restriction.getPropertyResourceValue(OWL2.onClass);
        // model.listObjectsOfProperty(cls,
        // OWL2.equivalentClass).forEach(equivalentClass -> {
        // model.listObjectsOfProperty((Resource) equivalentClass,
        // OWL2.oneOf).forEach(list -> {
        // model.getList((Resource) list).asJavaList().forEach(v ->
        // values.add((Resource) v));
        // });
        // });

        // // Include individuals of super classes?
        // if (traverseSuperClasses) {
        // model.listSubjectsWithProperty(RDFS.subClassOf, cls).forEach(superclass -> {
        // model.listSubjectsWithProperty(RDF.type, superclass).forEach(values::add);
        // });
        // }
        // } else
        if (restriction.hasProperty(OWL2.someValuesFrom)) { // non-qualified cardinality shape
            Resource someValuesFrom = restriction.getPropertyResourceValue(OWL2.someValuesFrom);
            model.listObjectsOfProperty(someValuesFrom, OWL2.equivalentClass).forEach(equivalentClass -> {
                model.listObjectsOfProperty((Resource) equivalentClass, OWL2.oneOf).forEach(list -> {
                    model.getList((Resource) list).asJavaList().forEach(v -> {
                        values.add((Resource) v);
                    });
                });
            });

            // // Include individuals of super classes?
            // if (traverseSuperClasses && values.isEmpty()) {
            // model.listSubjectsWithProperty(RDFS.subClassOf,
            // someValuesFrom).forEach(superclass -> {
            // model.listSubjectsWithProperty(RDF.type, superclass).forEach(values::add);
            // });
            // }
        }
        return values.isEmpty() ? null : values;
    }

    /**
     * Get the minimum value for max cardinality from the restriction.
     * 
     * @param restriction
     * @return
     */
    public static Literal getMaxCardinality(Restriction restriction) {
        Property[] cardinalityProps = new Property[] {
                OWL2.maxCardinality,
                OWL2.maxQualifiedCardinality,
                OWL2.cardinality,
                OWL2.qualifiedCardinality
        };
        int max = Integer.MAX_VALUE;
        for (Property prop : cardinalityProps) {
            if (restriction.hasProperty(prop)) {
                max = restriction.getPropertyValue(prop).asLiteral().getInt();
            }
        }
        return max != Integer.MAX_VALUE
                ? ResourceFactory.createTypedLiteral(String.valueOf(max), XSDDatatype.XSDinteger)
                : null;
    }

    /**
     * Get min cardinality value.
     * 
     * @param restriction
     * @return
     */
    public static Literal getMinCardinality(Restriction restriction) {
        Property[] cardinalityProps = new Property[] {
                OWL2.minCardinality,
                OWL2.minQualifiedCardinality,
                OWL2.cardinality,
                OWL2.qualifiedCardinality
        };
        int min = Integer.MIN_VALUE;
        for (Property prop : cardinalityProps) {
            if (restriction.hasProperty(prop)) {
                min = restriction.getPropertyValue(prop).asLiteral().getInt();
            }
        }

        return min != Integer.MIN_VALUE
                ? ResourceFactory.createTypedLiteral(String.valueOf(min), XSDDatatype.XSDinteger)
                : null;
    }

    /**
     * Return the abbreviated short name of a URL string.
     * 
     * @param name
     * @return
     */
    public static String getShortName(Resource resource) {
        return getShortName(resource.toString());
    }

    /**
     * Return the abbreviated short name of a URL string.
     * 
     * @param name
     * @return
     */
    public static String getShortName(String name) {
        String shortName = name.substring(name.lastIndexOf("/") + 1);
        return shortName.substring(shortName.lastIndexOf("#") + 1);
    }

    /**
     * Return the value defined by the restriction or null.
     * 
     * @param restriction
     * @return
     */
    public static RDFNode getValue(Restriction restriction) {
        return restriction.getPropertyValue(OWL2.hasValue);
    }

    /**
     * Execute SPARQL select query.
     * 
     * @param queryString
     * @param model
     * @return
     */
    public static ResultSet executeSparqlSelect(String queryString, Model model) {
        try (QueryExecution qexec = QueryExecutionFactory.create(queryString, model)) {
            ResultSet results = qexec.execSelect();
            return results;
        }
    }

    /**
     * Execute SPARQL ASK query.
     * 
     * @param queryString
     * @param model
     * @return
     */
    public static boolean executeSparqlAsk(String queryString, Model model) {
        try (QueryExecution qexec = QueryExecutionFactory.create(queryString, model)) {
            return qexec.execAsk();
        }
    }

}