package io.nexcope.inform_note.domain.log.mapper;

import io.nexcope.inform_note.domain.log.entity.DownEventLog;
import io.nexcope.inform_note.domain.log.entity.vo.DownEventLogSearchCriteria;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface DownEventLogMapper {
    // 단건 조회
    Optional<DownEventLog> findById(@Param("downEventId") String downEventId);
    // 전체 목록 조회
    List<DownEventLog> findAll();
    // 조건별 필터링 및 페이징 목록 조회
    List<DownEventLog> findByCriteria(DownEventLogSearchCriteria criteria);
    // 조건별 전체 건수 조회 (페이징용 Count)
    long countByCriteria(DownEventLogSearchCriteria criteria);
    // 등록 (Insert)
    int insert(DownEventLog downEventLog);
    // 수정 (Update)
    int update(DownEventLog downEventLog);
    // 삭제 (Delete)
    int deleteById(@Param("downEventId") String downEventId);
}
