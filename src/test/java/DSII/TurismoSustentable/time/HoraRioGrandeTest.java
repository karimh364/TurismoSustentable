package DSII.TurismoSustentable.time;

import DSII.TurismoSustentable.dto.HistorialEstadoAlquilerDTO;
import DSII.TurismoSustentable.models.Alquiler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import javax.persistence.Entity;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import static org.junit.jupiter.api.Assertions.*;

class HoraRioGrandeTest {
    private final FechaAlquilerLocalConverter converter = new FechaAlquilerLocalConverter();

    @Test void transformaSoloEnLaFronteraDePersistenciaInclusoAlCruzarMedianoche() {
        LocalDateTime utc = LocalDateTime.parse("2026-11-18T01:30:45.123456");
        LocalDateTime local = LocalDateTime.parse("2026-11-17T22:30:45.123456");
        assertEquals(local, converter.convertToDatabaseColumn(utc).toLocalDateTime());
        assertEquals(utc, converter.convertToEntityAttribute(Timestamp.valueOf(local)));
        assertEquals(utc.toInstant(ZoneOffset.UTC), HoraRioGrande.desdeUtc(utc).toInstant());
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test void hibernateReconoceElTipoJdbcDeLaFechaMigradaSinConectarseALaBd() {
        var registro = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", "org.hibernate.dialect.MariaDB103Dialect")
                .applySetting("hibernate.temp.use_jdbc_metadata_defaults", "false")
                .build();
        try {
            var scanner = new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
            var fuentes = new MetadataSources(registro);
            scanner.findCandidateComponents(Alquiler.class.getPackageName())
                    .forEach(bean -> fuentes.addAnnotatedClassName(bean.getBeanClassName()));
            var metadata = fuentes.buildMetadata();
            var modelo = metadata.getEntityBinding(Alquiler.class.getName());
            assertArrayEquals(new int[]{Types.TIMESTAMP},
                    modelo.getProperty("fechaHoraCreacion").getType().sqlTypes(metadata));
        } finally {
            StandardServiceRegistryBuilder.destroy(registro);
        }
    }

    @Test void apiIndicaElDesfaseDeLaHoraQueEntrega() throws Exception {
        var evento = new HistorialEstadoAlquilerDTO(1, 2, null, "RESERVADO", 3,
                "cliente.demo", "USUARIO", null, LocalDateTime.parse("2026-11-18T15:50:43.941144"));
        var mapper = new ObjectMapper();
        assertEquals("2026-11-18T12:50:43.941144-03:00", mapper.readTree(mapper.writeValueAsString(evento))
                .path("fechaHoraCambio").asText());
    }
}
