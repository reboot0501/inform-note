package io.nexcope.inform_note.domain.file.mapper;

import io.nexcope.inform_note.domain.file.entity.AttachedFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Mapper
public interface AttachedFileMapper {

    // 1. 단건 조회 (fileId 기준)
    Optional<AttachedFile> findById(@Param("fileId") String fileId);

    // 2. 단건 조회 (저장 파일명 storedFileName 기준)
    Optional<AttachedFile> findByStoredFileName(@Param("storedFileName") String storedFileName);

    // 3. HTML 등에서 추출된 파일 ID 다건 목록으로 파일 조회 (IN 조건) ★
    List<AttachedFile> findByIds(@Param("fileIds") List<String> fileIds);

    // 4. HTML 등에서 추출된 저장 파일명(storedFileName) 다건 목록으로 파일 조회 (IN 조건) ★
    List<AttachedFile> findByStoredFileNames(@Param("storedFileNames") List<String> storedFileNames);

    // 5. 연관 도메인 기준 파일 목록 조회 (예: DOWN_CONTENT_INLINE, down_event_id)
    List<AttachedFile> findByRef(@Param("refType") String refType, @Param("refId") String refId);

    // 6. 만료된 임시 파일 목록 조회 (가비지 컬렉션용)
    List<AttachedFile> findExpiredTempFiles(@Param("expiredBefore") OffsetDateTime expiredBefore);

    // 7. 등록 (Insert)
    int insert(AttachedFile attachedFile);

    // 8. 수정 (Update)
    int update(AttachedFile attachedFile);

    // 9. 상태 변경 (TEMP -> SAVED, DELETED 등)
    int updateStatus(@Param("fileId") String fileId,
                     @Param("status") String status,
                     @Param("updatedBy") String updatedBy);

    // 10. 파일 목록 일괄 저장 및 연관 ID 매핑
    int saveFiles(@Param("fileIds") Set<String> fileIds,
                  @Param("refId") String refId,
                  @Param("updatedBy") String updatedBy);

    // 11. 파일 목록 일괄 확정 및 연관 ID 매핑
    int confirmFiles(@Param("fileIds") List<String> fileIds,
                     @Param("refType") String refType,
                     @Param("refId") String refId,
                     @Param("updatedBy") String updatedBy);


    // 11. 단건 물리 삭제 (Delete)
    int deleteById(@Param("fileId") String fileId);

    // 12. 다건 물리 삭제 (Delete IN)
    int deleteByIds(@Param("fileIds") List<String> fileIds);
}
