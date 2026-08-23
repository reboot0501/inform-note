package io.nexcope.inform_note.domain.content.mapper;

import io.nexcope.inform_note.domain.content.entity.DownContent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface DownContentMapper {
    // 상세 본문 단건 조회
    Optional<DownContent> findById(@Param("downEventId") String downEventId);
    // 상세 본문 다건 조회 (IN 조건)
    List<DownContent> findByIds(@Param("downEventIds") List<String> downEventIds);
    // 상세 본문 등록 (Insert)
    int insert(DownContent downContent);
    // 상세 본문 수정 (Update)
    int update(DownContent downContent);
    // 상세 본문 등록 또는 수정 (Merge / Upsert)
    int saveOrUpdate(DownContent downContent);
    // 상세 본문 삭제 (Delete)
    int deleteById(@Param("downEventId") String downEventId);
    
}
