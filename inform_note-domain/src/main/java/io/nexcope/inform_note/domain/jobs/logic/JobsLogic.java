package io.nexcope.inform_note.domain.jobs.logic;

import io.nexcope.inform_note.domain.jobs.entity.Jobs;
import io.nexcope.inform_note.domain.jobs.mapper.JobsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class JobsLogic {

    private final JobsMapper mapper;

    @Transactional(readOnly = true)
    public Jobs findById(String jobId) {
        //
        return mapper.findById(jobId).orElseGet(null);
    }

    @Transactional(readOnly = true)
    public List<Jobs> findAll() {
        return mapper.findAll();
    }

    public Jobs register(Jobs jobs) {
        mapper.insert(jobs);
        return jobs;
    }

    public int modify(Jobs jobs) {
        return mapper.update(jobs);
    }

    public int saveOrUpdate(Jobs jobs) {
        return mapper.saveOrUpdate(jobs);
    }

    public int deleteById(String jobId) {
        return mapper.deleteById(jobId);
    }
}
