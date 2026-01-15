package com.openclassrooms.patientservice.domain;

import com.openclassrooms.patientservice.event.Event;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

import java.io.Serializable;
import java.util.Map;

import static com.openclassrooms.patientservice.utils.UserUtils.randomUUUID;
import static java.time.LocalTime.now;
import static org.springframework.messaging.MessageHeaders.ID;
import static org.springframework.messaging.MessageHeaders.TIMESTAMP;

/**
 * @author Kardigué MAGASSA
 * @version 1.0
 * @email magassakara@gmail.com
 * @since 2026-05-01
 */

@Builder
@Getter
@Setter
public class Notification implements Serializable {
    private Event payload;
    private Map<String, String> headers;

    public Notification(Event payload) {
        this(payload, Map.of(ID, randomUUUID.get(), TIMESTAMP, now().toString()));
    }

    public Notification(@NotNull Event payload, @NotNull Map<String, String> headers) {
        this.payload = payload;
        this.headers = headers;
    }

    public boolean equals(@Nullable Object other) {
        boolean var10000;
        if (this != other) {
            label28: {
                if (other instanceof Notification) {
                    var that = (Notification)other;
                    if (ObjectUtils.nullSafeEquals(this.payload, that.payload) && this.headers.equals(that.headers)) {
                        break label28;
                    }
                }
                var10000 = false;
                return var10000;
            }
        }
        var10000 = true;
        return var10000;
    }

    public int hashCode() {
        return ObjectUtils.nullSafeHash(this.payload, this.headers);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(this.getClass().getSimpleName());
        sb.append(" [payload=");
        Object var3 = this.payload;
        if (var3 instanceof byte[] bytes) {
            sb.append("byte[").append(bytes.length).append(']');
        } else {
            sb.append(this.payload);
        }
        sb.append(", headers=").append(this.headers).append(']');
        return sb.toString();
    }
}
