package DSII.TurismoSustentable.time;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FechaAlquilerLocalSerializer extends JsonSerializer<LocalDateTime> {
    @Override
    public void serialize(LocalDateTime fechaUtc, JsonGenerator json, SerializerProvider provider) throws IOException {
        json.writeString(HoraRioGrande.desdeUtc(fechaUtc).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }
}
