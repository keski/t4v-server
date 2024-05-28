package se.liu.semweb.t4v.owl2shacl.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.shacl.vocabulary.SHACL;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

public class ShapeUtils {
    // Property and Resource versions of SHACL vocab
    Object i = SHACL.NodeShape;
    public static Resource NodeShape = ResourceFactory.createResource(SHACL.NodeShape.getURI());
    public static Resource PropertyShape = ResourceFactory.createResource(SHACL.PropertyShape.getURI());
    public static Property minCount = ResourceFactory.createProperty(SHACL.minCount.getURI());
    public static Property maxCount = ResourceFactory.createProperty(SHACL.maxCount.getURI());
    public static Property in = ResourceFactory.createProperty(SHACL.in.getURI());
    public static Property targetClass = ResourceFactory.createProperty(SHACL.targetClass.getURI());
    public static Property class_ = ResourceFactory.createProperty(SHACL.class_.getURI());
    public static Property hasValue = ResourceFactory.createProperty(SHACL.hasValue.getURI());
    public static Property path = ResourceFactory.createProperty(SHACL.path.getURI());
    public static Property property = ResourceFactory.createProperty(SHACL.property.getURI());
    public static Property or = ResourceFactory.createProperty(SHACL.or.getURI());
    public static Property datatype = ResourceFactory.createProperty(SHACL.datatype.getURI());
    public static Property name = ResourceFactory.createProperty(SHACL.name.getURI());
    public static Property order = ResourceFactory.createProperty(SHACL.order.getURI());

    /**
     * Return a compressed version of a SHACL model.
     * 
     * @param model
     * @return
     */
    public static Model compressModel(Model model) {
        Model m = ModelFactory.createDefaultModel();
        model.listSubjectsWithProperty(RDF.type, ShapeUtils.NodeShape).forEach(nodeShape -> {
            m.add(ShapeUtils.compressShape(nodeShape));
        });
        m.setNsPrefixes(model.getNsPrefixMap());
        return m;
    }

    /**
     * Add sh:order to the fields all SHACL shapes.
     * 
     * @param model
     * @return
     */
    public static void orderModel(Model model) {
        model.listSubjectsWithProperty(RDF.type, ShapeUtils.NodeShape).forEach(nodeShape -> {
            List<Resource> props = new ArrayList<>();
            model.listObjectsOfProperty(nodeShape, ShapeUtils.property).forEach(p -> {
                props.add(p.asResource());
            });
            applyOrder(props);
        });

    }

    /**
     * Return a compressed version of a SHACL node shape, where property shapes
     * constraints defined over the same property are merged into a single property
     * shape.
     * 
     * @param nodeShape
     * @return
     */
    public static Model compressShape(Resource nodeShape) {
        // Create compressed model and add the basic shape with target class
        Model compressed = ModelFactory.createDefaultModel();
        compressed.add(nodeShape, RDF.type, ShapeUtils.NodeShape);
        compressed.add(nodeShape, ShapeUtils.targetClass, nodeShape.getPropertyResourceValue(ShapeUtils.targetClass));

        // Iterate the constraints
        Map<String, PropertyConstraint> propertyShapeMap = new HashMap<>();
        nodeShape.listProperties(ShapeUtils.property).forEach(stmt -> {
            // Get the path
            String path = stmt.getObject().asResource().getPropertyResourceValue(ShapeUtils.path).toString();
            if (!propertyShapeMap.containsKey(path)) {
                propertyShapeMap.put(path, new PropertyConstraint());
            }
            // Add the constraint to the property constraint class (this will "flatten" the
            // constraint)
            PropertyConstraint pc = propertyShapeMap.get(path);
            pc.add(stmt.getObject().asResource());
        });
        // Each property shape is associated with its own model.
        // Add the constraints and corresponding model to the compressed shape.
        propertyShapeMap.values().forEach(propertyShapeConstraint -> {
            Resource propertyShape = propertyShapeConstraint.get();
            compressed.add(nodeShape, ShapeUtils.property, propertyShape);
            compressed.add(propertyShape.getModel());
        });
        return compressed;
    }

    /**
     * Generate sort keys for all classes and properties in a model. No inferencing
     * should apply to the model (i.e., only explicit sub-class and sup-property
     * relations should be used when generating the keys).
     * 
     * @param model
     * @return
     */
    public static Map<Resource, String> generateSortKeyMap(Model model) {
        Map<Resource, String> sortKeyMap = new HashMap<>();

        // class keys
        model.listResourcesWithProperty(RDF.type, OWL2.Class).forEach(cls -> {
            String key = cls.toString();
            Resource superCls = cls;
            while (superCls != null) {
                superCls = superCls.getPropertyResourceValue(RDFS.subClassOf);
                if (superCls != null && !superCls.isAnon()) {
                    key = superCls + " -> " + key;
                }
            }
            sortKeyMap.put(cls, key);
        });

        // object property keys
        model.listResourcesWithProperty(RDF.type, OWL2.ObjectProperty).forEach(prop -> {
            String key = prop.toString();
            Resource superProp = prop;
            while (superProp != null) {
                superProp = superProp.getPropertyResourceValue(RDFS.subPropertyOf);
                if (superProp != null) {
                    key = superProp + " -> " + key;
                }
            }
            sortKeyMap.put(prop, key);
        });

        // data property keys
        model.listResourcesWithProperty(RDF.type, OWL2.DatatypeProperty).forEach(prop -> {
            String key = prop.toString();
            Resource superProp = prop;
            while (superProp != null) {
                superProp = superProp.getPropertyResourceValue(RDFS.subPropertyOf);
                if (superProp != null) {
                    key = superProp + " -> " + key;
                }
            }
            sortKeyMap.put(prop, key);
        });
        return sortKeyMap;
    }

    /**
     * Add names based on labels to property shapes.
     *
     * @param model
     */
    public static void addSHACLNames(Model model) {
        model.listStatements(null, ShapeUtils.path, (RDFNode) null).forEach(t1 -> {
            Resource s = t1.getSubject();
            Resource o = t1.getObject().asResource();
            model.listObjectsOfProperty(o, RDFS.label).forEach(label -> {
                model.add(s, ShapeUtils.name, label);
            });
        });
    }

    private static void applyOrder(List<Resource> props) {
        Collections.sort(props, new Comparator<Resource>() {
            @Override
            public int compare(Resource prop1, Resource prop2) {
                // Check required
                Statement s1 = prop1.getProperty(ShapeUtils.minCount);
                int maxCount1 = s1 != null ? s1.getObject().asLiteral().getInt() : 0;
                Statement s2 = prop2.getProperty(ShapeUtils.minCount);
                int maxCount2 = s2 != null ? s2.getObject().asLiteral().getInt() : 0;
                if(maxCount1 == 0 && maxCount2 == 1){
                    return 1;
                } else if(maxCount1 == 1 && maxCount2 == 0){
                    return -1;
                }

                RDFNode p1 = prop1.getProperty(ShapeUtils.path).getObject();
                RDFNode p2 = prop2.getProperty(ShapeUtils.path).getObject();

                return p1.toString().compareTo(p2.toString());
            };
        });
        int order = 1;
        for(Resource p: props){
            p.addProperty(ShapeUtils.order, ResourceFactory.createTypedLiteral(order));
            order++;
        }
    }
}

