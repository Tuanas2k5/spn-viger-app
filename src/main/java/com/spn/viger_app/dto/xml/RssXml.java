package com.spn.viger_app.dto.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "rss")
@XmlAccessorType(XmlAccessType.FIELD)
public class RssXml {

    @XmlElement(name = "channel")
    private ChannelXml channel;

    public RssXml() {
    }

    public ChannelXml getChannel() {
        return channel;
    }

    public void setChannel(ChannelXml channel) {
        this.channel = channel;
    }
}
