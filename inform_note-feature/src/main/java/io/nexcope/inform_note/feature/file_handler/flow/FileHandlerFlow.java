package io.nexcope.inform_note.feature.file_handler.flow;

import io.nexcope.inform_note.domain.file.entity.AttachedFile;
import io.nexcope.inform_note.domain.file.entity.dto.AttachedFileDto;
import io.nexcope.inform_note.domain.file.entity.vo.FileRefType;
import io.nexcope.inform_note.domain.file.entity.vo.FileStatus;
import io.nexcope.inform_note.domain.file.logic.AttachedFileLogic;
import io.nexcope.inform_note.domain.log.entity.vo.AssignedTechnician;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;


@Service
@Transactional
@RequiredArgsConstructor
public class FileHandlerFlow {
    //
    private final AttachedFileLogic attachedFileLogic;

    @Value("${file.upload-dir:D:/inform-note-workspace/files}")
    private String uploadDir;

    /**
     * 임시 첨부파일 및 에디터 인라인 이미지를 디스크에 저장하고 AttachedFile 엔티티를 TEMP 상태로 등록합니다.
     *
     * @param file               업로드 파일 바이너리
     * @param refType            참조 업무 구분 (기본값: DOWN_ATTACHMENT, 에디터 이미지 시 DOWN_CONTENT_INLINE 등)
     * @param assignedTechnician 등록자 정보 (사번 매핑)
     * @param refId              연관 마스터 ID (신규 작성 시 null, 기존 수정 시 전달 가능)
     * @return 생성된 파일 고유 UUID (fileId)
     */
    public String registerTempFile(MultipartFile file, FileRefType refType,
                                   AssignedTechnician assignedTechnician, String refId) {
        // 1. 고유 파일 ID (UUID) 생성 및 파일명/확장자 추출
        String originFileName = file.getOriginalFilename();
        String extension = StringUtils.getFilenameExtension(originFileName);

        try {

            // 2. AttachedFile DTO 생성 및 도메인 레코드 등록 (TEMP 상태)
            AttachedFileDto dto = AttachedFileDto.builder()
                    .refType(refType)
                    .refId(refId)
                    .originFileName(originFileName)
                    .fileSize(file.getSize())
                    .contentType(StringUtils.hasText(file.getContentType()) ? file.getContentType() : "application/octet-stream")
                    .status(FileStatus.TEMP)
                    .createdBy(assignedTechnician.getEmpNo())
                    .createdAt(OffsetDateTime.now())
                    .build();

            AttachedFile attachedFile = attachedFileLogic.register(dto);
            String fileId = attachedFile.getFileId();
            String storedFileName = fileId + (StringUtils.hasText(extension) ? "." + extension : "");
            // 3. 실제 물리 파일 저장 디렉토리 생성 및 파일 객체 저장
            Path targetPath = Paths.get(uploadDir).resolve(storedFileName);

            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            file.transferTo(targetPath.toFile());
            // 4. 파일 업로드 완료 처리
            AttachedFile fileForUpdate = attachedFileLogic.findById(fileId);
            fileForUpdate.completeFileUpload(storedFileName, targetPath.toString());
            attachedFileLogic.modify(fileForUpdate);
            // 5. 생성된 UUID 반환
            return fileId;
        } catch (IOException e) {
            //
            throw new IllegalStateException("Failed to store upload file: " + originFileName, e);
        }
    }

}


