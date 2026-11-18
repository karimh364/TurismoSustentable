package DSII.TurismoSustentable.time;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Convención del circuito de alquiler: instantes UTC en Java, hora local en sus columnas DATETIME. */
public final class HoraRioGrande {
    public static final ZoneId ZONA = ZoneId.of("America/Argentina/Ushuaia");

    private HoraRioGrande() { }

    public static OffsetDateTime desdeUtc(LocalDateTime fechaUtc) {
        return fechaUtc == null ? null : fechaUtc.atOffset(ZoneOffset.UTC)
                .atZoneSameInstant(ZONA).toOffsetDateTime();
    }

    public static LocalDateTime paraBase(LocalDateTime fechaUtc) {
        return fechaUtc == null ? null : desdeUtc(fechaUtc).toLocalDateTime();
    }

    public static LocalDateTime desdeBase(LocalDateTime fechaLocal) {
        return fechaLocal == null ? null : fechaLocal.atZone(ZONA)
                .withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
}
