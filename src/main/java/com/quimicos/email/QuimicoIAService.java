package com.quimicos.email;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@AllArgsConstructor
public class QuimicoIAService {

    private final AnthropicChatModel chatModel;
    private final EmailService emailService;
    private final ConcurrentHashMap<String, Long> requestTimestamps = new ConcurrentHashMap<>();

    public ResponseEntity<ChatResponse> enviarSolicitudAntrophic(String nombreQuimico) {
        String colorMeaning = """
                    Verde: Pueden almacenarse juntos. Verificar reactividad individual utilizando la MSDS.
                    Amarillo: Precaución, posibles restricciones. Revisar incompatibilidades individuales utilizando la MSDS, pueden ser incompatibles o pueden requerirse condiciones específicas.
                    Rojo: Se requiere almacenar por separado. Son incompatibles.
                """;
        long currentTime = System.currentTimeMillis();
        long oneMinuteInMillis = 60 * 1000;

        // Verificar si ya se hizo una solicitud en el último minuto
        Long lastRequestTime = requestTimestamps.get(nombreQuimico);
        if (lastRequestTime != null && (currentTime - lastRequestTime) < oneMinuteInMillis) {
            return ResponseEntity.status(429).body(null); // HTTP 429 Too Many Requests
        }

        // Actualizar el tiempo de la última solicitud
        requestTimestamps.put(nombreQuimico, currentTime);

        // Obtener la información del químico desde el archivo JSON
        String contexto = obtenerDataQuimico(nombreQuimico);
        if (contexto == null) {
            return ResponseEntity.status(404).body(null);
        }

        // Crear el mensaje para el modelo de IA
        ChatResponse response;
        try {
            response = chatModel.call(
                    new Prompt(
                            "Eres un experto en química. Acabo de tener un posible riesgo con este tipo de quimico: " + nombreQuimico +
                                    ". segun la base de datos que tengo tiene esta compativilidad con estos quimicos: " + contexto +
                                    ". Redacta una breve respuesta sobre los posibles riesgos en formato de cuerpo de email." +
                                    colorMeaning + ". Hazlo para enviar directamente"
                    )
            );

            log.info("Respuesta para quimico {}: {}", nombreQuimico, response.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Enviar la respuesta al cliente por email
        emailService.enviarEmail(response.getResult().getOutput().getText());

        return ResponseEntity.status(200).body(response);
    }

    private String obtenerDataQuimico(String nombreQuimico) {
        JsonNode quimicoData;
        try {
            // Load and parse the JSON file
            ObjectMapper objectMapper = new ObjectMapper();
            File file = new File("src/main/resources/QuimicosName.json");
            JsonNode rootNode = objectMapper.readTree(file);

            // Search for the chemical name in the JSON structure
            quimicoData = rootNode.get(nombreQuimico);

            if (quimicoData == null) {
                return null;
            }

        } catch (IOException e) {
            log.error("Error while reading the JSON file", e);
            return null;
        }
        return quimicoData.toString();
    }
}
