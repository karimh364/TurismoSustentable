package DSII.TurismoSustentable.configurations;
import DSII.TurismoSustentable.repositories.BicicletaRepository;
import DSII.TurismoSustentable.services.MantenimientoService;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.stream.*;
import static org.mockito.Mockito.*;
class PreventivoMantenimientoJobTest {
    @Test void cursorNoSaltaUnidadesCuandoCambianDeEstadoYUnFalloNoAbandonaLote(){
        var repo=mock(BicicletaRepository.class);var servicio=mock(MantenimientoService.class);
        when(repo.buscarCandidatasPreventiva(eq(0),any())).thenReturn(IntStream.rangeClosed(1,100).boxed().collect(Collectors.toList()));
        when(repo.buscarCandidatasPreventiva(eq(100),any())).thenReturn(List.of(101,102));
        when(servicio.aplicarPreventiva(20)).thenThrow(new IllegalStateException("Fallo aislado QA"));
        new PreventivoMantenimientoJob(repo,servicio).procesar();
        verify(servicio).aplicarPreventiva(1);verify(servicio).aplicarPreventiva(100);verify(servicio).aplicarPreventiva(102);
        verify(repo,times(2)).buscarCandidatasPreventiva(anyInt(),argThat(p->p.getPageNumber()==0));
    }
}
