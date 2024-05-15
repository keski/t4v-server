package se.liu.semweb.t4v.owl2shacl.utils;

import java.util.logging.Logger;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

public class PropertyConstraint {
    public Resource path = null;
    public Resource class_ = null;
    public Resource datatype = null;
    public RDFNode value = null;
    public Integer minCount = 0;
    public Integer maxCount = null;
    public RDFList in = null;
    public RDFList or = null;

    private static Logger logger = Logger.getLogger(PropertyConstraint.class.getName());

    public void add(Resource propertyShape) {
        setPath(propertyShape);
        setMinCount(propertyShape);
        setMaxCount(propertyShape);
        setClass(propertyShape);
        setDatatype(propertyShape);
        setValue(propertyShape);
        setIn(propertyShape);
        setOr(propertyShape);
    }

    /**
     * Set the value of the sh:path property.
     * 
     * @param propertyShape
     */
    public void setPath(Resource propertyShape) {
        Resource path = propertyShape.getPropertyResourceValue(ShapeUtils.path);
        if (this.path != null && !this.path.equals(this.path)) {
            logger.warning("Attempting to merge incompatible paths (sh:path): " + this.path + " <-> " + path);
        } else {
            this.path = path;
        }
    }

    /**
     * Attempt to the value of the sh:minCount property.
     * 
     * @param propertyShape
     */
    public void setMinCount(Resource propertyShape) {
        Statement stmt = propertyShape.getProperty(ShapeUtils.minCount);
        if (stmt != null) {
            int minCount = stmt.getObject().asLiteral().getInt();
            if (this.minCount == null || this.minCount < minCount) {
                this.minCount = minCount;
            }
        }
    }

    /**
     * Attempt to set the value of the sh:maxCount property.
     * 
     * @param propertyShape
     */
    public void setMaxCount(Resource propertyShape) {
        Statement stmt = propertyShape.getProperty(ShapeUtils.maxCount);
        if (stmt != null) {
            int maxCount = stmt.getObject().asLiteral().getInt();
            if (this.maxCount == null || this.maxCount > maxCount) {
                this.maxCount = maxCount;
            }
        }
    }

    /**
     * Attempt to set the value of the sh:datatype property.
     * 
     * @param propertyShape
     */
    public void setDatatype(Resource propertyShape) {
        Resource datatype = propertyShape.getPropertyResourceValue(ShapeUtils.datatype);
        if (datatype != null) {
            if (this.datatype != null) {
                if (!datatype.equals(this.datatype)) {
                    logger.warning(
                            "Incompatible datatypes (sh:datatype): " + datatype + " <-> " + this.datatype);
                }
            } else {
                this.datatype = datatype;
            }
        }
    }

    /**
     * Attempt to set the value of the sh:class property.
     * 
     * @param propertyShape
     */
    public void setClass(Resource propertyShape) {
        Resource class_ = propertyShape.getPropertyResourceValue(ShapeUtils.class_);
        if (class_ != null) {
            if (this.class_ != null) {
                if (this.class_.equals(class_) || class_.hasProperty(RDFS.subClassOf, this.class_)) {
                    this.class_ = class_;
                } else {
                    // logger.warning(
                    //         "Attempting to merge incompatible classes (sh:class): " + class_ + " <-> " + this.class_);
                }
            } else {
                this.class_ = class_;
            }
        }
    }

    /**
     * Attempt to set the value of the sh:value property.
     * 
     * @param propertyShape
     */
    public void setValue(Resource propertyShape) {
        Statement stmt = propertyShape.getProperty(ShapeUtils.hasValue);
        if (stmt != null) {
            RDFNode value = stmt.getObject();
            if (this.value != null && !this.value.equals(value)) {
                logger.warning("Attempting to merge incomaptible values (sh:value): " + value + " <-> " + this.value);
            } else {
                this.value = value;
            }
        }
    }

    /**
     * Attempt to set the value of the sh:in property.
     * 
     * @param propertyShape
     */
    public void setIn(Resource propertyShape) {
        Resource in = propertyShape.getPropertyResourceValue(ShapeUtils.in);
        if (in != null) {
            this.in = propertyShape.getModel().getList(in);
        }
    }

    /**
     * Attempt to set the value of the sh:or property.
     * 
     * @param propertyShape
     */
    public void setOr(Resource propertyShape) {
        Resource or = propertyShape.getPropertyResourceValue(ShapeUtils.or);
        if (or != null) {
            this.or = propertyShape.getModel().getList(or);
        }
    }

    /**
     * Return the a resource representing the merged property shape. The resource is
     * created using a unique default model.
     * 
     * @return
     */
    public Resource get() {
        Model m = ModelFactory.createDefaultModel();
        Resource propertyShape = m.createResource()
                .addProperty(RDF.type, ShapeUtils.PropertyShape)
                .addProperty(ShapeUtils.path, path);

        // if value is set, cardinalities are not relevant
        if (value == null) {
            propertyShape.addProperty(ShapeUtils.minCount, m.createTypedLiteral(minCount));
            if (maxCount != null) {
                propertyShape.addProperty(ShapeUtils.maxCount, m.createTypedLiteral(maxCount));
            }
        }

        // Constraints order: sh:value -> sh:in -> sh:class -> sh:datatype -> sh:or
        if (value != null) {
            propertyShape.addProperty(ShapeUtils.hasValue, value);
        } else if (in != null) {
            propertyShape.addProperty(ShapeUtils.in, m.createList(in.iterator()));
        } else if (class_ != null) {
            propertyShape.addProperty(ShapeUtils.class_, class_);
        } else if (datatype != null) {
            propertyShape.addProperty(ShapeUtils.datatype, datatype);
        } else if (or != null) {
            propertyShape.addProperty(ShapeUtils.or, m.createList(or.iterator()));
            or.iterator().forEach(item -> {
                if (item.isResource()) {
                    item.getModel().listStatements(item.asResource(), null, (RDFNode) null).forEach(m::add);
                }
            });
        }

        return propertyShape;
    }

}
