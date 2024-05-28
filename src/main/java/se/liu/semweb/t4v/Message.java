package se.liu.semweb.t4v;

public class Message {
    private String target;
    private String data;
    private String schema;

    public String getSchema() {
        return this.schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getTarget() {
        return target;
    }

    public String getData() {
        return data;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public void setData(String data) {
        this.data = data;
    }
}