package com.lexho.user_service.geo.events;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class GeoRoutingPartitionService {
    private final int partitionCount;
    private final boolean partitionEnabled;

    public GeoRoutingPartitionService(@Value("${app.geo.events.rabbit.partition.enabled:false}") boolean partitionEnabled,
                                      @Value("${app.geo.events.rabbit.partition.count:8}") int partitionCount) {
        this.partitionEnabled = partitionEnabled;
        this.partitionCount = Math.max(1, Math.min(256, partitionCount));
    }

    public String routingKey(String baseRoutingKey, GeoEvent event) {
        if (!partitionEnabled) {
            return baseRoutingKey;
        }
        int partition = partition(event);
        return baseRoutingKey + ".p" + partition;
    }

    private int partition(GeoEvent event) {
        Map<String, Object> payload = event.payload();
        Object lat = payload == null ? null : payload.get("lat");
        Object lng = payload == null ? null : payload.get("lng");
        if (lat instanceof Number latNum && lng instanceof Number lngNum) {
            long v = Math.round(latNum.doubleValue() * 1000.0d) * 31L
                    + Math.round(lngNum.doubleValue() * 1000.0d);
            return (int) Math.floorMod(v, partitionCount);
        }
        Long actorId = event.actorId();
        if (actorId != null) {
            return (int) Math.floorMod(actorId, partitionCount);
        }
        return 0;
    }
}

