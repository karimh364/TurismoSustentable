package DSII.TurismoSustentable.controllers;

import DSII.TurismoSustentable.configurations.SecurityConfig;
import DSII.TurismoSustentable.configurations.WebAuthorization;
import DSII.TurismoSustentable.dto.AuditoriaAdministradorDTO;
import DSII.TurismoSustentable.dto.AuditoriaResumenDTO;
import DSII.TurismoSustentable.exceptions.GlobalExceptionHandler;
import DSII.TurismoSustentable.repositories.AuditoriaSistemaRepository;
import DSII.TurismoSustentable.repositories.AuditoriaSistemaRepository.DetalleRegistrado;
import DSII.TurismoSustentable.security.SesionVigenteMockConfig;
import DSII.TurismoSustentable.services.ConsultaAuditoriaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuditoriaConsultaController.class)
@ContextConfiguration(classes = {AuditoriaConsultaController.class, WebAuthorization.class,
        SecurityConfig.class, GlobalExceptionHandler.class})
@Import({SesionVigenteMockConfig.class, ConsultaAuditoriaService.class})
class AuditoriaConsultaControllerTest {
    private static final String RUTA = "/api/admin/auditoria";

    @Autowired private MockMvc http;
    @Autowired private ConsultaAuditoriaService servicio;
    @MockBean private AuditoriaSistemaRepository repositorio;
    @MockBean private UserDetailsService credenciales;

