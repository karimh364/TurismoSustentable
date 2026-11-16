package DSII.TurismoSustentable.services;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
class CalendarioPreventivoTest {
    @Test void mesesCalendarioAjustanFinDeMesYLimite(){
        LocalDate alta=LocalDate.of(2024,1,31);
        assertFalse(CalendarioPreventivo.vencida(alta,null,1,LocalDateTime.of(2024,2,28,23,59)));
        assertTrue(CalendarioPreventivo.vencida(alta,null,1,LocalDateTime.of(2024,2,29,0,0)));
    }
    @Test void ultimaPreventivaAptaSustituyeAltaSinInventarOtra(){
        assertFalse(CalendarioPreventivo.vencida(LocalDate.of(2020,1,1),LocalDateTime.of(2026,11,1,0,0),3,LocalDateTime.of(2026,11,16,0,0)));
        assertTrue(CalendarioPreventivo.vencida(null,null,3,LocalDateTime.of(2026,11,16,0,0)));
    }
}
