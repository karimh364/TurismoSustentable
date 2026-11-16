# Material para la entrega de Prácticas Profesionalizantes III

Karim Homsi. CENT 35. 16 de noviembre de 2026.

## Archivos principales

- **Turismo_Sustentable_Parcial_2026_v2.pptx**: versión editable final, 16 diapositivas con notas del expositor. Presentar normalmente de la 1 a la 13; las 14 a 16 sirven como anexos para preguntas.
- **Turismo_Sustentable_Parcial_2026.pdf**: respaldo visual de las diapositivas si el equipo no abre PowerPoint.
- **Informe_Turismo_Sustentable_Parcial_2026.docx**: informe editable de 13 páginas con objetivos, alcance, 13 requerimientos funcionales, calidad, reglas, diagramas y dos casos de uso completos.
- **Informe_Turismo_Sustentable_Parcial_2026.pdf**: versión para leer o imprimir. CU 01 ocupa las páginas 7 y 8; CU 02, las páginas 9 y 10.
- **DER_v5_completo.pdf**: modelo lógico completo, vectorial y ampliable. Abrirlo por bloques con zoom; no intentar explicar sus 29 entidades en una sola vista de proyector.
- **DER_v5_completo.svg** y **DER_v5_fuente.mmd**: diagrama y fuente editable. Las 55 relaciones provienen del Mermaid vigente, no de una ingeniería inversa del legado físico.
- **Casos_de_uso_diagrama.svg**: diagrama de actores principales de las dos fichas, también incorporado al informe.
- **Guion_Exposicion_y_Demostracion.md**: orden sugerido para treinta minutos, notas por diapositiva, recorrido del front y respuestas breves para el debate.
- **Consultas_Demostracion_SOLO_LECTURA.sql**: consultas para seguir las operaciones en la base. No contiene escrituras de datos ni selección de contraseñas/tokens.

## Correspondencia con las consignas

| Consigna | Material |
| --- | --- |
| Presentación PowerPoint | PPTX final y PDF de respaldo |
| Requerimientos, alcance y diagramas | Informe, páginas 1 a 6 |
| Dos casos de uso | Informe, páginas 7 a 10 |
| Frontend funcionando | Proyecto existente: src/main/resources/static |
| Backend funcionando | Proyecto existente: src/main/java/DSII/TurismoSustentable y configuración Gradle |
| Diagrama Entidad Relación | DER completo en PDF/SVG y fuente Mermaid |

El ZIP reúne el material de exposición. **No reemplaza al proyecto ejecutable ni contiene un respaldo de la base.** Llevar también el proyecto y el SQL privado descargado, fuera de Git.

## Orden para la exposición

1. Frontend durante unos 8 minutos.
2. Diapositivas principales durante unos 9 minutos.
3. Base de datos durante unos 5 minutos.
4. Preguntas y debate durante el tiempo restante.

Evitar repetir todas las operaciones de mantenimiento si el profesor quiere discutir el modelo: la orden reparada y sus historias permiten mostrar lo ocurrido.

## Antes de presentar

- Mantener un respaldo SQL fuera del repositorio y comprobar que MariaDB esté iniciada.
- Ejecutar desde la raíz del proyecto:

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=refactor --spring.jpa.hibernate.ddl-auto=validate"
```

- **No usar el arranque anterior con create-drop ni ejecutar integraciones MySQL destructivas sobre esta demostración.** validate exige un esquema existente compatible, no restaura una copia.
- Iniciar sesión de nuevo después de cada reinicio. Usar qa.circuito con CLIENTE+ADMIN y qa.tecnico con solo TECNICO; no se incluyen las contraseñas.
- Comprobar el navegador, los recursos CDN y la consulta de cotización antes de la exposición.
- El pago usa saldo ficticio. QR/candado real y nube siguen previstos, no demostrados.
- Confirmar con la principal la comprobación real de las auditorías nuevas. La documentación incorpora su implementación y los 155 casos dirigidos aprobados, sin declarar que equivalgan a rollback SQL real o a seguridad transversal completa.

## Fuentes y límites

Se utilizaron el PDF original de alcance del 25/06/2026, la presentación histórica de la muestra, el diccionario v5, el DER Mermaid y los reportes de implementación hasta etapa 339. La presentación histórica no se tomó como estado actual del backend. La imagen de portada se conserva como imagen conceptual.

La documentación diferencia objetivos y funciones previstas de lo implementado. No declara el DER 100% migrado, pagos externos reales ni un despliegue de producción. Esta preparación solo crea archivos nuevos en esta carpeta; no modifica el trabajo de auditoría, Gradle, código ni datos de la principal.