    @Test
    void anonimoRecibe401SinConsultarElRepositorio() throws Exception {
        http.perform(get(RUTA)).andExpect(status().isUnauthorized());
        verifyNoInteractions(repositorio);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CLIENTE", "TECNICO"})
    void rolesNoAdministrativosReciben403(String rol) throws Exception {
        http.perform(get(RUTA).with(user("qa").authorities(new SimpleGrantedAuthority(rol))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(repositorio);
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void elServicioTambienProtegeUnaLlamadaDirecta() {
        assertThrows(AccessDeniedException.class, () -> servicio.listar(null, null, null, 0, 20));
        assertThrows(AccessDeniedException.class, servicio::administradores);
        verifyNoInteractions(repositorio);
    }

    @Test
    @WithMockUser(authorities = {"ADMIN", "CLIENTE"})
    void filtrosNormalizadosYPaginaDevuelvenSoloMetadatos() throws Exception {
        PageRequest pagina = PageRequest.of(1, 3);
        AuditoriaResumenDTO evento = new AuditoriaResumenDTO(35, 7, "admin.demo", "USUARIO", 4,
                "CAMBIAR_ROLES", LocalDateTime.parse("2026-11-18T13:50:43.941144"), "cliente.demo");
        when(repositorio.consultarResumen("USUARIO", "CAMBIAR_ROLES", 7, pagina))
                .thenReturn(new PageImpl<>(List.of(evento), pagina, 10));

        http.perform(get(RUTA).param("entidad", " usuario ").param("accion", "cambiar_roles")
                        .param("administradorId", "7").param("pagina", "1").param("tamano", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.tamano").value(3))
                .andExpect(jsonPath("$.totalEventos").value(10))
                .andExpect(jsonPath("$.totalPaginas").value(4))
                .andExpect(jsonPath("$.referenciaHoraria").value("NO_DECLARADA"))
                .andExpect(jsonPath("$.eventos[0].administradorId").value(7))
                .andExpect(jsonPath("$.eventos[0].administradorNickname").value("admin.demo"))
                .andExpect(jsonPath("$.eventos[0].entidadId").value(4))
                .andExpect(jsonPath("$.eventos[0].referenciaEntidad").value("cliente.demo"))
                .andExpect(jsonPath("$.eventos[0].accion").value("CAMBIAR_ROLES"))
                .andExpect(jsonPath("$.eventos[0].fechaRegistrada").value("2026-11-18T13:50:43.941144"))
                .andExpect(jsonPath("$.eventos[0].administrador").doesNotExist())
                .andExpect(jsonPath("$.eventos[0].email").doesNotExist())
                .andExpect(jsonPath("$.eventos[0].motivo").doesNotExist())
                .andExpect(jsonPath("$.eventos[0].valoresAnteriores").doesNotExist())
                .andExpect(jsonPath("$.eventos[0].valoresNuevos").doesNotExist());
        verify(repositorio).consultarResumen("USUARIO", "CAMBIAR_ROLES", 7, pagina);
        verifyNoMoreInteractions(repositorio);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void vacioConservaLosValoresPorDefecto() throws Exception {
        PageRequest pagina = PageRequest.of(0, 20);
        when(repositorio.consultarResumen(null, null, null, pagina)).thenReturn(Page.empty(pagina));

        http.perform(get(RUTA).param("entidad", "   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventos").isEmpty())
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamano").value(20))
                .andExpect(jsonPath("$.totalEventos").value(0));
        verify(repositorio).consultarResumen(null, null, null, pagina);
    }

    @ParameterizedTest
    @CsvSource({"pagina,-1", "pagina,10001", "tamano,0", "tamano,101", "tamano,abc",
            "entidad,USUARIO/ESTACION", "accion,CAMBIAR ROLES", "administradorId,-1", "administradorId,0",
            "administradorId,admin.demo", "administradorId,2147483648"})
    @WithMockUser(authorities = "ADMIN")
    void parametrosInvalidosDan400SinConsultar(String parametro, String valor) throws Exception {
        http.perform(get(RUTA).param(parametro, valor)).andExpect(status().isBadRequest());
        verifyNoInteractions(repositorio);
    }

    @Test
    void listaDeAdministradoresRequiereSesion() throws Exception {
        http.perform(get(RUTA + "/administradores")).andExpect(status().isUnauthorized());
        verifyNoInteractions(repositorio);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CLIENTE", "TECNICO"})
    void listaDeAdministradoresRequiereRolAdmin(String rol) throws Exception {
        http.perform(get(RUTA + "/administradores").with(user("qa").authorities(new SimpleGrantedAuthority(rol))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(repositorio);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void listaDeAdministradoresDevuelveSoloIdsYNicknamesDeActoresRegistrados() throws Exception {
        when(repositorio.consultarAdministradores()).thenReturn(List.of(
                new AuditoriaAdministradorDTO(3, "admin.demo"),
                new AuditoriaAdministradorDTO(8, "qa.roles")));

        http.perform(get(RUTA + "/administradores"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{"administradorId":3,"administradorNickname":"admin.demo"},
                         {"administradorId":8,"administradorNickname":"qa.roles"}]
                        """, true));
        verify(repositorio).consultarAdministradores();
        verifyNoMoreInteractions(repositorio);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void noHayEscriturasParaLaRutaDeAuditoria() throws Exception {
        http.perform(post(RUTA)).andExpect(status().isMethodNotAllowed());
        http.perform(put(RUTA)).andExpect(status().isMethodNotAllowed());
        http.perform(delete(RUTA)).andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(repositorio);
    }

    @Test
    void detalleAnonimoRecibe401SinConsultarElRepositorio() throws Exception {
        http.perform(get(RUTA + "/35")).andExpect(status().isUnauthorized());
        verifyNoInteractions(repositorio);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CLIENTE", "TECNICO"})
    void detalleRechazaRolesNoAdministrativos(String rol) throws Exception {
        http.perform(get(RUTA + "/35").with(user("qa").authorities(new SimpleGrantedAuthority(rol))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(repositorio);
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void detalleProtegeUnaLlamadaDirectaAlServicio() {
        assertThrows(AccessDeniedException.class, () -> servicio.detalle(35));
        verifyNoInteractions(repositorio);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc", "2147483648"})
    @WithMockUser(authorities = "ADMIN")
    void detalleInvalidoDa400SinConsultar(String id) throws Exception {
        http.perform(get(RUTA + "/{id}", id)).andExpect(status().isBadRequest());
        verifyNoInteractions(repositorio);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void detalleInexistenteDa404() throws Exception {
        when(repositorio.consultarDetalle(35)).thenReturn(Optional.empty());
        http.perform(get(RUTA + "/35")).andExpect(status().isNotFound());
        verify(repositorio).consultarDetalle(35);
    }

    @Test
    @WithMockUser(authorities = {"CLIENTE", "ADMIN"})
    void detalleExponeRolesHistoricosPeroNoSnapshotsNiDatosExtranos() throws Exception {
        detalleGuardado("USUARIO", "CAMBIAR_ROLES",
                """
                {"roles":["CLIENTE"],"passwordHash":"hash-de-prueba-no-publicar"}
                """, """
                {"roles":["TECNICO","CLIENTE"],"token":"token-de-prueba-no-publicar"}
                """, true, "qa.roles");

        http.perform(get(RUTA + "/35"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evento.id").value(35))
                .andExpect(jsonPath("$.evento.administradorId").value(3))
                .andExpect(jsonPath("$.evento.administradorNickname").value("admin.demo"))
                .andExpect(jsonPath("$.evento.entidadId").value(8))
                .andExpect(jsonPath("$.evento.referenciaEntidad").value("qa.roles"))
                .andExpect(jsonPath("$.evento.fechaRegistrada").value("2026-11-18T08:43:58.9322"))
                .andExpect(jsonPath("$.referenciaHoraria").value("NO_DECLARADA"))
                .andExpect(jsonPath("$.motivoRegistrado").value(true))
                .andExpect(jsonPath("$.detalleDisponible").value(true))
                .andExpect(jsonPath("$.cambios.length()").value(1))
                .andExpect(jsonPath("$.cambios[0].campo").value("roles"))
                .andExpect(jsonPath("$.cambios[0].antes[0]").value("CLIENTE"))
                .andExpect(jsonPath("$.cambios[0].despues[0]").value("CLIENTE"))
                .andExpect(jsonPath("$.cambios[0].despues[1]").value("TECNICO"))
                .andExpect(jsonPath("$.cambios[0].valoresReservados").value(false))
                .andExpect(jsonPath("$.motivo").doesNotExist())
                .andExpect(content().string(not(containsString("valoresAnteriores"))))
                .andExpect(content().string(not(containsString("valoresNuevos"))))
                .andExpect(content().string(not(containsString("no-publicar"))));
        verify(repositorio).consultarDetalle(35);
        verifyNoMoreInteractions(repositorio);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void detalleDeEdicionPersonalSoloInformaElCampoModificado() throws Exception {
        String antes = """
                {"email":"anterior@example.test","nombre":"QA","apellido":null,
                 "fechaNacimiento":null,"generoId":null}
                """;
        detalleGuardado("USUARIO", "ACTUALIZAR", antes,
                antes.replace("anterior@example.test", "nuevo@example.test"), false, null);

        http.perform(get(RUTA + "/35"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detalleDisponible").value(true))
                .andExpect(jsonPath("$.cambios.length()").value(1))
                .andExpect(jsonPath("$.cambios[0].campo").value("email"))
                .andExpect(jsonPath("$.cambios[0].valoresReservados").value(true))
                .andExpect(jsonPath("$.cambios[0].antes").doesNotExist())
                .andExpect(jsonPath("$.cambios[0].despues").doesNotExist())
                .andExpect(content().string(not(containsString("example.test"))));
    }

    @ParameterizedTest
    @CsvSource({"BICICLETA,ACTUALIZAR", "USUARIO,ACCION_NUEVA", "USUARIO,CAMBIAR_ROLES"})
    @WithMockUser(authorities = "ADMIN")
    void formatoNoInterpretadoDa200SoloConMetadatos(String entidad, String accion) throws Exception {
        detalleGuardado(entidad, accion, "contenido-no-publicar", "otro-contenido-no-publicar", false, null);

        http.perform(get(RUTA + "/35"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evento.id").value(35))
                .andExpect(jsonPath("$.evento.entidadAfectada").value(entidad))
                .andExpect(jsonPath("$.evento.accion").value(accion))
                .andExpect(jsonPath("$.evento.referenciaEntidad").doesNotExist())
                .andExpect(jsonPath("$.detalleDisponible").value(false))
                .andExpect(jsonPath("$.motivoRegistrado").value(false))
                .andExpect(jsonPath("$.cambios").isEmpty())
                .andExpect(content().string(not(containsString("no-publicar"))));
    }

    @ParameterizedTest
    @CsvSource({"BICICLETA,BM-10", "ESTACION,Plaza Circuito QA"})
    @WithMockUser(authorities = "ADMIN")
    void referenciaActualNoDependeDeQueHayaCambiosInterpretables(String entidad, String referencia) throws Exception {
        detalleGuardado(entidad, "ACTUALIZAR", null, null, false, referencia);

        http.perform(get(RUTA + "/35"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evento.entidadAfectada").value(entidad))
                .andExpect(jsonPath("$.evento.referenciaEntidad").value(referencia))
                .andExpect(jsonPath("$.detalleDisponible").value(false));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void detalleNoPermiteEscrituras() throws Exception {
        http.perform(post(RUTA + "/35")).andExpect(status().isMethodNotAllowed());
        http.perform(put(RUTA + "/35")).andExpect(status().isMethodNotAllowed());
        http.perform(delete(RUTA + "/35")).andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(repositorio);
    }

    private void detalleGuardado(String entidad, String accion, String antes, String despues,
                                 boolean motivoRegistrado, String referenciaEntidad) {
        DetalleRegistrado registro = mock(DetalleRegistrado.class);
        when(registro.getId()).thenReturn(35);
        when(registro.getAdministradorId()).thenReturn(3);
        when(registro.getAdministradorNickname()).thenReturn("admin.demo");
        when(registro.getEntidadId()).thenReturn(8);
        when(registro.getReferenciaEntidad()).thenReturn(referenciaEntidad);
        when(registro.getFechaOperacion()).thenReturn(LocalDateTime.parse("2026-11-18T08:43:58.9322"));
        when(registro.getEntidadAfectada()).thenReturn(entidad);
        when(registro.getAccion()).thenReturn(accion);
        when(registro.getValoresAnteriores()).thenReturn(antes);
        when(registro.getValoresNuevos()).thenReturn(despues);
        when(registro.getMotivoRegistrado()).thenReturn(motivoRegistrado);
        when(repositorio.consultarDetalle(35)).thenReturn(Optional.of(registro));
    }
}
