package vdt.se.demo.adapter.out.elasticsearch;

import org.springframework.stereotype.Component;
import vdt.se.demo.domain.model.SocEvent;

@Component
public class SocEventDocumentMapper {

    public SocEventDocument toDocument(SocEvent event) {
        SocEventDocument document = new SocEventDocument();
        document.setId(event.id());
        document.setTimestamp(event.timestamp());
        document.setSource(event.source());
        document.setSeverity(event.severity());
        document.setEventType(event.eventType());
        document.setUser(event.user());
        document.setHost(event.host());
        document.setIp(event.ip());
        document.setSrcIp(event.srcIp());
        document.setDstIp(event.dstIp());
        document.setAction(event.action());
        document.setMessage(event.message());
        document.setRaw(event.raw());
        document.setMetadata(event.metadata());
        return document;
    }
}
