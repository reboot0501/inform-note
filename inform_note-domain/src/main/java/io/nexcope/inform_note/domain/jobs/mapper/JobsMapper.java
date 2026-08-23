package io.nexcope.inform_note.domain.jobs.mapper;

import io.nexcope.inform_note.domain.jobs.entity.Jobs;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface JobsMapper {

    // 단건 조회 (PK 기준)
    Optional<Jobs> findById(@Param("jobId") String jobId);

    // 전체 목록 조회
    List<Jobs> findAll();

    // 등록 (Insert)
    int insert(Jobs jobs);

    // 수정 (Update)
    int update(Jobs jobs);

    // 등록 또는 수정 (Merge / Upsert)
    int saveOrUpdate(Jobs jobs);

    // 삭제 (Delete)
    int deleteById(@Param("jobId") String jobId);
}
