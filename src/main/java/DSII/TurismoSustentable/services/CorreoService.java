package DSII.TurismoSustentable.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class CorreoService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(CorreoService.class);

    private static final String MAILJET_URL =
            "https://api.mailjet.com/v3.1/send";

    private final RestTemplate restTemplate =
            new RestTemplate();

    @Value("${mailjet.api-key:}")
    private String apiKey;

    @Value("${mailjet.secret-key:}")
    private String secretKey;

    @Value("${mailjet.sender-email:}")
    private String senderEmail;

    @Value("${mailjet.sender-name:Turismo Sustentable}")
    private String senderName;

    public boolean enviarCorreo(
            String destinatario,
            String nombreDestinatario,
            String asunto,
            String contenidoHtml
    ) {

        if (!StringUtils.hasText(apiKey)
                || !StringUtils.hasText(secretKey)
                || !StringUtils.hasText(senderEmail)) {

            LOGGER.warn(
                    "Correo no enviado: Mailjet todavía no está configurado."
            );

            return false;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        /*
         * Mailjet utiliza autenticación básica:
         * API Key como usuario y Secret Key como contraseña.
         */
        headers.setBasicAuth(
                apiKey,
                secretKey
        );

        Map<String, String> remitente = new HashMap<>();
        remitente.put("Email", senderEmail);
        remitente.put("Name", senderName);

        Map<String, String> receptor = new HashMap<>();
        receptor.put("Email", destinatario);
        receptor.put(
                "Name",
                nombreDestinatario != null
                        ? nombreDestinatario
                        : ""
        );

        Map<String, Object> mensaje = new HashMap<>();
        mensaje.put("From", remitente);
        mensaje.put(
                "To",
                Collections.singletonList(receptor)
        );
        mensaje.put("Subject", asunto);
        mensaje.put("HTMLPart", contenidoHtml);

        /*
         * Los correos transaccionales no necesitan seguimiento.
         * Evita que Mailjet reescriba enlaces de recuperación.
         */
        mensaje.put("TrackClicks", "disabled");
        mensaje.put("TrackOpens", "disabled");

        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put(
                "Messages",
                Collections.singletonList(mensaje)
        );

        HttpEntity<Map<String, Object>> peticion =
                new HttpEntity<>(cuerpo, headers);

        try {
            ResponseEntity<String> respuesta =
                    restTemplate.exchange(
                            MAILJET_URL,
                            HttpMethod.POST,
                            peticion,
                            String.class
                    );

            boolean enviado =
                    respuesta.getStatusCode().is2xxSuccessful();

            if (enviado) {
                LOGGER.info(
                        "Correo enviado correctamente mediante Mailjet."
                );
            }

            return enviado;

        } catch (RestClientException e) {

            /*
             * Un fallo del correo no cancela el registro
             * ni el alquiler realizado.
             */
            LOGGER.error(
                    "No se pudo enviar el correo mediante Mailjet: {}",
                    e.getMessage()
            );

            return false;
        }
    }

    public boolean enviarBienvenida(
            String email,
            String nombre
    ) {

        String nombreSeguro = escaparHtml(nombre);

        String contenidoHtml =
                "<div style=\"font-family:Arial,sans-serif;"
                        + "max-width:600px;margin:auto;"
                        + "padding:24px;color:#24352b;\">"
                        + "<h1 style=\"color:#198754;\">"
                        + "¡Bienvenido a Turismo Sustentable!"
                        + "</h1>"
                        + "<p>Hola <strong>"
                        + nombreSeguro
                        + "</strong>,</p>"
                        + "<p>Tu cuenta fue registrada correctamente.</p>"
                        + "<p>Ya podés iniciar sesión, consultar las bicicletas "
                        + "disponibles y realizar tus viajes.</p>"
                        + "<hr>"
                        + "<small>Turismo Sustentable</small>"
                        + "</div>";

        return enviarCorreo(
                email,
                nombre,
                "Bienvenido a Turismo Sustentable",
                contenidoHtml
        );
    }

    public boolean enviarRecuperacionPassword(
            String email,
            String nombre,
            String enlaceRecuperacion
    ) {

        String nombreSeguro = escaparHtml(nombre);
        String enlaceSeguro = escaparHtml(enlaceRecuperacion);

        String contenidoHtml =
                "<div style=\"font-family:Arial,sans-serif;"
                        + "max-width:600px;margin:auto;"
                        + "padding:24px;color:#24352b;\">"
                        + "<h1 style=\"color:#198754;\">"
                        + "Recuperación de contraseña"
                        + "</h1>"
                        + "<p>Hola <strong>"
                        + nombreSeguro
                        + "</strong>,</p>"
                        + "<p>Recibimos una solicitud para cambiar "
                        + "la contraseña de tu cuenta.</p>"
                        + "<p style=\"margin:30px 0;\">"
                        + "<a href=\"" + enlaceSeguro + "\" "
                        + "style=\"background:#198754;color:white;"
                        + "padding:12px 20px;text-decoration:none;"
                        + "border-radius:20px;\">"
                        + "Cambiar contraseña"
                        + "</a>"
                        + "</p>"
                        + "<p>Este enlace vence dentro de 30 minutos.</p>"
                        + "<p>Si no solicitaste el cambio, podés ignorar "
                        + "este mensaje.</p>"
                        + "<hr>"
                        + "<small>Turismo Sustentable</small>"
                        + "</div>";

        return enviarCorreo(
                email,
                nombre,
                "Recuperación de contraseña",
                contenidoHtml
        );
    }

    private String escaparHtml(String valor) {

        if (valor == null) {
            return "";
        }

        return valor
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

}
