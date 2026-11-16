package DSII.TurismoSustentable.services;
import DSII.TurismoSustentable.exceptions.ConfiguracionSistemaException;
import java.time.*;

// Misma regla para reservar, devolver, liberar, reactivar y revisar mantenimiento.
public final class CalendarioPreventivo {
    private CalendarioPreventivo() {}
    public static boolean vencida(LocalDate alta, LocalDateTime ultimaApta, Integer meses, LocalDateTime ahora) {
        if (meses == null || meses <= 0) throw new ConfiguracionSistemaException("Falta un intervalo preventivo válido para el tipo de bicicleta.");
        if (ultimaApta != null && ultimaApta.isAfter(ahora)) throw new ConfiguracionSistemaException("La última preventiva no puede estar en el futuro.");
        LocalDate referencia = ultimaApta == null ? alta : ultimaApta.toLocalDate();
        if (referencia == null) return true;
        if (referencia.isAfter(ahora.toLocalDate())) throw new ConfiguracionSistemaException("La referencia preventiva no puede estar en el futuro.");
        try { return !ahora.toLocalDate().isBefore(referencia.plusMonths(meses)); }
        catch (DateTimeException ex) { throw new ConfiguracionSistemaException("El intervalo preventivo excede las fechas admitidas."); }
    }
}
