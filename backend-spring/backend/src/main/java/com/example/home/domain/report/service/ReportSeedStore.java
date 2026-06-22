package com.example.home.domain.report.service;

import com.example.home.domain.report.dto.ReportDocument;
import com.example.home.global.exception.BusinessException;
import com.example.home.global.exception.docs.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.springframework.stereotype.Component;

@Component
public class ReportSeedStore {

    private final ObjectMapper objectMapper;
    private final Path reportDirectory = Path.of("seed", "reports");

    public ReportSeedStore(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void save(ReportDocument document) {
        try {
            Files.createDirectories(reportDirectory);
            Path target = filePath(document.reportId());
            Path temporary = Files.createTempFile(reportDirectory, document.reportId(), ".tmp");
            objectMapper.writeValue(temporary.toFile(), document);
            moveAtomically(temporary, target);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "리포트 seed 파일 저장에 실패했습니다.");
        }
    }

    public ReportDocument get(String reportId) {
        try {
            Path file = filePath(reportId);
            if (!Files.exists(file)) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "리포트를 찾을 수 없습니다.");
            }
            return objectMapper.readValue(file.toFile(), ReportDocument.class);
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "리포트 seed 파일을 읽지 못했습니다.");
        }
    }

    private Path filePath(String reportId) {
        if (!reportId.matches("[a-f0-9-]{36}")) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "유효하지 않은 리포트 ID입니다.");
        }
        return reportDirectory.resolve("report-" + reportId + ".json");
    }

    private void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
