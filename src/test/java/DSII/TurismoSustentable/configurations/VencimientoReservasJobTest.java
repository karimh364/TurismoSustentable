package DSII.TurismoSustentable.configurations;

import DSII.TurismoSustentable.repositories.AlquilerRepository;
import DSII.TurismoSustentable.services.AlquilerService;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.List;
import static org.mockito.Mockito.*;

class VencimientoReservasJobTest {
    @Test void procesaSinSesionHumanaYUnFalloNoAbandonaElLote() {
        AlquilerRepository repo = mock(AlquilerRepository.class);
        AlquilerService servicio = mock(AlquilerService.class);
        Clock reloj = Clock.fixed(Instant.parse("2026-11-16T09:00:00Z"), ZoneOffset.UTC);
        when(repo.buscarReservasVencidas(any(),any())).thenReturn(List.of(1,2,3));
        when(servicio.vencerReserva(2)).thenThrow(new IllegalStateException("Fallo aislado QA"));
        new VencimientoReservasJob(repo,servicio,reloj).procesar();
        verify(repo).buscarReservasVencidas(eq(LocalDateTime.of(2026,11,16,9,0)),any());
        verify(servicio).vencerReserva(1); verify(servicio).vencerReserva(2); verify(servicio).vencerReserva(3);
    }
}
