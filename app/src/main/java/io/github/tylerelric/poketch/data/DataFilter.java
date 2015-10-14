package io.github.tylerelric.poketch.data;

import java.util.List;
import java.util.Map;

public class DataFilter {

    String field;
    Map<String,Object> model_data;

    public DataFilter(Map<String,Object> species,String field) {
        this.model_data = species;
        this.field = field;
    }

    public boolean match(Map<String,Object> doc) {
        if(field==null||model_data==null) {
            return true;
        }
        Object field_val = model_data.get(field);
        String rsrc = doc.get("resource_uri").toString();
        if(field_val instanceof List) {
            for(Object o:(List)field_val){
                Map<String,Object> o_ = (Map<String, Object>) o;
                if(o_!=null) {
                    String o_rsrc = ((Map)o).get("resource_uri").toString();
                    if(o_rsrc.equals(rsrc)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean effective() {
        return !(field==null||model_data==null);
    }
}