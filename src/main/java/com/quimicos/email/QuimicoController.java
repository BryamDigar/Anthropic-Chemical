package com.quimicos.email;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/quimicos")
@RequiredArgsConstructor
public class QuimicoController {

    private final QuimicoIAService quimicoIAService;

    @GetMapping("/{nombreQuimico}")
    public ResponseEntity<ChatResponse> obtenerDataQuimico(@PathVariable String nombreQuimico) {
        return quimicoIAService.enviarSolicitudAntrophic(nombreQuimico);
    }
}
