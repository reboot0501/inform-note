package io.nexcope.inform_note.feature.file_handler.flow;

import io.nexcope.inform_note.domain.file.entity.AttachedFile;
import io.nexcope.inform_note.domain.file.logic.AttachedFileLogic;
import io.nexcope.inform_note.feature.file_handler.domain.dto.FileViewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FileHandlerFetch {
    //
    private final AttachedFileLogic attachedFileLogic;

    /**
     * fileId에 해당하는 AttachedFile 메타데이터를 조회하고 물리 파일 Resource를 로드하여 반환합니다.
     *
     * @param fileId 파일 고유 ID (UUID)
     * @return FileViewResource (물리 Resource 및 AttachedFile 엔티티), 미존재 시 null
     */
    public FileViewResponse loadViewResource(String fileId) {
        // 1. DB에서 파일 메타데이터 조회
        AttachedFile file = attachedFileLogic.findById(fileId);
        if (file == null) {
            return null;
        }

        // 2. 물리 파일 리소스 로드 및 존재 검증
        Resource resource = new FileSystemResource(file.getFilePath());
        if (!resource.exists()) {
            return null;
        }

        return new FileViewResponse(resource, file);
    }

    
}

