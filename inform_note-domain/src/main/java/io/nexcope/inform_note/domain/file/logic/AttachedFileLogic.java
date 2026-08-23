package io.nexcope.inform_note.domain.file.logic;

import io.nexcope.inform_note.domain.file.entity.AttachedFile;
import io.nexcope.inform_note.domain.file.entity.dto.AttachedFileDto;
import io.nexcope.inform_note.domain.file.entity.vo.FileRefType;
import io.nexcope.inform_note.domain.file.entity.vo.FileStatus;
import io.nexcope.inform_note.domain.file.mapper.AttachedFileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
@RequiredArgsConstructor
public class AttachedFileLogic {
    //
    private final AttachedFileMapper mapper;

    // =================================================================
    // [3. 기본 조회 및 CRUD 비즈니스 메서드]
    // =================================================================

    @Transactional(readOnly = true)
    public AttachedFile findById(String fileId) {
        return mapper.findById(fileId).orElse(null);
    }

    @Transactional(readOnly = true)
    public AttachedFile findByStoredFileName(String storedFileName) {
        return mapper.findByStoredFileName(storedFileName).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<AttachedFile> findByIds(List<String> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return Collections.emptyList();
        }
        return mapper.findByIds(fileIds);
    }

    @Transactional(readOnly = true)
    public List<AttachedFile> findByRef(FileRefType refType, String refId) {
        return mapper.findByRef(refType.name(), refId);
    }

    @Transactional(readOnly = true)
    public List<AttachedFile> findByRef(String refType, String refId) {
        return mapper.findByRef(refType, refId);
    }

    @Transactional(readOnly = true)
    public List<AttachedFile> findExpiredTempFiles(OffsetDateTime expiredBefore) {
        return mapper.findExpiredTempFiles(expiredBefore);
    }

    public AttachedFile register(AttachedFileDto dto) {
        AttachedFile domain = new AttachedFile(dto);
        mapper.insert(domain);
        return domain;
    }

    public void modify(AttachedFile domain) {
        mapper.update(domain);
    }

    public int updateStatus(String fileId, FileStatus status, String modifierId) {
        return mapper.updateStatus(fileId, status.name(), modifierId);
    }

    public int saveFiles(Set<String> fileIds, String refId, String modifierId) {
        //
        if (fileIds == null || fileIds.isEmpty()) {
            return 0;
        }
        return mapper.saveFiles(fileIds, refId, modifierId);
    }

    public int deleteById(String fileId) {
        return mapper.deleteById(fileId);
    }

    public int deleteByIds(List<String> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return 0;
        }
        return mapper.deleteByIds(fileIds);
    }
}
