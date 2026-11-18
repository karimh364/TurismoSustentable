package DSII.TurismoSustentable.time;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/** Se aplica explícitamente al circuito de alquiler, no al resto de los LocalDateTime del modelo. */
@Converter(autoApply = false)
public class FechaAlquilerLocalConverter implements AttributeConverter<LocalDateTime, Timestamp> {
    @Override public Timestamp convertToDatabaseColumn(LocalDateTime fechaUtc) {
        // Hibernate 5.6 reconoce Timestamp como tipo JDBC; el DATETIME sigue siendo local.
        return fechaUtc == null ? null : Timestamp.valueOf(HoraRioGrande.paraBase(fechaUtc));
    }

    @Override public LocalDateTime convertToEntityAttribute(Timestamp fechaLocal) {
        return fechaLocal == null ? null : HoraRioGrande.desdeBase(fechaLocal.toLocalDateTime());
    }
}
