package io.nexcope.inform_note.facade.api.feature.file_handler.rest;

import io.nexcope.inform_note.domain.file.entity.vo.FileRefType;
import io.nexcope.inform_note.domain.log.entity.vo.AssignedTechnician;
import io.nexcope.inform_note.facade.api.feature.file_handler.command.RegisterTempFileCommand;
import io.nexcope.inform_note.feature.file_handler.domain.dto.TempFileRegisteredResponse;
import io.nexcope.inform_note.feature.file_handler.flow.FileHandlerFlow;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "FileHandler", description = "현상 및 조치 내용 의 파일 저장 API")
@RestController
@RequestMapping("/feature/file-handler")
@RequiredArgsConstructor
public class FileHandlerFlowResource {
    //
    private final FileHandlerFlow fileHandlerFlow;

    @Operation(summary = "이미지 파일 등록", description = "Rich Text 에디터에서 이미지 삽입 즉시 이미지 파일 업로드작업을 처리합니다.")
    @PostMapping("/register-temp-file/command")
    public ResponseEntity<TempFileRegisteredResponse> registerTempFile(@RequestBody RegisterTempFileCommand command) {
        //
        command.validate();
        MultipartFile file = command.getFile();
        FileRefType refType = command.getRefType();
        AssignedTechnician assignedTechnician = command.getAssignedTechnician();
        String refId = command.getRefId();
        String fileId = fileHandlerFlow.registerTempFile(file, refType, assignedTechnician, refId);
        // Record 객체로 반환
        return ResponseEntity.ok(TempFileRegisteredResponse.of(fileId));
    }

}
