
package com.etsisi.weathercompare.models;

import java.util.HashMap;
import java.util.Map;

public class Rain {

    private Double _1h;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Double get1h() {
        return _1h;
    }

    public void set1h(Double _1h) {
        this._1h = _1h;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
