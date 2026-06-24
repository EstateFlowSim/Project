package com.example.home.domain.scenario.service;

import com.example.home.domain.scenario.dto.ScenarioDocument;
import com.example.home.global.exception.BusinessException;
import com.example.home.global.exception.docs.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ScenarioSeedStore {

    private final ObjectMapper objectMapper;
    private final Path scenarioDirectory;

    public ScenarioSeedStore(
            ObjectMapper objectMapper,
            @Value("${scenario.storage.directory}") String scenarioDirectory) {
        this.objectMapper = objectMapper;
        this.scenarioDirectory = Path.of(scenarioDirectory);
    }

    public void save(ScenarioDocument document) {
        try {
            Files.createDirectories(scenarioDirectory);
            Path target = filePath(document.scenarioId());
            Path temporary = Files.createTempFile(scenarioDirectory, document.scenarioId(), ".tmp");
            objectMapper.writeValue(temporary.toFile(), document);
            moveAtomically(temporary, target);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "시나리오 seed 파일 저장에 실패했습니다.");
        }
    }

    public ScenarioDocument get(String scenarioId) {
        try {
            Path file = filePath(scenarioId);
            if (!Files.exists(file)) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "시나리오를 찾을 수 없습니다.");
            }
            return objectMapper.readValue(file.toFile(), ScenarioDocument.class);
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "시나리오 seed 파일을 읽지 못했습니다.");
        }
    }

    private Path filePath(String scenarioId) {
        if (!isCanonicalUuid(scenarioId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "유효하지 않은 시나리오 ID입니다.");
        }
        return scenarioDirectory.resolve("scenario-" + scenarioId + ".json");
    }

    private boolean isCanonicalUuid(String value) {
        try {
            return UUID.fromString(value).toString().equals(value);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
