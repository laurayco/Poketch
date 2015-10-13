package io.github.tylerelric.poketch.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public  class JSONUtilities {

    public static Object object_to_map(Object o) throws JSONException {
        if (o instanceof JSONArray ){
            List<Object> l = new ArrayList<>();
            JSONArray arr = ((JSONArray)o);
            for(int i=0;i<arr.length();i++) {
                l.add(object_to_map(arr.get(i)));
            }
            return l.toArray();
        } else if (o instanceof JSONObject) {
            Map<String,Object> mo = new HashMap<>();
            JSONObject dobj = (JSONObject) o;
            Iterator<String> k = dobj.keys();
            while(k.hasNext()) {
                String key = k.next();
                mo.put(key, object_to_map(dobj.get(key)));
            }
            return mo;
        }
        return o;
    }
}
