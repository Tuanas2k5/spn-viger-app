package com.spn.viger_app.dto.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class PostMetaXml {

    @XmlElement(name = "meta_key", namespace = "http://wordpress.org/export/1.2/")
    private String key;

    @XmlElement(name = "meta_value", namespace = "http://wordpress.org/export/1.2/")
    private String value;

    public PostMetaXml() {
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
