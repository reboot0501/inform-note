package io.nexcope.inform_note.domain.auth.logic;

import io.nexcope.inform_note.domain.auth.entity.Signup;
import io.nexcope.inform_note.domain.auth.entity.dto.SignupDto;
import io.nexcope.inform_note.domain.auth.entity.vo.SignupStatus;
import io.nexcope.inform_note.domain.auth.mapper.SignupMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class SignupLogic {
    //
    private final SignupMapper mapper;

    public Signup findById(String signupId) {
        //
        return mapper.findById(signupId).orElse(null);
    }

    public boolean existsByEmail(String email) {
        //
        return mapper.existsByEmail(email);
    }

    public List<Signup> findBySignupStatus(SignupStatus signupStatus) {
        //
        return mapper.findBySignupStatus(signupStatus);
    }

    public String registerSignup(SignupDto dto) {
        //
        Signup domain = new Signup(dto);
        mapper.insert(domain);
        return domain.getEmail();
    }

    public String modifySignup(Signup domain) {
        //
        mapper.update(domain);
        return domain.getEmail();
    }

    public void deleteById(String signupId) {
        //
        mapper.deleteById(signupId);
    }
}
