package com.spn.viger_app.dto.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class ChannelXml {

    @XmlElement(name = "title")
    private String title;

    @XmlElement(name = "link")
    private String link;

    @XmlElement(name = "description")
    private String description;

    @XmlElement(name = "item")
    private List<ItemXml> items = new ArrayList<>();

    public ChannelXml() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<ItemXml> getItems() {
        return items;
    }

    public void setItems(List<ItemXml> items) {
        this.items = items;
    }
}
