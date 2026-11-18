package DSII.TurismoSustentable.dto;

import java.util.List;

/** Respuesta pública del endpoint administrativo: nunca contiene snapshots ni motivo libre. */
public final class AuditoriaDetalleDTO {
    public final AuditoriaResumenDTO evento;
    public final String referenciaHoraria = "NO_DECLARADA";
    public final boolean detalleDisponible;
    public final boolean motivoRegistrado;
    public final List<Cambio> cambios;

    public AuditoriaDetalleDTO(AuditoriaResumenDTO evento, boolean motivoRegistrado, List<Cambio> cambios) {
        this.evento = evento;
        this.motivoRegistrado = motivoRegistrado;
        this.cambios = List.copyOf(cambios);
        this.detalleDisponible = !this.cambios.isEmpty();
    }

    public static final class Cambio {
        public final String campo;
        // Valores permitidos: lista de códigos de rol, entero, booleano o null.
        public final Object antes;
        public final Object despues;
        // true: se informa que el campo cambió sin devolver sus valores.
        public final boolean valoresReservados;

        public Cambio(String campo, Object antes, Object despues, boolean valoresReservados) {
            this.campo = campo;
            this.antes = antes;
            this.despues = despues;
            this.valoresReservados = valoresReservados;
        }
    }
}
